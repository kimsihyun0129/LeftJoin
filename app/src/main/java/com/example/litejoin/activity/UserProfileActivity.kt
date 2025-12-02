package com.example.litejoin.activity

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.bumptech.glide.Glide
import com.example.litejoin.databinding.ActivityUserProfileBinding
import com.example.litejoin.model.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage

class UserProfileActivity : AppCompatActivity() {

    private lateinit var binding: ActivityUserProfileBinding
    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()
    private val storage = FirebaseStorage.getInstance()
    private var selectedImageUri: Uri? = null

    // 갤러리 접근 권한 요청 계약
    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
            if (isGranted) {
                openGallery()
            } else {
                Toast.makeText(this, "사진을 첨부하려면 저장소 권한이 필요합니다.", Toast.LENGTH_SHORT).show()
            }
        }

    // 갤러리 이미지 선택 계약
    private val pickImageLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                selectedImageUri = result.data?.data
                // Glide를 사용하여 CircleImageView에 이미지 로드
                Glide.with(this).load(selectedImageUri).into(binding.ivProfile)
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityUserProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // TODO: [툴바 구현 필요] 커스텀 툴바 설정을 여기에 추가해야 합니다.
        // 현재는 편의상 뒤로가기 버튼만 임시로 처리합니다.
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "사용자 정보 입력"

        // 1. 프로필 이미지 첨부 버튼 리스너
        binding.btnAttachPhoto.setOnClickListener {
            checkStoragePermission()
        }

        // 2. 저장 버튼 리스너
        binding.btnSave.setOnClickListener {
            saveUserProfile()
        }

        // 기존 정보가 있다면 불러와서 표시 (수정 시나리오)
        loadUserProfile()
    }

    // 뒤로가기 버튼 처리
    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }

    // --- 이미지 처리 로직 ---
    private fun checkStoragePermission() {
        // Android 13 이상에서는 READ_MEDIA_IMAGES, 이하에서는 READ_EXTERNAL_STORAGE 권한 확인
        val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_IMAGES
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }

        when {
            ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED -> {
                // 권한이 이미 있다면 갤러리 열기
                openGallery()
            }
            shouldShowRequestPermissionRationale(permission) -> {
                Toast.makeText(this, "프로필 사진을 설정하려면 권한이 필요합니다.", Toast.LENGTH_LONG).show()
                requestPermissionLauncher.launch(permission)
            }
            else -> {
                // 권한 요청
                requestPermissionLauncher.launch(permission)
            }
        }
    }

    private fun openGallery() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        pickImageLauncher.launch(intent)
    }

    // --- 데이터 로드 로직 (수정 모드 대비) ---
    private fun loadUserProfile() {
        val uid = auth.currentUser?.uid ?: return

        firestore.collection("users").document(uid).get()
            .addOnSuccessListener { document ->
                val user = document.toObject(User::class.java)
                user?.let {
                    binding.etNickname.setText(it.nickname)
                    binding.etName.setText(it.name)
                    binding.etStudentId.setText(it.studentId)
                    binding.etPhoneNumber.setText(it.phoneNumber)

                    // 이미지 URL이 있다면 Glide로 로드
                    it.profileImageUrl?.let { url ->
                        Glide.with(this).load(url).into(binding.ivProfile)
                    }
                }
            }
    }

    // --- 데이터 저장 로직 ---
    private fun saveUserProfile() {
        val uid = auth.currentUser?.uid
        val nickname = binding.etNickname.text.toString().trim()
        val name = binding.etName.text.toString().trim()
        val studentId = binding.etStudentId.text.toString().trim()
        val phoneNumber = binding.etPhoneNumber.text.toString().trim()

        if (uid == null || nickname.isEmpty()) {
            Toast.makeText(this, "닉네임은 필수 입력 항목입니다.", Toast.LENGTH_SHORT).show()
            return
        }

        // 1. 이미지 업로드 (선택 사항)
        if (selectedImageUri != null) {
            uploadImageAndSaveProfile(uid, nickname, name, studentId, phoneNumber)
        } else {
            // 2. 이미지 없이 프로필 정보만 저장
            saveProfileToFirestore(uid, nickname, name, studentId, phoneNumber, null)
        }
    }

    private fun uploadImageAndSaveProfile(uid: String, nickname: String, name: String, studentId: String, phoneNumber: String) {
        val storageRef = storage.reference.child("profiles/$uid.jpg")

        // Storage에 이미지 업로드
        selectedImageUri?.let { uri ->
            storageRef.putFile(uri)
                .addOnSuccessListener {
                    // 업로드 성공 후 다운로드 URL 가져오기
                    storageRef.downloadUrl.addOnSuccessListener { downloadUri ->
                        // 3. Firestore에 URL과 함께 프로필 정보 저장
                        saveProfileToFirestore(uid, nickname, name, studentId, phoneNumber, downloadUri.toString())
                    }
                }
                .addOnFailureListener {
                    Toast.makeText(this, "이미지 업로드 실패: ${it.message}", Toast.LENGTH_LONG).show()
                }
        }
    }

    private fun saveProfileToFirestore(uid: String, nickname: String, name: String, studentId: String, phoneNumber: String, profileImageUrl: String?) {
        val userMap = User(
            uid = uid,
            nickname = nickname,
            name = if (name.isEmpty()) null else name,
            studentId = if (studentId.isEmpty()) null else studentId,
            phoneNumber = if (phoneNumber.isEmpty()) null else phoneNumber,
            profileImageUrl = profileImageUrl
            // createdAt 필드는 FireStore ServerTimestamp에 의해 자동 저장됨
        )

        firestore.collection("users").document(uid).set(userMap)
            .addOnSuccessListener {
                Toast.makeText(this, "프로필 정보가 저장되었습니다.", Toast.LENGTH_SHORT).show()
                // 저장 완료 후 메인 화면으로 이동
                navigateToMain()
            }
            .addOnFailureListener {
                Toast.makeText(this, "프로필 저장 실패: ${it.message}", Toast.LENGTH_LONG).show()
            }
    }

    private fun navigateToMain() {
        val intent = Intent(this, MainActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(intent)
        finish()
    }
}