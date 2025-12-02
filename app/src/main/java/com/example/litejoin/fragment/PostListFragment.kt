package com.example.litejoin.fragment

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.litejoin.R
import com.example.litejoin.activity.PostDetailActivity
import com.example.litejoin.activity.PostWriteActivity
import com.example.litejoin.adapter.PostAdapter
import com.example.litejoin.databinding.FragmentPostListBinding
import com.example.litejoin.model.Post
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

class PostListFragment : Fragment() {

    private var _binding: FragmentPostListBinding? = null
    private val binding get() = _binding!!

    private lateinit var postAdapter: PostAdapter
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    // 현재 로그인된 사용자의 UID
    private val currentUid: String?
        get() = auth.currentUser?.uid

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPostListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        loadPostList()

        // 1. FAB 클릭 시 게시글 작성 화면으로 이동 (작성 모드)
        // [중요] 이 기능이 앱을 종료시킨다면, PostWriteActivity가 AndroidManifest에 등록되었는지 확인하세요.
        binding.fabAddPost.setOnClickListener {
            val intent = Intent(requireContext(), PostWriteActivity::class.java)
            startActivity(intent)
        }
    }

    private fun setupRecyclerView() {
        // 어댑터 초기화 및 아이템 클릭 리스너 정의
        postAdapter = PostAdapter { post ->
            handlePostClick(post)
        }

        binding.rvPostList.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = postAdapter
            // 목록 구분을 위한 구분선 추가 (선택 사항)
            addItemDecoration(DividerItemDecoration(context, LinearLayoutManager.VERTICAL))
        }
    }

    private fun loadPostList() {
        // 'posts' 컬렉션에서 'createdAt' 기준으로 내림차순 정렬하여 데이터 가져오기 (최신순)
        firestore.collection("posts")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Toast.makeText(context, "게시글 로드 실패: ${e.message}", Toast.LENGTH_LONG).show()
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    val postList = snapshot.toObjects(Post::class.java)
                    postAdapter.submitList(postList)
                }
            }
    }

    private fun handlePostClick(post: Post) {
        // 2. 아이템 클릭 시 본인/타인 글 분기 로직
        if (post.authorUid == currentUid) {
            // **본인 글:** 게시글 수정/작성 화면으로 이동 (수정 모드)
            val intent = Intent(requireContext(), PostWriteActivity::class.java)
            intent.putExtra("POST_ID", post.postId) // 게시글 ID 전달 -> PostWriteActivity에서 '수정하기' 버튼 활성화
            startActivity(intent)

        } else {
            // **타인 글:** 게시글 상세 화면으로 이동 (조회 전용)
            val intent = Intent(requireContext(), PostDetailActivity::class.java)
            intent.putExtra("POST_ID", post.postId) // 상세 정보를 로드하기 위한 ID 전달
            startActivity(intent)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // 메모리 누수를 방지하기 위해 바인딩 해제
        _binding = null
    }
}