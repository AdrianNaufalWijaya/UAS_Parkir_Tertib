package com.example.parkirtertib

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.app.ProgressDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.example.parkirtertib.databinding.FragmentEditProfilBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

class EditProfileFragment : Fragment() {

    private var _binding: FragmentEditProfilBinding? = null
    private val binding get() = _binding!!

    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore
    private lateinit var storage: FirebaseStorage

    private var selectedImageUri: Uri? = null
    private var currentPhotoPath: String? = null
    private var hasUnsavedChanges = false
    private var progressDialog: ProgressDialog? = null

    // Permission launcher untuk kamera dan storage
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val cameraGranted = permissions[Manifest.permission.CAMERA] ?: false
        val storageGranted = when {
            android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU -> {
                permissions[Manifest.permission.READ_MEDIA_IMAGES] ?: false
            }
            else -> {
                permissions[Manifest.permission.READ_EXTERNAL_STORAGE] ?: false
            }
        }

        when {
            cameraGranted && storageGranted -> showImagePickerDialog()
            cameraGranted -> openCamera()
            storageGranted -> openGallery()
            else -> {
                Toast.makeText(
                    requireContext(),
                    "Izin diperlukan untuk mengubah foto profil",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    // Gallery image picker
    private val pickImageFromGallery = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            selectedImageUri = it
            loadImageIntoView(it)
            hasUnsavedChanges = true
        }
    }

    // Camera image capture dengan URI yang lebih baik
    private val takePictureFromCamera = registerForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            selectedImageUri?.let { uri ->
                loadImageIntoView(uri)
                hasUnsavedChanges = true
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentEditProfilBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize Firebase
        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()
        storage = FirebaseStorage.getInstance()

        setupUI()
        loadCurrentUserData()
        setupClickListeners()
    }

    private fun setupUI() {
        // Set up text watchers untuk mendeteksi perubahan
        binding.apply {
            etDisplayName.addTextChangedListener(createTextWatcher())
            etUsername.addTextChangedListener(createTextWatcher())
            etEmail.addTextChangedListener(createTextWatcher())
            etPhone.addTextChangedListener(createTextWatcher())
        }
    }

    private fun createTextWatcher(): TextWatcher {
        return object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                hasUnsavedChanges = true
            }
        }
    }

    private fun loadCurrentUserData() {
        val currentUser = auth.currentUser
        if (currentUser != null) {
            binding.apply {
                etDisplayName.setText(currentUser.displayName ?: "")
                etEmail.setText(currentUser.email ?: "")

                // Muat foto profil dari Firebase Auth
                currentUser.photoUrl?.let { uri ->
                    loadImageIntoView(uri)
                }
            }

            // Muat data tambahan dari Firestore
            loadUserDataFromFirestore(currentUser.uid)
        }
    }

    private fun loadUserDataFromFirestore(userId: String) {
        firestore.collection("users").document(userId)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val userData = document.data
                    userData?.let {
                        binding.apply {
                            // Load sesuai struktur database yang ada
                            etDisplayName.setText(it["displayName"] as? String ?: "")
                            etUsername.setText(it["name"] as? String ?: "")
                            etEmail.setText(it["email"] as? String ?: "")
                            etPhone.setText(it["phoneNumber"] as? String ?: "")

                            // Prioritaskan foto dari Firestore jika ada
                            val profilePhotoUrl = it["profilePhotoUrl"] as? String
                            if (!profilePhotoUrl.isNullOrEmpty()) {
                                loadImageIntoView(Uri.parse(profilePhotoUrl))
                            }
                        }
                    }
                }
            }
            .addOnFailureListener { exception ->
                Log.e("EditProfileFragment", "Error loading user data", exception)
            }
    }

    private fun setupClickListeners() {
        binding.apply {
            // Tombol kembali
            btnBack.setOnClickListener {
                handleBackPress()
            }

            // Tombol ubah foto - bisa klik icon atau foto langsung
            btnChangePhoto.setOnClickListener {
                checkPermissionAndPickImage()
            }

            // Tambahan: foto profil juga bisa diklik
            ivProfilePhoto.setOnClickListener {
                checkPermissionAndPickImage()
            }

            // Tombol simpan
            btnSaveProfile.setOnClickListener {
                if (validateForm()) {
                    saveProfileChanges()
                }
            }

            // Tombol batal
            btnCancel.setOnClickListener {
                handleBackPress()
            }
        }
    }

    private fun checkPermissionAndPickImage() {
        val permissions = mutableListOf<String>()

        // Check camera permission
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA)
            != PackageManager.PERMISSION_GRANTED) {
            permissions.add(Manifest.permission.CAMERA)
        }

        // Check storage permission based on Android version
        val storagePermission = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_IMAGES
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }

        if (ContextCompat.checkSelfPermission(requireContext(), storagePermission)
            != PackageManager.PERMISSION_GRANTED) {
            permissions.add(storagePermission)
        }

        if (permissions.isEmpty()) {
            // All permissions granted
            showImagePickerDialog()
        } else {
            // Request permissions
            requestPermissionLauncher.launch(permissions.toTypedArray())
        }
    }

    private fun showImagePickerDialog() {
        val options = arrayOf("📷 Ambil Foto", "🖼️ Pilih dari Galeri", "❌ Hapus Foto")
        val builder = AlertDialog.Builder(requireContext())
            .setTitle("Ubah Foto Profil")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> openCamera()
                    1 -> openGallery()
                    2 -> removeProfilePhoto()
                }
            }
            .setNegativeButton("Batal", null)

        builder.show()
    }

    private fun openCamera() {
        try {
            // Buat file untuk menyimpan foto
            val photoFile = createImageFile()
            photoFile?.let {
                selectedImageUri = FileProvider.getUriForFile(
                    requireContext(),
                    "${requireContext().packageName}.fileprovider",
                    it
                )
                takePictureFromCamera.launch(selectedImageUri)
            }
        } catch (e: Exception) {
            Log.e("EditProfileFragment", "Error opening camera", e)
            // Fallback ke intent biasa jika FileProvider gagal
            openCameraFallback()
        }
    }

    private fun openCameraFallback() {
        // Buat URI sementara untuk foto
        val values = android.content.ContentValues()
        values.put(MediaStore.Images.Media.TITLE, "Profile_Photo_${System.currentTimeMillis()}")
        values.put(MediaStore.Images.Media.DESCRIPTION, "Profile photo from camera")

        selectedImageUri = requireContext().contentResolver.insert(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values
        )

        selectedImageUri?.let {
            takePictureFromCamera.launch(it)
        } ?: run {
            Toast.makeText(requireContext(), "Tidak dapat membuka kamera", Toast.LENGTH_SHORT).show()
        }
    }

    @Throws(IOException::class)
    private fun createImageFile(): File? {
        return try {
            // Create an image file name
            val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val imageFileName = "JPEG_${timeStamp}_"
            val storageDir = requireContext().getExternalFilesDir(android.os.Environment.DIRECTORY_PICTURES)

            File.createTempFile(
                imageFileName,
                ".jpg",
                storageDir
            ).apply {
                currentPhotoPath = absolutePath
            }
        } catch (e: Exception) {
            Log.e("EditProfileFragment", "Error creating image file", e)
            null
        }
    }

    private fun openGallery() {
        pickImageFromGallery.launch("image/*")
    }

    private fun removeProfilePhoto() {
        AlertDialog.Builder(requireContext())
            .setTitle("Hapus Foto Profil")
            .setMessage("Apakah Anda yakin ingin menghapus foto profil?")
            .setPositiveButton("Hapus") { _, _ ->
                binding.ivProfilePhoto.setImageResource(android.R.drawable.ic_menu_gallery)
                selectedImageUri = null
                hasUnsavedChanges = true

                // Set flag untuk menghapus foto dari storage saat save
                binding.ivProfilePhoto.tag = "remove_photo"
            }
            .setNegativeButton("Batal", null)
            .show()
    }

    private fun loadImageIntoView(uri: Uri) {
        try {
            Glide.with(this)
                .load(uri)
                .apply(RequestOptions.circleCropTransform())
                .placeholder(android.R.drawable.ic_menu_gallery)
                .error(android.R.drawable.ic_menu_gallery)
                .into(binding.ivProfilePhoto)

            // Clear remove flag if any
            binding.ivProfilePhoto.tag = null

            // Tampilkan toast sukses
            Toast.makeText(requireContext(), "Foto berhasil dipilih", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Log.e("EditProfileFragment", "Error loading image", e)
            Toast.makeText(requireContext(), "Gagal memuat foto", Toast.LENGTH_SHORT).show()
        }
    }

    private fun validateForm(): Boolean {
        binding.apply {
            // Clear previous errors
            etDisplayName.error = null
            etEmail.error = null
            etPhone.error = null

            // Validate display name
            if (etDisplayName.text.toString().trim().isEmpty()) {
                etDisplayName.error = "Nama lengkap tidak boleh kosong"
                etDisplayName.requestFocus()
                return false
            }

            // Validate email
            val email = etEmail.text.toString().trim()
            if (email.isEmpty()) {
                etEmail.error = "Email tidak boleh kosong"
                etEmail.requestFocus()
                return false
            }
            if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                etEmail.error = "Format email tidak valid"
                etEmail.requestFocus()
                return false
            }

            // Validate phone if filled
            val phone = etPhone.text.toString().trim()
            if (phone.isNotEmpty() && phone.length < 10) {
                etPhone.error = "Nomor telepon tidak valid (minimal 10 digit)"
                etPhone.requestFocus()
                return false
            }
        }
        return true
    }

    private fun saveProfileChanges() {
        showLoadingDialog()

        val currentUser = auth.currentUser
        if (currentUser == null) {
            hideLoadingDialog()
            Toast.makeText(requireContext(), "Pengguna tidak ditemukan", Toast.LENGTH_SHORT).show()
            return
        }

        // Check if photo should be removed
        if (binding.ivProfilePhoto.tag == "remove_photo") {
            // Remove photo from storage
            removePhotoFromStorage { success ->
                if (success) {
                    updateUserProfile(null)
                } else {
                    hideLoadingDialog()
                    Toast.makeText(requireContext(), "Gagal menghapus foto", Toast.LENGTH_SHORT).show()
                }
            }
        } else if (selectedImageUri != null) {
            // Upload new photo
            uploadProfilePhoto { photoUrl ->
                if (photoUrl != null) {
                    updateUserProfile(photoUrl)
                } else {
                    hideLoadingDialog()
                    Toast.makeText(requireContext(), "Gagal mengunggah foto", Toast.LENGTH_SHORT).show()
                }
            }
        } else {
            // No photo changes
            updateUserProfile(null)
        }
    }

    private fun removePhotoFromStorage(onComplete: (Boolean) -> Unit) {
        val currentUser = auth.currentUser ?: run {
            onComplete(false)
            return
        }

        // List dan delete semua foto profil user di folder mereka
        val storageRef = storage.reference.child("foto_profil/${currentUser.uid}")

        storageRef.listAll()
            .addOnSuccessListener { listResult ->
                if (listResult.items.isEmpty()) {
                    onComplete(true)
                } else {
                    // Delete semua foto profil lama
                    val deleteTasks = listResult.items.map { it.delete() }

                    com.google.android.gms.tasks.Tasks.whenAllComplete(deleteTasks)
                        .addOnSuccessListener {
                            onComplete(true)
                        }
                        .addOnFailureListener {
                            onComplete(false)
                        }
                }
            }
            .addOnFailureListener {
                // Folder mungkin tidak ada, tetap anggap sukses
                onComplete(true)
            }
    }

    private fun uploadProfilePhoto(onComplete: (String?) -> Unit) {
        val currentUser = auth.currentUser ?: run {
            onComplete(null)
            return
        }

        // Path sesuai dengan Storage Rules: foto_profil/{userId}/profile_{timestamp}.jpg
        val fileName = "profile_${System.currentTimeMillis()}.jpg"
        val storageRef = storage.reference.child("foto_profil/${currentUser.uid}/$fileName")

        selectedImageUri?.let { uri ->
            // Compress image before upload
            try {
                val bitmap = MediaStore.Images.Media.getBitmap(requireContext().contentResolver, uri)
                val baos = ByteArrayOutputStream()

                // Compress to max 1MB
                var quality = 100
                do {
                    baos.reset()
                    bitmap.compress(Bitmap.CompressFormat.JPEG, quality, baos)
                    quality -= 10
                } while (baos.toByteArray().size > 1024 * 1024 && quality > 10)

                val data = baos.toByteArray()

                // Upload compressed image
                val uploadTask = storageRef.putBytes(data)

                uploadTask.addOnProgressListener { taskSnapshot ->
                    val progress = (100.0 * taskSnapshot.bytesTransferred / taskSnapshot.totalByteCount).toInt()
                    progressDialog?.setMessage("Mengunggah foto... $progress%")
                }
                    .addOnSuccessListener {
                        storageRef.downloadUrl.addOnSuccessListener { downloadUri ->
                            onComplete(downloadUri.toString())
                        }.addOnFailureListener { exception ->
                            Log.e("EditProfileFragment", "Error getting download URL", exception)
                            onComplete(null)
                        }
                    }
                    .addOnFailureListener { exception ->
                        Log.e("EditProfileFragment", "Error uploading photo", exception)
                        onComplete(null)
                    }

            } catch (e: Exception) {
                Log.e("EditProfileFragment", "Error compressing image", e)
                onComplete(null)
            }
        } ?: onComplete(null)
    }

    private fun updateUserProfile(photoUrl: String?) {
        val currentUser = auth.currentUser ?: return

        // Update Firebase Auth profile
        val profileUpdates = UserProfileChangeRequest.Builder()
            .setDisplayName(binding.etDisplayName.text.toString().trim())

        if (binding.ivProfilePhoto.tag == "remove_photo") {
            profileUpdates.setPhotoUri(null)
        } else if (photoUrl != null) {
            profileUpdates.setPhotoUri(Uri.parse(photoUrl))
        }

        currentUser.updateProfile(profileUpdates.build())
            .addOnSuccessListener {
                // Update email if changed
                val newEmail = binding.etEmail.text.toString().trim()
                if (newEmail != currentUser.email) {
                    updateUserEmail(newEmail, photoUrl)
                } else {
                    updateFirestoreData(photoUrl)
                }
            }
            .addOnFailureListener { exception ->
                hideLoadingDialog()
                Log.e("EditProfileFragment", "Error updating profile", exception)
                Toast.makeText(
                    requireContext(),
                    "Gagal memperbarui profil: ${exception.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
    }

    private fun updateUserEmail(newEmail: String, photoUrl: String?) {
        val currentUser = auth.currentUser ?: return

        currentUser.updateEmail(newEmail)
            .addOnSuccessListener {
                updateFirestoreData(photoUrl)
            }
            .addOnFailureListener { exception ->
                hideLoadingDialog()
                Log.e("EditProfileFragment", "Error updating email", exception)

                // Show specific error message
                val errorMessage = when {
                    exception.message?.contains("requires recent authentication") == true -> {
                        "Silakan login ulang untuk mengubah email"
                    }
                    exception.message?.contains("already in use") == true -> {
                        "Email sudah digunakan akun lain"
                    }
                    else -> "Gagal mengubah email: ${exception.message}"
                }

                Toast.makeText(requireContext(), errorMessage, Toast.LENGTH_LONG).show()
            }
    }

    private fun updateFirestoreData(photoUrl: String?) {
        val currentUser = auth.currentUser ?: return

        val userData = hashMapOf<String, Any>(
            "displayName" to binding.etDisplayName.text.toString().trim(),
            "name" to binding.etUsername.text.toString().trim(),
            "phoneNumber" to binding.etPhone.text.toString().trim(),
            "email" to binding.etEmail.text.toString().trim(),
            "language" to "English", // Default language
            "updatedAt" to Date()
        )

        // Handle photo URL
        if (binding.ivProfilePhoto.tag == "remove_photo") {
            userData["profilePhotoUrl"] = ""
        } else if (photoUrl != null) {
            userData["profilePhotoUrl"] = photoUrl
        }

        firestore.collection("users").document(currentUser.uid)
            .set(userData, com.google.firebase.firestore.SetOptions.merge())
            .addOnSuccessListener {
                hideLoadingDialog()
                hasUnsavedChanges = false
                Toast.makeText(requireContext(), "✅ Profil berhasil diperbarui", Toast.LENGTH_SHORT).show()
                findNavController().navigateUp()
            }
            .addOnFailureListener { exception ->
                hideLoadingDialog()
                Log.e("EditProfileFragment", "Error updating Firestore", exception)
                Toast.makeText(
                    requireContext(),
                    "Gagal menyimpan data: ${exception.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
    }

    private fun handleBackPress() {
        if (hasUnsavedChanges) {
            AlertDialog.Builder(requireContext())
                .setTitle("Perubahan Belum Disimpan")
                .setMessage("Apakah Anda yakin ingin keluar tanpa menyimpan perubahan?")
                .setPositiveButton("Ya, Keluar") { _, _ ->
                    findNavController().navigateUp()
                }
                .setNegativeButton("Batal", null)
                .show()
        } else {
            findNavController().navigateUp()
        }
    }

    private fun showLoadingDialog() {
        try {
            if (progressDialog == null) {
                progressDialog = ProgressDialog(requireContext()).apply {
                    setMessage("Menyimpan perubahan...")
                    setCancelable(false)
                    setProgressStyle(ProgressDialog.STYLE_SPINNER)
                }
            }
            progressDialog?.show()
        } catch (e: Exception) {
            Log.e("EditProfileFragment", "Error showing loading dialog", e)
        }
    }

    private fun hideLoadingDialog() {
        try {
            progressDialog?.dismiss()
        } catch (e: Exception) {
            Log.e("EditProfileFragment", "Error hiding loading dialog", e)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        hideLoadingDialog()
        progressDialog = null
        _binding = null
    }
}