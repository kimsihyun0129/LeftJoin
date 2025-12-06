package com.example.litejoin.activity

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.litejoin.databinding.ActivityPostWriteBinding
import com.example.litejoin.databinding.CustomToolbarBinding
import com.example.litejoin.model.Post
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class PostWriteActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPostWriteBinding
    private lateinit var toolbarBinding: CustomToolbarBinding

    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()

    private var postId: String? = null
    private var authorNickname: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPostWriteBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 툴바 설정
        toolbarBinding = CustomToolbarBinding.bind(binding.toolbar.root)
        setSupportActionBar(toolbarBinding.root)
        supportActionBar?.setDisplayShowTitleEnabled(false)
        toolbarBinding.btnBack.setOnClickListener { onBackPressedDispatcher.onBackPressed() }
        // 툴바 오른쪽 사용자 정보 로직은 필요하다면 추가 (현재는 무시)

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
            binding.btnDeletePost.visibility = View.VISIBLE // 삭제 버튼 표시
            loadPostData(postId!!)

            // 2. 삭제 버튼 리스너 설정
            binding.btnDeletePost.setOnClickListener {
                showDeleteConfirmationDialog()
            }
        } else {
            // 3. 작성 모드 (FAB 클릭 시)
            binding.btnWriteOrEdit.text = "작성하기"
            binding.btnDeletePost.visibility = View.GONE // 삭제 버튼 숨김 (필수)
        }
    }

    // ... (loadAuthorNickname, loadPostData 함수는 이전과 동일) ...
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

    private fun loadPostData(id: String) {
        firestore.collection("posts").document(id).get()
            .addOnSuccessListener { document ->
                val post = document.toObject(Post::class.java)
                post?.let {
                    binding.etTitle.setText(it.title)
                    binding.etShortDescription.setText(it.shortDescription)
                    binding.etRecruitmentCount.setText(it.recruitmentCount.toString())
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

    private fun savePost() {
        val uid = auth.currentUser?.uid
        val title = binding.etTitle.text.toString().trim()
        val shortDescription = binding.etShortDescription.text.toString().trim()
        val location = binding.etLocation.text.toString().trim()
        val recruitmentCountStr = binding.etRecruitmentCount.text.toString().trim()
        val estimatedCostStr = binding.etEstimatedCost.text.toString().trim()

        if (uid == null || authorNickname == null) {
            Toast.makeText(this, "사용자 인증 정보가 유효하지 않습니다.", Toast.LENGTH_SHORT).show()
            return
        }

        if (title.isEmpty() || shortDescription.isEmpty() || location.isEmpty() ||
            recruitmentCountStr.isEmpty() || estimatedCostStr.isEmpty()) {

            Toast.makeText(this, "제목, 한줄 소개, 인원, 장소, 예상 비용은 모두 필수 입력 항목입니다.", Toast.LENGTH_LONG).show()
            return
        }

        val recruitmentCount = recruitmentCountStr.toIntOrNull()
        val estimatedCost = estimatedCostStr.toIntOrNull()

        if (recruitmentCount == null || recruitmentCount <= 0) { // ⬅️ 모집 인원 확인
            Toast.makeText(this, "모집 인원은 1명 이상의 숫자로 입력해야 합니다.", Toast.LENGTH_LONG).show()
            return
        }
        if (estimatedCost == null || estimatedCost < 0) {
            Toast.makeText(this, "예상 비용은 0 이상의 숫자로 입력해야 합니다.", Toast.LENGTH_LONG).show()
            return
        }

        savePostToFirestore(uid, recruitmentCount, estimatedCost)
    }

    private fun savePostToFirestore(uid: String, recruitmentCount: Int, estimatedCost: Int) {
        val newPost = Post(
            postId = postId ?: firestore.collection("posts").document().id,
            authorUid = uid,
            authorNickname = authorNickname!!,
            title = binding.etTitle.text.toString().trim(),
            shortDescription = binding.etShortDescription.text.toString().trim(),
            recruitmentCount = recruitmentCount,
            location = binding.etLocation.text.toString().trim(),
            estimatedCost = estimatedCost,
            content = binding.etContent.text.toString().trim()
        )

        firestore.collection("posts").document(newPost.postId).set(newPost)
            .addOnSuccessListener {
                Toast.makeText(this, if (postId == null) "게시글이 작성되었습니다." else "게시글이 수정되었습니다.", Toast.LENGTH_SHORT).show()
                finish() // 메인 화면으로 돌아가기 (PostListFragment가 자동으로 목록 새로고침)
            }
            .addOnFailureListener {
                Toast.makeText(this, "게시글 저장 실패: ${it.message}", Toast.LENGTH_LONG).show()
            }
    }

    // 삭제 확인 대화 상자
    private fun showDeleteConfirmationDialog() {
        AlertDialog.Builder(this)
            .setTitle("게시글 삭제 확인")
            .setMessage("이 게시글을 정말로 삭제하시겠습니까? 이 작업은 되돌릴 수 없습니다.")
            .setPositiveButton("삭제") { _, _ ->
                deletePost()
            }
            .setNegativeButton("취소", null)
            .show()
    }

    // 게시글 삭제 로직
    private fun deletePost() {
        postId?.let { id ->
            firestore.collection("posts").document(id).delete()
                .addOnSuccessListener {
                    Toast.makeText(this, "게시글이 삭제되었습니다.", Toast.LENGTH_SHORT).show()
                    finish() // 삭제 후 메인 화면으로 돌아가기
                }
                .addOnFailureListener {
                    Toast.makeText(this, "게시글 삭제 실패: ${it.message}", Toast.LENGTH_LONG).show()
                }
        }
    }
}