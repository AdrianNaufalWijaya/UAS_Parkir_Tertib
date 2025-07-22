package com.example.parkirtertib

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.parkirtertib.databinding.FragmentFormMasukBinding
import com.example.parkirtertib.ParkirMasuk
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import android.util.Log

class FormMasukFragment : Fragment() {

    private var _binding: FragmentFormMasukBinding? = null
    private val binding get() = _binding!!
    private val db = FirebaseFirestore.getInstance()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentFormMasukBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val jenisList = listOf("Mobil", "Motor", "Truk")
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, jenisList)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerJenis.adapter = adapter

        binding.buttonSimpan.setOnClickListener {
            val platNomor = binding.editTextPlatNomor.text.toString().trim()
            Log.d("FormMasuk", "Plat Nomor: $platNomor")
            Log.d("FormMasuk", "Proses Firestore selesai")
            val jenisKendaraan = binding.spinnerJenis.selectedItem.toString()

            if (platNomor.isEmpty()) {
                Toast.makeText(requireContext(), "Plat Nomor tidak boleh kosong", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val data = ParkirMasuk(
                plat_nomor = platNomor,
                jenis_kendaraan = jenisKendaraan,
                waktu_masuk = Timestamp.now()
            )

            db.collection("parkir_masuk")
                .add(data)
                .addOnSuccessListener {
                    Toast.makeText(requireContext(), "Data berhasil disimpan", Toast.LENGTH_SHORT).show()
                    binding.editTextPlatNomor.setText("")
                }
                .addOnFailureListener {
                    Toast.makeText(requireContext(), "Gagal menyimpan: ${it.message}", Toast.LENGTH_SHORT).show()
                }
                .addOnCompleteListener {
                    Log.d("FormMasuk", "Proses Firestore selesai")
                }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
