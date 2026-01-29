package com.mtgo.decklistmanager.data.local.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.mtgo.decklistmanager.data.local.dao.CardDao
import com.mtgo.decklistmanager.data.local.dao.CardInfoDao
import com.mtgo.decklistmanager.data.local.dao.DecklistDao
import com.mtgo.decklistmanager.data.local.dao.EventDao
import com.mtgo.decklistmanager.data.local.dao.FavoriteDecklistDao
import com.mtgo.decklistmanager.data.local.entity.CardEntity
import com.mtgo.decklistmanager.data.local.entity.CardInfoEntity
import com.mtgo.decklistmanager.data.local.entity.DecklistEntity
import com.mtgo.decklistmanager.data.local.entity.EventEntity
import com.mtgo.decklistmanager.data.local.entity.FavoriteDecklistEntity
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
        EventEntity::class,
        FavoriteDecklistEntity::class  // 收藏表
    ],
    version = 7,  // 版本升级：6 -> 7 (添加双面牌反面详细信息字段)
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun decklistDao(): DecklistDao
    abstract fun cardDao(): CardDao
    abstract fun cardInfoDao(): CardInfoDao
    abstract fun eventDao(): EventDao
    abstract fun favoriteDecklistDao(): FavoriteDecklistDao  // 收藏 DAO

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
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7)
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
            override fun migrate(db: SupportSQLiteDatabase) {
                // 1. 创建 events 表
                db.execSQL("""
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
                db.execSQL("CREATE INDEX IF NOT EXISTS index_event_name ON events(event_name)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_date ON events(date)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_format ON events(format)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_source ON events(source)")

                // 2. 为 decklists 表添加 event_id 列
                db.execSQL("ALTER TABLE decklists ADD COLUMN event_id INTEGER REFERENCES events(id) ON DELETE SET NULL")

                // 3. 从现有 decklists 数据聚合创建 events
                db.execSQL("""
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
                db.execSQL("""
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

        /**
         * 数据库版本 2 -> 3 迁移
         * 添加 FavoriteDecklist 表
         */
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // 创建 favorites 表
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS favorites (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        decklist_id INTEGER NOT NULL,
                        created_at INTEGER NOT NULL,
                        FOREIGN KEY(decklist_id) REFERENCES decklists(id) ON DELETE CASCADE
                    )
                """)

                // 创建索引
                db.execSQL("CREATE INDEX IF NOT EXISTS index_favorites_decklist_id ON favorites(decklist_id)")
            }
        }

        /**
         * 数据库版本 3 -> 4 迁移
         * 添加双面牌支持字段到 card_info 表
         */
        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // 为 card_info 表添加双面牌相关字段
                db.execSQL("ALTER TABLE card_info ADD COLUMN is_dual_faced INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE card_info ADD COLUMN card_faces_json TEXT")
                db.execSQL("ALTER TABLE card_info ADD COLUMN front_face_name TEXT")
                db.execSQL("ALTER TABLE card_info ADD COLUMN back_face_name TEXT")
                db.execSQL("ALTER TABLE card_info ADD COLUMN front_image_uri TEXT")
                db.execSQL("ALTER TABLE card_info ADD COLUMN back_image_uri TEXT")
            }
        }

        /**
         * 数据库版本 4 -> 5 迁移
         * 添加 deck_name 字段到 decklists 表
         */
        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // 为 decklists 表添加 deck_name 列
                db.execSQL("ALTER TABLE decklists ADD COLUMN deck_name TEXT")
            }
        }

        /**
         * 数据库版本 5 -> 6 迁移
         * 添加 en_name 字段到 card_info 表
         */
        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // 为 card_info 表添加 en_name 列
                db.execSQL("ALTER TABLE card_info ADD COLUMN en_name TEXT")

                // 创建索引
                db.execSQL("CREATE INDEX IF NOT EXISTS index_card_info_en_name ON card_info(en_name)")
            }
        }

        /**
         * 数据库版本 6 -> 7 迁移
         * 添加双面牌反面详细信息字段到 card_info 表
         */
        val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // 为 card_info 表添加反面信息列
                db.execSQL("ALTER TABLE card_info ADD COLUMN back_face_mana_cost TEXT")
                db.execSQL("ALTER TABLE card_info ADD COLUMN back_face_type_line TEXT")
                db.execSQL("ALTER TABLE card_info ADD COLUMN back_face_oracle_text TEXT")
            }
        }
    }
}
