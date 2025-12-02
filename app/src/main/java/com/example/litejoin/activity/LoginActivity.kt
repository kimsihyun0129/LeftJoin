package com.example.litejoin.activity

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.litejoin.R
import com.example.litejoin.databinding.ActivityLoginBinding
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var googleSignInClient: GoogleSignInClient

    // Google Sign-In 요청 코드
    private val RC_SIGN_IN = 9001

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 툴바 숨기기 (요구사항)
        supportActionBar?.hide()

        // Firebase Auth 및 Google Sign-In 초기화
        auth = FirebaseAuth.getInstance()
        setupGoogleSignIn()

        // 1. 이메일 로그인 버튼 리스너
        binding.btnLogin.setOnClickListener {
            emailLogin()
        }

        // 2. 구글 인증 버튼 리스너
        binding.btnGoogleSignIn.setOnClickListener {
            signInWithGoogle()
        }

        // 3. 회원가입 버튼 리스너
        binding.tvSignUp.setOnClickListener {
            val intent = Intent(this, SignUpActivity::class.java) // SignUpActivity는 다음 단계에서 구현
            startActivity(intent)
        }
    }

    // --- 이메일 로그인 로직 ---
    private fun emailLogin() {
        val email = binding.etEmail.text.toString()
        val password = binding.etPassword.text.toString()

        if (email.isBlank() || password.isBlank()) {
            Toast.makeText(this, "이메일과 비밀번호를 모두 입력해주세요.", Toast.LENGTH_SHORT).show()
            return
        }

        // Firebase 이메일 로그인
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    // 로그인 성공
                    navigateToNextScreen()
                } else {
                    // 로그인 실패 (유효성 검사 실패 포함)
                    Toast.makeText(this, "로그인 실패: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                }
            }
    }

    // --- Google 로그인 로직 ---
    private fun setupGoogleSignIn() {
        // R.string.default_web_client_id는 Firebase 콘솔 설정 시 자동으로 생성됩니다.
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id)) // R.string 경로는 실제 프로젝트에 맞게 수정해야 할 수 있습니다.
            .requestEmail()
            .build()

        googleSignInClient = GoogleSignIn.getClient(this, gso)
    }

    private fun signInWithGoogle() {
        val signInIntent = googleSignInClient.signInIntent
        startActivityForResult(signInIntent, RC_SIGN_IN)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == RC_SIGN_IN) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            try {
                // Google Sign In 성공, Firebase에 인증
                val account = task.getResult(com.google.android.gms.common.api.ApiException::class.java)
                firebaseAuthWithGoogle(account.idToken!!)
            } catch (e: com.google.android.gms.common.api.ApiException) {
                // Google Sign In 실패
                Toast.makeText(this, "Google 로그인 실패: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun firebaseAuthWithGoogle(idToken: String) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    // Firebase 인증 성공
                    navigateToNextScreen()
                } else {
                    // Firebase 인증 실패
                    Toast.makeText(this, "Firebase 인증 실패: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                }
            }
    }

    // --- 화면 전환 로직 ---
    // 사용자 정보 입력 여부를 확인하여 분기하는 로직이 들어갈 예정입니다.
    private fun navigateToNextScreen() {
        // TODO: [중요] 사용자 정보가 입력되었는지 Firestore에서 확인하는 로직 추가 필요
        // 현재는 편의상 바로 MainActivity로 이동하도록 임시 구현합니다.
        val intent = Intent(this, MainActivity::class.java) // MainActivity는 다음 단계에서 구현
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK) // 이전 스택 모두 제거
        startActivity(intent)
        finish()
    }
}