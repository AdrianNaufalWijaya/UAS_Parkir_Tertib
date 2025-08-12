package com.example.parkirtertib.login

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.parkirtertib.R
import com.example.parkirtertib.databinding.FragmentLupaPasswordBinding
import com.google.firebase.auth.FirebaseAuth

class LupaPasswordFragment : Fragment() {

    private var _binding: FragmentLupaPasswordBinding? = null
    private val binding get() = _binding!!
    private lateinit var auth: FirebaseAuth

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLupaPasswordBinding.inflate(inflater, container, false)
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

        // Reset password button
        binding.btnResetPassword.setOnClickListener {
            val email = binding.editEmailReset.text.toString().trim()

            if (validateEmail(email)) {
                sendPasswordResetEmail(email)
            }
        }

        // Back to login link
        binding.textBackToLogin.setOnClickListener {
            findNavController().navigate(R.id.action_loginFragment_to_lupaPasswordFragment)
        }
    }

    private fun validateEmail(email: String): Boolean {
        if (email.isEmpty()) {
            binding.editEmailReset.error = "Email tidak boleh kosong"
            binding.editEmailReset.requestFocus()
            return false
        }

        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.editEmailReset.error = "Format email tidak valid"
            binding.editEmailReset.requestFocus()
            return false
        }

        return true
    }

    private fun sendPasswordResetEmail(email: String) {
        // Show loading state
        binding.btnResetPassword.isEnabled = false
        binding.btnResetPassword.text = "Mengirim..."

        auth.sendPasswordResetEmail(email)
            .addOnCompleteListener { task ->
                // Reset button state
                binding.btnResetPassword.isEnabled = true
                binding.btnResetPassword.text = "KIRIM LINK RESET"

                if (task.isSuccessful) {
                    Toast.makeText(
                        context,
                        "Link reset password telah dikirim ke $email. Silakan cek email Anda.",
                        Toast.LENGTH_LONG
                    ).show()

                    // Clear the email field
                    binding.editEmailReset.text?.clear()

                    // Navigate back to login after successful reset
                    findNavController().navigate(R.id.action_loginFragment_to_lupaPasswordFragment)
                } else {
                    val errorMessage = when (task.exception?.message) {
                        "There is no user record corresponding to this identifier. The user may have been deleted." -> "Email tidak terdaftar dalam sistem"
                        "The email address is badly formatted." -> "Format email tidak valid"
                        "A network error (such as timeout, interrupted connection or unreachable host) has occurred." -> "Periksa koneksi internet Anda"
                        else -> "Gagal mengirim email reset: ${task.exception?.message}"
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