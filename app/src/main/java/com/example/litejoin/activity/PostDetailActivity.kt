// com.example.litejoin/activity/PostDetailActivity.kt (수정)

package com.example.litejoin.activity

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.litejoin.databinding.ActivityPostDetailBinding
import com.example.litejoin.databinding.CustomToolbarBinding
import com.example.litejoin.model.Post
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.NumberFormat
import java.util.Locale

class PostDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPostDetailBinding
    private lateinit var toolbarBinding: CustomToolbarBinding

    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()

    private var currentPost: Post? = null
    private val currentUid: String? = auth.currentUser?.uid
    private var currentUserName: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPostDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 툴바 설정
        toolbarBinding = CustomToolbarBinding.bind(binding.toolbar.root)
        setSupportActionBar(toolbarBinding.root)
        supportActionBar?.setDisplayShowTitleEnabled(false)

        // 뒤로가기 버튼 기능 구현
        toolbarBinding.btnBack.setOnClickListener { finish() }

        // 툴바 오른쪽 사용자 정보 로딩
        loadUserNicknameForToolbar()

        val postId = intent.getStringExtra("POST_ID")

        if (postId == null) {
            Toast.makeText(this, "잘못된 접근입니다.", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        loadPostDetail(postId)

        // 채팅하기 버튼 리스너 (타인 글만 들어오므로, 항상 활성화)
        binding.btnStartChat.setOnClickListener {
            currentPost?.let { post ->
                // TODO: 10단계에서 구현할 채팅 화면으로 이동
                startChatWithUser(post.authorUid, post.authorNickname)
            }
        }

        // [수정] 이 Activity는 타인 글만 보므로, 채팅 버튼을 항상 표시합니다.
        binding.btnStartChat.visibility = android.view.View.VISIBLE
    }

    // --- 툴바 오른쪽 닉네임 로드 로직 (유지) ---
    private fun loadUserNicknameForToolbar() {
        currentUid?.let { uid ->
            firestore.collection("users").document(uid).get()
                .addOnSuccessListener { document ->
                    currentUserName = document.getString("nickname")
                    toolbarBinding.tvNickname.text = currentUserName ?: "사용자"

                    toolbarBinding.layoutUserProfileTrigger.setOnClickListener {
                        startActivity(Intent(this, UserProfileActivity::class.java))
                    }
                }
        }
    }


    // --- 데이터 로드 및 UI 업데이트 (본인/타인 구분 로직 제거) ---
    private fun loadPostDetail(postId: String) {
        firestore.collection("posts").document(postId).get()
            .addOnSuccessListener { document ->
                currentPost = document.toObject(Post::class.java)

                currentPost?.let { post ->
                    updateUI(post)
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, "상세 정보를 불러오는 데 실패했습니다.", Toast.LENGTH_LONG).show()
                finish()
            }
    }

    private fun updateUI(post: Post) {
        val currencyFormat = NumberFormat.getNumberInstance(Locale.getDefault())

        binding.tvTitle.text = "제목: ${post.title}"
        binding.tvShortDescription.text = "한줄 소개: ${post.shortDescription}"
        binding.tvMembers.text = "인원: ${post.currentMembers} / ${post.maxMembers}명"
        binding.tvLocation.text = "장소: ${post.location}"
        binding.tvEstimatedCost.text = "예상 비용: ${currencyFormat.format(post.estimatedCost)}원"
        binding.tvContent.text = post.content
    }

    // --- 수정/삭제 메뉴 처리 (제거) ---
    // [수정] 이 Activity는 타인 글만 보므로, 메뉴를 표시하지 않습니다.
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        // 메뉴를 표시할 조건이 없으므로 항상 false 반환
        return false
    }

    // [수정] onOptionsItemSelected에서 수정/삭제 항목 제거 (메뉴가 표시되지 않으므로 사실상 불필요)
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return super.onOptionsItemSelected(item)
    }

    // --- 채팅 시작 임시 로직 (10단계에서 완성) ---
    private fun startChatWithUser(partnerUid: String, partnerNickname: String) {
        if (currentUid == partnerUid) {
            // 이 경로는 PostListFragment에서 이미 차단되어야 하지만, 혹시 모를 상황 대비
            Toast.makeText(this, "본인과의 채팅은 불가능합니다.", Toast.LENGTH_SHORT).show()
            return
        }

        Toast.makeText(this, "$partnerNickname 님과 채팅을 시작합니다. (다음 단계 구현 예정)", Toast.LENGTH_SHORT).show()
        // TODO: ChatActivity로 이동하는 실제 로직 추가 (채팅방 ID 생성 또는 조회)
    }
}