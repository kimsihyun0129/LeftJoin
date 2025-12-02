package com.example.litejoin.model

data class ChatRoom(
    val chatRoomId: String = "", // 채팅방 고유 ID (UID1_UID2)
    val lastMessage: String = "", // 마지막 메시지 내용
    val lastMessageTime: Long = 0, // 마지막 메시지 전송 시간
    val users: HashMap<String, Boolean> = HashMap() // 참여 사용자 UID 목록
    // ChatRoom 자체에는 Realtime DB에서 직접 가져올 사용자 닉네임/프로필 정보는 포함하지 않습니다.
    // 이 정보는 Firestore의 User 컬렉션에서 가져와 사용해야 합니다.
)
