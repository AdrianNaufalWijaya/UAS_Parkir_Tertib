package com.example.parkirtertib

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.parkirtertib.databinding.ActivityRegisterBinding
import com.google.firebase.auth.FirebaseAuth

class RegisterActivity : AppCompatActivity() {
    private lateinit var binding: ActivityRegisterBinding
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()

        binding.btnRegister.setOnClickListener {
            val email = binding.editEmail.text.toString()
            val password = binding.editPassword.text.toString()

            if (email.isNotBlank() && password.length >= 6) {
                auth.createUserWithEmailAndPassword(email, password)
                    .addOnSuccessListener {
                        Toast.makeText(this, "Registrasi berhasil", Toast.LENGTH_SHORT).show()
                        startActivity(Intent(this, MainActivity::class.java))
                        finish()
                    }
                    .addOnFailureListener {
                        Toast.makeText(this, "Gagal daftar: ${it.message}", Toast.LENGTH_LONG).show()
                    }
            } else {
                Toast.makeText(this, "Isi email & password (min 6 karakter)", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
