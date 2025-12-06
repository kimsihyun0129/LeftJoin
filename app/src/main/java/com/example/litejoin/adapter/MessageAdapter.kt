package com.example.litejoin.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.litejoin.R
import com.example.litejoin.databinding.ItemDateHeaderBinding
import com.example.litejoin.databinding.ItemMessageReceivedBinding
import com.example.litejoin.databinding.ItemMessageSentBinding
import com.example.litejoin.model.Message
import com.example.litejoin.util.Constants
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import com.bumptech.glide.Glide
import com.example.litejoin.model.User

class MessageAdapter(private val currentUid: String) :
    ListAdapter<Any, RecyclerView.ViewHolder>(MessageDiffCallback()) {

    // Firestore 및 Storage 인스턴스 (상대방 프로필 정보 로딩용)
    private val firestore = FirebaseFirestore.getInstance()
    private val storage = FirebaseStorage.getInstance()
    private val timeFormat = SimpleDateFormat("a h:mm", Locale.getDefault()) // 오전/오후 1:10
    private val dateFormat = SimpleDateFormat("yyyy년 M월 d일", Locale.getDefault())

    // 뷰 타입 반환
    override fun getItemViewType(position: Int): Int {
        return when (val item = getItem(position)) {
            is Message -> if (item.senderUid == currentUid) Constants.VIEW_TYPE_MESSAGE_SENT else Constants.VIEW_TYPE_MESSAGE_RECEIVED
            else -> Constants.VIEW_TYPE_DATE_HEADER // Message가 아닌 경우 Date Header
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            Constants.VIEW_TYPE_MESSAGE_SENT -> SentMessageViewHolder(ItemMessageSentBinding.inflate(inflater, parent, false))
            Constants.VIEW_TYPE_MESSAGE_RECEIVED -> ReceivedMessageViewHolder(ItemMessageReceivedBinding.inflate(inflater, parent, false))
            else -> DateHeaderViewHolder(ItemDateHeaderBinding.inflate(inflater, parent, false))
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is SentMessageViewHolder -> holder.bind(getItem(position) as Message)
            is ReceivedMessageViewHolder -> holder.bind(getItem(position) as Message)
            is DateHeaderViewHolder -> holder.bind(getItem(position) as DateHeader)
        }
    }

    // --- ViewHolder 정의 ---

    // 1. 내가 보낸 메시지
    inner class SentMessageViewHolder(private val binding: ItemMessageSentBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(message: Message) {
            binding.tvMessageBody.text = message.text
            binding.tvTimeSent.text = timeFormat.format(Date(message.timestamp))
        }
    }

    // 2. 상대방이 보낸 메시지
    inner class ReceivedMessageViewHolder(private val binding: ItemMessageReceivedBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(message: Message) {
            binding.tvMessageBody.text = message.text
            binding.tvTimeReceived.text = timeFormat.format(Date(message.timestamp))

            // 상대방 닉네임과 프로필 이미지 로딩 시작
            loadPartnerInfo(message.senderUid)
        }

        // 상대방 정보 로딩 및 바인딩
        private fun loadPartnerInfo(uid: String) {
            // Firestore에서 사용자 정보 문서 조회
            firestore.collection("users").document(uid).get()
                .addOnSuccessListener { document ->
                    val user = document.toObject(User::class.java)
                    user?.let {
                        // 닉네임 설정
                        binding.tvPartnerNickname.text = it.nickname ?: "알 수 없는 사용자"

                        // 프로필 이미지 로드
                        if (!it.profileImageUrl.isNullOrEmpty()) {
                            // Glide를 사용하여 CircleImageView에 이미지 로드
                            Glide.with(binding.root.context)
                                .load(it.profileImageUrl)
                                .placeholder(R.drawable.ic_default_profile) // 로딩 중 기본 이미지
                                .error(R.drawable.ic_default_profile)      // 로드 실패 시 기본 이미지
                                .into(binding.ivPartnerProfile) // iv_partner_profile은 item_message_received.xml에 정의된 ID입니다.
                        } else {
                            // 프로필 URL이 없으면 기본 이미지 설정
                            binding.ivPartnerProfile.setImageResource(R.drawable.ic_default_profile)
                        }
                    }
                }
                .addOnFailureListener {
                    // 로드 실패 시 기본 이미지 및 닉네임 설정
                    binding.tvPartnerNickname.text = "오류"
                    binding.ivPartnerProfile.setImageResource(R.drawable.ic_default_profile)
                }
        }
    }

    // 3. 날짜 헤더
    inner class DateHeaderViewHolder(private val binding: ItemDateHeaderBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(dateHeader: DateHeader) {
            binding.tvDateHeader.text = dateFormat.format(dateHeader.date)
        }
    }

    // --- 날짜 헤더 삽입을 위한 List 처리 ---

    // 날짜 헤더를 나타내기 위한 데이터 클래스
    data class DateHeader(val date: Date)

    // Realtime Database에서 가져온 메시지 리스트에 날짜 헤더를 삽입하여 최종 리스트를 구성합니다.
    fun submitListWithHeaders(messages: List<Message>) {
        if (messages.isEmpty()) {
            submitList(emptyList())
            return
        }

        val finalItems = mutableListOf<Any>()
        var lastDate: Date? = null

        for (message in messages.sortedBy { it.timestamp }) {
            val currentDate = Date(message.timestamp)

            // 날짜가 바뀌었는지 확인 (일 단위)
            if (lastDate == null || !isSameDay(lastDate, currentDate)) {
                finalItems.add(DateHeader(currentDate))
                lastDate = currentDate
            }
            finalItems.add(message)
        }
        submitList(finalItems)
    }

    // 두 Date 객체가 같은 날짜(일)인지 확인하는 헬퍼 함수
    private fun isSameDay(date1: Date, date2: Date): Boolean {
        val format = SimpleDateFormat("yyyyMMdd", Locale.getDefault())
        return format.format(date1) == format.format(date2)
    }
}

class MessageDiffCallback : DiffUtil.ItemCallback<Any>() {
    override fun areItemsTheSame(oldItem: Any, newItem: Any): Boolean {
        return if (oldItem is Message && newItem is Message) {
            oldItem.timestamp == newItem.timestamp && oldItem.senderUid == newItem.senderUid
        } else if (oldItem is MessageAdapter.DateHeader && newItem is MessageAdapter.DateHeader) {
            oldItem.date.time == newItem.date.time
        } else {
            false
        }
    }

    override fun areContentsTheSame(oldItem: Any, newItem: Any): Boolean {
        // 메시지 내용은 변경되지 않으므로, 데이터 클래스의 기본 equals() 사용
        return oldItem == newItem
    }
}