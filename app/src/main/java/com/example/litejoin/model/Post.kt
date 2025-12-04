package com.example.litejoin.model

import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

data class Post(
    val postId: String = "", // 게시글 고유 ID
    val authorUid: String = "", // 작성자 UID
    val authorNickname: String = "", // 작성자 닉네임
    val title: String = "", // 게시글 제목
    val shortDescription: String = "", // 한줄 소개
    val recruitmentCount: Int = 1, // 모집 인원
    val estimatedCost: Int = 0, // 예상 비용
    val location: String = "", // 만남/활동 장소 (추가된 필드)
    val content: String = "", // 추가 사항 (본문 내용)
    @ServerTimestamp
    val createdAt: Date? = null // 게시글 작성 시간 (정렬 기준)
)
