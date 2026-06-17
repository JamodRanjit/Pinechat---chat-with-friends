package com.example.data.local

import androidx.room.*
import com.example.data.model.Friend
import com.example.data.model.Message
import com.example.data.model.UserProfile
import com.example.data.model.DailyGoal
import com.example.data.model.ChatRoom
import com.example.data.model.RoomMember
import com.example.data.model.UserActivity
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {
    @Query("SELECT * FROM user_profile WHERE id = 1")
    fun getUserProfileFlow(): Flow<UserProfile?>

    @Query("SELECT * FROM user_profile WHERE id = 1")
    suspend fun getUserProfile(): UserProfile?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUserProfile(profile: UserProfile)

    @Query("UPDATE user_profile SET points = points + :amount WHERE id = 1")
    suspend fun addPoints(amount: Int)

    @Query("UPDATE user_profile SET name = :name, bio = :bio, avatar = :avatar WHERE id = 1")
    suspend fun updateProfileInfo(name: String, bio: String, avatar: String)

    @Query("UPDATE user_profile SET isLoggedIn = 0 WHERE id = 1")
    suspend fun logout()

    @Query("UPDATE user_profile SET isStreaming = :isStreaming, streamTitle = :title, activeViewers = :viewers WHERE id = 1")
    suspend fun updateStreamingState(isStreaming: Boolean, title: String, viewers: Int)
}

@Dao
interface FriendDao {
    @Query("SELECT * FROM friends ORDER BY points DESC")
    fun getAllFriendsFlow(): Flow<List<Friend>>

    @Query("SELECT * FROM friends")
    suspend fun getAllFriends(): List<Friend>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFriends(friends: List<Friend>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFriend(friend: Friend)

    @Update
    suspend fun updateFriend(friend: Friend)

    @Query("UPDATE friends SET points = points + :amount WHERE id = :friendId")
    suspend fun addPointsToFriend(friendId: Int, amount: Int)

    @Query("UPDATE friends SET isStreaming = :isStreaming, streamTitle = :title, viewersCount = :viewers WHERE id = :friendId")
    suspend fun updateFriendStreaming(friendId: Int, isStreaming: Boolean, title: String, viewers: Int)
}

@Dao
interface MessageDao {
    @Query("SELECT * FROM messages WHERE friendId = :friendId ORDER BY timestamp ASC")
    fun getMessagesForFriendFlow(friendId: Int): Flow<List<Message>>

    @Query("SELECT * FROM messages WHERE roomId = :roomId ORDER BY timestamp ASC")
    fun getMessagesForRoomFlow(roomId: Int): Flow<List<Message>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: Message)

    @Query("SELECT * FROM messages ORDER BY timestamp DESC")
    suspend fun getAllMessages(): List<Message>

    @Query("DELETE FROM messages")
    suspend fun clearAllMessages()
}

@Dao
interface RoomDao {
    @Query("SELECT * FROM chat_rooms")
    fun getAllRoomsFlow(): Flow<List<ChatRoom>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRoom(room: ChatRoom): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRoomMember(member: RoomMember)

    @Query("SELECT * FROM chat_rooms WHERE id IN (SELECT roomId FROM room_members WHERE userId = :userId)")
    fun getRoomsForUserFlow(userId: Int): Flow<List<ChatRoom>>
}

@Dao
interface DailyGoalDao {
    @Query("SELECT * FROM daily_goals WHERE date = :date")
    fun getGoalsForDateFlow(date: String): Flow<List<DailyGoal>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGoal(goal: DailyGoal)

    @Update
    suspend fun updateGoal(goal: DailyGoal)
}

@Dao
interface UserActivityDao {
    @Query("SELECT * FROM user_activities ORDER BY timestamp DESC LIMIT 10")
    fun getRecentActivitiesFlow(): Flow<List<UserActivity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertActivity(activity: UserActivity)
}
