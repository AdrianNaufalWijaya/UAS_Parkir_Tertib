package com.example.parkirtertib.service

import android.util.Log
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.ceil

class ParkirService {

    private val firestore = FirebaseFirestore.getInstance()

    companion object {
        private const val TAG = "ParkirService"
        private const val COLLECTION_PARKIR_MASUK = "parkir_masuk"
        private const val COLLECTION_RIWAYAT_PARKIR = "riwayat_parkir"

        // Tarif parkir per jam (dalam Rupiah)
        private const val TARIF_MOBIL_PER_JAM = 5000
        private const val TARIF_MOTOR_PER_JAM = 3000
        private const val TARIF_MINIMUM_JAM = 1 // Minimal charge 1 jam
    }

    /**
     * Interface untuk callback hasil proses
     */
    interface ParkirCallback {
        fun onSuccess(message: String, biaya: Int = 0, durasi: String = "")
        fun onError(message: String)
        fun onLoading(isLoading: Boolean)
    }

    /**
     * Data class untuk hasil perhitungan parkir
     */
    data class HasilParkir(
        val biaya: Int,
        val durasi: String,
        val durasiJam: Int,
        val waktuMasuk: Timestamp,
        val waktuKeluar: Timestamp
    )

    /**
     * Proses kendaraan masuk parkir
     * Menyimpan data ke collection parkir_masuk dengan status "aktif"
     */
    suspend fun prosesMasukParkir(
        platNomor: String,
        jenisKendaraan: String,
        idUser: String,
        callback: ParkirCallback
    ) {
        try {
            callback.onLoading(true)
            Log.d(TAG, "Memproses masuk parkir untuk plat: $platNomor")

            // 1. Cek apakah kendaraan sudah parkir
            val isAlreadyParked = cekKendaraanSudahParkir(platNomor, idUser)
            if (isAlreadyParked) {
                callback.onLoading(false)
                callback.onError("Kendaraan dengan plat $platNomor sudah terdaftar parkir!")
                return
            }

            // 2. Buat data parkir masuk
            val waktuMasuk = Timestamp.now()
            val dataParkir = hashMapOf(
                "idUser" to idUser,
                "platNomor" to platNomor.uppercase().trim(),
                "jenisKendaraan" to jenisKendaraan,
                "waktuMasuk" to waktuMasuk,
                "status" to "aktif",
                "createdAt" to waktuMasuk
            )

            // 3. Simpan ke Firestore
            firestore.collection(COLLECTION_PARKIR_MASUK)
                .add(dataParkir)
                .await()

            callback.onLoading(false)
            callback.onSuccess(
                "Kendaraan $platNomor berhasil masuk parkir pada ${formatWaktu(waktuMasuk)}"
            )

            Log.d(TAG, "Berhasil menyimpan data parkir masuk untuk: $platNomor")

        } catch (e: Exception) {
            callback.onLoading(false)
            Log.e(TAG, "Error dalam proses masuk parkir", e)
            callback.onError("Gagal memproses masuk parkir: ${e.localizedMessage}")
        }
    }

    /**
     * Proses kendaraan keluar parkir
     * 1. Cari data di collection parkir_masuk berdasarkan plat nomor
     * 2. Hitung biaya parkir berdasarkan durasi
     * 3. Pindahkan data ke collection riwayat_parkir dengan status "selesai"
     * 4. Hapus data dari collection parkir_masuk
     */
    suspend fun prosesKeluarParkir(
        platNomor: String,
        idUser: String,
        callback: ParkirCallback
    ) {
        try {
            callback.onLoading(true)
            Log.d(TAG, "Memproses keluar parkir untuk plat: $platNomor")

            // 1. Cari data parkir masuk
            val querySnapshot = firestore.collection(COLLECTION_PARKIR_MASUK)
                .whereEqualTo("platNomor", platNomor.uppercase().trim())
                .whereEqualTo("idUser", idUser)
                .get()
                .await()

            if (querySnapshot.isEmpty) {
                callback.onLoading(false)
                callback.onError("Data parkir tidak ditemukan untuk plat nomor: $platNomor")
                return
            }

            val dokumenParkir = querySnapshot.documents.first()
            val dataParkir = dokumenParkir.data ?: run {
                callback.onLoading(false)
                callback.onError("Data parkir kosong atau rusak")
                return
            }

            Log.d(TAG, "Data parkir ditemukan: $dataParkir")

            // 2. Ambil data yang diperlukan
            val waktuMasuk = dataParkir["waktuMasuk"] as? Timestamp ?: run {
                callback.onLoading(false)
                callback.onError("Waktu masuk tidak valid")
                return
            }

            val jenisKendaraan = dataParkir["jenisKendaraan"] as? String ?: "Mobil"
            val waktuKeluar = Timestamp.now()

            // 3. Hitung durasi dan biaya
            val hasilParkir = hitungBiayaParkir(waktuMasuk, waktuKeluar, jenisKendaraan)

            Log.d(TAG, "Hasil perhitungan - Durasi: ${hasilParkir.durasi}, Biaya: ${hasilParkir.biaya}")

            // 4. Buat data untuk riwayat parkir
            val dataRiwayat = hashMapOf(
                "idUser" to idUser,
                "platNomor" to platNomor.uppercase().trim(),
                "jenisKendaraan" to jenisKendaraan,
                "waktuMasuk" to waktuMasuk,
                "waktuKeluar" to waktuKeluar,
                "durasi" to hasilParkir.durasi,
                "biaya" to hasilParkir.biaya,
                "status" to "selesai",
                "createdAt" to waktuKeluar,
                "updatedAt" to waktuKeluar
            )

            // 5. Simpan ke collection riwayat_parkir
            firestore.collection(COLLECTION_RIWAYAT_PARKIR)
                .add(dataRiwayat)
                .await()

            Log.d(TAG, "Data berhasil disimpan ke riwayat_parkir")

            // 6. Hapus dari collection parkir_masuk
            dokumenParkir.reference.delete().await()

            Log.d(TAG, "Data berhasil dihapus dari parkir_masuk")

            // 7. Callback success
            callback.onLoading(false)
            callback.onSuccess(
                "Parkir selesai!\nDurasi: ${hasilParkir.durasi}\nBiaya: Rp ${formatRupiah(hasilParkir.biaya)}",
                hasilParkir.biaya,
                hasilParkir.durasi
            )

        } catch (e: Exception) {
            callback.onLoading(false)
            Log.e(TAG, "Error dalam proses keluar parkir", e)
            callback.onError("Gagal memproses keluar parkir: ${e.localizedMessage}")
        }
    }

    /**
     * Cek apakah kendaraan dengan plat nomor tertentu sedang parkir
     */
    suspend fun cekStatusParkir(
        platNomor: String,
        idUser: String
    ): Pair<Boolean, Timestamp?> {
        return try {
            val querySnapshot = firestore.collection(COLLECTION_PARKIR_MASUK)
                .whereEqualTo("platNomor", platNomor.uppercase().trim())
                .whereEqualTo("idUser", idUser)
                .get()
                .await()

            if (querySnapshot.isEmpty) {
                Pair(false, null)
            } else {
                val dokumen = querySnapshot.documents.first()
                val waktuMasuk = dokumen.data?.get("waktuMasuk") as? Timestamp
                Pair(true, waktuMasuk)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error cek status parkir", e)
            Pair(false, null)
        }
    }

    /**
     * Cek apakah kendaraan sudah terdaftar parkir (untuk mencegah duplikat)
     */
    private suspend fun cekKendaraanSudahParkir(platNomor: String, idUser: String): Boolean {
        return try {
            val querySnapshot = firestore.collection(COLLECTION_PARKIR_MASUK)
                .whereEqualTo("platNomor", platNomor.uppercase().trim())
                .whereEqualTo("idUser", idUser)
                .get()
                .await()

            !querySnapshot.isEmpty
        } catch (e: Exception) {
            Log.e(TAG, "Error cek kendaraan sudah parkir", e)
            false
        }
    }

    /**
     * Hitung biaya parkir berdasarkan durasi dan jenis kendaraan
     */
    private fun hitungBiayaParkir(
        waktuMasuk: Timestamp,
        waktuKeluar: Timestamp,
        jenisKendaraan: String
    ): HasilParkir {
        // Hitung durasi dalam milidetik
        val durasiMillis = waktuKeluar.toDate().time - waktuMasuk.toDate().time

        // Konversi ke jam (dengan pembulatan ke atas)
        val durasiJamDouble = durasiMillis / (1000.0 * 60 * 60)
        val durasiJam = ceil(durasiJamDouble).toInt().coerceAtLeast(TARIF_MINIMUM_JAM)

        // Tentukan tarif berdasarkan jenis kendaraan
        val tarifPerJam = when (jenisKendaraan.lowercase()) {
            "motor", "sepeda motor", "motorcycle" -> TARIF_MOTOR_PER_JAM
            else -> TARIF_MOBIL_PER_JAM
        }

        // Hitung total biaya
        val totalBiaya = durasiJam * tarifPerJam

        // Format durasi untuk display
        val hours = durasiMillis / (1000 * 60 * 60)
        val minutes = (durasiMillis % (1000 * 60 * 60)) / (1000 * 60)
        val durasiString = "${hours}j ${minutes}m"

        return HasilParkir(
            biaya = totalBiaya,
            durasi = durasiString,
            durasiJam = durasiJam,
            waktuMasuk = waktuMasuk,
            waktuKeluar = waktuKeluar
        )
    }

    /**
     * Sinkronisasi data parkir (untuk membersihkan data yang tidak konsisten)
     */
    suspend fun sinkronisasiData(idUser: String, callback: ParkirCallback) {
        try {
            callback.onLoading(true)
            Log.d(TAG, "Memulai sinkronisasi data untuk user: $idUser")

            // Ambil semua data dari parkir_masuk
            val parkirMasukSnapshot = firestore.collection(COLLECTION_PARKIR_MASUK)
                .whereEqualTo("idUser", idUser)
                .get()
                .await()

            // Ambil semua data dari riwayat_parkir
            val riwayatSnapshot = firestore.collection(COLLECTION_RIWAYAT_PARKIR)
                .whereEqualTo("idUser", idUser)
                .get()
                .await()

            // Buat set plat nomor yang sudah ada di riwayat
            val riwayatPlatNumbers = riwayatSnapshot.documents.mapNotNull { doc ->
                doc.data?.get("platNomor") as? String
            }.toSet()

            var jumlahDibersihkan = 0

            // Cari data di parkir_masuk yang mungkin sudah selesai tapi belum dipindah
            for (dokumen in parkirMasukSnapshot.documents) {
                val platNomor = dokumen.data?.get("platNomor") as? String
                val waktuMasuk = dokumen.data?.get("waktuMasuk") as? Timestamp

                if (platNomor != null && waktuMasuk != null) {
                    // Cek apakah data ini sudah lama (lebih dari 24 jam) dan belum keluar
                    val currentTime = System.currentTimeMillis()
                    val masukTime = waktuMasuk.toDate().time
                    val diffHours = (currentTime - masukTime) / (1000 * 60 * 60)

                    if (diffHours > 24) {
                        Log.d(TAG, "Ditemukan data parkir lama (${diffHours}h): $platNomor")
                        // Bisa ditambahkan logika untuk handle data lama
                    }
                }
            }

            callback.onLoading(false)
            callback.onSuccess("Sinkronisasi selesai. $jumlahDibersihkan data dibersihkan.")

            Log.d(TAG, "Sinkronisasi selesai. Data dibersihkan: $jumlahDibersihkan")

        } catch (e: Exception) {
            callback.onLoading(false)
            Log.e(TAG, "Error dalam sinkronisasi", e)
            callback.onError("Gagal melakukan sinkronisasi: ${e.localizedMessage}")
        }
    }

    /**
     * Dapatkan statistik parkir untuk user
     */
    suspend fun getStatistikParkir(idUser: String): Map<String, Any>? {
        return try {
            val riwayatSnapshot = firestore.collection(COLLECTION_RIWAYAT_PARKIR)
                .whereEqualTo("idUser", idUser)
                .get()
                .await()

            val parkirMasukSnapshot = firestore.collection(COLLECTION_PARKIR_MASUK)
                .whereEqualTo("idUser", idUser)
                .get()
                .await()

            val totalSelesai = riwayatSnapshot.documents.size
            val totalAktif = parkirMasukSnapshot.documents.size
            val totalBiaya = riwayatSnapshot.documents.sumOf { doc ->
                (doc.data?.get("biaya") as? Number)?.toInt() ?: 0
            }

            mapOf(
                "total" to (totalSelesai + totalAktif),
                "aktif" to totalAktif,
                "selesai" to totalSelesai,
                "totalBiaya" to totalBiaya
            )

        } catch (e: Exception) {
            Log.e(TAG, "Error mendapatkan statistik", e)
            null
        }
    }

    // Utility functions
    private fun formatWaktu(timestamp: Timestamp): String {
        val format = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale("id", "ID"))
        return format.format(timestamp.toDate())
    }

    private fun formatRupiah(amount: Int): String {
        return when {
            amount >= 1000000 -> "${String.format("%.1f", amount / 1000000.0)}M"
            amount >= 1000 -> "${String.format("%.0f", amount / 1000.0)}K"
            else -> String.format("%,d", amount)
        }
    }
}