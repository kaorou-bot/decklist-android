# 开发快速参考指南

> 快速查阅常用文件、依赖和代码模式

---

## 📁 核心文件位置

### 数据层 (Data)
```
data/
├── local/
│   ├── dao/
│   │   ├── EventDao.kt          # 赛事数据访问
│   │   ├── DecklistDao.kt       # 套牌数据访问
│   │   ├── CardInfoDao.kt       # 卡牌信息访问
│   │   └── FavoriteDecklistDao.kt
│   ├── entity/
│   │   ├── EventEntity.kt       # 赛事实体
│   │   ├── DecklistEntity.kt    # 套牌实体
│   │   ├── CardEntity.kt        # 卡牌实体
│   │   └── CardInfoEntity.kt    # 卡牌信息实体
│   └── database/
│       └── AppDatabase.kt       # 数据库配置
├── remote/
│   └── api/
│       ├── mtgch/
│       │   ├── MtgchApi.kt      # MTGCH API 接口
│       │   ├── MtgchMapper.kt   # MTGCH 数据映射
│       │   └── MtgchCardDto.kt  # MTGCH 数据模型
│       ├── MtgTop8Scraper.kt    # MTGTop8 爬虫
│       └── ScryfallApi.kt       # Scryfall API
└── repository/
    └── DecklistRepository.kt    # 数据仓库（核心）
```

### 领域层 (Domain)
```
domain/model/
├── Event.kt          # 赛事领域模型
├── Decklist.kt       # 套牌领域模型
├── Card.kt           # 卡牌领域模型
└── CardInfo.kt       # 卡牌信息领域模型
```

### UI 层
```
ui/
├── decklist/
│   ├── MainActivity.kt           # 主界面
│   ├── MainViewModel.kt          # 主界面 ViewModel
│   ├── DeckDetailActivity.kt     # 套牌详情
│   ├── DeckDetailViewModel.kt    # 套牌详情 ViewModel
│   ├── EventDetailActivity.kt    # 赛事详情
│   ├── EventDetailViewModel.kt
│   └── CardAdapter.kt            # 卡牌列表适配器
└── carddetail/
    ├── CardDetailActivity.kt     # 卡牌详情
    └── CardDetailViewModel.kt
```

---

## 🔧 常用依赖

### 当前版本（v4.0.0）
```gradle
// 核心依赖
implementation 'androidx.core:core-ktx:1.12.0'
implementation 'androidx.appcompat:appcompat:1.6.1'
implementation 'com.google.android.material:material:1.10.0'

// 生命周期
def lifecycle_version = "2.7.0"
implementation "androidx.lifecycle:lifecycle-viewmodel-ktx:$lifecycle_version"
implementation "androidx.lifecycle:lifecycle-livedata-ktx:$lifecycle_version"

// 协程
def coroutine_version = "1.7.3"
implementation "org.jetbrains.kotlinx:kotlinx-coroutines-core:$coroutine_version"
implementation "org.jetbrains.kotlinx:kotlinx-coroutines-android:$coroutine_version"

// Room 数据库
def room_version = "2.6.1"
implementation "androidx.room:room-runtime:$room_version"
implementation "androidx.room:room-ktx:$room_version"
kapt "androidx.room:room-compiler:$room_version"

// Hilt 依赖注入
def hilt_version = "2.48"
implementation "com.google.dagger:hilt-android:$hilt_version"
kapt "com.google.dagger:hilt-compiler:$hilt_version"

// 网络
def retrofit_version = "2.9.0"
def okhttp_version = "4.12.0"
implementation "com.squareup.retrofit2:retrofit:$retrofit_version"
implementation "com.squareup.retrofit2:converter-gson:$retrofit_version"
implementation "com.squareup.okhttp3:okhttp:$okhttp_version"

// HTML 解析
implementation 'org.jsoup:jsoup:1.17.1'

// 图片加载
def glide_version = "4.16.0"
implementation "com.github.bumptech.glide:glide:$glide_version"
kapt "com.github.bumptech.glide:compiler:$glide_version"

// WorkManager
def work_version = "2.9.0"
implementation "androidx.work:work-runtime-ktx:$work_version"
```

### 新功能需要添加的依赖

```gradle
// v4.2.0 - 图表库
implementation 'com.github.PhilJay:MPAndroidChart:v3.1.0'

// v4.7.0 - 二维码
implementation 'com.google.zxing:core:3.5.1'
implementation 'com.journeyapps:zxing-android-embedded:4.3.0'

// v5.0.0 - AI API
implementation 'com.aallam.openai:openai-client:3.0.0'
```

---

## 💾 数据库模式

### 当前数据库版本
```kotlin
@Database(
    entities = [
        EventEntity::class,
        DecklistEntity::class,
        CardEntity::class,
        CardInfoEntity::class,
        FavoriteDecklistEntity::class
    ],
    version = 4,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase()
```

### 数据库迁移规则
```kotlin
// 版本 3 -> 4: 添加双面牌支持
val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(database: SupportSQLiteDatabase) {
        // 添加 card_faces_json 字段
        database.execSQL(
            "ALTER TABLE card_info ADD COLUMN card_faces_json TEXT"
        )
    }
}
```

---

## 🔄 常用代码模式

### ViewModel 模式
```kotlin
@HiltViewModel
class ExampleViewModel @Inject constructor(
    private val repository: DecklistRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<UiState>(UiState.Loading)
    val uiState: StateFlow<UiState> = _uiState

    fun loadData() {
        viewModelScope.launch {
            _uiState.value = UiState.Loading
            try {
                val data = repository.getData()
                _uiState.value = UiState.Success(data)
            } catch (e: Exception) {
                _uiState.value = UiState.Error(e.message)
            }
        }
    }
}
```

### Repository 模式
```kotlin
@Singleton
class DecklistRepository @Inject constructor(
    private val eventDao: EventDao,
    private val decklistDao: DecklistDao,
    private val mtgchApi: MtgchApi
) {
    suspend fun getDecklists(format: String?, date: String?): List<Decklist> {
        return withContext(Dispatchers.IO) {
            // 从数据库查询
            decklistDao.getDecklists(format, date).map { it.toDomainModel() }
        }
    }
}
```

### API 调用模式
```kotlin
// MTGCH API
suspend fun searchCard(cardName: String): CardInfo? {
    return try {
        val response = mtgchApi.searchCard(cardName)
        if (response.isSuccessful && response.body() != null) {
            val cards = response.body()!!.data
            if (!cards.isNullOrEmpty()) {
                mtgchMapper.toDomainModel(cards[0])
            } else null
        } else null
    } catch (e: Exception) {
        AppLogger.e("Card search failed", e)
        null
    }
}
```

### 数据库操作模式
```kotlin
@Dao
interface ExampleDao {
    @Query("SELECT * FROM table WHERE column = :value")
    suspend fun getByValue(value: String): List<Entity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: Entity)

    @Delete
    suspend fun delete(entity: Entity)
}
```

---

## 🎨 UI 组件模式

### Adapter 模式
```kotlin
class ExampleAdapter(
    private val onItemClick: (Item) -> Unit
) : RecyclerView.Adapter<ExampleAdapter.ViewHolder>() {

    private val items = mutableListOf<Item>()

    fun submitList(newItems: List<Item>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_layout, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        fun bind(item: Item) {
            // 绑定数据
            itemView.setOnClickListener { onItemClick(item) }
        }
    }
}
```

### Activity 模式
```kotlin
@AndroidEntryPoint
class ExampleActivity : BaseActivity() {

    private val viewModel: ExampleViewModel by viewModels()
    private lateinit var binding: ActivityExampleBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityExampleBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupObservers()
        setupClickListeners()
        viewModel.loadData()
    }

    private fun setupObservers() {
        lifecycleScope.launch {
            viewModel.uiState.collect { state ->
                when (state) {
                    is UiState.Loading -> showProgress()
                    is UiState.Success -> showData(state.data)
                    is UiState.Error -> showError(state.message)
                }
            }
        }
    }
}
```

---

## 🔍 常用调试技巧

### 查看数据库
```bash
# 使用 adb 查看数据库
adb shell
run-as com.mtgo.decklistmanager
cd databases
sqlite3 app_database.db
.tables
.schema events
SELECT * FROM events;
```

### 查看 Log
```kotlin
// 使用 AppLogger
AppLogger.d("Debug message")
AppLogger.e("Error message", exception)
AppLogger.i("Info message")

// 查看 Logcat
adb logcat -s "DecklistManager"
```

### 性能分析
```kotlin
// 使用 MeasureTime
val time = measureTimeMillis {
    // 执行操作
}
AppLogger.d("Operation took $time ms")
```

---

## 📝 版本发布流程

### 1. 更新版本
```gradle
// app/build.gradle
defaultConfig {
    versionCode 77        // 递增
    versionName "4.1.0"  // 修改
}
```

### 2. 更新 CHANGELOG
```markdown
## v4.1.0 (2026-02-XX)

### 新增
- 套牌导出功能
- 卡牌搜索功能

### 优化
- 深色模式支持
- 手势操作增强

### 修复
- 修复导出格式问题
```

### 3. 构建和签名
```bash
# 构建 Release APK
./gradlew assembleRelease

# 签名（已在 build.gradle 配置）
jarsigner -verbose -sigalg SHA256withRSA -digestalg SHA-256 \
  -keystore debug.keystore \
  -storepass android \
  -keypass android \
  app/build/outputs/apk/release/decklist-manager-v4.1.0-release-unsigned.apk \
  androiddebugkey

# 对齐
zipalign -v -p 4 \
  app/build/outputs/apk/release/decklist-manager-v4.1.0-release-unsigned.apk \
  decklist-manager-v4.1.0-release.apk
```

### 4. 测试清单
- [ ] 功能测试
- [ ] 兼容性测试
- [ ] 性能测试
- [ ] 崩溃测试
- [ ] 内存泄漏检查

---

## 🐛 常见问题解决

### 崩溃问题
```kotlin
// 全局异常处理
class CrashHandler : Thread.UncaughtExceptionHandler {
    override fun uncaughtException(t: Thread, e: Throwable) {
        AppLogger.e("Uncaught exception", e)
        // 保存日志
        // 重启应用
    }
}
```

### 网络问题
```kotlin
// 重试机制
suspend fun <T> retryIO(
    times: Int = 3,
    delay: Long = 1000,
    block: suspend () -> T
): T {
    repeat(times - 1) {
        try {
            return block()
        } catch (e: Exception) {
            delay(delay)
        }
    }
    return block() // 最后一次尝试
}
```

### 内存优化
```kotlin
// Glide 优化
Glide.with(context)
    .load(imageUrl)
    .placeholder(R.drawable.placeholder)
    .diskCacheStrategy(DiskCacheStrategy.ALL)
    .override(300, 400) // 限制尺寸
    .into(imageView)

// RecyclerView 优化
recyclerView.setHasFixedSize(true)
recyclerView.setItemViewCacheSize(20)
```

---

## 📚 参考资源

### 官方文档
- [Android Developers](https://developer.android.com/)
- [Kotlin Documentation](https://kotlinlang.org/docs/)
- [Material Design](https://material.io/design)

### 第三方库
- [Room Database](https://developer.android.com/training/data-storage/room)
- [Retrofit](https://square.github.io/retrofit/)
- [Hilt](https://developer.android.com/training/dependency-injection/hilt-android)
- [Glide](https://github.com/bumptech/glide)
- [MPAndroidChart](https://github.com/PhilJay/MPAndroidChart)

### MTG 相关
- [MTGCH API](https://mtgch.com/)
- [Scryfall API](https://scryfall.com/docs/api)
- [MTGTop8](https://mtgtop8.com/)

---

**最后更新：** 2026-01-31
**当前版本：** v4.0.0
