package com.example.litejoin.activity

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.litejoin.R
import com.example.litejoin.databinding.ActivityMainBinding
import com.example.litejoin.databinding.CustomToolbarMainBinding
import com.example.litejoin.fragment.ChatListFragment
import com.example.litejoin.fragment.PostListFragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var toolbarBinding: CustomToolbarMainBinding
    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()

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

    // 이전에 로그인 Activity를 만들 때 finish() 했으므로 onResume에서 별도의 로그인 체크는 필요하지 않습니다.
}