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
            // 1. 수정 모드 (게시글 목록에서 본인 글 클릭 시)
            binding.btnWriteOrEdit.text = "수정하기"
            loadPostData(postId!!)
        } else {
            // 2. 작성 모드 (FAB 클릭 시)
            binding.btnWriteOrEdit.text = "작성하기"
            // 데이터 로드는 필요 없음
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
        val shortDescription = binding.etShortDescription.text.toString().trim() // ⬅️ 추가
        val location = binding.etLocation.text.toString().trim()
        val maxMembersStr = binding.etMaxMembers.text.toString().trim()
        val estimatedCostStr = binding.etEstimatedCost.text.toString().trim() // ⬅️ 추가

        // 닉네임, 제목, 한줄 소개, 인원, 장소, 예상 비용을 모두 필수로 검사합니다.
        if (uid == null || authorNickname == null) {
            Toast.makeText(this, "사용자 인증 정보가 유효하지 않습니다.", Toast.LENGTH_SHORT).show()
            return
        }

        if (title.isEmpty() || shortDescription.isEmpty() || location.isEmpty() ||
            maxMembersStr.isEmpty() || estimatedCostStr.isEmpty()) {

            Toast.makeText(this, "제목, 한줄 소개, 인원, 장소, 예상 비용은 모두 필수 입력 항목입니다.", Toast.LENGTH_LONG).show()
            return
        }

        // 숫자 필드 유효성 검사 (숫자 형태인지 확인)
        val maxMembers = maxMembersStr.toIntOrNull()
        val estimatedCost = estimatedCostStr.toIntOrNull()

        if (maxMembers == null || maxMembers <= 0) {
            Toast.makeText(this, "모집 인원은 1명 이상의 숫자로 입력해야 합니다.", Toast.LENGTH_LONG).show()
            return
        }
        if (estimatedCost == null || estimatedCost < 0) {
            Toast.makeText(this, "예상 비용은 0 이상의 숫자로 입력해야 합니다.", Toast.LENGTH_LONG).show()
            return
        }

        // 유효성 검사를 통과하면 Firestore에 저장하는 로직을 실행합니다.
        savePostToFirestore(uid)
    }

    // savePostToFirestore 함수는 변경된 파라미터(maxMembers, estimatedCost)를 사용하여 업데이트 필요
    private fun savePostToFirestore(uid: String) {
        // 이전 savePost 로직에서 maxMembers와 estimatedCost를 계산했으므로,
        // 이 함수를 호출할 때 해당 값을 전달하도록 수정해야 합니다.

        // 이전에 PostWriteActivity.kt 에서 savePostToFirestore(uid)를 호출했는데,
        // savePost(uid) 내에서 maxMembers와 estimatedCost를 계산하므로
        // savePostToFirestore(uid, maxMembers, estimatedCost)로 파라미터를 추가하는 것이 더 깔끔합니다.

        // 편의를 위해, savePost 내부에서 모든 변수를 정의하고 바로 저장합니다.

        val maxMembers = binding.etMaxMembers.text.toString().toIntOrNull() ?: 1
        val estimatedCost = binding.etEstimatedCost.text.toString().toIntOrNull() ?: 0

        val newPost = Post(
            postId = postId ?: firestore.collection("posts").document().id,
            authorUid = uid,
            authorNickname = authorNickname!!,
            title = binding.etTitle.text.toString().trim(),
            shortDescription = binding.etShortDescription.text.toString().trim(),
            maxMembers = maxMembers,
            currentMembers = if (postId == null) 1 else maxMembers,
            location = binding.etLocation.text.toString().trim(),
            estimatedCost = estimatedCost,
            content = binding.etContent.text.toString().trim()
        )

        // Firestore 저장 (새 글이든 수정이든 set() 사용)
        firestore.collection("posts").document(newPost.postId).set(newPost)
            .addOnSuccessListener {
                Toast.makeText(this, if (postId == null) "게시글이 작성되었습니다." else "게시글이 수정되었습니다.", Toast.LENGTH_SHORT).show()
                finish()
            }
            .addOnFailureListener {
                Toast.makeText(this, "게시글 저장 실패: ${it.message}", Toast.LENGTH_LONG).show()
            }
    }
}