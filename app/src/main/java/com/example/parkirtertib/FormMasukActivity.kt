package com.example.parkirtertib

import android.os.Bundle
import android.util.Log
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.parkirtertib.databinding.ActivityFormMasukBinding
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FieldValue

class FormMasukActivity : AppCompatActivity() {

    private lateinit var binding: ActivityFormMasukBinding
    private val db = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityFormMasukBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val jenisKendaraan = listOf("Motor", "Mobil", "Truk")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, jenisKendaraan)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerJenisKendaraan.adapter = adapter

        binding.btnSimpan.setOnClickListener {
            val platNomor = binding.editTextPlatNomor.text.toString()
            val jenis = binding.spinnerJenisKendaraan.selectedItem.toString()

            if (platNomor.isNotBlank()) {

                val data = hashMapOf(
                    "plat_nomor" to platNomor,
                    "jenis_kendaraan" to jenis,
                    "waktu_masuk" to FieldValue.serverTimestamp()
                )

                db.collection("parkir_masuk")
                    .add(data)
                    .addOnSuccessListener {
                        Toast.makeText(this, "Berhasil menyimpan", Toast.LENGTH_SHORT).show()
                        finish()
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(this, "Gagal menyimpan: ${e.message}", Toast.LENGTH_LONG).show()
                        Log.e("FormMasuk", "Gagal simpan data", e)
                    }

            } else {
                Toast.makeText(this, "Plat nomor harus diisi", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
