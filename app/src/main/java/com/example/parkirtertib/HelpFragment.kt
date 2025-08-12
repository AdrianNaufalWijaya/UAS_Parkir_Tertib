package com.example.parkirtertib

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController

class HelpFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_help, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupClickListeners()
    }

    private fun setupClickListeners() {
        // Back button
        view?.findViewById<View>(R.id.btn_back)?.setOnClickListener {
            findNavController().navigateUp()
        }

        // FAQ button
        view?.findViewById<View>(R.id.btn_faq)?.setOnClickListener {
            Toast.makeText(requireContext(), "FAQ akan segera tersedia", Toast.LENGTH_SHORT).show()
        }

        // Contact support
        view?.findViewById<View>(R.id.btn_contact_support)?.setOnClickListener {
            contactSupport()
        }

        // Privacy policy
        view?.findViewById<View>(R.id.btn_privacy_policy)?.setOnClickListener {
            Toast.makeText(requireContext(), "Kebijakan privasi akan segera tersedia", Toast.LENGTH_SHORT).show()
        }

        // Terms of service
        view?.findViewById<View>(R.id.btn_terms_service)?.setOnClickListener {
            Toast.makeText(requireContext(), "Syarat layanan akan segera tersedia", Toast.LENGTH_SHORT).show()
        }

        // App version info
        view?.findViewById<View>(R.id.btn_app_version)?.setOnClickListener {
            showAppVersion()
        }
    }

    private fun contactSupport() {
        try {
            val intent = Intent(Intent.ACTION_SENDTO).apply {
                data = Uri.parse("mailto:support@parkirtertib.com")
                putExtra(Intent.EXTRA_SUBJECT, "Bantuan Parkir Tertib")
                putExtra(Intent.EXTRA_TEXT, "Halo, saya memerlukan bantuan terkait aplikasi Parkir Tertib...")
            }

            if (intent.resolveActivity(requireContext().packageManager) != null) {
                startActivity(intent)
            } else {
                Toast.makeText(
                    requireContext(),
                    "Tidak dapat membuka aplikasi email",
                    Toast.LENGTH_SHORT
                ).show()
            }
        } catch (e: Exception) {
            Toast.makeText(
                requireContext(),
                "Terjadi kesalahan saat membuka email",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun showAppVersion() {
        try {
            val packageInfo = requireContext().packageManager.getPackageInfo(
                requireContext().packageName, 0
            )
            val version = packageInfo.versionName
            Toast.makeText(
                requireContext(),
                "Parkir Tertib v$version",
                Toast.LENGTH_LONG
            ).show()
        } catch (e: Exception) {
            Toast.makeText(
                requireContext(),
                "Tidak dapat menampilkan versi aplikasi",
                Toast.LENGTH_SHORT
            ).show()
        }
    }
}