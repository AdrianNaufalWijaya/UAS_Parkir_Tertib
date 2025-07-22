package com.example.parkirtertib

import com.google.firebase.Timestamp

data class ParkirMasuk(
    val plat_nomor: String = "",
    val jenis_kendaraan: String = "",
    val waktu_masuk: Timestamp? = null
)
