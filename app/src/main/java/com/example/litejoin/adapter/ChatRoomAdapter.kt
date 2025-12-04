// com.example.litejoin/adapter/ChatRoomAdapter.kt

package com.example.litejoin.adapter

import android.view.LayoutInflater
import android.view.View // View import 추가
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.litejoin.R
import com.example.litejoin.databinding.ItemChatRoomBinding
import com.example.litejoin.model.ChatRoom
import com.example.litejoin.model.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ChatRoomAdapter(private val itemClickListener: (ChatRoom, String) -> Unit) :
    ListAdapter<ChatRoom, ChatRoomAdapter.ChatRoomViewHolder>(ChatRoomDiffCallback()) {

    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()
    private val currentUid = auth.currentUser?.uid
    private val timeFormat = SimpleDateFormat("a h:mm", Locale.getDefault())

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChatRoomViewHolder {
        val binding = ItemChatRoomBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ChatRoomViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ChatRoomViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ChatRoomViewHolder(private val binding: ItemChatRoomBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(chatRoom: ChatRoom) {
            // 1. 상대방 UID 찾기
            val partnerUid = chatRoom.users.keys.firstOrNull { it != currentUid }

            if (partnerUid != null) {
                // 2. 상대방 정보 로드 (닉네임, 프로필)
                loadPartnerInfo(partnerUid) { user ->
                    if (user != null) {
                        binding.tvNickname.text = user.nickname
                        // Glide를 사용하여 프로필 이미지 로드
                        if (!user.profileImageUrl.isNullOrEmpty()) {
                            Glide.with(binding.root.context).load(user.profileImageUrl).into(binding.ivProfile)
                        } else {
                            binding.ivProfile.setImageResource(R.drawable.ic_default_profile)
                        }

                        // 3. 아이템 클릭 리스너 설정
                        binding.root.setOnClickListener {
                            itemClickListener(chatRoom, partnerUid)
                        }
                    }
                }
            }

            // 4. 메시지 및 시간 표시
            binding.tvLastMessage.text = chatRoom.lastMessage
            binding.tvLastTime.text = timeFormat.format(Date(chatRoom.lastMessageTime))

            // ⬅️ [핵심 수정] 게시글 제목 표시 로직 추가
            if (!chatRoom.postTitle.isNullOrEmpty()) {
                binding.tvPostTitle.visibility = View.VISIBLE
                binding.tvPostTitle.text = "/ ${chatRoom.postTitle}"
            } else {
                binding.tvPostTitle.visibility = View.GONE
            }
        }

        private fun loadPartnerInfo(uid: String, callback: (User?) -> Unit) {
            firestore.collection("users").document(uid).get()
                .addOnSuccessListener { document ->
                    callback(document.toObject(User::class.java))
                }
                .addOnFailureListener {
                    callback(null)
                }
        }
    }
}

class ChatRoomDiffCallback : DiffUtil.ItemCallback<ChatRoom>() {
    override fun areItemsTheSame(oldItem: ChatRoom, newItem: ChatRoom): Boolean {
        return oldItem.chatRoomId == newItem.chatRoomId
    }

    override fun areContentsTheSame(oldItem: ChatRoom, newItem: ChatRoom): Boolean {
        // 마지막 메시지와 시간, 그리고 게시글 제목까지 비교하여 변경 여부를 판단해야 합니다.
        return oldItem.lastMessage == newItem.lastMessage &&
                oldItem.lastMessageTime == newItem.lastMessageTime &&
                oldItem.postTitle == newItem.postTitle // ⬅️ postTitle 비교 추가
    }
}