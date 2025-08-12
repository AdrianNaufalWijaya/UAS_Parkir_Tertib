package com.example.parkirtertib.model

import com.google.firebase.Timestamp

data class RiwayatParkir(
    val id: String = "",
    val idUser: String = "",
    val tanggal: String = "",
    val platNomor: String = "",
    val jenisKendaraan: String = "",
    val jamMasuk: String = "",
    val jamKeluar: String? = null,
    val waktuMasuk: Timestamp? = null,
    val waktuKeluar: Timestamp? = null,
    val durasi: String? = null,
    val biaya: Int = 0,
    val status: String = "aktif"
)