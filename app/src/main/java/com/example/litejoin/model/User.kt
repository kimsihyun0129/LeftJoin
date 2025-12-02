package com.example.litejoin.model

import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

data class User(
    val uid: String = "", // Firebase Auth UID
    val nickname: String = "", // 닉네임 (필수)
    val name: String? = null, // 이름
    val studentId: String? = null, // 학번
    val phoneNumber: String? = null, // 전화번호
    val profileImageUrl: String? = null, // 프로필 이미지 URL (Firebase Storage)
    @ServerTimestamp // 서버 시간을 자동 저장
    val createdAt: Date? = null
)
