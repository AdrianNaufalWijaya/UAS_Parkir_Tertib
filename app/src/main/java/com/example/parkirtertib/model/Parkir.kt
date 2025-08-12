package com.example.parkirtertib.model

import com.google.firebase.Timestamp

data class Parkir(
    val id: String = "",
    val idUser: String = "",
    val tanggal: String = "",
    val platNomor: String = "",
    val jenisKendaraan: String = "",
    val waktuMasuk: Timestamp? = null,
    val waktuKeluar: Timestamp? = null,
    val durasi: String = "",
    val biaya: Long = 0,
    val status: String = "aktif"
)