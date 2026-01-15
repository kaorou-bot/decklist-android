package com.mtgo.decklistmanager.data.local.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.mtgo.decklistmanager.data.local.dao.*
import com.mtgo.decklistmanager.data.local.entity.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * App Database - Room 数据库主类
 */
@Database(
    entities = [
        DecklistEntity::class,
        CardEntity::class,
        CardInfoEntity::class,
        EventEntity::class  // 新增 Event 表
    ],
    version = 2,  // 版本升级：1 -> 2
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun decklistDao(): DecklistDao
    abstract fun cardDao(): CardDao
    abstract fun cardInfoDao(): CardInfoDao
    abstract fun eventDao(): EventDao  // 新增 EventDao

    companion object {
        private const val DATABASE_NAME = "decklists.db"

        @Volatile
        private var INSTANCE: AppDatabase? = null

        /**
         * 获取数据库实例（单例模式）
         */
        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    DATABASE_NAME
                )
                    .addCallback(DatabaseCallback())
                    .addMigrations(MIGRATION_1_2)  // 添加数据库迁移
                    .build()
                INSTANCE = instance
                instance
            }
        }

        /**
         * 数据库回调 - 用于初始化数据
         */
        private class DatabaseCallback : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                // 数据库创建时的操作
            }

            override fun onOpen(db: SupportSQLiteDatabase) {
                super.onOpen(db)
                // 数据库打开时的操作（如数据迁移）
            }
        }

        /**
         * 数据库版本 1 -> 2 迁移
         * 添加 Event 表，为 Decklist 添加外键
         */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // 1. 创建 events 表
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS events (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        event_name TEXT NOT NULL,
                        event_type TEXT,
                        format TEXT NOT NULL,
                        date TEXT NOT NULL,
                        source_url TEXT,
                        source TEXT NOT NULL,
                        deck_count INTEGER NOT NULL DEFAULT 0,
                        created_at INTEGER NOT NULL
                    )
                """)

                // 创建索引
                database.execSQL("CREATE INDEX IF NOT EXISTS index_event_name ON events(event_name)")
                database.execSQL("CREATE INDEX IF NOT EXISTS index_date ON events(date)")
                database.execSQL("CREATE INDEX IF NOT EXISTS index_format ON events(format)")
                database.execSQL("CREATE INDEX IF NOT EXISTS index_source ON events(source)")

                // 2. 为 decklists 表添加 event_id 列
                database.execSQL("ALTER TABLE decklists ADD COLUMN event_id INTEGER REFERENCES events(id) ON DELETE SET NULL")

                // 3. 从现有 decklists 数据聚合创建 events
                database.execSQL("""
                    INSERT INTO events (event_name, event_type, format, date, source_url, source, deck_count, created_at)
                    SELECT
                        event_name,
                        event_type,
                        format,
                        date,
                        url as source_url,
                        'MTGTop8' as source,
                        COUNT(*) as deck_count,
                        MAX(created_at) as created_at
                    FROM decklists
                    GROUP BY event_name, format, date
                    HAVING COUNT(*) > 0
                """)

                // 4. 更新 decklists 表的 event_id 外键
                database.execSQL("""
                    UPDATE decklists
                    SET event_id = (
                        SELECT id FROM events
                        WHERE events.event_name = decklists.event_name
                        AND events.format = decklists.format
                        AND events.date = decklists.date
                        LIMIT 1
                    )
                    WHERE event_id IS NULL
                """)
            }
        }
    }
}
