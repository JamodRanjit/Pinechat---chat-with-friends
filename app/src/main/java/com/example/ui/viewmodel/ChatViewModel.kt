package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.model.Friend
import com.example.data.model.Message
import com.example.data.model.UserProfile
import com.example.data.model.DailyGoal
import com.example.data.model.UserActivity
import com.example.data.repository.ChatRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.random.Random
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

import com.google.firebase.auth.FirebaseAuth

class ChatViewModel(application: Application) : AndroidViewModel(application) {
    private val authManager = com.example.util.FirebaseAuthManager()
    private val repository: ChatRepository

    val userProfile: StateFlow<UserProfile?>
    val friends: StateFlow<List<Friend>>

    val friendSuggestions: StateFlow<List<Friend>> by lazy {
        friends
            .map { currentFriends ->
                val addedNames = currentFriends.map { it.name }.toSet()
                repository.getPredefinedSuggestions().filter { it.name !in addedNames }
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    }

    private val _selectedFriendId = MutableStateFlow<Int?>(null)
    val selectedFriendId: StateFlow<Int?> = _selectedFriendId.asStateFlow()

    private val _showPointAnimation = MutableSharedFlow<Boolean>(extraBufferCapacity = 8)
    val showPointAnimation = _showPointAnimation.asSharedFlow()

    // Haptic feedback events
    private val _hapticEvents = MutableSharedFlow<String>(extraBufferCapacity = 8)
    val hapticEvents = _hapticEvents.asSharedFlow()

    // Dynamic state for active messages of selected friend
    val activeMessages: StateFlow<List<Message>> = _selectedFriendId
        .flatMapLatest { id ->
            if (id != null) {
                repository.getMessagesForFriend(id)
            } else {
                flowOf(emptyList())
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Simulated comments in the watch stream screen
    private val _streamComments = MutableStateFlow<List<StreamComment>>(emptyList())
    val streamComments: StateFlow<List<StreamComment>> = _streamComments.asStateFlow()

    // Live streaming simulations
    private val _activeStreamerFriend = MutableStateFlow<Friend?>(null)
    val activeStreamerFriend: StateFlow<Friend?> = _activeStreamerFriend.asStateFlow()

    // Floating hearts count on screen
    private val _floatingHeartsCount = MutableStateFlow(0)
    val floatingHeartsCount: StateFlow<Int> = _floatingHeartsCount.asStateFlow()

    // User's own stream comments
    private val _ownStreamComments = MutableStateFlow<List<StreamComment>>(emptyList())
    val ownStreamComments: StateFlow<List<StreamComment>> = _ownStreamComments.asStateFlow()

    private val _ownStreamViewerCount = MutableStateFlow(0)
    val ownStreamViewerCount: StateFlow<Int> = _ownStreamViewerCount.asStateFlow()

    private val _streak = MutableStateFlow(0)
    val streak: StateFlow<Int> = _streak

    init {
        val db = AppDatabase.getDatabase(application)
        repository = ChatRepository(db.userDao(), db.friendDao(), db.messageDao(), db.dailyGoalDao(), db.roomDao())
        
        loadStreak()
        
        userProfile = repository.userProfile.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            null
        )
        
        friends = repository.allFriends.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            emptyList()
        )

        viewModelScope.launch {
            repository.initializeDataIfNeeded()
            generateDailyQuestIfNeeded()
        }
    }

    private suspend fun generateDailyQuestIfNeeded() {
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val goals = repository.getGoalsForDate(today).first()
        if (goals.none { it.isAiGenerated }) {
            val quest = com.example.util.GeminiManager.generateQuest()
            repository.insertGoal(DailyGoal(task = quest, date = today, isAiGenerated = true))
        }
    }

    val dailyGoals = repository.getGoalsForDate(SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()))
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    
    private val userActivityDao = AppDatabase.getDatabase(application).userActivityDao()
    val recentActivities: StateFlow<List<UserActivity>> = userActivityDao.getRecentActivitiesFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun logActivity(description: String) {
        viewModelScope.launch {
            userActivityDao.insertActivity(com.example.data.model.UserActivity(description = description))
        }
    }

    private fun loadStreak() {
        viewModelScope.launch {
            _streak.value = repository.getStreak()
        }
    }

    fun addGoal(task: String) {
        viewModelScope.launch {
            repository.insertGoal(DailyGoal(task = task, date = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())))
        }
    }
    
    fun toggleGoal(goal: DailyGoal) {
        viewModelScope.launch {
            val newIsCompleted = !goal.isCompleted
            repository.updateGoal(goal.copy(isCompleted = newIsCompleted))
            if (newIsCompleted) {
                _hapticEvents.emit("success")
            }
        }
    }

    fun selectFriend(friendId: Int?) {
        _selectedFriendId.value = friendId
    }

    fun sendMessage(friendId: Int, text: String) {
        if (text.isBlank()) return
        viewModelScope.launch {
            checkAndApplyEasterEggs(text)
            repository.sendMessage(friendId, "user", text)
            logActivity("Sent message to a friend")
            _showPointAnimation.emit(true)

            // Auto mock response from friend
            delay(200) // Reduced delay for lower latency
            val friend = repository.getFriendById(friendId)
            if (friend != null) {
                val reply = getMockReply(friend.name, text)
                repository.sendMessage(friendId, "friend", reply)
                // Friend gains score to keep leaderboard interesting
                repository.updateFriendPoints(friendId, Random.nextInt(2, 5))
                _hapticEvents.emit("message")
            }
        }
    }

    fun sendVoiceMessage(friendId: Int, audioPath: String) {
        viewModelScope.launch {
            repository.insertVoiceMessage(friendId, audioPath)
            _showPointAnimation.emit(true)
            _hapticEvents.emit("message")
        }
    }

    fun sendAttachmentMessage(friendId: Int, attachmentType: String, attachmentPath: String) {
        viewModelScope.launch {
            repository.insertAttachmentMessage(friendId, attachmentType, attachmentPath)
            _showPointAnimation.emit(true)
            _hapticEvents.emit("message")
        }
    }

    // Session management for sign-up
    fun skipSignUp(onResult: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            val randomNames = listOf("Guest_User", "Random_Player", "Anonymous_Chatter", "New_Traveler")
            val name = randomNames.random()
            
            val db = AppDatabase.getDatabase(getApplication())
            val updatedUser = UserProfile(
                id = 1,
                name = name,
                bio = "Just browsing!",
                avatar = "avatar_user",
                points = 0,
                isStreaming = false,
                streamTitle = "",
                activeViewers = 0,
                isLoggedIn = true,
                passwordHash = ""
            )
            db.userDao().insertUserProfile(updatedUser)
            onResult(true)
        }
    }

    fun signUp(name: String, bio: String, avatar: String, email: String, password: String, onResult: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            val success = authManager.signUp(email, password)
            if (success) {
                val db = AppDatabase.getDatabase(getApplication())
                val updatedUser = UserProfile(
                    id = 1,
                    name = name,
                    bio = bio,
                    avatar = avatar,
                    points = 0,
                    isStreaming = false,
                    streamTitle = "",
                    activeViewers = 0,
                    isLoggedIn = true,
                    passwordHash = "" // Use Firebase Auth for security
                )
                db.userDao().insertUserProfile(updatedUser)
                onResult(true)
            } else {
                onResult(false)
            }
        }
    }

    // Session management for sign-in
    fun signIn(email: String, password: String, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val success = authManager.signIn(email, password)
            if (success) {
                // In a full implementation, you'd fetch the profile from Firestore here,
                // but for simplicity we keep the existing local profile.
                val db = AppDatabase.getDatabase(getApplication())
                val user = db.userDao().getUserProfile()
                if (user != null) {
                    db.userDao().insertUserProfile(user.copy(isLoggedIn = true))
                    onResult(true)
                } else {
                    // Force create a local profile if not exists
                    onResult(true)
                }
            } else {
                onResult(false)
            }
        }
    }

    // Log out user
    fun signOut() {
        viewModelScope.launch {
            authManager.signOut()
            val db = AppDatabase.getDatabase(getApplication())
            db.userDao().logout()
        }
    }

    // Friend management
    fun addFriend(friend: Friend) {
        viewModelScope.launch {
            repository.addFriend(friend.copy(id = 0))
        }
    }

    fun addCustomFriend(name: String, bio: String = "Pine Chat User") {
        viewModelScope.launch {
            val customFriend = Friend(
                name = name,
                avatar = "avatar_${Random.nextInt(1, 7)}",
                points = Random.nextInt(10, 200),
                isOnline = Random.nextBoolean(),
                isStreaming = false,
                streamTitle = "",
                viewersCount = 0,
                bio = bio
            )
            repository.addFriend(customFriend)
        }
    }

    // Update custom profile info (username, bio, avatar string)
    fun updateProfile(name: String, bio: String, avatar: String) {
        viewModelScope.launch {
            val db = AppDatabase.getDatabase(getApplication())
            db.userDao().updateProfileInfo(name, bio, avatar)
            
            // Sync with Firebase
            val user = userProfile.value ?: UserProfile(id = 1, name = name, bio = bio, avatar = avatar, points = 0, isStreaming = false, streamTitle = "", activeViewers = 0, isLoggedIn = true, passwordHash = "")
            val updatedUser = user.copy(name = name, bio = bio, avatar = avatar)
            com.example.util.FirebaseManager.updateProfile(updatedUser, null)
        }
    }

    // Update custom profile info (username, bio, avatarUri)
    fun updateProfile(name: String, bio: String, avatarUri: Uri?) {
        val user = userProfile.value ?: return
        viewModelScope.launch {
            val db = AppDatabase.getDatabase(getApplication())
            
            // Define a temporary URI for the avatar if it's already a URL
            // This logic is simplified; in a production app, you'd handle file uploading appropriately.                
            val newAvatar = avatarUri?.toString() ?: user.avatar
            
            db.userDao().updateProfileInfo(name, bio, newAvatar)
            
            // Sync with Firebase
            val updatedUser = user.copy(name = name, bio = bio, avatar = newAvatar)
            com.example.util.FirebaseManager.updateProfile(updatedUser, avatarUri)
        }
    }

    // When chatting in live stream chat, that message also earns +1 point and gets written as direct message!
    fun sendStreamChatMessage(friendId: Int, text: String) {
        if (text.isBlank()) return
        viewModelScope.launch {
            checkAndApplyEasterEggs(text)
            val user = userProfile.value
            val userName = user?.name ?: "You"
            val userAvatar = user?.avatar ?: "avatar_user"
            
            // Add to simulated local comments visually
            val newComment = StreamComment(
                senderName = userName,
                text = text,
                avatarId = userAvatar,
                isMe = true
            )
            _streamComments.value = _streamComments.value + newComment

            // Earn user points directly, do not spam DM history
            repository.addUserPoints(1)
            _showPointAnimation.emit(true)

            // Auto-simulate the streamer replying after a delay
            delay(1500)
            if (_activeStreamerFriend.value?.id == friendId) {
                val reply = getMockReply(_activeStreamerFriend.value!!.name, text)
                
                // Add the streamer's reply to comments overlay
                val streamerComment = StreamComment(
                    senderName = _activeStreamerFriend.value!!.name,
                    text = reply,
                    avatarId = _activeStreamerFriend.value!!.avatar
                )
                _streamComments.value = _streamComments.value + streamerComment
                
                // Increment host's points, do not spam DM history
                repository.updateFriendPoints(friendId, 2)
            }
        }
    }

    private var lastChatterFriendId: Int? = null

    fun sendCommentOnOwnStream(text: String) {
        if (text.isBlank()) return
        viewModelScope.launch {
            checkAndApplyEasterEggs(text)
            val user = userProfile.value
            val userName = user?.name ?: "You"
            val userAvatar = user?.avatar ?: "avatar_user"
            
            val newComment = StreamComment(
                senderName = userName,
                text = text,
                avatarId = userAvatar,
                isMe = true
            )
            _ownStreamComments.value = _ownStreamComments.value + newComment

            // Earn +1 point for user commentary directly, do not spam DM history
            repository.addUserPoints(1)
            _showPointAnimation.emit(true)
        }
    }

    fun addHeart() {
        _floatingHeartsCount.value = _floatingHeartsCount.value + 1
    }

    // Start watching a friend's stream
    fun startWatchingStream(friend: Friend) {
        _activeStreamerFriend.value = friend
        _streamComments.value = listOf(
            StreamComment(friend.name, "Started the live stream! Welcome everyone! 🎉", friend.avatar)
        )
        _floatingHeartsCount.value = 0
        
        // Start simulated comments from other friends
        viewModelScope.launch {
            val otherFriends = friends.value.filter { it.name != friend.name }
            while (_activeStreamerFriend.value?.id == friend.id) {
                delay(Random.nextLong(2000, 5000))
                if (_activeStreamerFriend.value?.id != friend.id) break

                val randomFriend = otherFriends.randomOrNull() ?: continue
                val commentText = getRandomComment(randomFriend.name, friend.name, friend.streamTitle)
                val newComment = StreamComment(
                    senderName = randomFriend.name,
                    text = commentText,
                    avatarId = randomFriend.avatar
                )
                _streamComments.value = _streamComments.value + newComment
                
                // Randomly trigger standard floating hearts from audience
                if (Random.nextBoolean()) {
                    _floatingHeartsCount.value = _floatingHeartsCount.value + 1
                }
            }
        }
    }

    fun stopWatchingStream() {
        _activeStreamerFriend.value = null
        _streamComments.value = emptyList()
    }

    // Host own stream
    fun startOwnStream(title: String) {
        viewModelScope.launch {
            repository.startStreaming(title)
            _ownStreamComments.value = listOf(
                StreamComment("System", "Stream initialized. You are now Live! 🔴", "system_live")
            )
            _ownStreamViewerCount.value = 0
            lastChatterFriendId = null

            // Simulate viewers joining & typing comments
            launch {
                val availableFriends = friends.value
                val joinedFriends = mutableSetOf<String>()
                while (userProfile.value?.isStreaming == true) {
                    delay(Random.nextLong(1500, 3500))
                    if (userProfile.value?.isStreaming != true) break

                    // viewer joining code
                    if (joinedFriends.size < availableFriends.size && Random.nextDouble() < 0.7) {
                        val colleague = availableFriends.firstOrNull { it.name !in joinedFriends }
                        if (colleague != null) {
                            joinedFriends.add(colleague.name)
                            _ownStreamViewerCount.value = joinedFriends.size + Random.nextInt(10, 25)
                            _ownStreamComments.value = _ownStreamComments.value + StreamComment(
                                senderName = "System",
                                text = "${colleague.name} joined your live stream! 👋",
                                avatarId = colleague.avatar,
                                isSystem = true
                            )
                            delay(1000)
                            // Colleague types simple greeting
                            _ownStreamComments.value = _ownStreamComments.value + StreamComment(
                                senderName = colleague.name,
                                text = "Hey Alex! Awesome stream, title is sweet! 🔥",
                                avatarId = colleague.avatar
                            )
                            
                            // Save this viewer comment in local memory simulation, do not spam DM history
                            lastChatterFriendId = colleague.id
                            continue
                        }
                    }

                    // Otherwise friend leaves comment or reaction
                    if (joinedFriends.isNotEmpty() && Random.nextDouble() < 0.6) {
                        val commentator = joinedFriends.random()
                        val randomComment = getRandomCommentForUserStream(commentator)
                        val friendObj = availableFriends.firstOrNull { it.name == commentator }
                        val avatar = friendObj?.avatar ?: "avatar_1"
                        _ownStreamComments.value = _ownStreamComments.value + StreamComment(
                            senderName = commentator,
                            text = randomComment,
                            avatarId = avatar
                        )
                        
                        // Save this viewer comment in local memory simulation, do not spam DM history
                        if (friendObj != null) {
                            lastChatterFriendId = friendObj.id
                        }
                    }
                    
                    // Increment viewer count slightly
                    _ownStreamViewerCount.value = _ownStreamViewerCount.value + Random.nextInt(-2, 3).coerceAtLeast(1)
                }
            }
        }
    }

    fun stopOwnStream() {
        viewModelScope.launch {
            repository.stopStreaming()
            _ownStreamComments.value = emptyList()
            _ownStreamViewerCount.value = 0
        }
    }

    private suspend fun checkAndApplyEasterEggs(text: String) = withContext(Dispatchers.IO) {
        val trimmed = text.trim()
        val lower = trimmed.lowercase()
        if (lower.contains("yo yo bro")) {
            val prefs = getApplication<Application>().getSharedPreferences("pine_chat_prefs", Context.MODE_PRIVATE)
            val alreadyClaimed = prefs.getBoolean("yo_yo_bro_claimed", false)
            if (!alreadyClaimed) {
                prefs.edit().putBoolean("yo_yo_bro_claimed", true).apply()
                repository.addUserPoints(2)
                _showPointAnimation.emit(true)
            }
        }
        if (lower.contains("ranjit is best")) {
            repository.addUserPoints(999)
            _showPointAnimation.emit(true)
        }
    }

    private fun getMockReply(senderName: String, text: String): String {
        val lower = text.lowercase()
        return when {
            lower.contains("hello") || lower.contains("hi") || lower.contains("hey") -> {
                listOf(
                    "Hey! Hope you are having a great day! 😊",
                    "Hello! What's up?",
                    "Hey there, always good to hear from you."
                ).random()
            }
            lower.contains("stream") || lower.contains("live") || lower.contains("watch") -> {
                listOf(
                    "Omg yes, you should join my stream, I'm online! 🎥",
                    "I love streaming! Have you seen Sophia's workout stream?",
                    "I might start streaming in custom setup soon."
                ).random()
            }
            lower.contains("points") || lower.contains("earn") -> {
                "Yeah! 1 message is 1 point. I'm trying to beat your highscore on the board! 🏆"
            }
            lower.contains("how are you") || lower.contains("how's") -> {
                "Doing fantastic, just relaxing and chatting with you! You?"
            }
            lower.contains("cool") || lower.contains("awesome") || lower.contains("great") -> {
                "Absolutely! High five ✋"
            }
            else -> {
                listOf(
                    "That's so fascinating! Tell me more.",
                    "Haha wow, sounds like fun! Let's chat more later.",
                    "Super cool inside the app! Have you checked our leaderboard today? 🥇",
                    "True true. Let's start a stream party together!"
                ).random()
            }
        }
    }

    private fun getRandomComment(colleague: String, streamer: String, title: String): String {
        return listOf(
            "This stream is so relaxing! 😍",
            "Can you tell us more about this? 🤔",
            "You are doing amazing, ${streamer}!",
            "OMG this timing is perfect!",
            "Wooohoooo, go ${streamer}! 🔥🔥🔥",
            "I love the music/vibes!",
            "What setup are you using?",
            "Can't stop watching this live stream right now."
        ).random()
    }

    private fun getRandomCommentForUserStream(colleague: String): String {
        return listOf(
            "This is amazing, Alex! 👏👏",
            "Great to see you streaming today!",
            "Wow, how is your day going? 😊",
            "Can I ask a question about your stream setups?",
            "Pure quality!! 🚀🚀",
            "I'm showing this stream to all my friends!",
            "Let's goooo!",
            "Mind-blowing content right here."
        ).random()
    }
}

data class StreamComment(
    val senderName: String,
    val text: String,
    val avatarId: String,
    val isMe: Boolean = false,
    val isSystem: Boolean = false,
    val timestamp: Long = System.currentTimeMillis()
)
