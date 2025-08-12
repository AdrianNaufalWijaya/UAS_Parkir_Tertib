package com.example.parkirtertib.login

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.parkirtertib.R
import com.example.parkirtertib.databinding.FragmentLoginBinding
import com.google.firebase.auth.FirebaseAuth

class LoginFragment : Fragment() {

    private var _binding: FragmentLoginBinding? = null
    private val binding get() = _binding!!
    private lateinit var auth: FirebaseAuth

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLoginBinding.inflate(inflater, container, false)
        auth = FirebaseAuth.getInstance()
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

        // Login button
        binding.btnLogin.setOnClickListener {
            val email = binding.editEmail.text.toString().trim()
            val password = binding.editPassword.text.toString().trim()

            if (validateInput(email, password)) {
                performLogin(email, password)
            }
        }

        // Register link
        binding.textRegister.setOnClickListener {
            findNavController().navigate(R.id.action_loginFragment_to_registerFragment)
        }

        // Forgot password link
        binding.textLupaPassword.setOnClickListener {
            findNavController().navigate(R.id.action_loginFragment_to_lupaPasswordFragment)
        }
    }

    private fun validateInput(email: String, password: String): Boolean {
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

        return true
    }

    private fun performLogin(email: String, password: String) {
        // Show loading state
        binding.btnLogin.isEnabled = false
        binding.btnLogin.text = "Masuk..."

        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                // Reset button state
                binding.btnLogin.isEnabled = true
                binding.btnLogin.text = "MASUK"

                if (task.isSuccessful) {
                    Toast.makeText(context, "Login berhasil! Selamat datang", Toast.LENGTH_SHORT).show()
                    // Navigate back to home
                    findNavController().navigate(R.id.action_loginFragment_to_homeFragment)
                } else {
                    val errorMessage = when (task.exception?.message) {
                        "The email address is badly formatted." -> "Format email tidak valid"
                        "There is no user record corresponding to this identifier. The user may have been deleted." -> "Email tidak terdaftar"
                        "The password is invalid or the user does not have a password." -> "Password salah"
                        "A network error (such as timeout, interrupted connection or unreachable host) has occurred." -> "Periksa koneksi internet Anda"
                        else -> "Login gagal: ${task.exception?.message}"
                    }
                    Toast.makeText(context, errorMessage, Toast.LENGTH_LONG).show()
                }
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}