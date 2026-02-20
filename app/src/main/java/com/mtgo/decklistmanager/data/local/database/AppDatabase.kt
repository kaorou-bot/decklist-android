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
        EventEntity::class,
        FavoriteDecklistEntity::class,
        SearchHistoryEntity::class,
        FolderEntity::class,
        TagEntity::class,
        DecklistNoteEntity::class,
        DecklistFolderRelationEntity::class,
        DecklistTagRelationEntity::class
    ],
    version = 13,  // 版本升级：12 -> 13 (添加 oracle_id 和 en_name 字段到 cards 表)
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun decklistDao(): DecklistDao
    abstract fun cardDao(): CardDao
    abstract fun cardInfoDao(): CardInfoDao
    abstract fun eventDao(): EventDao
    abstract fun favoriteDecklistDao(): FavoriteDecklistDao
    abstract fun searchHistoryDao(): SearchHistoryDao
    abstract fun folderDao(): FolderDao
    abstract fun tagDao(): TagDao
    abstract fun decklistNoteDao(): DecklistNoteDao
    abstract fun decklistFolderRelationDao(): DecklistFolderRelationDao
    abstract fun decklistTagRelationDao(): DecklistTagRelationDao

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
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8, MIGRATION_8_9, MIGRATION_9_10, MIGRATION_10_11, MIGRATION_11_12, MIGRATION_12_13)
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

        /**
         * 数据库版本 7 -> 8 迁移
         * 添加 display_name 字段到 cards 表
         */
        val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // 为 cards 表添加 display_name 列
                db.execSQL("ALTER TABLE cards ADD COLUMN display_name TEXT")
            }
        }

        /**
         * 数据库版本 8 -> 9 迁移
         * 添加搜索历史表
         */
        val MIGRATION_8_9 = object : Migration(8, 9) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // 创建搜索历史表
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS search_history (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        query TEXT NOT NULL,
                        searchTime INTEGER NOT NULL,
                        resultCount INTEGER NOT NULL DEFAULT 0
                    )
                """)

                // 创建索引
                db.execSQL("CREATE INDEX IF NOT EXISTS index_search_time ON search_history(searchTime)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_query ON search_history(query)")
            }
        }

        /**
         * 数据库版本 9 -> 10 迁移
         * 添加双面牌反面攻防字段到 card_info 表
         */
        val MIGRATION_9_10 = object : Migration(9, 10) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // 为 card_info 表添加反面攻防列
                db.execSQL("ALTER TABLE card_info ADD COLUMN back_face_power TEXT")
                db.execSQL("ALTER TABLE card_info ADD COLUMN back_face_toughness TEXT")
                db.execSQL("ALTER TABLE card_info ADD COLUMN back_face_loyalty TEXT")
            }
        }

        /**
         * 数据库版本 10 -> 11 迁移
         * v4.3.0 新功能：文件夹、标签、备注
         */
        val MIGRATION_10_11 = object : Migration(10, 11) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // 创建 folders 表
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS folders (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        name TEXT NOT NULL,
                        color INTEGER NOT NULL,
                        icon TEXT NOT NULL,
                        created_at INTEGER NOT NULL,
                        updated_at INTEGER NOT NULL
                    )
                """)
                db.execSQL("CREATE INDEX IF NOT EXISTS index_folders_name ON folders(name)")

                // 创建 tags 表
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS tags (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        name TEXT NOT NULL,
                        color INTEGER NOT NULL,
                        created_at INTEGER NOT NULL
                    )
                """)
                db.execSQL("CREATE INDEX IF NOT EXISTS index_tags_name ON tags(name)")

                // 创建 decklist_notes 表
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS decklist_notes (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        decklist_id INTEGER NOT NULL,
                        note TEXT NOT NULL,
                        created_at INTEGER NOT NULL,
                        updated_at INTEGER NOT NULL,
                        UNIQUE(decklist_id)
                    )
                """)
                db.execSQL("CREATE INDEX IF NOT EXISTS index_decklist_notes_decklist_id ON decklist_notes(decklist_id)")

                // 创建 decklist_folder_relations 表
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS decklist_folder_relations (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        decklist_id INTEGER NOT NULL,
                        folder_id INTEGER NOT NULL,
                        created_at INTEGER NOT NULL,
                        FOREIGN KEY(decklist_id) REFERENCES decklists(id) ON DELETE CASCADE,
                        FOREIGN KEY(folder_id) REFERENCES folders(id) ON DELETE CASCADE
                    )
                """)
                db.execSQL("CREATE INDEX IF NOT EXISTS index_dfr_decklist_id ON decklist_folder_relations(decklist_id)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_dfr_folder_id ON decklist_folder_relations(folder_id)")

                // 创建 decklist_tag_relations 表
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS decklist_tag_relations (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        decklist_id INTEGER NOT NULL,
                        tag_id INTEGER NOT NULL,
                        created_at INTEGER NOT NULL
                    )
                """)
                db.execSQL("CREATE INDEX IF NOT EXISTS index_dtr_decklist_id ON decklist_tag_relations(decklist_id)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_dtr_tag_id ON decklist_tag_relations(tag_id)")
            }
        }

        /**
         * 数据库版本 11 -> 12 迁移
         * 添加 oracle_id 字段到 card_info 表用于印刷版本切换
         */
        val MIGRATION_11_12 = object : Migration(11, 12) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // 为 card_info 表添加 oracle_id 列
                db.execSQL("ALTER TABLE card_info ADD COLUMN oracle_id TEXT")
            }
        }

        /**
         * 数据库版本 12 -> 13 迁移
         * 添加 oracle_id 和 en_name 字段到 cards 表用于印刷版本切换和搜索
         */
        val MIGRATION_12_13 = object : Migration(12, 13) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // 为 cards 表添加 oracle_id 列
                db.execSQL("ALTER TABLE cards ADD COLUMN oracle_id TEXT")
                // 为 cards 表添加 en_name 列（英文名称，用于 API 搜索）
                db.execSQL("ALTER TABLE cards ADD COLUMN en_name TEXT")
            }
        }
    }
}
