package com.example.litejoin.activity

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.litejoin.R
import com.example.litejoin.databinding.ActivityMainBinding
import com.example.litejoin.databinding.CustomToolbarMainBinding
import com.example.litejoin.fragment.ChatListFragment
import com.example.litejoin.fragment.PostListFragment
import com.google.android.gms.tasks.OnCompleteListener
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessaging

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var toolbarBinding: CustomToolbarMainBinding
    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()

    // 권한 요청 런처 선언
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            // 권한 허용 시 토큰 설정
            setupFCMToken()
        } else {
            Toast.makeText(this, "알림 권한이 거부되어 푸시 알림을 받을 수 없습니다.", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 1. 커스텀 툴바 설정
        toolbarBinding = CustomToolbarMainBinding.bind(binding.toolbar.root)
        setSupportActionBar(toolbarBinding.root)
        supportActionBar?.setDisplayShowTitleEnabled(false) // 기본 타이틀 숨김
        setupToolbar()

        // 2. 하단 탭바 설정
        setupBottomNavigationView()

        // 3. 앱 시작 시 기본 화면 설정 (게시글 탭)
        if (savedInstanceState == null) {
            // 초기에는 게시글 목록 Fragment를 로드
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, PostListFragment())
                .commit()
        }
        
        // 4. 알림 권한 요청 및 FCM 토큰 갱신
        askNotificationPermission()
    }

    // --- 툴바 로직 ---
    private fun setupToolbar() {
        // 오른쪽 사용자 정보 레이아웃 클릭 시 프로필 화면으로 이동
        toolbarBinding.layoutUserProfileTrigger.setOnClickListener {
            val intent = Intent(this, UserProfileActivity::class.java)
            startActivity(intent)
        }

        loadUserNickname()
    }

    private fun loadUserNickname() {
        val uid = auth.currentUser?.uid ?: return

        firestore.collection("users").document(uid).get()
            .addOnSuccessListener { document ->
                val nickname = document.getString("nickname")
                if (nickname != null) {
                    toolbarBinding.tvNickname.text = nickname
                } else {
                    // (비상 상황) 닉네임이 없으면 정보 입력 화면으로 다시 이동
                    Toast.makeText(this, "프로필 정보 오류, 재입력이 필요합니다.", Toast.LENGTH_SHORT).show()
                    startActivity(Intent(this, UserProfileActivity::class.java))
                    finish()
                }
            }
            .addOnFailureListener {
                toolbarBinding.tvNickname.text = "오류"
            }
    }

    // --- 권한 요청 및 FCM 토큰 설정 ---
    private fun askNotificationPermission() {
        // 안드로이드 13 (TIRAMISU) 이상인 경우 권한 요청 필요
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) ==
                PackageManager.PERMISSION_GRANTED
            ) {
                // 이미 권한이 있는 경우 토큰 설정
                setupFCMToken()
            } else {
                // 권한이 없는 경우 요청
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        } else {
            // 안드로이드 12 이하에서는 권한 자동 허용
            setupFCMToken()
        }
    }

    private fun setupFCMToken() {
        val uid = auth.currentUser?.uid ?: return

        FirebaseMessaging.getInstance().token.addOnCompleteListener(OnCompleteListener { task ->
            if (!task.isSuccessful) {
                return@OnCompleteListener
            }

            // 새로운 FCM 등록 토큰 가져오기
            val token = task.result

            // Firestore에 토큰 저장 (fcmToken 필드 업데이트)
            firestore.collection("users").document(uid)
                .update("fcmToken", token)
        })
    }

    // --- 탭바 로직 ---
    private fun setupBottomNavigationView() {
        binding.bottomNav.setOnItemSelectedListener { item ->
            val fragment = when (item.itemId) {
                R.id.nav_post -> PostListFragment()
                R.id.nav_chat -> ChatListFragment()
                else -> null
            }

            if (fragment != null) {
                supportFragmentManager.beginTransaction()
                    .replace(R.id.fragment_container, fragment)
                    .commit()
                true
            } else {
                false
            }
        }
    }
}