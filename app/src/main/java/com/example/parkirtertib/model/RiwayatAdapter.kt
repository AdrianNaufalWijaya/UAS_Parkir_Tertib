package com.example.parkirtertib

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.parkirtertib.databinding.ItemRiwayatBinding
import com.example.parkirtertib.model.RiwayatParkir
import android.util.Log
import java.text.NumberFormat
import java.util.*

class RiwayatAdapter(
    private val listRiwayat: MutableList<RiwayatParkir>
) : RecyclerView.Adapter<RiwayatAdapter.RiwayatViewHolder>() {

    inner class RiwayatViewHolder(private val binding: ItemRiwayatBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(riwayat: RiwayatParkir) {
            binding.apply {
                // Basic info
                tvTanggal.text = if (riwayat.tanggal.isNotEmpty()) riwayat.tanggal else "N/A"
                tvPlatNomor.text = if (riwayat.platNomor.isNotEmpty()) riwayat.platNomor else "N/A"
                tvJamMasuk.text = if (riwayat.jamMasuk.isNotEmpty()) riwayat.jamMasuk else "N/A"
                tvJamKeluar.text = riwayat.jamKeluar ?: "-"

                // DEBUG: Log untuk memastikan status yang diterima
                Log.d("RiwayatAdapter", "Binding data - Plat: ${riwayat.platNomor}, Status: '${riwayat.status}', Durasi: '${riwayat.durasi}'")

                val context = binding.root.context

                // PASTIKAN MENAMPILKAN STATUS, BUKAN DURASI
                when (riwayat.status.lowercase().trim()) {
                    "aktif" -> {
                        tvStatus.text = "Aktif"
                        tvStatus.setTextColor(ContextCompat.getColor(context, android.R.color.holo_orange_light))

                        // Styling untuk data aktif
                        tvTanggal.setTextColor(ContextCompat.getColor(context, android.R.color.white))
                        tvPlatNomor.setTextColor(ContextCompat.getColor(context, android.R.color.white))
                        tvJamMasuk.setTextColor(ContextCompat.getColor(context, android.R.color.white))
                        tvJamKeluar.setTextColor(ContextCompat.getColor(context, android.R.color.darker_gray))
                    }
                    "selesai" -> {
                        tvStatus.text = "Selesai"
                        tvStatus.setTextColor(ContextCompat.getColor(context, android.R.color.holo_green_light))

                        // Styling untuk data selesai
                        tvTanggal.setTextColor(ContextCompat.getColor(context, android.R.color.white))
                        tvPlatNomor.setTextColor(ContextCompat.getColor(context, android.R.color.white))
                        tvJamMasuk.setTextColor(ContextCompat.getColor(context, android.R.color.white))
                        tvJamKeluar.setTextColor(ContextCompat.getColor(context, android.R.color.holo_green_light))
                    }
                    else -> {
                        // Jika status tidak dikenali, tampilkan apa adanya tapi dengan log
                        Log.w("RiwayatAdapter", "Status tidak dikenali: '${riwayat.status}' untuk plat ${riwayat.platNomor}")
                        tvStatus.text = "Unknown"
                        tvStatus.setTextColor(ContextCompat.getColor(context, android.R.color.darker_gray))

                        tvTanggal.setTextColor(ContextCompat.getColor(context, android.R.color.darker_gray))
                        tvPlatNomor.setTextColor(ContextCompat.getColor(context, android.R.color.darker_gray))
                        tvJamMasuk.setTextColor(ContextCompat.getColor(context, android.R.color.darker_gray))
                        tvJamKeluar.setTextColor(ContextCompat.getColor(context, android.R.color.darker_gray))
                    }
                }

                // JIKA ADA FIELD DURASI TERPISAH, TAMPILKAN DURASI DI SINI
                // Cek apakah ada TextView untuk durasi di layout
                try {
                    // Jika ada tvDurasi di layout, tampilkan durasi di sana
                    // binding.tvDurasi?.text = calculateDuration(riwayat)

                    // ALTERNATIF: Jika ternyata tvStatus yang menampilkan durasi,
                    // mungkin ada kesalahan mapping di XML. Pastikan ID yang benar.
                } catch (e: Exception) {
                    Log.d("RiwayatAdapter", "Field durasi tidak ada di layout")
                }

                // Format biaya
                val formattedBiaya = formatBiaya(riwayat.biaya)
                tvBiaya.text = formattedBiaya

                // Color coding untuk biaya
                when {
                    riwayat.biaya > 0 && riwayat.status.lowercase() == "selesai" -> {
                        tvBiaya.setTextColor(ContextCompat.getColor(context, android.R.color.holo_green_light))
                    }
                    riwayat.biaya > 0 && riwayat.status.lowercase() == "aktif" -> {
                        tvBiaya.setTextColor(ContextCompat.getColor(context, android.R.color.holo_orange_light))
                    }
                    else -> {
                        tvBiaya.setTextColor(ContextCompat.getColor(context, android.R.color.darker_gray))
                    }
                }
            }
        }

        private fun formatBiaya(biaya: Int): String {
            return when {
                biaya == 0 -> "Rp 0"
                biaya >= 1000000 -> "Rp ${String.format("%.1f", biaya / 1000000.0)}M"
                biaya >= 1000 -> "Rp ${String.format("%.0f", biaya / 1000.0)}K"
                else -> "Rp ${String.format("%,d", biaya)}"
            }
        }

        private fun calculateDuration(riwayat: RiwayatParkir): String {
            return if (riwayat.waktuMasuk != null && riwayat.waktuKeluar != null) {
                val durationMillis = riwayat.waktuKeluar.toDate().time - riwayat.waktuMasuk.toDate().time
                val hours = durationMillis / (1000 * 60 * 60)
                val minutes = (durationMillis % (1000 * 60 * 60)) / (1000 * 60)
                "${hours}j ${minutes}m"
            } else if (riwayat.waktuMasuk != null && riwayat.status.lowercase() == "aktif") {
                val currentTime = System.currentTimeMillis()
                val durationMillis = currentTime - riwayat.waktuMasuk.toDate().time
                val hours = durationMillis / (1000 * 60 * 60)
                val minutes = (durationMillis % (1000 * 60 * 60)) / (1000 * 60)
                "${hours}j ${minutes}m"
            } else {
                "-"
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RiwayatViewHolder {
        val binding = ItemRiwayatBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return RiwayatViewHolder(binding)
    }

    override fun onBindViewHolder(holder: RiwayatViewHolder, position: Int) {
        holder.bind(listRiwayat[position])
    }

    override fun getItemCount(): Int = listRiwayat.size

    // Method untuk update data
    fun updateData(newList: List<RiwayatParkir>) {
        listRiwayat.clear()
        listRiwayat.addAll(newList)
        notifyDataSetChanged()
    }

    // Method untuk filter data
    fun filterData(filteredList: List<RiwayatParkir>) {
        listRiwayat.clear()
        listRiwayat.addAll(filteredList)
        notifyDataSetChanged()
    }
}