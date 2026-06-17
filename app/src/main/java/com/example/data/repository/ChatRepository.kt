package com.example.data.repository

import com.example.data.local.UserDao
import com.example.data.local.FriendDao
import com.example.data.local.MessageDao
import com.example.data.local.DailyGoalDao
import com.example.data.local.RoomDao
import com.example.data.model.Friend
import com.example.data.model.Message
import com.example.data.model.UserProfile
import com.example.data.model.DailyGoal
import com.example.data.model.ChatRoom
import com.example.data.model.RoomMember
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlin.random.Random
import kotlinx.coroutines.flow.map
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.Calendar

class ChatRepository(
    private val userDao: UserDao,
    private val friendDao: FriendDao,
    private val messageDao: MessageDao,
    private val dailyGoalDao: DailyGoalDao,
    private val roomDao: RoomDao
) {
    val userProfile: Flow<UserProfile?> = userDao.getUserProfileFlow()
    val allFriends: Flow<List<Friend>> = friendDao.getAllFriendsFlow()

    fun getRoomsForUser(userId: Int): Flow<List<ChatRoom>> = roomDao.getRoomsForUserFlow(userId)
    suspend fun createRoom(name: String, members: List<Int>) {
        val roomId = roomDao.insertRoom(ChatRoom(name = name)).toInt()
        members.forEach { userId ->
            roomDao.insertRoomMember(RoomMember(roomId = roomId, userId = userId))
        }
    }

    suspend fun sendMessageToRoom(roomId: Int, senderId: String, text: String) {
        val userName = if (senderId == "user") {
            userDao.getUserProfile()?.name ?: "You"
        } else {
            "Friend"
        }
        val msg = Message(roomId = roomId, senderId = senderId, senderName = userName, text = text)
        messageDao.insertMessage(msg)
        if (senderId == "user") userDao.addPoints(1)
    }
    
    fun getMessagesForRoom(roomId: Int): Flow<List<Message>> = messageDao.getMessagesForRoomFlow(roomId)

    // ... (rest of the file as before)

    fun getGoalsForDate(date: String): Flow<List<DailyGoal>> = dailyGoalDao.getGoalsForDateFlow(date)

    suspend fun getStreak(): Int {
        val messages = messageDao.getAllMessages()
        if (messages.isEmpty()) return 0
        
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val dates = messages.map { sdf.format(Date(it.timestamp)) }.distinct().sortedDescending()
        
        var count = 0
        val calendar = Calendar.getInstance()
        val todayStr = sdf.format(Date())
        
        while (true) {
            val dateStr = sdf.format(calendar.time)
            if (dates.contains(dateStr)) {
                count++
            } else if (count == 0 && dateStr == todayStr) {
            } else {
                break
            }
            calendar.add(Calendar.DAY_OF_YEAR, -1)
        }
        
        return count
    }
    suspend fun insertGoal(goal: DailyGoal) = dailyGoalDao.insertGoal(goal)
    suspend fun updateGoal(goal: DailyGoal) = dailyGoalDao.updateGoal(goal)

    fun getMessagesForFriend(friendId: Int): Flow<List<Message>> {
        return messageDao.getMessagesForFriendFlow(friendId).map { messages ->
            messages.map { msg ->
                if (msg.isEncrypted) {
                    msg.copy(text = com.example.util.CryptoUtils.decrypt(msg.text))
                } else {
                    msg
                }
            }
        }
    }

    suspend fun initializeDataIfNeeded() {
        // Initialize User if not exists
        val user = userDao.getUserProfile()
        if (user == null) {
            userDao.insertUserProfile(
                UserProfile(
                    id = 1,
                    name = "Alex Rivera",
                    bio = "Android Dev. Build, break, iterate.",
                    avatar = "avatar_user",
                    points = 0,
                    isStreaming = false,
                    streamTitle = "",
                    activeViewers = 0,
                    isLoggedIn = false, // Starts logged out to trigger customauth screen
                    passwordHash = ""
                )
            )
        }
    }

    suspend fun addFriend(friend: Friend) {
        friendDao.insertFriend(friend)
    }

    fun getPredefinedSuggestions(): List<Friend> {
        return listOf(
            Friend(
                name = "Emma Myers",
                avatar = "avatar_1",
                points = 340,
                isOnline = true,
                isStreaming = true,
                streamTitle = "Chilling & Playing Piano 🎹",
                viewersCount = 65,
                bio = "Tech artist, amateur gamer, music enthusiast."
            ),
            Friend(
                name = "Olivia Chen",
                avatar = "avatar_3",
                points = 420,
                isOnline = true,
                isStreaming = false,
                streamTitle = "",
                viewersCount = 0,
                bio = "Digital painter & coffee addict and lo-fi playlist lover."
            ),
            Friend(
                name = "Lucas Miller",
                avatar = "avatar_4",
                points = 195,
                isOnline = false,
                isStreaming = false,
                streamTitle = "",
                viewersCount = 0,
                bio = "Chef in training, foodie first. Cooking is life."
            ),
            Friend(
                name = "Sophia K.",
                avatar = "avatar_5",
                points = 640,
                isOnline = true,
                isStreaming = true,
                streamTitle = "Morning Stretch Run Workout 👟",
                viewersCount = 210,
                bio = "Personal trainer, runner & wellness explorer."
            ),
            Friend(
                name = "Ethan Drake",
                avatar = "avatar_6",
                points = 80,
                isOnline = true,
                isStreaming = false,
                streamTitle = "",
                viewersCount = 0,
                bio = "Retro nerd. NES speedruns and game collector."
            )
        )
    }

    suspend fun insertAttachmentMessage(friendId: Int, attachmentType: String, attachmentPath: String): Message {
        val userName = userDao.getUserProfile()?.name ?: "You"
        val msg = Message(
            friendId = friendId,
            senderId = "user",
            senderName = userName,
            text = "Sent a $attachmentType",
            isEncrypted = false,
            attachmentType = attachmentType,
            attachmentPath = attachmentPath
        )
        messageDao.insertMessage(msg)
        userDao.addPoints(1)
        return msg
    }

    suspend fun insertVoiceMessage(friendId: Int, audioPath: String): Message {
        val userName = userDao.getUserProfile()?.name ?: "You"
        val msg = Message(
            friendId = friendId,
            senderId = "user",
            senderName = userName,
            text = "Voice message",
            isEncrypted = false,
            audioFilePath = audioPath,
            isVoiceMessage = true
        )
        messageDao.insertMessage(msg)
        userDao.addPoints(1)
        return msg
    }                

    suspend fun sendMessage(friendId: Int, senderId: String, text: String): Message {
        val userName = if (senderId == "user") {
            val user = userDao.getUserProfile()
            user?.name ?: "You"
        } else {
            val friends = friendDao.getAllFriends()
            friends.firstOrNull { it.id == friendId }?.name ?: "Friend"
        }

        val encryptedText = com.example.util.CryptoUtils.encrypt(text)

        val msg = Message(
            friendId = friendId,
            senderId = senderId,
            senderName = userName,
            text = encryptedText,
            isEncrypted = true
        )
        messageDao.insertMessage(msg)

        if (senderId == "user") {
            // Earn points: 1 message = 1 point!
            userDao.addPoints(1)
        } else {
            // Friend gains a point too
            friendDao.addPointsToFriend(friendId, 1)
        }

        return msg
    }

    suspend fun getFriendById(friendId: Int): Friend? {
        return friendDao.getAllFriends().firstOrNull { it.id == friendId }
    }

    suspend fun addUserPoints(amount: Int) {
        userDao.addPoints(amount)
    }

    suspend fun updateFriendPoints(friendId: Int, score: Int) {
        friendDao.addPointsToFriend(friendId, score)
    }

    suspend fun startStreaming(title: String) {
        userDao.updateStreamingState(
            isStreaming = true,
            title = title,
            viewers = Random.nextInt(45, 128)
        )
    }

    suspend fun stopStreaming() {
        userDao.updateStreamingState(
            isStreaming = false,
            title = "",
            viewers = 0
        )
    }

    suspend fun cleanMessages() {
        messageDao.clearAllMessages()
    }
}
