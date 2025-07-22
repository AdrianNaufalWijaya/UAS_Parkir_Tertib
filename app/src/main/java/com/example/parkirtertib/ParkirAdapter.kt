package com.example.parkirtertib.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.parkirtertib.databinding.ItemParkirBinding
import com.example.parkirtertib.ParkirMasuk
import java.text.SimpleDateFormat
import com.google.firebase.Timestamp
import java.util.*

class ParkirAdapter(private val dataList: List<ParkirMasuk>) :
    RecyclerView.Adapter<ParkirAdapter.ViewHolder>() {

    class ViewHolder(val binding: ItemParkirBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemParkirBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun getItemCount(): Int = dataList.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = dataList[position]
        holder.binding.textPlatNomor.text = "Plat: ${item.plat_nomor}"
        holder.binding.textJenisKendaraan.text = item.jenis_kendaraan

        val waktuFormatted = item.waktu_masuk?.toDate()?.let {
            SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(it)
        } ?: "-"
        holder.binding.textWaktuMasuk.text = "Masuk: $waktuFormatted"
    }
}
