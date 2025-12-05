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
import com.example.litejoin.activity.ChatActivity
import com.example.litejoin.adapter.ChatRoomAdapter
import com.example.litejoin.databinding.FragmentChatListBinding
import com.example.litejoin.model.ChatRoom
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class ChatListFragment : Fragment() {

    private var _binding: FragmentChatListBinding? = null
    private val binding get() = _binding!!

    private lateinit var chatRoomAdapter: ChatRoomAdapter
    private val auth = FirebaseAuth.getInstance()
    private val realdb = FirebaseDatabase.getInstance()

    // 리스너 관리를 위한 변수 추가
    private var chatRoomListener: ValueEventListener? = null

    private val currentUid: String
        get() = auth.currentUser?.uid ?: throw IllegalStateException("User not logged in")

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentChatListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        loadChatRooms()
    }

    private fun setupRecyclerView() {
        // 어댑터 초기화 및 아이템 클릭 리스너 정의
        chatRoomAdapter = ChatRoomAdapter { chatRoom, partnerUid ->
            // ⬅️ [핵심 수정] requireContext() 대신 안전하게 binding.root.context 사용
            // 프래그먼트가 Detached 상태일 때 requireContext()가 호출되면 IllegalStateException 발생 가능
            val context = binding.root.context
            val intent = Intent(context, ChatActivity::class.java)
            intent.putExtra("PARTNER_UID", partnerUid)
            startActivity(intent)
        }

        binding.rvChatRooms.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = chatRoomAdapter

            // 일관된 구분선 추가
            addItemDecoration(DividerItemDecoration(context, LinearLayoutManager.VERTICAL))
        }
    }

    private fun loadChatRooms() {
        // 기존 리스너가 있다면 제거 (안전장치)
        chatRoomListener?.let {
            realdb.getReference("chatRooms").orderByChild("lastMessageTime").removeEventListener(it)
        }

        // 'chatRooms' 노드에서 현재 사용자가 참여 중인 채팅방 목록을 가져옴
        val query = realdb.getReference("chatRooms").orderByChild("lastMessageTime")

        chatRoomListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                // 뷰가 유효하지 않으면 업데이트 중단 (NPE 방지)
                if (_binding == null) return

                val chatRooms = mutableListOf<ChatRoom>()
                for (data in snapshot.children) {
                    data.getValue(ChatRoom::class.java)?.let { chatRoom ->
                        // 현재 사용자가 이 채팅방에 참여하고 있는지 확인
                        if (chatRoom.users.containsKey(currentUid)) {
                            chatRooms.add(chatRoom.copy(chatRoomId = data.key ?: ""))
                        }
                    }
                }

                // 최신 메시지 순으로 내림차순 정렬 후 업데이트
                chatRooms.sortByDescending { it.lastMessageTime }
                chatRoomAdapter.submitList(chatRooms)

                // 목록이 비어있을 때 메시지 표시
                binding.tvNoChats.visibility = if (chatRooms.isEmpty()) View.VISIBLE else View.GONE
            }

            override fun onCancelled(error: DatabaseError) {
                if (_binding != null) {
                    Toast.makeText(context, "채팅 목록 로드 실패: ${error.message}", Toast.LENGTH_LONG).show()
                }
            }
        }

        query.addValueEventListener(chatRoomListener!!)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // 프래그먼트 뷰가 파괴될 때 Firebase 리스너 반드시 제거
        chatRoomListener?.let {
            realdb.getReference("chatRooms").orderByChild("lastMessageTime").removeEventListener(it)
        }
        chatRoomListener = null
        _binding = null
    }
}