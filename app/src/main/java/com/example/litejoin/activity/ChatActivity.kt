package com.example.litejoin.activity

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.litejoin.adapter.MessageAdapter
import com.example.litejoin.databinding.ActivityChatBinding
import com.example.litejoin.databinding.CustomToolbarChatBinding
import com.example.litejoin.model.Message
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.firestore.FirebaseFirestore

class ChatActivity : AppCompatActivity() {

    private lateinit var binding: ActivityChatBinding
    private lateinit var toolbarBinding: CustomToolbarChatBinding

    private val auth = FirebaseAuth.getInstance()
    private val realdb = FirebaseDatabase.getInstance()
    private val firestore = FirebaseFirestore.getInstance()

    private lateinit var chatRoomId: String
    private lateinit var partnerUid: String
    private lateinit var messageAdapter: MessageAdapter

    private val currentUid: String
        get() = auth.currentUser?.uid ?: throw IllegalStateException("User not logged in")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // ⬅️ [추가] 키보드 조정 모드를 강제로 적용하여 시스템 UI와의 충돌을 방지합니다.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.setDecorFitsSystemWindows(false)
        } else {
            @Suppress("DEPRECATION")
            window.setSoftInputMode(android.view.WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
        }

        binding = ActivityChatBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Intent에서 데이터 수신
        partnerUid = intent.getStringExtra("PARTNER_UID") ?: run {
            Toast.makeText(this, "채팅 상대방 정보가 없습니다.", Toast.LENGTH_SHORT).run { finish() }
            return
        }

        // 채팅방 ID 생성
        chatRoomId = generateChatRoomId(currentUid, partnerUid)

        setupToolbar()
        setupMessageList()

        binding.btnSend.setOnClickListener {
            sendMessage()
        }
    }

    private fun setupToolbar() {
        toolbarBinding = CustomToolbarChatBinding.bind(binding.toolbar.root)
        setSupportActionBar(toolbarBinding.root)
        supportActionBar?.setDisplayShowTitleEnabled(false)

        toolbarBinding.btnBack.setOnClickListener { finish() }

        // 상대방 닉네임 로드 및 툴바에 설정
        firestore.collection("users").document(partnerUid).get()
            .addOnSuccessListener { document ->
                val nickname = document.getString("nickname") ?: "알 수 없는 사용자"
                toolbarBinding.tvPartnerNickname.text = nickname
            }
    }

    private fun generateChatRoomId(uid1: String, uid2: String): String {
        return if (uid1 < uid2) "${uid1}_${uid2}" else "${uid2}_${uid1}"
    }

    // --- 메시지 목록 설정 및 로드 (Realtime Database) ---
    private fun setupMessageList() {
        messageAdapter = MessageAdapter(currentUid)
        binding.rvMessages.adapter = messageAdapter

        val layoutManager = binding.rvMessages.layoutManager as androidx.recyclerview.widget.LinearLayoutManager
        layoutManager.stackFromEnd = true // 메시지를 아래에서 위로 쌓기

        realdb.getReference("messages").child(chatRoomId)
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val messages = mutableListOf<Message>()
                    for (data in snapshot.children) {
                        data.getValue(Message::class.java)?.let { messages.add(it) }
                    }

                    // 날짜 헤더를 포함하여 리스트 업데이트
                    messageAdapter.submitListWithHeaders(messages)

                    // 최신 메시지로 스크롤
                    binding.rvMessages.scrollToPosition(messageAdapter.itemCount - 1)

                    // TODO: 채팅 목록 (ChatListFragment) 업데이트 로직 구현 필요
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(this@ChatActivity, "메시지 로드 실패: ${error.message}", Toast.LENGTH_SHORT).show()
                }
            })
    }

    // --- 메시지 전송 ---
    private fun sendMessage() {
        val text = binding.etMessageInput.text.toString().trim()
        if (text.isEmpty()) return

        val messageRef = realdb.getReference("messages").child(chatRoomId).push()
        val message = Message(
            senderUid = currentUid,
            text = text,
            timestamp = System.currentTimeMillis()
        )

        messageRef.setValue(message)
            .addOnSuccessListener {
                binding.etMessageInput.setText("") // 입력창 초기화

                // [필수] 채팅 목록 (ChatListFragment)의 lastMessage 업데이트 호출
                updateChatRoomLastMessage(text) // ⬅️ 이 함수가 성공적으로 호출되어야 합니다.
            }
            .addOnFailureListener {
                Toast.makeText(this, "메시지 전송 실패", Toast.LENGTH_SHORT).show()
            }
    }

    // --- 채팅방 정보 업데이트 (마지막 메시지/시간) ---
    private fun updateChatRoomLastMessage(lastMessage: String) {
        // [중요] Realtime DB의 chatRooms 노드에 데이터를 기록합니다.
        val chatRoomUpdate = mapOf(
            "lastMessage" to lastMessage,
            "lastMessageTime" to System.currentTimeMillis(),
            // 채팅방이 없다면 users 필드가 누락될 수 있으므로, 생성 로직을 포함합니다.
            "users/$currentUid" to true,
            "users/$partnerUid" to true
        )

        realdb.getReference("chatRooms").child(chatRoomId).updateChildren(chatRoomUpdate) // ⬅️ updateChildren 사용
        // updateChildren을 사용하면 chatRooms 문서가 없어도 자동으로 생성되며,
        // 기존 필드는 유지되고 새로운 필드(lastMessage, users)가 추가/업데이트됩니다.
    }
}