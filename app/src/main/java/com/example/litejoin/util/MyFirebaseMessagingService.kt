package com.example.litejoin.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.TaskStackBuilder
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.litejoin.R
import com.example.litejoin.activity.ChatActivity
import com.example.litejoin.activity.MainActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class MyFirebaseMessagingService : FirebaseMessagingService() {

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        val data = remoteMessage.data
        val notification = remoteMessage.notification

        // 알림 제목과 내용은 notification 필드 우선, 없으면 data 필드 사용
        val title = notification?.title ?: data["title"] ?: "알림"
        val body = notification?.body ?: data["body"] ?: "새 메시지가 도착했습니다."

        sendNotification(title, body, data)
    }

    override fun onNewToken(token: String) {
        Log.d(TAG, "Refreshed token: $token")
        sendRegistrationToServer(token)
    }

    private fun sendRegistrationToServer(token: String) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val db = FirebaseFirestore.getInstance()
        
        db.collection("users").document(uid)
            .update("fcmToken", token)
            .addOnSuccessListener { Log.d(TAG, "Token saved to Firestore") }
            .addOnFailureListener { e -> Log.w(TAG, "Error saving token", e) }
    }

    private fun sendNotification(title: String, messageBody: String, data: Map<String, String>) {
        val partnerUid = data["partnerUid"]
        val pendingIntent: PendingIntent

        if (partnerUid != null) {
            // 채팅방으로 바로 이동 (MainActivity -> ChatActivity 스택 생성)
            val mainIntent = Intent(this, MainActivity::class.java)
            val chatIntent = Intent(this, ChatActivity::class.java)
            chatIntent.putExtra("PARTNER_UID", partnerUid)

            val stackBuilder = TaskStackBuilder.create(this)
            stackBuilder.addNextIntent(mainIntent)
            stackBuilder.addNextIntent(chatIntent)

            pendingIntent = stackBuilder.getPendingIntent(
                partnerUid.hashCode(), // 고유 ID 사용하여 덮어쓰기 방지
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        } else {
            // 일반 알림은 MainActivity로 이동
            val intent = Intent(this, MainActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            pendingIntent = PendingIntent.getActivity(
                this, 0, intent,
                PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
            )
        }

        val channelId = getString(R.string.default_notification_channel_id)
        val notificationBuilder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_app_logo)
            .setContentTitle(title)
            .setContentText(messageBody)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Channel human readable title",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            notificationManager.createNotificationChannel(channel)
        }

        notificationManager.notify(System.currentTimeMillis().toInt(), notificationBuilder.build())
    }

    companion object {
        private const val TAG = "MyFirebaseMsgService"
    }
}