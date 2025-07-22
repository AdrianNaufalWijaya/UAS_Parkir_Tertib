package com.example.parkirtertib.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.parkirtertib.databinding.FragmentHomeBinding
import com.example.parkirtertib.model.DataParkir
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.auth.FirebaseAuth
import java.text.SimpleDateFormat
import java.util.*
import android.app.AlertDialog
import com.google.firebase.Timestamp

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance() // TAMBAHAN: Missing auth instance

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Button Masuk - untuk parkir masuk
        binding.btnMasuk.setOnClickListener {
            val plat = binding.etPlatNomor.text.toString().trim()
            val jenis = binding.etJenisKendaraan.text.toString().trim()
            val waktuMasuk = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())

            if (plat.isEmpty() || jenis.isEmpty()) {
                Toast.makeText(requireContext(), "Isi semua data kendaraan", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val data = DataParkir(plat, jenis, waktuMasuk)

            db.collection("parkir_masuk")
                .add(data)
                .addOnSuccessListener {
                    Toast.makeText(requireContext(), "Data parkir berhasil disimpan", Toast.LENGTH_SHORT).show()
                    binding.etPlatNomor.text?.clear()
                    binding.etJenisKendaraan.text?.clear()
                }
                .addOnFailureListener {
                    Toast.makeText(requireContext(), "Gagal menyimpan data", Toast.LENGTH_SHORT).show()
                }
        }

        // Button Keluar - untuk parkir keluar
        // PERBAIKAN: Menghapus duplicate setOnClickListener
        binding.btnKeluar.setOnClickListener {
            val user = auth.currentUser
            if (user != null) {
                val userId = user.uid
                db.collection("parkir")
                    .whereEqualTo("id_user", userId)
                    .get()
                    .addOnSuccessListener { result ->
                        if (!result.isEmpty) {
                            val document = result.documents[0]
                            val waktuMasuk = document.getTimestamp("waktu_masuk")?.toDate()
                            val idParkir = document.id

                            if (waktuMasuk != null) {
                                val waktuKeluar = Date()
                                val durasiMs = waktuKeluar.time - waktuMasuk.time
                                val durasiJam = (durasiMs / (1000 * 60 * 60)).toInt().coerceAtLeast(1)
                                val tarifPerJam = 2000
                                val biaya = durasiJam * tarifPerJam

                                // Konfirmasi kepada user
                                AlertDialog.Builder(requireContext())
                                    .setTitle("Konfirmasi Keluar")
                                    .setMessage("Durasi: $durasiJam jam\nBiaya: Rp $biaya")
                                    .setPositiveButton("Bayar") { _, _ ->
                                        val dataRiwayat = hashMapOf(
                                            "id_user" to userId,
                                            "waktu_masuk" to waktuMasuk,
                                            "waktu_keluar" to waktuKeluar,
                                            "durasi_jam" to durasiJam,
                                            "biaya" to biaya
                                        )

                                        db.collection("riwayat")
                                            .add(dataRiwayat)
                                            .addOnSuccessListener {
                                                db.collection("parkir").document(idParkir)
                                                    .delete()
                                                    .addOnSuccessListener {
                                                        Toast.makeText(requireContext(), "Keluar parkir berhasil", Toast.LENGTH_SHORT).show()
                                                    }
                                                    .addOnFailureListener { e ->
                                                        Toast.makeText(requireContext(), "Gagal menghapus data parkir: ${e.message}", Toast.LENGTH_SHORT).show()
                                                    }
                                            }
                                            .addOnFailureListener { e ->
                                                Toast.makeText(requireContext(), "Gagal menyimpan riwayat: ${e.message}", Toast.LENGTH_SHORT).show()
                                            }
                                    }
                                    .setNegativeButton("Batal", null)
                                    .show()
                            }
                        } else {
                            Toast.makeText(requireContext(), "Tidak ada data parkir aktif", Toast.LENGTH_SHORT).show()
                        }
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(requireContext(), "Gagal mengambil data: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
            } else {
                Toast.makeText(requireContext(), "User belum login", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}