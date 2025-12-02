// com.example.litejoin/fragment/PostListFragment.kt (로직 추가)

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
//import com.example.litejoin.activity.PostDetailActivity
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

        // FAB 클릭 시 게시글 작성 화면으로 이동
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
        // 클릭한 게시글이 본인이 작성한 글인지 확인
        if (post.authorUid == currentUid) {
            // 본인 글: 게시글 수정/작성 화면으로 이동 (PostWriteActivity)
            val intent = Intent(requireContext(), PostWriteActivity::class.java)
            intent.putExtra("POST_ID", post.postId) // 수정 모드임을 알림
            startActivity(intent)

        } else {
            // 타인 글: 게시글 상세 화면으로 이동 (PostDetailActivity)
//            val intent = Intent(requireContext(), PostDetailActivity::class.java)
//            intent.putExtra("POST_ID", post.postId) // 상세 정보를 로드하기 위한 ID 전달
//            startActivity(intent)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}