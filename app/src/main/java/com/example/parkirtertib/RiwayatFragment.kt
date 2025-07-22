package com.example.parkirtertib

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.parkirtertib.databinding.FragmentRiwayatBinding
import com.google.firebase.firestore.FirebaseFirestore

class RiwayatFragment : Fragment() {

    private var _binding: FragmentRiwayatBinding? = null
    private val binding get() = _binding!!
    private lateinit var adapter: RiwayatAdapter
    private val db = FirebaseFirestore.getInstance()
    private val listRiwayat = mutableListOf<ParkirMasuk>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRiwayatBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = RiwayatAdapter(listRiwayat)
        binding.recyclerViewRiwayat.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerViewRiwayat.adapter = adapter

        ambilDataDariFirestore()
    }

    private fun ambilDataDariFirestore() {
        db.collection("parkir_masuk")
            .get()
            .addOnSuccessListener { result ->
                listRiwayat.clear()
                for (document in result) {
                    val data = document.toObject(ParkirMasuk::class.java)
                    listRiwayat.add(data)
                }
                adapter.notifyDataSetChanged()
            }
            .addOnFailureListener { exception ->
                Log.e("RiwayatFragment", "Error ambil data", exception)
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
