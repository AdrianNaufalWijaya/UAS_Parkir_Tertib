package com.example.parkirtertib

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.parkirtertib.databinding.FragmentHomeBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        try {
            // Initialize Firebase
            auth = FirebaseAuth.getInstance()
            firestore = FirebaseFirestore.getInstance()

            setupUI()
            setupClickListeners()
            setupAuthButtons() // Add this new method

            Log.d("HomeFragment", "Fragment initialized successfully")
        } catch (e: Exception) {
            Log.e("HomeFragment", "Error in onViewCreated", e)
            Toast.makeText(requireContext(), "Terjadi kesalahan saat memuat", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupUI() {
        try {
            loadUserData()
            updateUIBasedOnAuthState()
        } catch (e: Exception) {
            Log.e("HomeFragment", "Error in setupUI", e)
        }
    }

    private fun updateUIBasedOnAuthState() {
        val currentUser = auth.currentUser

        try {
            // Check if the views exist in the layout
            val layoutNotLoggedIn = try {
                binding.layoutNotLoggedIn
            } catch (e: Exception) {
                null
            }

            val layoutLoggedIn = try {
                binding.layoutLoggedIn
            } catch (e: Exception) {
                null
            }

            if (currentUser != null) {
                // User is logged in
                layoutNotLoggedIn?.visibility = View.GONE
                layoutLoggedIn?.visibility = View.VISIBLE
                Log.d("HomeFragment", "Showing logged in UI")
            } else {
                // User is not logged in
                layoutNotLoggedIn?.visibility = View.VISIBLE
                layoutLoggedIn?.visibility = View.GONE
                Log.d("HomeFragment", "Showing not logged in UI")
            }
        } catch (e: Exception) {
            Log.e("HomeFragment", "Error updating UI based on auth state", e)
        }
    }

    private fun setupAuthButtons() {
        try {
            // Setup Login Button (for not logged in users)
            try {
                binding.btnLogin.setOnClickListener {
                    Log.d("HomeFragment", "Login button clicked")
                    navigateToLogin()
                }
            } catch (e: Exception) {
                Log.e("HomeFragment", "btnLogin not found in layout", e)
            }

            // Setup Daftar/Register Button (for not logged in users)
            try {
                binding.btnDaftar.setOnClickListener {
                    Log.d("HomeFragment", "Daftar button clicked")
                    navigateToRegister()
                }
            } catch (e: Exception) {
                Log.e("HomeFragment", "btnDaftar not found in layout", e)
            }

            // Setup Lupa Password Button
            try {
                binding.btnLupaPassword.setOnClickListener {
                    Log.d("HomeFragment", "Lupa Password button clicked")
                    navigateToForgotPassword()
                }
            } catch (e: Exception) {
                Log.e("HomeFragment", "btnLupaPassword not found in layout", e)
            }

        } catch (e: Exception) {
            Log.e("HomeFragment", "Error setting up auth buttons", e)
        }
    }

    private fun navigateToLogin() {
        try {
            findNavController().navigate(R.id.action_homeFragment_to_loginFragment)
            Log.d("HomeFragment", "Navigating to LoginFragment")
        } catch (e: Exception) {
            Log.e("HomeFragment", "Navigation to login failed", e)
            // Try alternative navigation
            try {
                findNavController().navigate(R.id.loginFragment)
            } catch (e2: Exception) {
                Toast.makeText(requireContext(), "Tidak dapat membuka halaman login", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun navigateToRegister() {
        try {
            findNavController().navigate(R.id.action_homeFragment_to_registerFragment)
            Log.d("HomeFragment", "Navigating to RegisterFragment")
        } catch (e: Exception) {
            Log.e("HomeFragment", "Navigation to register failed", e)
            // Try alternative navigation
            try {
                findNavController().navigate(R.id.registerFragment)
            } catch (e2: Exception) {
                Toast.makeText(requireContext(), "Tidak dapat membuka halaman pendaftaran", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun navigateToForgotPassword() {
        try {
            findNavController().navigate(R.id.action_homeFragment_to_lupaPasswordFragment)
            Log.d("HomeFragment", "Navigating to LupaPasswordFragment")
        } catch (e: Exception) {
            Log.e("HomeFragment", "Navigation to forgot password failed", e)
            Toast.makeText(requireContext(), "Fitur lupa password akan segera hadir", Toast.LENGTH_SHORT).show()
        }
    }

    private fun loadUserData() {
        val currentUser = auth.currentUser
        if (currentUser != null) {
            val displayName = currentUser.displayName ?: "Pengguna"
            val email = currentUser.email ?: ""

            Log.d("HomeFragment", "User logged in: $email")

            // Update UI dengan data user jika view tersedia
            updateUserInfoViews(displayName, email)

            // Load additional data from Firestore
            loadUserDataFromFirestore(currentUser.uid)
        } else {
            Log.d("HomeFragment", "No user logged in")
            // Set default greeting untuk user yang belum login
            setDefaultGreeting()
        }
    }

    private fun updateUserInfoViews(displayName: String, email: String) {
        try {
            // Try to update tvUserEmail if it exists (for logged in layout)
            try {
                binding.tvUserEmail.text = email
                Log.d("HomeFragment", "Updated email: $email")
            } catch (e: Exception) {
                Log.d("HomeFragment", "tvUserEmail not found in layout")
            }

            // Coba update berbagai kemungkinan ID untuk nama user
            val possibleUserNameIds = listOf("tvUserName", "tv_user_name", "textUserName", "tvGreeting")
            val possibleEmailIds = listOf("tvUserEmail", "tv_user_email", "textUserEmail")

            // Update nama user - gunakan reflection untuk mencoba berbagai ID
            updateViewText(possibleUserNameIds, "Halo, $displayName!")
            updateViewText(possibleEmailIds, email)

        } catch (e: Exception) {
            Log.e("HomeFragment", "Error updating user info views", e)
        }
    }

    private fun updateViewText(possibleIds: List<String>, text: String) {
        for (id in possibleIds) {
            try {
                val field = binding::class.java.getDeclaredField(id)
                field.isAccessible = true
                val textView = field.get(binding) as? android.widget.TextView
                textView?.text = text
                Log.d("HomeFragment", "Updated $id with: $text")
                return // Exit after first successful update
            } catch (e: Exception) {
                // Continue to next possible ID
                continue
            }
        }
    }

    private fun setDefaultGreeting() {
        val currentHour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
        val greeting = when (currentHour) {
            in 5..10 -> "Selamat Pagi!"
            in 11..14 -> "Selamat Siang!"
            in 15..17 -> "Selamat Sore!"
            else -> "Selamat Malam!"
        }

        val possibleGreetingIds = listOf("tvGreeting", "tv_greeting", "textGreeting", "tvUserName")
        updateViewText(possibleGreetingIds, greeting)
    }

    private fun loadUserDataFromFirestore(userId: String) {
        firestore.collection("users").document(userId)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val userData = document.data
                    val name = userData?.get("name") as? String ?: "Pengguna"

                    // Update display name if we got it from Firestore
                    updateUserInfoViews(name, auth.currentUser?.email ?: "")

                    Log.d("HomeFragment", "User data loaded from Firestore")
                }
            }
            .addOnFailureListener { exception ->
                Log.e("HomeFragment", "Error loading user data", exception)
            }
    }

    private fun setupClickListeners() {
        try {
            // Daftar semua kemungkinan ID untuk tombol/card
            setupButtonClickListeners()
            setupCardClickListeners()
            setupBottomNavigation()

        } catch (e: Exception) {
            Log.e("HomeFragment", "Error setting up click listeners", e)
        }
    }

    private fun setupButtonClickListeners() {
        // Tombol masuk parkir (for logged in users)
        val masukParkirIds = listOf("btnMasukParkir", "btn_masuk_parkir", "buttonMasukParkir")
        setupClickListener(masukParkirIds) {
            handleMasukParkirClick()
        }

        // Tombol profil
        val profilIds = listOf("btnProfile", "btn_profile", "buttonProfile")
        setupClickListener(profilIds) {
            navigateToProfile()
        }

        // Tombol riwayat
        val riwayatIds = listOf("btnRiwayat", "btn_riwayat", "buttonRiwayat")
        setupClickListener(riwayatIds) {
            navigateToRiwayat()
        }

        // Tombol bantuan
        val bantuanIds = listOf("btnBantuan", "btn_bantuan", "buttonBantuan")
        setupClickListener(bantuanIds) {
            navigateToHelp()
        }

        // Tombol pengaturan
        val pengaturanIds = listOf("btnPengaturan", "btn_pengaturan", "buttonPengaturan")
        setupClickListener(pengaturanIds) {
            navigateToSettings()
        }
    }

    private fun setupCardClickListeners() {
        // CardView masuk parkir
        val cardMasukIds = listOf("cvMasukParkir", "cv_masuk_parkir", "cardMasukParkir", "llMasukParkir")
        setupClickListener(cardMasukIds) {
            handleMasukParkirClick()
        }

        // CardView profil
        val cardProfilIds = listOf("cvProfile", "cv_profile", "cardProfile", "llProfile", "cardProfile")
        setupClickListener(cardProfilIds) {
            navigateToProfile()
        }

        // CardView riwayat
        val cardRiwayatIds = listOf("cvRiwayat", "cv_riwayat", "cardRiwayat", "llRiwayat", "cardRiwayat")
        setupClickListener(cardRiwayatIds) {
            navigateToRiwayat()
        }

        // CardView bantuan
        val cardBantuanIds = listOf("cvBantuan", "cv_bantuan", "cardBantuan", "llBantuan", "cardBantuan")
        setupClickListener(cardBantuanIds) {
            navigateToHelp()
        }

        // CardView pengaturan
        val cardPengaturanIds = listOf("cvPengaturan", "cv_pengaturan", "cardPengaturan", "llPengaturan", "cardPengaturan")
        setupClickListener(cardPengaturanIds) {
            navigateToSettings()
        }
    }

    private fun setupClickListener(possibleIds: List<String>, action: () -> Unit) {
        for (id in possibleIds) {
            try {
                val field = binding::class.java.getDeclaredField(id)
                field.isAccessible = true
                val view = field.get(binding) as? View
                view?.setOnClickListener {
                    action()
                }
                Log.d("HomeFragment", "Click listener set for: $id")
            } catch (e: Exception) {
                // Continue to next possible ID
                continue
            }
        }
    }

    private fun handleMasukParkirClick() {
        val currentUser = auth.currentUser
        if (currentUser != null) {
            // User sudah login, bisa masuk ke form parkir
            try {
                findNavController().navigate(R.id.action_homeFragment_to_formMasukFragment)
            } catch (e: Exception) {
                Log.e("HomeFragment", "Navigation to form masuk failed", e)
                Toast.makeText(requireContext(), "Tidak dapat membuka form parkir", Toast.LENGTH_SHORT).show()
            }
        } else {
            // User belum login, arahkan ke login
            Toast.makeText(requireContext(), "Silakan login terlebih dahulu", Toast.LENGTH_SHORT).show()
            navigateToLogin()
        }
    }

    private fun setupBottomNavigation() {
        try {
            val bottomNavIds = listOf("bottomNavigation", "bottom_navigation", "navView")

            for (id in bottomNavIds) {
                try {
                    val field = binding::class.java.getDeclaredField(id)
                    field.isAccessible = true
                    val bottomNav = field.get(binding) as? com.google.android.material.bottomnavigation.BottomNavigationView

                    bottomNav?.setOnItemSelectedListener { item ->
                        handleBottomNavigation(item.itemId)
                    }

                    Log.d("HomeFragment", "Bottom navigation setup successful for: $id")
                    return
                } catch (e: Exception) {
                    continue
                }
            }
        } catch (e: Exception) {
            Log.e("HomeFragment", "Error setting up bottom navigation", e)
        }
    }

    private fun handleBottomNavigation(itemId: Int): Boolean {
        return try {
            when (itemId) {
                R.id.navigation_home -> true // Already on home
                R.id.navigation_riwayat -> {
                    navigateToRiwayat()
                    true
                }
                R.id.navigation_profil -> {
                    navigateToProfile()
                    true
                }
                else -> false
            }
        } catch (e: Exception) {
            Log.e("HomeFragment", "Error handling bottom navigation", e)
            false
        }
    }

    private fun navigateToProfile() {
        val currentUser = auth.currentUser
        if (currentUser != null) {
            try {
                findNavController().navigate(R.id.action_homeFragment_to_profileFragment)
            } catch (e: Exception) {
                Log.e("HomeFragment", "Navigation to profile failed", e)
                Toast.makeText(requireContext(), "Tidak dapat membuka profil", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(requireContext(), "Silakan login untuk melihat profil", Toast.LENGTH_SHORT).show()
            navigateToLogin()
        }
    }

    private fun navigateToRiwayat() {
        val currentUser = auth.currentUser
        if (currentUser != null) {
            try {
                findNavController().navigate(R.id.action_homeFragment_to_riwayatFragment)
            } catch (e: Exception) {
                Log.e("HomeFragment", "Navigation to riwayat failed", e)
                Toast.makeText(requireContext(), "Tidak dapat membuka riwayat", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(requireContext(), "Silakan login untuk melihat riwayat", Toast.LENGTH_SHORT).show()
            navigateToLogin()
        }
    }

    private fun navigateToSettings() {
        try {
            findNavController().navigate(R.id.action_homeFragment_to_settingsFragment)
        } catch (e: Exception) {
            Log.e("HomeFragment", "Navigation to settings failed", e)
            Toast.makeText(requireContext(), "Fitur pengaturan akan segera hadir", Toast.LENGTH_SHORT).show()
        }
    }

    private fun navigateToHelp() {
        try {
            findNavController().navigate(R.id.action_homeFragment_to_helpFragment)
        } catch (e: Exception) {
            Log.e("HomeFragment", "Navigation to help failed", e)
            Toast.makeText(requireContext(), "Fitur bantuan akan segera hadir", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onResume() {
        super.onResume()
        Log.d("HomeFragment", "Fragment resumed")
        // Refresh user data when fragment becomes visible
        try {
            loadUserData()
            updateUIBasedOnAuthState()
        } catch (e: Exception) {
            Log.e("HomeFragment", "Error refreshing data", e)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        Log.d("HomeFragment", "Fragment destroyed")
        _binding = null
    }
}