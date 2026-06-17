package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.model.Friend
import com.example.data.model.Message
import com.example.data.model.UserProfile
import com.example.data.model.DailyGoal
import com.example.data.model.ChatRoom
import com.example.data.model.RoomMember
import com.example.data.model.UserActivity
import com.example.data.local.UserDao
import com.example.data.local.FriendDao
import com.example.data.local.MessageDao
import com.example.data.local.DailyGoalDao
import com.example.data.local.RoomDao
import com.example.data.local.UserActivityDao

@Database(entities = [UserProfile::class, Friend::class, Message::class, DailyGoal::class, ChatRoom::class, RoomMember::class, UserActivity::class], version = 10, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun friendDao(): FriendDao
    abstract fun messageDao(): MessageDao
    abstract fun dailyGoalDao(): DailyGoalDao
    abstract fun roomDao(): RoomDao
    abstract fun userActivityDao(): UserActivityDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "chat_stream_points_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
