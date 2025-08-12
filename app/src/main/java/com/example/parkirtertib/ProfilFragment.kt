package com.example.parkirtertib

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.parkirtertib.databinding.FragmentProfilBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.NumberFormat
import java.util.*

class ProfilFragment : Fragment() {

    private var _binding: FragmentProfilBinding? = null
    private val binding get() = _binding!!

    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfilBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize Firebase
        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

        // Check if user is logged in
        if (auth.currentUser == null) {
            redirectToLogin()
            return
        }

        setupUserInterface()
        setupClickListeners()
        loadUserData()
        loadUserStatistics()
    }

    private fun setupUserInterface() {
        // Set loading state initially
        binding.apply {
            tvUserName.text = "Memuat..."
            tvUserEmail.text = "Memuat..."
            tvTotalParkirCount.text = "0"
            tvTotalSpent.text = "Rp 0"
            tvStatus.text = "Memuat..."
        }
    }

    private fun setupClickListeners() {
        binding.apply {
            // Logout button with confirmation dialog
            btnLogout.setOnClickListener {
                showLogoutConfirmationDialog()
            }

            // Edit Profile
            cardEditProfile.setOnClickListener {
                navigateToEditProfile()
            }

            // History
            cardHistory.setOnClickListener {
                navigateToHistory()
            }

            // Settings
            cardSettings.setOnClickListener {
                navigateToSettings()
            }

            // Help & Support
            cardHelp.setOnClickListener {
                navigateToHelp()
            }
        }
    }

    private fun loadUserData() {
        val currentUser = auth.currentUser
        if (currentUser != null) {
            // Display basic user info from Firebase Auth
            binding.apply {
                tvUserEmail.text = currentUser.email ?: "Email tidak tersedia"

                // Set initial name from Firebase Auth
                val authDisplayName = currentUser.displayName
                if (!authDisplayName.isNullOrEmpty()) {
                    tvUserName.text = authDisplayName
                } else {
                    tvUserName.text = "Pengguna"
                }

                // Check email verification status
                if (currentUser.isEmailVerified) {
                    tvStatus.text = "✓ Akun Terverifikasi"
                    // The CardView background is already set in XML to green (#4CAF50)
                } else {
                    tvStatus.text = "⚠ Email Belum Terverifikasi"
                    // Optionally change the card background color for unverified
                    try {
                        val statusCard = tvStatus.parent as? androidx.cardview.widget.CardView
                        statusCard?.setCardBackgroundColor(resources.getColor(android.R.color.holo_orange_dark, null))
                    } catch (e: Exception) {
                        Log.e("ProfilFragment", "Error changing status card color", e)
                    }
                }
            }

            // Load additional user data from Firestore
            loadUserProfileFromFirestore(currentUser.uid)
        } else {
            // User not logged in, redirect to login
            redirectToLogin()
        }
    }

    private fun loadUserProfileFromFirestore(userId: String) {
        firestore.collection("users").document(userId)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val userData = document.data
                    userData?.let {
                        // Update UI with Firestore data
                        val firestoreName = it["name"] as? String ?: it["displayName"] as? String
                        val phoneNumber = it["phoneNumber"] as? String

                        if (!firestoreName.isNullOrEmpty()) {
                            binding.tvUserName.text = firestoreName
                        }

                        // You can add phone number display if you have a TextView for it
                        // binding.tvPhoneNumber.text = phoneNumber ?: "Tidak ada nomor"

                        Log.d("ProfilFragment", "User profile loaded successfully from Firestore")
                    }
                } else {
                    Log.d("ProfilFragment", "No Firestore document found for user")
                }
            }
            .addOnFailureListener { exception ->
                Log.e("ProfilFragment", "Error loading user profile from Firestore", exception)
                // Keep the display name from Firebase Auth if Firestore fails
            }
    }

    private fun loadUserStatistics() {
        val currentUser = auth.currentUser
        if (currentUser != null) {
            val userId = currentUser.uid

            // Load statistics from riwayat_parkir collection
            firestore.collection("riwayat_parkir")
                .whereEqualTo("idUser", userId)
                .get()
                .addOnSuccessListener { documents ->
                    var totalParkir = 0
                    var totalBiaya = 0

                    for (document in documents) {
                        totalParkir++
                        val biaya = document.getLong("biaya")?.toInt() ?: 0
                        totalBiaya += biaya
                    }

                    // Update UI with statistics
                    updateStatisticsUI(totalParkir, totalBiaya)

                    Log.d("ProfilFragment", "Statistics loaded: $totalParkir parkir, total biaya: $totalBiaya")
                }
                .addOnFailureListener { exception ->
                    Log.e("ProfilFragment", "Error loading statistics from riwayat_parkir", exception)
                    // Try alternative collection if first one fails
                    loadStatisticsFromAlternativeCollection(userId)
                }
        }
    }

    private fun loadStatisticsFromAlternativeCollection(userId: String) {
        // Try loading from parkir_masuk collection as fallback
        firestore.collection("parkir_masuk")
            .whereEqualTo("idUser", userId)
            .get()
            .addOnSuccessListener { documents ->
                var totalParkir = 0
                var totalBiaya = 0

                for (document in documents) {
                    totalParkir++
                    // Try different field names for cost
                    val biaya = document.getLong("biaya")?.toInt()
                        ?: document.getLong("totalBiaya")?.toInt()
                        ?: document.getLong("cost")?.toInt()
                        ?: 0
                    totalBiaya += biaya
                }

                // Update UI with statistics
                updateStatisticsUI(totalParkir, totalBiaya)

                Log.d("ProfilFragment", "Alternative statistics loaded: $totalParkir parkir, total biaya: $totalBiaya")
            }
            .addOnFailureListener { exception ->
                Log.e("ProfilFragment", "Error loading alternative statistics", exception)
                // Set default values
                updateStatisticsUI(0, 0)
            }
    }

    private fun updateStatisticsUI(totalParkir: Int, totalBiaya: Int) {
        binding.apply {
            tvTotalParkirCount.text = totalParkir.toString()
            tvTotalSpent.text = formatCurrency(totalBiaya)
        }
    }

    private fun formatCurrency(amount: Int): String {
        return when {
            amount >= 1000000 -> {
                val millions = amount / 1000000.0
                "Rp ${String.format("%.1f", millions)}M"
            }
            amount >= 1000 -> {
                val thousands = amount / 1000.0
                "Rp ${String.format("%.0f", thousands)}K"
            }
            else -> "Rp $amount"
        }
    }

    private fun navigateToEditProfile() {
        try {
            // Try to navigate to edit profile fragment
            findNavController().navigate(R.id.action_profilFragment_to_editProfileFragment)
        } catch (e: Exception) {
            Log.e("ProfilFragment", "Navigation to edit profile failed", e)
            Toast.makeText(requireContext(), "Fitur Edit Profil akan segera hadir", Toast.LENGTH_SHORT).show()
        }
    }

    private fun navigateToHistory() {
        try {
            // Try multiple possible navigation paths
            findNavController().navigate(R.id.action_profilFragment_to_riwayatFragment)
        } catch (e: Exception) {
            try {
                // Fallback to direct navigation
                findNavController().navigate(R.id.navigation_riwayat)
            } catch (e2: Exception) {
                try {
                    // Another fallback
                    findNavController().navigate(R.id.navigation_riwayat)
                } catch (e3: Exception) {
                    Log.e("ProfilFragment", "All navigation attempts to history failed", e3)
                    Toast.makeText(requireContext(), "Tidak dapat membuka riwayat", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun navigateToSettings() {
        try {
            findNavController().navigate(R.id.action_profilFragment_to_settingsFragment)
        } catch (e: Exception) {
            Log.e("ProfilFragment", "Navigation to settings failed", e)
            Toast.makeText(requireContext(), "Fitur Pengaturan akan segera hadir", Toast.LENGTH_SHORT).show()
        }
    }

    private fun navigateToHelp() {
        try {
            findNavController().navigate(R.id.action_profilFragment_to_helpFragment)
        } catch (e: Exception) {
            Log.e("ProfilFragment", "Navigation to help failed", e)
            Toast.makeText(requireContext(), "Fitur Bantuan akan segera hadir", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showLogoutConfirmationDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle("Konfirmasi Keluar")
            .setMessage("Apakah Anda yakin ingin keluar dari aplikasi?")
            .setPositiveButton("Ya") { _, _ ->
                performLogout()
            }
            .setNegativeButton("Batal") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    private fun performLogout() {
        try {
            // Sign out from Firebase
            auth.signOut()

            // Show success message
            Toast.makeText(requireContext(), "Berhasil keluar", Toast.LENGTH_SHORT).show()

            // Navigate to home (which will show login screen if not logged in)
            navigateToHome()

        } catch (e: Exception) {
            Log.e("ProfilFragment", "Error during logout", e)
            Toast.makeText(requireContext(), "Gagal keluar: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun redirectToLogin() {
        try {
            // Navigate to home fragment which will handle login redirect
            findNavController().navigate(R.id.navigation_home)
        } catch (e: Exception) {
            Log.e("ProfilFragment", "Error redirecting to login", e)
            try {
                // Fallback navigation
                findNavController().navigate(R.id.navigation_home)
            } catch (e2: Exception) {
                Log.e("ProfilFragment", "Fallback navigation also failed", e2)
            }
        }
    }

    private fun navigateToHome() {
        try {
            // Clear back stack and navigate to home
            findNavController().popBackStack(R.id.navigation_home, false)
        } catch (e: Exception) {
            try {
                // Alternative navigation
                findNavController().navigate(R.id.navigation_home)
            } catch (e2: Exception) {
                try {
                    // Another fallback
                    findNavController().navigate(R.id.navigation_home)
                } catch (e3: Exception) {
                    Log.e("ProfilFragment", "All navigation attempts to home failed", e3)
                    // Last resort - try to restart the activity
                    requireActivity().finish()
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Refresh data when fragment becomes visible
        val currentUser = auth.currentUser
        if (currentUser != null) {
            loadUserData()
            loadUserStatistics()
        } else {
            redirectToLogin()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}