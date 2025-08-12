package com.example.parkirtertib.login

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.parkirtertib.R
import com.example.parkirtertib.databinding.FragmentRegisterBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class RegisterFragment : Fragment() {
    private var _binding: FragmentRegisterBinding? = null
    private val binding get() = _binding!!
    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRegisterBinding.inflate(inflater, container, false)
        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupClickListeners()
    }

    private fun setupClickListeners() {
        // Back button
        binding.ivBack.setOnClickListener {
            findNavController().navigateUp()
        }

        // Register button
        binding.btnRegister.setOnClickListener {
            val name = binding.editName.text.toString().trim()
            val email = binding.editEmail.text.toString().trim()
            val password = binding.editPassword.text.toString().trim()
            val confirmPassword = binding.editConfirmPassword.text.toString().trim()

            if (validateInput(name, email, password, confirmPassword)) {
                performRegister(name, email, password)
            }
        }

        // Login link
        binding.textLogin.setOnClickListener {
            findNavController().navigate(R.id.action_registerFragment_to_loginFragment)
        }
    }

    private fun validateInput(name: String, email: String, password: String, confirmPassword: String): Boolean {
        if (name.isEmpty()) {
            binding.editName.error = "Nama tidak boleh kosong"
            binding.editName.requestFocus()
            return false
        }

        if (name.length < 2) {
            binding.editName.error = "Nama minimal 2 karakter"
            binding.editName.requestFocus()
            return false
        }

        if (email.isEmpty()) {
            binding.editEmail.error = "Email tidak boleh kosong"
            binding.editEmail.requestFocus()
            return false
        }

        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.editEmail.error = "Format email tidak valid"
            binding.editEmail.requestFocus()
            return false
        }

        if (password.isEmpty()) {
            binding.editPassword.error = "Password tidak boleh kosong"
            binding.editPassword.requestFocus()
            return false
        }

        if (password.length < 6) {
            binding.editPassword.error = "Password minimal 6 karakter"
            binding.editPassword.requestFocus()
            return false
        }

        if (confirmPassword.isEmpty()) {
            binding.editConfirmPassword.error = "Konfirmasi password tidak boleh kosong"
            binding.editConfirmPassword.requestFocus()
            return false
        }

        if (password != confirmPassword) {
            binding.editConfirmPassword.error = "Password tidak sama"
            binding.editConfirmPassword.requestFocus()
            return false
        }

        return true
    }

    private fun performRegister(name: String, email: String, password: String) {
        // Show loading state
        binding.btnRegister.isEnabled = false
        binding.btnRegister.text = "Mendaftar..."

        auth.createUserWithEmailAndPassword(email, password)
            .addOnSuccessListener { authResult ->
                // Save additional user data to Firestore
                val userId = authResult.user?.uid
                if (userId != null) {
                    val userData = hashMapOf(
                        "name" to name,
                        "email" to email,
                        "createdAt" to System.currentTimeMillis(),
                        "vehicleType" to "",
                        "phoneNumber" to ""
                    )

                    firestore.collection("users").document(userId)
                        .set(userData)
                        .addOnSuccessListener {
                            // Reset button state
                            binding.btnRegister.isEnabled = true
                            binding.btnRegister.text = "DAFTAR SEKARANG"

                            Toast.makeText(requireContext(), "Registrasi berhasil! Selamat datang $name", Toast.LENGTH_SHORT).show()
                            findNavController().navigate(R.id.navigation_home)
                        }
                        .addOnFailureListener { e ->
                            // Reset button state
                            binding.btnRegister.isEnabled = true
                            binding.btnRegister.text = "DAFTAR SEKARANG"

                            Toast.makeText(requireContext(), "Gagal menyimpan data: ${e.message}", Toast.LENGTH_LONG).show()
                        }
                }
            }
            .addOnFailureListener { e ->
                // Reset button state
                binding.btnRegister.isEnabled = true
                binding.btnRegister.text = "DAFTAR SEKARANG"

                val errorMessage = when (e.message) {
                    "The email address is already in use by another account." -> "Email sudah terdaftar"
                    "The email address is badly formatted." -> "Format email tidak valid"
                    "A network error (such as timeout, interrupted connection or unreachable host) has occurred." -> "Periksa koneksi internet Anda"
                    else -> "Gagal daftar: ${e.message}"
                }
                Toast.makeText(requireContext(), errorMessage, Toast.LENGTH_LONG).show()
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}