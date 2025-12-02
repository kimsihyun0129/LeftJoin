package com.example.litejoin.activity

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.litejoin.databinding.ActivityPostWriteBinding
import com.example.litejoin.databinding.CustomToolbarWriteBinding
import com.example.litejoin.model.Post
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class PostWriteActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPostWriteBinding
    private lateinit var toolbarBinding: CustomToolbarWriteBinding

    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()

    private var postId: String? = null // 수정 모드일 경우 게시글 ID 저장
    private var authorNickname: String? = null // 작성자 닉네임 저장

    // 이미지 관련 로직 및 변수 모두 제거됨

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPostWriteBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 툴바 설정
        toolbarBinding = CustomToolbarWriteBinding.bind(binding.toolbar.root)
        setSupportActionBar(toolbarBinding.root)
        supportActionBar?.setDisplayShowTitleEnabled(false)
        toolbarBinding.btnBack.setOnClickListener { finish() }

        // 수정 모드 확인
        postId = intent.getStringExtra("POST_ID")

        // 작성자 닉네임 미리 로드
        loadAuthorNickname()

        // 작성/수정 버튼 리스너
        binding.btnWriteOrEdit.setOnClickListener {
            savePost()
        }

        if (postId != null) {
            // 수정 모드: 데이터 로드 및 UI 업데이트
            loadPostData(postId!!)
        }
    }

    // --- 닉네임 로딩 ---
    private fun loadAuthorNickname() {
        val uid = auth.currentUser?.uid ?: return
        firestore.collection("users").document(uid).get()
            .addOnSuccessListener { document ->
                authorNickname = document.getString("nickname")
                if (authorNickname == null) {
                    Toast.makeText(this, "닉네임 정보가 없어 게시글 작성이 불가합니다. 프로필을 먼저 설정해주세요.", Toast.LENGTH_LONG).show()
                    finish()
                }
            }
    }

    // --- 수정 모드 데이터 로드 ---
    private fun loadPostData(id: String) {
        binding.btnWriteOrEdit.text = "수정하기" // 버튼 텍스트 변경

        firestore.collection("posts").document(id).get()
            .addOnSuccessListener { document ->
                val post = document.toObject(Post::class.java)
                post?.let {
                    binding.etTitle.setText(it.title)
                    binding.etShortDescription.setText(it.shortDescription)
                    binding.etMaxMembers.setText(it.maxMembers.toString())
                    binding.etLocation.setText(it.location)
                    binding.etEstimatedCost.setText(it.estimatedCost.toString())
                    binding.etContent.setText(it.content)
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, "게시글 로드 실패: ${it.message}", Toast.LENGTH_LONG).show()
                finish()
            }
    }

    // --- 데이터 저장/수정 로직 (이미지 로직 제거) ---
    private fun savePost() {
        val uid = auth.currentUser?.uid
        val title = binding.etTitle.text.toString().trim()
        val location = binding.etLocation.text.toString().trim()
        val maxMembersStr = binding.etMaxMembers.text.toString().trim()

        // 필수 필드 유효성 검사
        if (uid == null || authorNickname == null || title.isEmpty() || location.isEmpty()) {
            Toast.makeText(this, "제목, 장소, 닉네임은 필수입니다.", Toast.LENGTH_SHORT).show()
            return
        }

        val maxMembers = maxMembersStr.toIntOrNull() ?: 1
        val estimatedCost = binding.etEstimatedCost.text.toString().toIntOrNull() ?: 0

        val newPost = Post(
            postId = postId ?: firestore.collection("posts").document().id, // 새 문서 ID 생성
            authorUid = uid,
            authorNickname = authorNickname!!,
            title = title,
            shortDescription = binding.etShortDescription.text.toString().trim(),
            maxMembers = maxMembers,
            currentMembers = if (postId == null) 1 else maxMembers, // 수정 시에는 현재 인원수를 최대 인원과 동일하게 설정하는 임시 로직 유지
            location = location,
            estimatedCost = estimatedCost,
            content = binding.etContent.text.toString().trim()
            // postImageUrl 필드는 없음
        )

        // Firestore 저장 (새 글이든 수정이든 set() 사용)
        firestore.collection("posts").document(newPost.postId).set(newPost)
            .addOnSuccessListener {
                Toast.makeText(this, if (postId == null) "게시글이 작성되었습니다." else "게시글이 수정되었습니다.", Toast.LENGTH_SHORT).show()
                finish() // 메인 화면으로 돌아가기
            }
            .addOnFailureListener {
                Toast.makeText(this, "게시글 저장 실패: ${it.message}", Toast.LENGTH_LONG).show()
            }
    }
}