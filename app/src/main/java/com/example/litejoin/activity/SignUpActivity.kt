package com.example.litejoin.activity

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.litejoin.databinding.ActivitySignUpBinding
import com.google.firebase.auth.FirebaseAuth

class SignUpActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySignUpBinding
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // 뷰 바인딩 초기화
        binding = ActivitySignUpBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 툴바 숨기기 (요구사항)
        supportActionBar?.hide()

        // Firebase Auth 인스턴스 초기화
        auth = FirebaseAuth.getInstance()

        // 1. 회원가입 버튼 리스너
        binding.btnSignUp.setOnClickListener {
            performSignUp()
        }

        // 2. 로그인 화면 이동 버튼 리스너
        binding.tvGoToLogin.setOnClickListener {
            // 회원가입 화면 종료 후 로그인 화면으로 돌아감
            finish()
        }
    }

    private fun performSignUp() {
        val email = binding.etEmail.text.toString().trim()
        val password = binding.etPassword.text.toString().trim()

        // 입력 유효성 검사
        if (email.isBlank() || password.isBlank()) {
            Toast.makeText(this, "이메일과 비밀번호를 모두 입력해주세요.", Toast.LENGTH_SHORT).show()
            return
        }

        // Firebase Auth에서 요구하는 최소 비밀번호 길이 (6자) 검사
        if (password.length < 6) {
            Toast.makeText(this, "비밀번호는 6자 이상이어야 합니다.", Toast.LENGTH_SHORT).show()
            return
        }

        // Firebase 이메일 회원가입 실행
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    // 회원가입 성공
                    Toast.makeText(this, "회원가입 성공! 로그인해주세요.", Toast.LENGTH_LONG).show()

                    // 회원가입 성공 후 로그인 화면으로 이동 (현재 화면 종료)
                    finish()
                } else {
                    // 회원가입 실패 (예: 이미 존재하는 계정, 형식 오류 등)
                    // Firebase Auth 예외 메시지를 사용자에게 표시
                    Toast.makeText(this, "회원가입 실패: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                }
            }
    }
}