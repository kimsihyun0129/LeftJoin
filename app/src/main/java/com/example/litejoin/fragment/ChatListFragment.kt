package com.example.litejoin.fragment

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
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
            // 채팅방 클릭 시 ChatActivity로 이동
            val intent = Intent(requireContext(), ChatActivity::class.java)
            intent.putExtra("PARTNER_UID", partnerUid)
            startActivity(intent)
        }

        binding.rvChatRooms.adapter = chatRoomAdapter
    }

    private fun loadChatRooms() {
        // 'chatRooms' 노드에서 현재 사용자가 참여 중인 채팅방 목록을 가져옴
        realdb.getReference("chatRooms")
            .orderByChild("lastMessageTime") // 마지막 메시지 시간을 기준으로 정렬
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
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
                    Toast.makeText(context, "채팅 목록 로드 실패: ${error.message}", Toast.LENGTH_LONG).show()
                }
            })
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}