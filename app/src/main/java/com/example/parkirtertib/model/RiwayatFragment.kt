package com.example.parkirtertib

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.parkirtertib.databinding.FragmentRiwayatBinding
import com.example.parkirtertib.model.RiwayatParkir
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ListenerRegistration
import java.text.SimpleDateFormat
import java.util.*

class RiwayatFragment : Fragment() {

    private var _binding: FragmentRiwayatBinding? = null
    private val binding get() = _binding!!

    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore
    private lateinit var adapter: RiwayatAdapter
    private val listRiwayat = mutableListOf<RiwayatParkir>()
    private val originalList = mutableListOf<RiwayatParkir>()

    // Listener untuk real-time updates
    private var riwayatListener: ListenerRegistration? = null
    private var parkirMasukListener: ListenerRegistration? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRiwayatBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

        setupRecyclerView()
        setupClickListeners()
        fetchDataWithRealTimeUpdates()
    }

    private fun setupRecyclerView() {
        adapter = RiwayatAdapter(listRiwayat)
        binding.recyclerriwayat.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = this@RiwayatFragment.adapter
        }
    }

    private fun setupClickListeners() {
        // Refresh button
        binding.btnRefresh?.setOnClickListener {
            fetchDataWithRealTimeUpdates()
        }

        // Search functionality
        binding.etSearch?.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                filterData(s.toString())
            }
        })

        // Filter buttons
        binding.btnShowAll?.setOnClickListener { showAllData() }
        binding.btnFilterAktif?.setOnClickListener { filterByStatus("aktif") }
        binding.btnFilterSelesai?.setOnClickListener { filterByStatus("selesai") }
        binding.btnSortTanggal?.setOnClickListener { sortByDate() }
        binding.btnSortBiaya?.setOnClickListener { sortByBiaya() }
    }

    private fun fetchDataWithRealTimeUpdates() {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            Log.e("RiwayatFragment", "User belum login")
            showEmptyState("Silakan login terlebih dahulu")
            return
        }

        val userId = currentUser.uid
        Log.d("RiwayatFragment", "Setting up real-time listeners for user: $userId")

        // Show loading
        showLoading(true)

        // Clear existing listeners
        riwayatListener?.remove()
        parkirMasukListener?.remove()

        // Setup real-time listener untuk riwayat_parkir
        setupRiwayatListener(userId)

        // Setup real-time listener untuk parkir_masuk (untuk status aktif)
        setupParkirMasukListener(userId)
    }

    private fun setupRiwayatListener(userId: String) {
        riwayatListener = firestore.collection("riwayat_parkir")
            .whereEqualTo("idUser", userId)
            .orderBy("waktuMasuk", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("RiwayatFragment", "Error listening to riwayat_parkir", error)
                    return@addSnapshotListener
                }

                if (snapshot != null && !snapshot.isEmpty) {
                    Log.d("RiwayatFragment", "Riwayat data updated: ${snapshot.documents.size} documents")
                    processRiwayatData(snapshot.documents)
                } else {
                    Log.d("RiwayatFragment", "No riwayat data found")
                    // Jika tidak ada data di riwayat_parkir, data akan diambil dari parkir_masuk listener
                }
            }
    }

    private fun setupParkirMasukListener(userId: String) {
        parkirMasukListener = firestore.collection("parkir_masuk")
            .whereEqualTo("idUser", userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("RiwayatFragment", "Error listening to parkir_masuk", error)
                    showLoading(false)
                    showError("Gagal memuat data: ${error.localizedMessage}")
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    Log.d("RiwayatFragment", "Parkir masuk data updated: ${snapshot.documents.size} documents")
                    processParkirMasukData(snapshot.documents)
                }

                showLoading(false)
            }
    }

    private fun processRiwayatData(documents: List<com.google.firebase.firestore.DocumentSnapshot>) {
        val riwayatData = mutableListOf<RiwayatParkir>()

        for (document in documents) {
            try {
                val riwayat = createRiwayatFromDocument(document)
                riwayat?.let { riwayatData.add(it) }
            } catch (e: Exception) {
                Log.e("RiwayatFragment", "Error processing riwayat document: ${document.id}", e)
            }
        }

        // Update list dengan data dari riwayat_parkir
        updateListWithRiwayatData(riwayatData)
    }

    private fun processParkirMasukData(documents: List<com.google.firebase.firestore.DocumentSnapshot>) {
        val parkirMasukData = mutableListOf<RiwayatParkir>()

        for (document in documents) {
            try {
                val riwayat = createRiwayatFromDocument(document)
                riwayat?.let { parkirMasukData.add(it) }
            } catch (e: Exception) {
                Log.e("RiwayatFragment", "Error processing parkir_masuk document: ${document.id}", e)
            }
        }

        // Update list dengan data dari parkir_masuk
        updateListWithParkirMasukData(parkirMasukData)
    }

    private fun updateListWithRiwayatData(riwayatData: List<RiwayatParkir>) {
        // Hapus data yang sudah ada dari riwayat_parkir
        originalList.removeAll { existingData ->
            riwayatData.any { newData -> newData.id == existingData.id }
        }

        // Tambahkan data baru dari riwayat_parkir
        originalList.addAll(riwayatData)

        updateUI()
        updateStatistics()
    }

    private fun updateListWithParkirMasukData(parkirMasukData: List<RiwayatParkir>) {
        // Hapus data lama dari parkir_masuk
        originalList.removeAll { existingData ->
            parkirMasukData.any { newData -> newData.platNomor == existingData.platNomor && existingData.status == "aktif" }
        }

        // Tambahkan data baru dari parkir_masuk yang belum ada di riwayat_parkir
        for (parkirData in parkirMasukData) {
            val existsInRiwayat = originalList.any {
                it.platNomor == parkirData.platNomor && it.status == "selesai"
            }
            if (!existsInRiwayat) {
                originalList.add(parkirData)
            }
        }

        // Sort berdasarkan waktu masuk
        originalList.sortByDescending { it.waktuMasuk }

        updateUI()
        updateStatistics()
    }

    private fun createRiwayatFromDocument(document: com.google.firebase.firestore.DocumentSnapshot): RiwayatParkir? {
        try {
            val data = document.data ?: return null

            // Extract fields dengan berbagai kemungkinan nama field
            val platNomor = data["platNomor"] as? String ?: data["plat_nomor"] as? String ?: ""
            val jenisKendaraan = data["jenisKendaraan"] as? String ?: data["jenis_kendaraan"] as? String ?: "Mobil"

            // Handle biaya dengan berbagai tipe data
            val biaya = when (val biayaField = data["biaya"]) {
                is Long -> biayaField.toInt()
                is Number -> biayaField.toInt()
                is String -> biayaField.toIntOrNull() ?: 0
                else -> 0
            }

            val durasi = data["durasi"] as? String ?: data["duration"] as? String

            // Tentukan status berdasarkan keberadaan waktuKeluar
            val waktuKeluar = data["waktuKeluar"] as? com.google.firebase.Timestamp
            val status = if (waktuKeluar != null) "selesai" else "aktif"

            // Handle timestamps
            val waktuMasuk = data["waktuMasuk"] as? com.google.firebase.Timestamp

            // Format dates dan times
            val dateFormat = SimpleDateFormat("dd MMM", Locale("id", "ID"))
            val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())

            val tanggal = waktuMasuk?.toDate()?.let { dateFormat.format(it) } ?: ""
            val jamMasuk = waktuMasuk?.toDate()?.let { timeFormat.format(it) } ?: ""
            val jamKeluar = waktuKeluar?.toDate()?.let { timeFormat.format(it) }

            return RiwayatParkir(
                id = document.id,
                idUser = data["idUser"] as? String ?: "",
                tanggal = tanggal,
                platNomor = platNomor,
                jenisKendaraan = jenisKendaraan,
                jamMasuk = jamMasuk,
                jamKeluar = jamKeluar,
                waktuMasuk = waktuMasuk,
                waktuKeluar = waktuKeluar,
                durasi = durasi,
                biaya = biaya,
                status = status
            )
        } catch (e: Exception) {
            Log.e("RiwayatFragment", "Error creating RiwayatParkir from document", e)
            return null
        }
    }

    private fun updateUI() {
        if (originalList.isEmpty()) {
            showEmptyState("Belum ada riwayat parkir")
        } else {
            showData()
            // Perbarui listRiwayat untuk tampilan
            listRiwayat.clear()
            listRiwayat.addAll(originalList)
            adapter.notifyDataSetChanged()
        }
    }

    private fun updateStatistics() {
        val totalParkir = originalList.size
        val parkirAktif = originalList.count { it.status.lowercase() == "aktif" }
        val parkirSelesai = originalList.count { it.status.lowercase() == "selesai" }
        val totalBiaya = originalList.sumOf { it.biaya }

        val totalDurasiMinutes = originalList
            .filter { it.status.lowercase() == "selesai" && it.waktuMasuk != null && it.waktuKeluar != null }
            .sumOf { riwayat ->
                val durationMillis = riwayat.waktuKeluar!!.toDate().time - riwayat.waktuMasuk!!.toDate().time
                durationMillis / (1000 * 60)
            }

        binding.tvTotalParkir?.text = "$totalParkir"
        binding.tvParkirAktif?.text = "$parkirAktif"
        binding.tvParkirSelesai?.text = "$parkirSelesai"
        binding.tvTotalBiaya?.text = "Rp ${String.format("%,d", totalBiaya)}"
    }

    private fun filterData(query: String) {
        val filteredList = if (query.isEmpty()) {
            originalList.toList()
        } else {
            originalList.filter {
                it.platNomor.contains(query, ignoreCase = true) ||
                        it.jenisKendaraan.contains(query, ignoreCase = true)
            }
        }

        listRiwayat.clear()
        listRiwayat.addAll(filteredList)
        adapter.notifyDataSetChanged()

        if (listRiwayat.isEmpty() && query.isNotEmpty()) {
            showEmptyState("Tidak ada data yang cocok dengan pencarian")
        } else if (listRiwayat.isNotEmpty()) {
            showData()
        }
    }

    private fun showAllData() {
        listRiwayat.clear()
        listRiwayat.addAll(originalList)
        adapter.notifyDataSetChanged()
        updateUI()
    }

    private fun filterByStatus(status: String) {
        val filteredList = originalList.filter { it.status.lowercase() == status.lowercase() }
        listRiwayat.clear()
        listRiwayat.addAll(filteredList)
        adapter.notifyDataSetChanged()

        if (listRiwayat.isEmpty()) {
            showEmptyState("Tidak ada data dengan status $status")
        } else {
            showData()
        }
    }

    private fun sortByDate() {
        listRiwayat.sortByDescending { it.waktuMasuk }
        adapter.notifyDataSetChanged()
    }

    private fun sortByBiaya() {
        listRiwayat.sortByDescending { it.biaya }
        adapter.notifyDataSetChanged()
    }

    private fun showLoading(show: Boolean) {
        binding.apply {
            if (show) {
                progressBar?.visibility = View.VISIBLE
                recyclerriwayat.visibility = View.GONE
                tvEmptyMessage?.visibility = View.GONE
                layoutEmpty?.visibility = View.GONE
            } else {
                progressBar?.visibility = View.GONE
            }
        }
    }

    private fun showData() {
        binding.apply {
            recyclerriwayat.visibility = View.VISIBLE
            layoutEmpty?.visibility = View.GONE
            tvEmptyMessage?.visibility = View.GONE
        }
    }

    private fun showEmptyState(message: String) {
        binding.apply {
            recyclerriwayat.visibility = View.GONE
            layoutEmpty?.visibility = View.VISIBLE
            tvEmptyMessage?.apply {
                visibility = View.VISIBLE
                text = message
            }
        }
    }

    private fun showError(message: String) {
        binding.apply {
            recyclerriwayat.visibility = View.GONE
            layoutEmpty?.visibility = View.VISIBLE
            tvEmptyMessage?.apply {
                visibility = View.VISIBLE
                text = message
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // Hapus listeners untuk mencegah memory leak
        riwayatListener?.remove()
        parkirMasukListener?.remove()
        _binding = null
    }
}