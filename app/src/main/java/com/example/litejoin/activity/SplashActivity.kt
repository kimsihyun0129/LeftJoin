package com.example.litejoin.activity

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import com.example.litejoin.databinding.ActivitySplashBinding
import com.google.firebase.auth.FirebaseAuth

class SplashActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySplashBinding
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySplashBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 툴바 숨기기 (요구사항)
        supportActionBar?.hide()

        // Firebase Auth 인스턴스 초기화
        auth = FirebaseAuth.getInstance()

        // 1.5초 지연 후 화면 전환 로직 실행
        Handler(Looper.getMainLooper()).postDelayed({
            checkLoginStatus()
        }, 1500)
    }

    private fun checkLoginStatus() {
        val currentUser = auth.currentUser

        // 사용자가 로그인 되어 있는지 확인
        if (currentUser != null) {
            // 로그인 상태: 바로 메인 화면으로 이동
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
        } else {
            // 미로그인 상태: 로그인 화면으로 이동
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
        }

        // 스플래시 화면 종료
        finish()
    }
}