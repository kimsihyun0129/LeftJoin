package com.example.litejoin.activity

import android.content.Intent
import android.os.Bundle
import android.view.WindowManager // WindowManager import 추가
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

    private var postTitle: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // ⬅️ [수정] 키보드 조정 모드를 강제로 적용하는 코드는 제거합니다.
        // 이 기능은 AndroidManifest.xml의 adjustPan 설정으로 처리합니다.

        binding = ActivityChatBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Intent에서 데이터 수신
        partnerUid = intent.getStringExtra("PARTNER_UID") ?: run {
            Toast.makeText(this, "채팅 상대방 정보가 없습니다.", Toast.LENGTH_SHORT).run { finish() }
            return
        }

        postTitle = intent.getStringExtra("POST_TITLE")

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

                    // ⬅️ [TODO] 채팅 목록 갱신은 ChatListFragment의 ValueEventListener가 담당하므로, 별도 로직 불필요.
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
                updateChatRoomLastMessage(text) // 채팅방 정보 업데이트 호출
            }
            .addOnFailureListener {
                Toast.makeText(this, "메시지 전송 실패", Toast.LENGTH_SHORT).show()
            }
    }

    // --- 채팅방 정보 업데이트 (마지막 메시지/시간) ---
    private fun updateChatRoomLastMessage(lastMessage: String) {
        // ⬅️ [수정] postTitle을 안전하게 추가하기 위해 mutableMapOf 사용
        val chatRoomUpdate = mutableMapOf<String, Any>(
            "lastMessage" to lastMessage,
            "lastMessageTime" to System.currentTimeMillis(),
            "users/$currentUid" to true,
            "users/$partnerUid" to true
        )

        postTitle?.let { title ->
            chatRoomUpdate["postTitle"] = title
        }

        realdb.getReference("chatRooms").child(chatRoomId).updateChildren(chatRoomUpdate)
    }
}