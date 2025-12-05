package com.example.litejoin.adapter

import android.view.LayoutInflater
import android.view.View
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

    // CircleImageView의 테두리 너비를 설정하는 DP 값 (안 읽었을 때)
    private val UNREAD_BORDER_WIDTH_DP = 4f
    private var density: Float = 1f // DPI 밀도 값

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChatRoomViewHolder {
        val binding = ItemChatRoomBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        density = parent.context.resources.displayMetrics.density // ⬅️ 밀도 계산
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

            // 게시글 제목 표시
            if (!chatRoom.postTitle.isNullOrEmpty()) {
                binding.tvPostTitle.visibility = View.VISIBLE
                binding.tvPostTitle.text = " / +${chatRoom.postTitle}"
            } else {
                binding.tvPostTitle.visibility = View.GONE
            }

            // ⬅️ [핵심 수정] 안 읽음 상태 확인 및 테두리 적용
            val lastReadTime = chatRoom.lastRead[currentUid] ?: 0L
            val lastMessageTime = chatRoom.lastMessageTime

            // lastReadTime이 lastMessageTime보다 작을 경우 (즉, 메시지가 읽음 시간보다 최신인 경우) 안 읽음
            val isUnread = lastMessageTime > lastReadTime

            // 1. 테두리 너비 설정
            if (isUnread) {
                // 읽지 않았으면 테두리 표시 (4dp)
                binding.ivProfile.setBorderWidth((UNREAD_BORDER_WIDTH_DP * density).toInt())

                // 닉네임과 마지막 메시지를 굵게 표시
                binding.tvNickname.setTypeface(null, android.graphics.Typeface.BOLD)
                binding.tvLastMessage.setTypeface(null, android.graphics.Typeface.BOLD)

            } else {
                // ⬅️ [수정] 읽었으면 테두리 숨김 (0dp)
                binding.ivProfile.setBorderWidth(0)

                // 닉네임과 마지막 메시지를 보통 굵기로 표시
                binding.tvNickname.setTypeface(null, android.graphics.Typeface.NORMAL)
                binding.tvLastMessage.setTypeface(null, android.graphics.Typeface.NORMAL)
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
        // 읽음 상태를 정확히 비교하기 위해 lastRead 맵 전체가 아닌, 해당 사용자의 읽음 시간만 비교합니다.
        val currentUid = FirebaseAuth.getInstance().currentUser?.uid
        val oldReadTime = oldItem.lastRead[currentUid] ?: 0L
        val newReadTime = newItem.lastRead[currentUid] ?: 0L

        // 데이터가 변경되었는지 확인하는 조건에 lastReadTime과 lastMessageTime의 변경을 포함
        return oldItem.lastMessage == newItem.lastMessage &&
                oldItem.lastMessageTime == newItem.lastMessageTime &&
                oldItem.postTitle == newItem.postTitle &&
                oldReadTime == newReadTime // ⬅️ 읽음 상태 비교
    }
}