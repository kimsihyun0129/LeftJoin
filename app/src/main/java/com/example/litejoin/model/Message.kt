package com.example.litejoin.model

data class Message(
    val senderUid: String = "", // 메시지 전송자 UID
    val text: String = "", // 메시지 내용
    val timestamp: Long = 0 // 전송 시간 (Epoch time)
)
