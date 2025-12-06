package com.example.litejoin.model

data class ChatRoom(
    val chatRoomId: String = "", // 채팅방 고유 ID (UID1_UID2)
    val lastMessage: String = "", // 마지막 메시지 내용
    val lastMessageTime: Long = 0, // 마지막 메시지 전송 시간
    val users: HashMap<String, Boolean> = HashMap(), // 참여 사용자 UID 목록
    val postTitle: String? = null, // 게시글 제목
    val lastRead: HashMap<String, Long> = HashMap() // 사용자별 마지막 읽음 시간
)
