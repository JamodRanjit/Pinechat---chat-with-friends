package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_profile")
data class UserProfile(
    @PrimaryKey val id: Int = 1,
    val name: String = "Alex Rivera",
    val bio: String = "Android Dev. Build, break, iterate.",
    val avatar: String = "avatar_user", // predefined ID (avatar_1..6) or custom image URL
    val points: Int = 0,
    val isStreaming: Boolean = false,
    val streamTitle: String = "",
    val activeViewers: Int = 0,
    val isVerified: Boolean = false,
    val isLoggedIn: Boolean = false,
    val passwordHash: String = ""
)

@Entity(tableName = "friends")
data class Friend(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val avatar: String, // e.g. "avatar_1", "avatar_2", etc.
    val points: Int,
    val isOnline: Boolean,
    val isStreaming: Boolean,
    val streamTitle: String,
    val viewersCount: Int = 0,
    val bio: String = "",
    val isVerified: Boolean = false
)

@Entity(tableName = "messages")
data class Message(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val friendId: Int? = null,
    val roomId: Int? = null,
    val senderId: String, // "user" or "friend"
    val senderName: String,
    val text: String, // Stored encrypted
    val timestamp: Long = System.currentTimeMillis(),
    val isEncrypted: Boolean = true,
    val senderIsVerified: Boolean = false,
    val audioFilePath: String? = null,
    val isVoiceMessage: Boolean = false,
    val attachmentType: String? = null, // "photo", "video", "location", "doc", "file"
    val attachmentPath: String? = null
)

@Entity(tableName = "chat_rooms")
data class ChatRoom(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "room_members",
        primaryKeys = ["roomId", "userId"])
data class RoomMember(
    val roomId: Int,
    val userId: Int
)

@Entity(tableName = "daily_goals")
data class DailyGoal(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val task: String,
    val isCompleted: Boolean = false,
    val date: String, // "yyyy-MM-dd"
    val isAiGenerated: Boolean = false
)

@Entity(tableName = "user_activities")
data class UserActivity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val description: String,
    val timestamp: Long = System.currentTimeMillis()
)

