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
import com.example.litejoin.databinding.CustomToolbarUserInfoBinding
import com.example.litejoin.model.User
import com.google.android.gms.auth.api.signin.GoogleSignIn // GoogleSignIn import
import com.google.android.gms.auth.api.signin.GoogleSignInOptions // GoogleSignInOptions import
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage

class UserProfileActivity : AppCompatActivity() {

    private lateinit var binding: ActivityUserProfileBinding
    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()
    private val storage = FirebaseStorage.getInstance()
    private var selectedImageUri: Uri? = null
    private lateinit var toolbarBinding: CustomToolbarUserInfoBinding

    // 갤러리 접근 권한 요청 계약 (생략)
    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
            if (isGranted) { openGallery() } else { Toast.makeText(this, "사진을 첨부하려면 저장소 권한이 필요합니다.", Toast.LENGTH_SHORT).show() }
        }

    // 갤러리 이미지 선택 계약 (생략)
    private val pickImageLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                selectedImageUri = result.data?.data
                Glide.with(this).load(selectedImageUri).into(binding.ivProfile)
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityUserProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 1. 툴바 설정 및 초기화
        binding.toolbar?.let {
            // activity_user_profile.xml의 include ID가 'toolbar'라고 가정
            toolbarBinding = CustomToolbarUserInfoBinding.bind(it.root)
            setSupportActionBar(toolbarBinding.root)
            supportActionBar?.setDisplayShowTitleEnabled(false)

            // 2. 툴바 뒤로가기 버튼 리스너 설정
            toolbarBinding.btnBack.setOnClickListener {
                onBackPressedDispatcher.onBackPressed()
            }
        }


        // 3. 프로필 이미지 첨부 버튼 리스너
        binding.btnAttachPhoto.setOnClickListener {
            checkStoragePermission()
        }

        // 4. 저장 버튼 리스너
        binding.btnSave.setOnClickListener {
            saveUserProfile()
        }

        // 5. 로그아웃 버튼 리스너
        binding.btnLogout.setOnClickListener {
            performLogout()
        }

        // 6. 기존 정보 로드 (수정 시나리오)
        loadUserProfile()
    }

    // --- 이미지 처리 로직 (생략) ---
    private fun checkStoragePermission() {
        val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_IMAGES
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }

        when {
            ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED -> {
                openGallery()
            }
            shouldShowRequestPermissionRationale(permission) -> {
                Toast.makeText(this, "프로필 사진을 설정하려면 권한이 필요합니다.", Toast.LENGTH_LONG).show()
                requestPermissionLauncher.launch(permission)
            }
            else -> {
                requestPermissionLauncher.launch(permission)
            }
        }
    }

    private fun openGallery() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        pickImageLauncher.launch(intent)
    }

    // --- 데이터 로드 로직 (생략) ---
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

                    it.profileImageUrl?.let { url ->
                        Glide.with(this).load(url).into(binding.ivProfile)
                    }
                }
            }
    }

    // --- 데이터 저장 로직 (생략) ---
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

        if (selectedImageUri != null) {
            uploadImageAndSaveProfile(uid, nickname, name, studentId, phoneNumber)
        } else {
            saveProfileToFirestore(uid, nickname, name, studentId, phoneNumber, null)
        }
    }

    private fun uploadImageAndSaveProfile(uid: String, nickname: String, name: String, studentId: String, phoneNumber: String) {
        val storageRef = storage.reference.child("profiles/$uid.jpg")

        selectedImageUri?.let { uri ->
            storageRef.putFile(uri)
                .addOnSuccessListener {
                    storageRef.downloadUrl.addOnSuccessListener { downloadUri ->
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
        )

        firestore.collection("users").document(uid).set(userMap)
            .addOnSuccessListener {
                Toast.makeText(this, "프로필 정보가 저장되었습니다.", Toast.LENGTH_SHORT).show()
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

    // --- 로그아웃 로직 (안정화된 버전) ---
    private fun performLogout() {
        // 1. Firebase 로그아웃
        auth.signOut()

        // 2. ⬅️ [핵심] Google Sign-In 세션 해제 시도 (크래시 방지를 위해 try-catch로 감쌉니다)
        try {
            val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail()
                .build()
            val googleSignInClient = GoogleSignIn.getClient(this, gso)
            googleSignInClient.signOut()
        } catch (e: Exception) {
            // Google 관련 초기화 오류 시 무시하고 Firebase 로그아웃만 반영합니다.
        }

        Toast.makeText(this, "로그아웃 되었습니다.", Toast.LENGTH_SHORT).show()

        // 3. 로그인 화면으로 이동하며, 이전 Activity 스택을 모두 지웁니다.
        val intent = Intent(this, LoginActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(intent)
        finish()
    }
}