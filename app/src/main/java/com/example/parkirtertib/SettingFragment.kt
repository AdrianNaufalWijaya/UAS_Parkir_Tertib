package com.example.parkirtertib

import android.app.AlertDialog
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.widget.SwitchCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.parkirtertib.databinding.FragmentSettingBinding
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class SettingsFragment : Fragment() {

    private var _binding: FragmentSettingBinding? = null
    private val binding get() = _binding!!

    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettingBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

        setupUI()
        setupClickListeners()
        loadUserPreferences()
    }

    private fun setupUI() {
        // Update email verification status
        val currentUser = auth.currentUser
        if (currentUser != null) {
            if (currentUser.isEmailVerified) {
                binding.tvEmailStatus.text = "Email sudah terverifikasi"
                binding.tvEmailStatus.setTextColor(resources.getColor(android.R.color.holo_green_dark, null))
            } else {
                binding.tvEmailStatus.text = "Email belum terverifikasi"
                binding.tvEmailStatus.setTextColor(resources.getColor(android.R.color.holo_orange_dark, null))
            }
        }
    }

    private fun setupClickListeners() {
        // Back button
        binding.btnBack.setOnClickListener {
            findNavController().navigateUp()
        }

        // Logout button
        binding.btnLogout.setOnClickListener {
            showLogoutDialog()
        }

        // Change Password
        binding.cardChangePassword.setOnClickListener {
            showChangePasswordDialog()
        }

        // Email Verification
        binding.cardEmailVerification.setOnClickListener {
            handleEmailVerification()
        }

        // Language Settings
        binding.cardLanguage.setOnClickListener {
            showLanguageDialog()
        }

        // Privacy Policy
        binding.cardPrivacy.setOnClickListener {
            Toast.makeText(requireContext(), "Membuka Kebijakan Privasi...", Toast.LENGTH_SHORT).show()
            // TODO: Open privacy policy webpage or fragment
        }

        // About
        binding.cardAbout.setOnClickListener {
            showAboutDialog()
        }

        // Push Notifications Switch
        binding.switchPushNotifications.setOnCheckedChangeListener { _, isChecked ->
            saveNotificationPreference("push_notifications", isChecked)
            val message = if (isChecked) "Notifikasi push diaktifkan" else "Notifikasi push dinonaktifkan"
            Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
        }

        // Email Notifications Switch
        binding.switchEmailNotifications.setOnCheckedChangeListener { _, isChecked ->
            saveNotificationPreference("email_notifications", isChecked)
            val message = if (isChecked) "Notifikasi email diaktifkan" else "Notifikasi email dinonaktifkan"
            Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
        }
    }

    private fun loadUserPreferences() {
        val userId = auth.currentUser?.uid ?: return

        firestore.collection("users").document(userId)
            .collection("preferences")
            .document("settings")
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val pushEnabled = document.getBoolean("push_notifications") ?: true
                    val emailEnabled = document.getBoolean("email_notifications") ?: false

                    binding.switchPushNotifications.isChecked = pushEnabled
                    binding.switchEmailNotifications.isChecked = emailEnabled
                }
            }
            .addOnFailureListener { e ->
                Log.e("SettingsFragment", "Error loading preferences", e)
            }
    }

    private fun saveNotificationPreference(key: String, value: Boolean) {
        val userId = auth.currentUser?.uid ?: return

        val preferences = hashMapOf(
            key to value,
            "updated_at" to System.currentTimeMillis()
        )

        firestore.collection("users").document(userId)
            .collection("preferences")
            .document("settings")
            .set(preferences, com.google.firebase.firestore.SetOptions.merge())
            .addOnFailureListener { e ->
                Log.e("SettingsFragment", "Error saving preference", e)
                Toast.makeText(requireContext(), "Gagal menyimpan pengaturan", Toast.LENGTH_SHORT).show()
            }
    }

    private fun showChangePasswordDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_change_password, null)

        AlertDialog.Builder(requireContext())
            .setTitle("Ganti Password")
            .setView(dialogView)
            .setPositiveButton("Ganti") { dialog, _ ->
                // TODO: Implement password change logic
                val currentPassword = dialogView.findViewById<android.widget.EditText>(R.id.edit_current_password)?.text.toString()
                val newPassword = dialogView.findViewById<android.widget.EditText>(R.id.edit_new_password)?.text.toString()
                val confirmPassword = dialogView.findViewById<android.widget.EditText>(R.id.edit_confirm_password)?.text.toString()

                if (newPassword == confirmPassword && newPassword.length >= 6) {
                    changePassword(currentPassword, newPassword)
                } else {
                    Toast.makeText(requireContext(), "Password baru tidak valid atau tidak cocok", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Batal") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    private fun changePassword(currentPassword: String, newPassword: String) {
        val user = auth.currentUser
        if (user != null && user.email != null) {
            val credential = EmailAuthProvider.getCredential(user.email!!, currentPassword)

            user.reauthenticate(credential)
                .addOnSuccessListener {
                    user.updatePassword(newPassword)
                        .addOnSuccessListener {
                            Toast.makeText(requireContext(), "Password berhasil diganti", Toast.LENGTH_SHORT).show()
                        }
                        .addOnFailureListener { e ->
                            Toast.makeText(requireContext(), "Gagal mengganti password: ${e.message}", Toast.LENGTH_LONG).show()
                        }
                }
                .addOnFailureListener { e ->
                    Toast.makeText(requireContext(), "Password lama salah", Toast.LENGTH_SHORT).show()
                }
        }
    }

    private fun handleEmailVerification() {
        val user = auth.currentUser
        if (user != null) {
            if (!user.isEmailVerified) {
                user.sendEmailVerification()
                    .addOnSuccessListener {
                        Toast.makeText(requireContext(), "Email verifikasi telah dikirim ke ${user.email}", Toast.LENGTH_LONG).show()
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(requireContext(), "Gagal mengirim email verifikasi: ${e.message}", Toast.LENGTH_LONG).show()
                    }
            } else {
                Toast.makeText(requireContext(), "Email sudah terverifikasi", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showLanguageDialog() {
        val languages = arrayOf("Bahasa Indonesia", "English", "中文")
        var selectedLanguage = 0 // Default to Bahasa Indonesia

        AlertDialog.Builder(requireContext())
            .setTitle("Pilih Bahasa")
            .setSingleChoiceItems(languages, selectedLanguage) { _, which ->
                selectedLanguage = which
            }
            .setPositiveButton("OK") { _, _ ->
                binding.tvCurrentLanguage.text = languages[selectedLanguage]
                Toast.makeText(requireContext(), "Bahasa diubah ke ${languages[selectedLanguage]}", Toast.LENGTH_SHORT).show()
                // TODO: Implement actual language change logic
                saveLanguagePreference(languages[selectedLanguage])
            }
            .setNegativeButton("Batal") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    private fun saveLanguagePreference(language: String) {
        val userId = auth.currentUser?.uid ?: return

        firestore.collection("users").document(userId)
            .update("language", language)
            .addOnFailureListener { e ->
                Log.e("SettingsFragment", "Error saving language preference", e)
            }
    }

    private fun showAboutDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle("Tentang Parkir Tertib")
            .setMessage("""
                Parkir Tertib v1.0.0
                
                Aplikasi manajemen parkir modern yang memudahkan Anda dalam:
                • Mencatat waktu masuk parkir
                • Menghitung biaya parkir otomatis
                • Melihat riwayat parkir
                • Mendapatkan notifikasi pengingat
                
                Dikembangkan dengan ❤️ untuk kemudahan Anda
                
                © 2025 oleh Adrian Naufal Wijaya
            """.trimIndent())
            .setPositiveButton("OK") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    private fun showLogoutDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle("Keluar Akun")
            .setMessage("Apakah Anda yakin ingin keluar dari akun?")
            .setPositiveButton("Ya") { _, _ ->
                logout()
            }
            .setNegativeButton("Batal") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    private fun logout() {
        try {
            // Sign out from Firebase
            auth.signOut()

            // Clear any cached data if needed
            clearUserCache()

            Toast.makeText(requireContext(), "Berhasil keluar", Toast.LENGTH_SHORT).show()

            // Navigate to home/login
            navigateToHome()

        } catch (e: Exception) {
            Log.e("SettingsFragment", "Error during logout", e)
            Toast.makeText(requireContext(), "Gagal keluar: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun clearUserCache() {
        // Clear any SharedPreferences or cached data
        val sharedPref = requireContext().getSharedPreferences("user_prefs", android.content.Context.MODE_PRIVATE)
        sharedPref.edit().clear().apply()
    }

    private fun navigateToHome() {
        try {
            // Try to navigate using action
            findNavController().navigate(R.id.action_settingsFragment_to_homeFragment)
        } catch (e: Exception) {
            try {
                // Fallback to direct navigation
                findNavController().popBackStack(R.id.navigation_home, false)
            } catch (e2: Exception) {
                try {
                    // Another fallback
                    findNavController().navigate(R.id.navigation_home)
                } catch (e3: Exception) {
                    Log.e("SettingsFragment", "All navigation attempts failed", e3)
                    // Last resort - finish activity
                    requireActivity().finish()
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}