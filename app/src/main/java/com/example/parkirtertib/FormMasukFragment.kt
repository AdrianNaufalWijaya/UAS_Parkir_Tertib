package com.example.parkirtertib

import android.app.AlertDialog
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.parkirtertib.R
import com.example.parkirtertib.FormMasukFragment
import com.example.parkirtertib.model.Parkir
import android.util.Log
import com.example.parkirtertib.databinding.FragmentFormMasukBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import java.text.SimpleDateFormat
import java.util.*

class FormMasukFragment : Fragment() {

    private var _binding: FragmentFormMasukBinding? = null
    private val binding get() = _binding!!
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private lateinit var timeHandler: Handler
    private lateinit var timeRunnable: Runnable

    // Vehicle types for dropdown
    private val vehicleTypes = arrayOf(
        "Motor",
        "Mobil",
        "Truk Kecil"
    )

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentFormMasukBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Debug authentication status first
        debugAuthStatus()

        setupUI()
        setupDropdown()
        setupClickListeners()
        startTimeUpdater()
        loadDashboardStats()
        loadUserProfile()
    }

    private fun debugAuthStatus() {
        val currentUser = auth.currentUser
        if (currentUser != null) {
            Log.d("FormMasukFragment", "User authenticated: ${currentUser.uid}")
            Log.d("FormMasukFragment", "User email: ${currentUser.email}")
            Log.d("FormMasukFragment", "User display name: ${currentUser.displayName}")

            // Test Firestore connection
            testFirestoreConnection()
        } else {
            Log.e("FormMasukFragment", "No authenticated user found!")
            Toast.makeText(requireContext(), "User belum login. Silakan login terlebih dahulu.", Toast.LENGTH_LONG).show()

            // Navigate to login if available
            // Uncomment if you have login navigation
            // findNavController().navigate(R.id.navigation_login)
        }
    }

    private fun testFirestoreConnection() {
        val currentUser = auth.currentUser ?: return

        Log.d("FormMasukFragment", "Testing Firestore connection...")

        // Test write operation
        val testData = hashMapOf(
            "test" to "connection",
            "timestamp" to Date(),
            "idUser" to currentUser.uid
        )

        db.collection("parkir_masuk")
            .add(testData)
            .addOnSuccessListener { documentReference ->
                Log.d("FormMasukFragment", "Test document written with ID: ${documentReference.id}")

                // Delete test document
                documentReference.delete()
                    .addOnSuccessListener {
                        Log.d("FormMasukFragment", "Test document successfully deleted!")
                        Log.d("FormMasukFragment", "Firestore connection is working properly")
                    }
                    .addOnFailureListener { e ->
                        Log.e("FormMasukFragment", "Error deleting test document", e)
                    }
            }
            .addOnFailureListener { e ->
                Log.e("FormMasukFragment", "Error adding test document", e)
                Toast.makeText(requireContext(), "Firestore connection error: ${e.message}", Toast.LENGTH_LONG).show()

                // Show detailed error information
                when (e.message) {
                    "PERMISSION_DENIED" -> {
                        Log.e("FormMasukFragment", "Permission denied - check Firestore rules")
                        Toast.makeText(requireContext(), "Akses ditolak. Periksa konfigurasi Firebase.", Toast.LENGTH_LONG).show()
                    }
                    else -> {
                        Log.e("FormMasukFragment", "Firestore error: ${e.message}")
                    }
                }
            }
    }

    private fun setupUI() {
        // Setup greeting based on time
        val currentHour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        val greeting = when (currentHour) {
            in 5..10 -> "Selamat Pagi"
            in 11..14 -> "Selamat Siang"
            in 15..17 -> "Selamat Sore"
            else -> "Selamat Malam"
        }
        binding.tvGreeting.text = greeting

        // Setup current date
        val dateFormat = SimpleDateFormat("EEEE, dd MMMM yyyy", Locale("id"))
        binding.tvCurrentDate.text = dateFormat.format(Date())
    }

    private fun setupDropdown() {
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, vehicleTypes)
        binding.etJenisKendaraan.setAdapter(adapter)
    }

    private fun setupClickListeners() {
        // tombol Masuk
        binding.btnMasuk.setOnClickListener {
            handleParkingEntry()
        }

        // Tombol Keluar
        binding.btnKeluar.setOnClickListener {
            handleParkingExit()
        }

        // hapus / Clear Form
        binding.btnClearForm.setOnClickListener {
            clearForm()
        }

        // View History Button
        binding.btnViewHistory.setOnClickListener {

            // Navigate to history fragment
            try {
                findNavController().navigate(R.id.navigation_riwayat)
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Navigasi ke riwayat", Toast.LENGTH_SHORT).show()
            }
        }

        // Statistics Button
        binding.btnStatistics.setOnClickListener {
            showStatisticsDialog()
        }
    }

    private fun startTimeUpdater() {
        timeHandler = Handler(Looper.getMainLooper())
        timeRunnable = object : Runnable {
            override fun run() {
                val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
                binding.tvCurrentTime.text = "${timeFormat.format(Date())} WIB"
                timeHandler.postDelayed(this, 1000)
            }
        }
        timeHandler.post(timeRunnable)
    }

    private fun handleParkingEntry() {
        val plat = binding.etPlatNomor.text.toString().trim().uppercase()
        val jenis = binding.etJenisKendaraan.text.toString().trim()

        // Validation
        if (plat.isEmpty()) {
            binding.etPlatNomor.error = "Plat nomor tidak boleh kosong"
            return
        }

        if (jenis.isEmpty()) {
            Toast.makeText(requireContext(), "Pilih jenis kendaraan", Toast.LENGTH_SHORT).show()
            return
        }

        // Check if vehicle already parked
        checkExistingVehicle(plat) { exists ->
            if (exists) {
                AlertDialog.Builder(requireContext())
                    .setTitle("Kendaraan Sudah Parkir")
                    .setMessage("Kendaraan dengan plat $plat sudah terdaftar parkir. Lanjutkan?")
                    .setPositiveButton("Ya") { _, _ ->
                        saveParkingEntry(plat, jenis)
                    }
                    .setNegativeButton("Batal", null)
                    .show()
            } else {
                saveParkingEntry(plat, jenis)
            }
        }
    }

    private fun checkExistingVehicle(plat: String, callback: (Boolean) -> Unit) {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            Log.e("FormMasukFragment", "User not authenticated in checkExistingVehicle")
            callback(false)
            return
        }

        Log.d("FormMasukFragment", "Checking existing vehicle for plat: $plat")

        db.collection("parkir_masuk")
            .whereEqualTo("idUser", currentUser.uid)
            .whereEqualTo("platNomor", plat)
            .whereEqualTo("status", "aktif")
            .get()
            .addOnSuccessListener { documents ->
                val exists = !documents.isEmpty
                Log.d("FormMasukFragment", "Existing vehicle check result: $exists")
                callback(exists)
            }
            .addOnFailureListener { e ->
                Log.e("FormMasukFragment", "Error checking existing vehicle", e)
                callback(false)
            }
    }

    private fun saveParkingEntry(plat: String, jenis: String) {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            Toast.makeText(requireContext(), "User belum login", Toast.LENGTH_SHORT).show()
            // Debug: cek kenapa user null
            debugAuthStatus()
            return
        }

        Log.d("FormMasukFragment", "Attempting to save parking entry for user: ${currentUser.uid}")
        Log.d("FormMasukFragment", "Plat: $plat, Jenis: $jenis")

        val currentTime = Date()
        val data = hashMapOf(
            "idUser" to currentUser.uid,
            "platNomor" to plat,
            "jenisKendaraan" to jenis,
            "waktuMasuk" to currentTime,
            "status" to "aktif"
        )

        Log.d("FormMasukFragment", "Data to be saved: $data")

        // Show loading
        binding.btnMasuk.isEnabled = false
        binding.btnMasuk.text = "Menyimpan..."

        db.collection("parkir_masuk")
            .add(data)
            .addOnSuccessListener { documentReference ->
                Log.d("FormMasukFragment", "Document written with ID: ${documentReference.id}")
                Toast.makeText(requireContext(), "Kendaraan berhasil masuk parkir", Toast.LENGTH_SHORT).show()
                clearForm()
                loadDashboardStats()

                // Reset button
                binding.btnMasuk.isEnabled = true
                binding.btnMasuk.text = "Masuk"
            }
            .addOnFailureListener { e ->
                Log.e("FormMasukFragment", "Error adding document", e)

                // More detailed error handling
                val errorMessage = when {
                    e.message?.contains("PERMISSION_DENIED") == true -> {
                        "Akses ditolak. Periksa konfigurasi Firebase Firestore Rules."
                    }
                    e.message?.contains("UNAUTHENTICATED") == true -> {
                        "User belum terautentikasi. Silakan login ulang."
                    }
                    else -> {
                        "Gagal menyimpan: ${e.message}"
                    }
                }

                Toast.makeText(requireContext(), errorMessage, Toast.LENGTH_LONG).show()

                // Reset button
                binding.btnMasuk.isEnabled = true
                binding.btnMasuk.text = "Masuk"
            }
    }

    private fun handleParkingExit() {
        val plat = binding.etPlatNomor.text.toString().trim().uppercase()

        if (plat.isEmpty()) {
            binding.etPlatNomor.error = "Masukkan plat nomor kendaraan yang akan keluar"
            return
        }

        val currentUser = auth.currentUser
        if (currentUser == null) {
            Toast.makeText(requireContext(), "User belum login", Toast.LENGTH_SHORT).show()
            debugAuthStatus()
            return
        }

        Log.d("FormMasukFragment", "Searching for active parking with plat: $plat")

        // Find active parking record
        db.collection("parkir_masuk")
            .whereEqualTo("idUser", currentUser.uid)
            .whereEqualTo("platNomor", plat)
            .whereEqualTo("status", "aktif")
            .get()
            .addOnSuccessListener { documents ->
                if (documents.isEmpty) {
                    Toast.makeText(requireContext(), "Kendaraan dengan plat $plat tidak ditemukan atau sudah keluar", Toast.LENGTH_SHORT).show()
                    Log.d("FormMasukFragment", "No active parking found for plat: $plat")
                    return@addOnSuccessListener
                }

                val document = documents.documents[0]
                val waktuMasuk = document.getTimestamp("waktuMasuk")?.toDate()
                val jenisKendaraan = document.getString("jenisKendaraan") ?: ""

                Log.d("FormMasukFragment", "Found parking record: ${document.id}")

                if (waktuMasuk != null) {
                    calculateAndShowPayment(document.id, plat, jenisKendaraan, waktuMasuk)
                }
            }
            .addOnFailureListener { e ->
                Log.e("FormMasukFragment", "Error searching parking data", e)
                Toast.makeText(requireContext(), "Gagal mencari data: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun calculateAndShowPayment(documentId: String, plat: String, jenis: String, waktuMasuk: Date) {
        val waktuKeluar = Date()
        val durasiMs = waktuKeluar.time - waktuMasuk.time
        val durasiJam = (durasiMs / (1000 * 60 * 60)).toInt().coerceAtLeast(1)

        // Tarif berdasarkan jenis kendaraan
        val tarifPerJam = when (jenis.lowercase()) {
            "motor", "sepeda" -> 2000
            "mobil" -> 5000
            "truk kecil" -> 8000
            else -> 3000
        }

        val biaya = durasiJam * tarifPerJam

        Log.d("FormMasukFragment", "Calculated payment - Duration: $durasiJam hours, Cost: $biaya")

        val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
        val message = """
            Plat: $plat
            Jenis: $jenis
            Masuk: ${timeFormat.format(waktuMasuk)}
            Keluar: ${timeFormat.format(waktuKeluar)}
            Durasi: $durasiJam jam
            Tarif: Rp ${String.format("%,d", tarifPerJam)}/jam
            Total Biaya: Rp ${String.format("%,d", biaya)}
        """.trimIndent()

        AlertDialog.Builder(requireContext())
            .setTitle("Konfirmasi Pembayaran")
            .setMessage(message)
            .setPositiveButton("Bayar & Keluar") { _, _ ->
                processExit(documentId, plat, waktuMasuk, waktuKeluar, durasiJam, biaya)
            }
            .setNegativeButton("Batal", null)
            .show()
    }

    private fun processExit(documentId: String, plat: String, waktuMasuk: Date, waktuKeluar: Date, durasi: Int, biaya: Int) {
        val currentUser = auth.currentUser ?: return

        Log.d("FormMasukFragment", "Processing exit for document: $documentId")

        // Show loading
        binding.btnKeluar.isEnabled = false
        binding.btnKeluar.text = "Memproses..."

        // Save to history
        val riwayatData = hashMapOf(
            "idUser" to currentUser.uid,
            "platNomor" to plat,
            "jenisKendaraan" to getVehicleTypeFromPlat(plat), // Get from the original data
            "waktuMasuk" to waktuMasuk,
            "waktuKeluar" to waktuKeluar,
            "durasi" to durasi,
            "biaya" to biaya
        )

        Log.d("FormMasukFragment", "Saving to history: $riwayatData")

        db.collection("parkir_masuk")
            .add(riwayatData)
            .addOnSuccessListener { historyRef ->
                Log.d("FormMasukFragment", "History saved with ID: ${historyRef.id}")

                // Delete from active parking
                db.collection("parkir_masuk").document(documentId)
                    .delete()
                    .addOnSuccessListener {
                        Log.d("FormMasukFragment", "Active parking deleted successfully")
                        Toast.makeText(requireContext(), "Pembayaran berhasil! Kendaraan telah keluar", Toast.LENGTH_SHORT).show()
                        clearForm()
                        loadDashboardStats()

                        // Reset button
                        binding.btnKeluar.isEnabled = true
                        binding.btnKeluar.text = "Keluar"
                    }
                    .addOnFailureListener { e ->
                        Log.e("FormMasukFragment", "Error deleting active parking", e)
                        Toast.makeText(requireContext(), "Gagal menghapus data parkir: ${e.message}", Toast.LENGTH_SHORT).show()

                        // Reset button
                        binding.btnKeluar.isEnabled = true
                        binding.btnKeluar.text = "Keluar"
                    }
            }
            .addOnFailureListener { e ->
                Log.e("FormMasukFragment", "Error saving history", e)
                Toast.makeText(requireContext(), "Gagal menyimpan riwayat: ${e.message}", Toast.LENGTH_SHORT).show()

                // Reset button
                binding.btnKeluar.isEnabled = true
                binding.btnKeluar.text = "Keluar"
            }
    }

    private fun getVehicleTypeFromPlat(plat: String): String {
        // You might want to get this from the original parking data
        // For now, return a default or try to get from the form
        return binding.etJenisKendaraan.text.toString().ifEmpty { "Motor" }
    }

    private fun clearForm() {
        binding.etPlatNomor.text?.clear()
        binding.etJenisKendaraan.text?.clear()
        binding.etPlatNomor.error = null
    }

    private fun loadDashboardStats() {
        val currentUser = auth.currentUser ?: return

        Log.d("FormMasukFragment", "Loading dashboard stats")

        // Load active parking count
        db.collection("parkir_masuk")
            .whereEqualTo("idUser", currentUser.uid)
            .whereEqualTo("status", "aktif")
            .get()
            .addOnSuccessListener { documents ->
                val count = documents.size()
                Log.d("FormMasukFragment", "Active parking count: $count")
                binding.tvActiveParking?.text = "$count Kendaraan"
            }
            .addOnFailureListener { e ->
                Log.e("FormMasukFragment", "Error loading active parking count", e)
                binding.tvActiveParking?.text = "0 Kendaraan"
            }

        // Load today's income
        val startOfDay = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.time

        val endOfDay = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
            set(Calendar.MILLISECOND, 999)
        }.time

        db.collection("parkir_masuk")
            .whereEqualTo("idUser", currentUser.uid)
            .whereGreaterThanOrEqualTo("waktuKeluar", startOfDay)
            .whereLessThanOrEqualTo("waktuKeluar", endOfDay)
            .get()
            .addOnSuccessListener { documents ->
                var totalIncome = 0
                for (document in documents) {
                    val biaya = document.getLong("biaya")?.toInt() ?: 0
                    totalIncome += biaya
                }
            }
    }

    private fun loadUserProfile() {
        val currentUser = auth.currentUser
        if (currentUser != null) {
            val displayName = currentUser.displayName ?: currentUser.email ?: "User"
            binding.tvUserName?.text = displayName
            Log.d("FormMasukFragment", "User profile loaded: $displayName")
        } else {
            Log.e("FormMasukFragment", "No user found when loading profile")
        }
    }

    private fun showStatisticsDialog() {
        val currentUser = auth.currentUser ?: return

        Log.d("FormMasukFragment", "Loading statistics dialog")

        // Calculate this month's statistics
        val startOfMonth = Calendar.getInstance().apply {
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.time

        db.collection("parkir_masuk")
            .whereEqualTo("idUser", currentUser.uid)
            .whereGreaterThanOrEqualTo("waktuKeluar", startOfMonth)
            .get()
            .addOnSuccessListener { documents ->
                var totalIncome = 0
                var totalVehicles = documents.size()
                val vehicleStats = mutableMapOf<String, Int>()

                for (document in documents) {
                    val biaya = document.getLong("biaya")?.toInt() ?: 0
                    totalIncome += biaya

                    val jenisKendaraan = document.getString("jenisKendaraan") ?: "Lainnya"
                    vehicleStats[jenisKendaraan] = vehicleStats.getOrDefault(jenisKendaraan, 0) + 1
                }

                Log.d("FormMasukFragment", "Monthly stats - Income: $totalIncome, Vehicles: $totalVehicles")

                val statsMessage = buildString {
                    appendLine("STATISTIK BULAN INI")
                    appendLine("═══════════════════")
                    appendLine("Total Pendapatan: Rp ${String.format("%,d", totalIncome)}")
                    appendLine("Total Kendaraan: $totalVehicles")
                    appendLine("")
                    appendLine("DETAIL KENDARAAN:")
                    for ((jenis, jumlah) in vehicleStats) {
                        appendLine("• $jenis: $jumlah kendaraan")
                    }
                }

                AlertDialog.Builder(requireContext())
                    .setTitle("Statistik Parkir")
                    .setMessage(statsMessage)
                    .setPositiveButton("OK", null)
                    .show()
            }
            .addOnFailureListener { e ->
                Log.e("FormMasukFragment", "Error loading statistics", e)
                Toast.makeText(requireContext(), "Gagal memuat statistik: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        if (::timeHandler.isInitialized) {
            timeHandler.removeCallbacks(timeRunnable)
        }
        _binding = null
        Log.d("FormMasukFragment", "Fragment destroyed")
    }
}