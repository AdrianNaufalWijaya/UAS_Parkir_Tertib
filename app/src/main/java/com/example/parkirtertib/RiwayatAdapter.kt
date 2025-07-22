package com.example.parkirtertib

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.parkirtertib.databinding.ItemRiwayatBinding
import java.text.SimpleDateFormat
import java.util.*

class RiwayatAdapter(private val data: List<ParkirMasuk>) :
    RecyclerView.Adapter<RiwayatAdapter.ViewHolder>() {

    class ViewHolder(val binding: ItemRiwayatBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemRiwayatBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun getItemCount(): Int = data.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = data[position]
        holder.binding.textPlat.text = item.plat_nomor
        holder.binding.textJenis.text = item.jenis_kendaraan
        holder.binding.textWaktu.text = item.waktu_masuk?.toDate()?.let {
            SimpleDateFormat("dd MMM yyyy HH:mm", Locale.getDefault()).format(it)
        } ?: "-"
    }
}
