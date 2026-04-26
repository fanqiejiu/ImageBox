package com.example.myimageapplication

import android.Manifest
import android.content.Context
import android.content.ContentValues
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Base64
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DividerDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myimageapplication.ui.theme.MyImageApplicationTheme
import java.io.BufferedReader
import java.io.InputStreamReader
import java.text.SimpleDateFormat
import java.net.HttpURLConnection
import java.net.URLEncoder
import java.net.URL
import java.util.Date
import java.util.Locale
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val prefs = remember { getSharedPreferences("image-platform", Context.MODE_PRIVATE) }
            var darkMode by rememberSaveable { mutableStateOf(prefs.getBoolean("dark_mode", false)) }
            fun changeDarkMode(enabled: Boolean) {
                darkMode = enabled
                prefs.edit().putBoolean("dark_mode", enabled).apply()
            }

            MyImageApplicationTheme(darkTheme = darkMode, dynamicColor = false) {
                ImagePlatformRoot(
                    darkMode = darkMode,
                    onDarkModeChange = ::changeDarkMode,
                )
            }
        }
    }
}

@Composable
private fun ImagePlatformRoot(
    darkMode: Boolean,
    onDarkModeChange: (Boolean) -> Unit,
) {
    var showSplash by rememberSaveable { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        delay(1200)
        showSplash = false
    }

    if (showSplash) {
        LaunchSplashScreen()
    } else {
        ImagePlatformApp(
            darkMode = darkMode,
            onDarkModeChange = onDarkModeChange,
        )
    }
}

@Composable
private fun LaunchSplashScreen() {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            BrandAvatar(92.dp)
            Spacer(Modifier.height(18.dp))
            Text("Image box", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(6.dp))
            Text("v0.2.1beta", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
        }
    }
}

private enum class AppTab(val title: String, val glyph: String) {
    Create("创作", "✦"),
    Result("结果", "▣"),
    History("历史", "◷"),
    Tasks("任务", "↻"),
    Settings("设置", "⚙"),
}

private data class ModelInfo(
    val id: String,
    val name: String,
    val price: Int,
    val enabled: Boolean,
    val deprecated: Boolean,
    val supportsImageSize: Boolean,
    val endpoint: String = "/v1/draw/nano-banana",
)

private data class CustomProviderSummary(
    val id: String,
    val name: String,
    val apiUrl: String,
    val model: String,
    val apiKeySet: Boolean,
)

private data class CustomProviderDraft(
    val id: String,
    val name: String,
    val apiUrl: String,
    val apiKey: String = "",
    val apiKeySet: Boolean = false,
    val model: String,
)

private data class CustomProviderConfig(
    val id: String,
    val name: String,
    val apiUrl: String,
    val apiKey: String,
    val model: String,
)

private data class ModelStatusInfo(
    val checked: Boolean = false,
    val ok: Boolean = false,
    val error: String = "",
)

private data class SettingsInfo(
    val apiHost: String = "",
    val apiKeySet: Boolean = false,
    val defaultApiKeySet: Boolean = false,
    val modelProvider: String = "default",
    val activeCustomProviderId: String = DEFAULT_CUSTOM_PROVIDER_ID,
    val customProviders: List<CustomProviderSummary> = emptyList(),
    val customApiUrl: String = "",
    val customApiKeySet: Boolean = false,
    val customModel: String = "",
    val platformApiHost: String = "",
    val platformApiKeySet: Boolean = false,
    val platformTokenSet: Boolean = false,
    val creditQueryMode: String = "api_key",
    val llmApiUrl: String = "",
    val llmApiKeySet: Boolean = false,
    val llmModel: String = "",
    val llmRolePrompt: String = DEFAULT_LLM_ROLE_PROMPT,
    val saveDir: String = "output",
    val maxConcurrency: Int = 3,
    val maxRetries: Int = 2,
    val apiPlatform: String = "grsai",
)

private data class CreditsInfo(
    val balance: String = "--",
    val status: String = "idle",
    val error: String = "",
)

private data class ImageItem(
    val displayUrl: String,
    val filename: String,
    val prompt: String,
    val model: String,
    val modelName: String,
    val info: String,
    val createdAt: String,
    val taskId: String,
    val savedImageUri: String = "",
)

private data class QueueTask(
    val id: String,
    val prompt: String,
    val modelName: String,
    val cost: Int,
    val createdAt: String,
)

private data class ActiveTask(
    val id: String,
    val prompt: String,
    val modelName: String,
    val status: String,
    val progress: Double,
)

private data class FailedTask(
    val id: String,
    val prompt: String,
    val reason: String,
    val time: String,
)

private data class ServerState(
    val models: List<ModelInfo> = emptyList(),
    val modelStatus: Map<String, ModelStatusInfo> = emptyMap(),
    val defaultModel: String = "",
    val aspectRatios: List<String> = emptyList(),
    val imageSizes: List<String> = emptyList(),
    val settings: SettingsInfo = SettingsInfo(),
    val credits: CreditsInfo = CreditsInfo(),
    val history: List<ImageItem> = emptyList(),
    val archiveDates: List<String> = emptyList(),
    val pending: List<QueueTask> = emptyList(),
    val active: List<ActiveTask> = emptyList(),
    val failed: List<FailedTask> = emptyList(),
    val totalCount: Int = 0,
    val serverTime: String = "",
)

private data class ArchiveState(
    val date: String = "",
    val dates: List<String> = emptyList(),
    val history: List<ImageItem> = emptyList(),
)

private data class LocalImage(
    val name: String,
    val dataUrl: String,
)

private data class DirectJob(
    val taskId: String,
    val prompt: String,
    val model: String,
    val modelName: String,
    val endpoint: String,
    val cost: Int,
    val aspectRatio: String,
    val imageSize: String,
    val urls: List<String>,
    val startedAtMs: Long,
    val retries: Int = 0,
)

private data class DirectResult(
    val status: String,
    val progress: Double,
    val finalUrl: String = "",
    val failureReason: String = "",
)

private const val DEFAULT_API_HOST = "https://grsai.dakka.com.cn"
private const val CUSTOM_PROVIDERS_PREF = "custom_providers"
private const val ACTIVE_CUSTOM_PROVIDER_PREF = "active_custom_provider_id"
private const val DEFAULT_CUSTOM_PROVIDER_ID = "custom-default"
private const val DEFAULT_LLM_ROLE_PROMPT = "你是商业图像生成提示词优化助手。只输出优化后的提示词，不要解释，不要加标题。保留用户核心意图，补充主体、构图、光线、材质、风格、质量和画面完整性，适合图像生成模型。"

private data class ApiPlatformInfo(
    val id: String,
    val name: String,
    val apiUrl: String,
    val websiteUrl: String,
    val description: String,
)

private val API_PLATFORMS = listOf(
    ApiPlatformInfo(
        id = "grsai",
        name = "grsai",
        apiUrl = "https://grsai.dakka.com.cn",
        websiteUrl = "https://grsai.com/zh",
        description = "grsai AI 图像生成平台，提供 Nano Banana 等多款图像模型，支持高画质商业图像输出。",
    ),
    ApiPlatformInfo(
        id = "deepseek",
        name = "DeepSeek",
        apiUrl = "https://api.deepseek.com",
        websiteUrl = "https://platform.deepseek.com",
        description = "DeepSeek 深度求索，专注 AI 基础研究，提供先进的通用大语言模型与多模态能力。",
    ),
)
private val DIRECT_ASPECT_RATIOS = listOf("auto", "16:9", "9:16", "1:1", "4:3")
private val DIRECT_IMAGE_SIZES = listOf("1K", "2K", "4K")
private val PROMPT_PRESETS = listOf(
    "清透二次元头像，柔和自然光，精致五官，干净背景，高完成度插画",
    "商业产品摄影，干净棚拍光，真实材质，高级灰背景，电商主图构图",
    "游戏像素风角色立绘，清晰轮廓，明亮配色，适合移动端图标",
)
private val DIRECT_MODELS = listOf(
    ModelInfo("nano-banana-pro", "Nano Banana Pro", 1800, enabled = true, deprecated = false, supportsImageSize = true, endpoint = "/v1/draw/nano-banana"),
    ModelInfo("nano-banana", "Nano Banana", 1400, enabled = true, deprecated = false, supportsImageSize = true, endpoint = "/v1/draw/nano-banana"),
    ModelInfo("nano-banana-fast", "Nano Banana Fast", 440, enabled = true, deprecated = false, supportsImageSize = true, endpoint = "/v1/draw/nano-banana"),
    ModelInfo("gpt-image-2", "GPT Image 2", 600, enabled = true, deprecated = false, supportsImageSize = false, endpoint = "/v1/draw/completions"),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ImagePlatformApp(
    darkMode: Boolean,
    onDarkModeChange: (Boolean) -> Unit,
) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("image-platform", Context.MODE_PRIVATE) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    var serverState by remember { mutableStateOf(initialDirectState(prefs)) }
    var archiveState by remember { mutableStateOf(ArchiveState()) }
    var currentTab by rememberSaveable { mutableStateOf(AppTab.Create) }
    var selectedModel by rememberSaveable { mutableStateOf("") }
    var aspectRatio by rememberSaveable { mutableStateOf("auto") }
    var imageSize by rememberSaveable { mutableStateOf("4K") }
    var prompt by rememberSaveable { mutableStateOf("") }
    var networkUrls by rememberSaveable { mutableStateOf("") }
    var queueCount by rememberSaveable { mutableStateOf(1) }
    var saveToAlbum by rememberSaveable { mutableStateOf(prefs.getBoolean("save_to_album", false)) }
    var localImages by remember { mutableStateOf(emptyList<LocalImage>()) }
    var selectedResult by remember { mutableStateOf<ImageItem?>(null) }
    var galleryMode by rememberSaveable { mutableStateOf("session") }
    var selectedArchiveDate by rememberSaveable { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }
    var optimizingPrompt by remember { mutableStateOf(false) }
    var archiveLoading by remember { mutableStateOf(false) }
    var albumPermissionGranted by remember { mutableStateOf(hasAlbumWritePermission(context)) }
    var enableAlbumAfterPermissionRequest by remember { mutableStateOf(false) }
    var settingsDraft by remember { mutableStateOf(SettingsDraft.from(readDirectSettings(prefs))) }
    var settingsHydrated by remember { mutableStateOf(false) }
    var historySeenHydrated by remember { mutableStateOf(false) }
    var seenImageKeys by remember { mutableStateOf(setOf<String>()) }
    var directJobs by remember { mutableStateOf(emptyList<DirectJob>()) }

    fun directClient() = DirectApiClient.fromPrefs(prefs)

    fun show(message: String) {
        scope.launch { snackbarHostState.showSnackbar(message) }
    }

    val albumPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        albumPermissionGranted = granted
        if (granted && enableAlbumAfterPermissionRequest) {
            saveToAlbum = true
        }
        if (!granted) {
            saveToAlbum = false
        }
        enableAlbumAfterPermissionRequest = false
        show(if (granted) "相册写入权限已授权" else "未获得相册写入权限")
    }

    fun requestAlbumPermission(enableAfterGrant: Boolean = false) {
        if (!needsAlbumWritePermission()) {
            albumPermissionGranted = true
            if (enableAfterGrant) saveToAlbum = true
            show("当前 Android 版本保存到相册无需额外权限")
            return
        }
        if (hasAlbumWritePermission(context)) {
            albumPermissionGranted = true
            if (enableAfterGrant) saveToAlbum = true
            show("相册写入权限已授权")
            return
        }
        enableAlbumAfterPermissionRequest = enableAfterGrant
        albumPermissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
    }

    fun changeSaveToAlbum(enabled: Boolean) {
        if (enabled && !hasAlbumWritePermission(context)) {
            requestAlbumPermission(enableAfterGrant = true)
        } else {
            saveToAlbum = enabled
        }
    }

    fun itemKey(item: ImageItem): String =
        item.displayUrl.ifBlank { item.filename.ifBlank { item.taskId } }

    fun saveItemToAlbum(item: ImageItem, automatic: Boolean = false) {
        scope.launch {
            if (!hasAlbumWritePermission(context)) {
                show("请先在设置里授权相册写入权限")
                return@launch
            }
            val saved = saveRemoteImageToGallery(
                context = context,
                url = absoluteUrl("", item.displayUrl),
                requestedName = item.filename.ifBlank { "generated_${System.currentTimeMillis()}.png" },
            )
            if (saved != null) {
                val savedUri = saved.toString()
                val updatedHistory = serverState.history.map { historyItem ->
                    when {
                        item.taskId.isNotBlank() && historyItem.taskId == item.taskId -> historyItem.copy(savedImageUri = savedUri)
                        item.displayUrl.isNotBlank() && historyItem.displayUrl == item.displayUrl -> historyItem.copy(savedImageUri = savedUri)
                        else -> historyItem
                    }
                }
                val updatedState = serverState.copy(history = updatedHistory)
                serverState = updatedState
                persistDirectHistory(prefs, updatedState.history)
            }
            show(
                when {
                    saved != null && automatic -> "已自动保存到手机相册"
                    saved != null -> "已保存到手机相册"
                    else -> "保存到相册失败，请确认图片可访问"
                }
            )
        }
    }

    fun applyDirectState(newState: ServerState) {
        val previousKeys = seenImageKeys
        val nextKeys = newState.history.map(::itemKey).filter { it.isNotBlank() }.toSet()
        val newItems = if (historySeenHydrated) {
            newState.history.filter { itemKey(it).isNotBlank() && itemKey(it) !in previousKeys }
        } else {
            emptyList()
        }

        serverState = newState
        persistDirectHistory(prefs, newState.history)
        val availableModelIds = newState.models.filter { it.enabled && !it.deprecated }.map { it.id }
        if (selectedModel.isBlank() || selectedModel !in availableModelIds) {
            selectedModel = newState.defaultModel.ifBlank { availableModelIds.firstOrNull().orEmpty() }
        }
        if (!newState.aspectRatios.contains(aspectRatio)) aspectRatio = newState.aspectRatios.firstOrNull() ?: "auto"
        if (!newState.imageSizes.contains(imageSize)) imageSize = newState.imageSizes.firstOrNull() ?: "4K"
        if (!settingsHydrated) {
            settingsDraft = SettingsDraft.from(newState.settings)
            settingsHydrated = true
        }
        if (selectedArchiveDate.isBlank()) selectedArchiveDate = newState.archiveDates.firstOrNull() ?: ""

        seenImageKeys = previousKeys + nextKeys
        historySeenHydrated = true
        if (saveToAlbum && newItems.isNotEmpty()) {
            newItems.reversed().forEach { saveItemToAlbum(it, automatic = true) }
        }
    }

    fun refreshDirectState(showLoading: Boolean = false) {
        scope.launch {
            if (showLoading) loading = true
            try {
                val credits = directClient().refreshCredits()
                val models = readDirectModels(prefs)
                val statuses = directClient().modelStatuses(models)
                applyDirectState(
                    serverState.copy(
                        models = models,
                        defaultModel = readDefaultModelId(prefs),
                        settings = readDirectSettings(prefs),
                        credits = credits,
                        modelStatus = statuses,
                        serverTime = "直连平台 API",
                    )
                )
            } catch (error: Exception) {
                applyDirectState(serverState.copy(settings = readDirectSettings(prefs), serverTime = "直连平台 API"))
                show(error.message ?: "刷新失败")
            } finally {
                loading = false
            }
        }
    }

    fun submitCurrent() {
        scope.launch {
            try {
                loading = true
                val currentModel = serverState.models
                    .filter { it.enabled && !it.deprecated }
                    .let { models -> models.find { it.id == selectedModel } ?: models.firstOrNull() }
                if (currentModel == null) error("没有可用模型")
                if (!serverState.settings.apiKeySet) error("缺少生成 API Key")
                val invalidUrls = invalidNetworkUrlCount(networkUrls)
                if (invalidUrls > 0) error("有 $invalidUrls 个图片链接格式不正确")
                val urls = localImages.map { it.dataUrl } + parseNetworkUrls(networkUrls)
                val taskCost = if (serverState.settings.modelProvider != "custom") currentModel.price else -1
                val pendingTasks = (1..queueCount).map { index ->
                    QueueTask(
                        id = UUID.randomUUID().toString(),
                        prompt = prompt.trim(),
                        modelName = currentModel.name,
                        cost = taskCost,
                        createdAt = nowText("HH:mm:ss") + " · $index/$queueCount",
                    )
                }
                applyDirectState(serverState.copy(pending = serverState.pending + pendingTasks))
                selectedResult = null
                currentTab = AppTab.Tasks

                pendingTasks.forEach { pending ->
                    try {
                        applyDirectState(serverState.copy(pending = serverState.pending.filterNot { it.id == pending.id }))
                        val taskId = directClient().submitImage(
                            model = currentModel,
                            prompt = prompt.trim(),
                            aspectRatio = aspectRatio,
                            imageSize = imageSize,
                            urls = urls,
                        )
                        val job = DirectJob(
                            taskId = taskId,
                            prompt = prompt.trim(),
                            model = currentModel.id,
                            modelName = currentModel.name,
                            endpoint = currentModel.endpoint,
                            cost = taskCost,
                            aspectRatio = aspectRatio,
                            imageSize = imageSize,
                            urls = urls,
                            startedAtMs = System.currentTimeMillis(),
                        )
                        directJobs = directJobs + job
                        applyDirectState(
                            serverState.copy(
                                active = serverState.active + ActiveTask(taskId, prompt.trim(), currentModel.name, "submitted", 0.0),
                            )
                        )
                    } catch (error: Exception) {
                        val failed = FailedTask(pending.id, prompt.trim(), error.message ?: "提交失败", nowText("HH:mm:ss"))
                        applyDirectState(
                            serverState.copy(
                                pending = serverState.pending.filterNot { it.id == pending.id },
                                failed = listOf(failed) + serverState.failed,
                            )
                        )
                    }
                }
                show("任务已直连提交")
            } catch (error: Exception) {
                show(error.message ?: "提交失败")
            } finally {
                loading = false
            }
        }
    }

    fun optimizeCurrentPrompt() {
        scope.launch {
            try {
                optimizingPrompt = true
                val currentModelName = serverState.models.firstOrNull { it.id == selectedModel }?.name.orEmpty()
                val optimized = directClient().optimizePrompt(prompt, currentModelName)
                prompt = optimized
                show("提示词已优化")
            } catch (error: Exception) {
                show(error.message ?: "提示词优化失败")
            } finally {
                optimizingPrompt = false
            }
        }
    }

    fun loadArchive(date: String = selectedArchiveDate) {
        archiveState = ArchiveState(date = date)
        archiveLoading = false
    }

    LaunchedEffect(Unit) {
        refreshDirectState(showLoading = true)
    }

    LaunchedEffect(saveToAlbum) {
        prefs.edit().putBoolean("save_to_album", saveToAlbum).apply()
    }

    val activeModels = serverState.models.filter { it.enabled && !it.deprecated }
    val model = activeModels.find { it.id == selectedModel } ?: activeModels.firstOrNull()
    val currentHistory = if (galleryMode == "archive") archiveState.history else serverState.history
    val latestResult = selectedResult ?: currentHistory.firstOrNull()
    val invalidUrlCount = invalidNetworkUrlCount(networkUrls)
    val canSubmit = serverState.settings.apiKeySet && prompt.isNotBlank() && model != null && invalidUrlCount == 0 && !loading
    val useCreditEstimate = serverState.settings.modelProvider != "custom"
    val estimatedCost = if (useCreditEstimate) (model?.price ?: 0) * queueCount else 0
    val submitLabel = when {
        !serverState.settings.apiKeySet -> "先配置 Key"
        model == null -> "等待模型"
        invalidUrlCount > 0 -> "修正链接"
        prompt.isBlank() -> "填写提示词"
        else -> "提交 $queueCount 张"
    }
    val submitMeta = model?.let {
        if (useCreditEstimate) "${it.name} · 预计 $estimatedCost 积分" else it.name
    } ?: "保存 Key 后选择模型"

    suspend fun pollDirectJobsOnce() {
        val snapshot = directJobs
        snapshot.forEach { job ->
            val result = runCatching { directClient().result(job.taskId) }.getOrNull() ?: return@forEach
            if (result.status == "succeeded" && result.finalUrl.isNotBlank()) {
                directJobs = directJobs.filterNot { it.taskId == job.taskId }
                val item = ImageItem(
                    displayUrl = result.finalUrl,
                    filename = sanitizeImageFileName(result.finalUrl.substringAfterLast('/').substringBefore('?').ifBlank { "${job.taskId}.png" }),
                    prompt = job.prompt,
                    model = job.model,
                    modelName = job.modelName,
                    info = "${job.imageSize} | ${((System.currentTimeMillis() - job.startedAtMs) / 60000.0).formatOne()} 分 | 重试 ${job.retries} 次",
                    createdAt = nowText(),
                    taskId = job.taskId,
                )
                applyDirectState(
                    serverState.copy(
                        active = serverState.active.filterNot { it.id == job.taskId },
                        history = (listOf(item) + serverState.history).distinctBy { it.displayUrl }.take(120),
                        totalCount = serverState.totalCount + 1,
                    )
                )
                runCatching {
                    val credits = directClient().refreshCredits()
                    applyDirectState(serverState.copy(credits = credits))
                }
            } else if (result.status == "failed") {
                directJobs = directJobs.filterNot { it.taskId == job.taskId }
                applyDirectState(
                    serverState.copy(
                        active = serverState.active.filterNot { it.id == job.taskId },
                        failed = listOf(FailedTask(job.taskId, job.prompt, result.failureReason.ifBlank { "任务失败" }, nowText("HH:mm:ss"))) + serverState.failed,
                    )
                )
            } else {
                applyDirectState(
                    serverState.copy(
                        active = serverState.active.map {
                            if (it.id == job.taskId) it.copy(status = result.status.ifBlank { it.status }, progress = result.progress) else it
                        },
                    )
                )
            }
        }
    }

    LaunchedEffect(directJobs.size) {
        while (directJobs.isNotEmpty()) {
            delay(2500)
            pollDirectJobsOnce()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        BrandAvatar(36.dp)
                        Text("Image box", fontWeight = FontWeight.Bold)
                    }
                },
                actions = {
                    TextButton(onClick = { refreshDirectState(showLoading = true) }) {
                        Text(if (loading) "刷新中" else "刷新")
                    }
                },
            )
        },
        bottomBar = {
            PlatformBottomBar(
                currentTab = currentTab,
                onTabChange = { currentTab = it },
                taskCount = serverState.pending.size + serverState.active.size,
                historyCount = serverState.history.size,
                showDock = currentTab == AppTab.Create,
                submitLabel = submitLabel,
                submitMeta = submitMeta,
                canSubmit = canSubmit,
                onSubmit = ::submitCurrent,
            )
        },
    ) { padding ->
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            color = MaterialTheme.colorScheme.background,
        ) {
            when (currentTab) {
                AppTab.Create -> CreateScreen(
                    state = serverState,
                    models = activeModels,
                    modelStatus = serverState.modelStatus,
                    selectedModel = model,
                    selectedModelId = selectedModel,
                    showCreditEstimate = useCreditEstimate,
                    onModelChange = { selectedModel = it },
                    aspectRatio = aspectRatio,
                    onAspectRatioChange = { aspectRatio = it },
                    imageSize = imageSize,
                    onImageSizeChange = { imageSize = it },
                    prompt = prompt,
                    onPromptChange = { prompt = it },
                    onAppendPrompt = { extra ->
                        prompt = if (prompt.isBlank()) extra else "$prompt，$extra"
                    },
                    optimizingPrompt = optimizingPrompt,
                    onOptimizePrompt = ::optimizeCurrentPrompt,
                    invalidUrlCount = invalidUrlCount,
                    networkUrls = networkUrls,
                    onNetworkUrlsChange = { networkUrls = it },
                    queueCount = queueCount,
                    onQueueCountChange = { queueCount = it.coerceIn(1, 100) },
                    localImages = localImages,
                    onAddImages = { localImages = localImages + it },
                    onClearImages = { localImages = emptyList() },
                    onRemoveImage = { localImages = localImages - it },
                )

                AppTab.Result -> ResultScreen(
                    baseUrl = "",
                    item = latestResult,
                    busyText = if (serverState.active.isNotEmpty()) "正在生成..." else "等待开始...",
                    onOpen = { openUrl(context, absoluteUrl("", it.displayUrl)) },
                    onReusePrompt = {
                        prompt = it.prompt
                        currentTab = AppTab.Create
                    },
                    onUseAsReference = {
                        val reference = LocalImage(
                            name = it.filename.ifBlank { "result_${it.taskId.take(8)}.png" },
                            dataUrl = absoluteUrl("", it.displayUrl),
                        )
                        localImages = listOf(reference)
                        currentTab = AppTab.Create
                        show("已设为参考图，原有参考图已清空")
                    },
                    onSaveToAlbum = { saveItemToAlbum(it) },
                    onShowLatest = { selectedResult = null },
                    pinned = selectedResult != null,
                )

                AppTab.History -> HistoryScreen(
                    baseUrl = "",
                    mode = galleryMode,
                    onModeChange = {
                        galleryMode = it
                        selectedResult = null
                        if (it == "archive") {
                            val date = selectedArchiveDate.ifBlank { serverState.archiveDates.firstOrNull().orEmpty() }
                            selectedArchiveDate = date
                            loadArchive(date)
                        }
                    },
                    archiveDates = serverState.archiveDates,
                    selectedArchiveDate = selectedArchiveDate,
                    onArchiveDateChange = {
                        selectedArchiveDate = it
                        selectedResult = null
                        loadArchive(it)
                    },
                    archiveLoading = archiveLoading,
                    items = currentHistory,
                    onPick = {
                        selectedResult = it
                        currentTab = AppTab.Result
                    },
                    onClearCurrent = {
                        applyDirectState(serverState.copy(history = emptyList(), totalCount = 0))
                        selectedResult = null
                        show("当前记录已清空，手机相册图片保留")
                    },
                )

                AppTab.Tasks -> TasksScreen(
                    state = serverState,
                    onCancel = { queueId ->
                        applyDirectState(serverState.copy(pending = serverState.pending.filterNot { it.id == queueId }))
                    },
                )

                AppTab.Settings -> SettingsScreen(
                    draft = settingsDraft,
                    onDraftChange = { settingsDraft = it },
                    defaultKeySet = serverState.settings.defaultApiKeySet,
                    platformKeySet = serverState.settings.platformApiKeySet,
                    tokenSet = serverState.settings.platformTokenSet,
                    llmKeySet = serverState.settings.llmApiKeySet,
                    darkMode = darkMode,
                    onDarkModeChange = onDarkModeChange,
                    saveToAlbum = saveToAlbum,
                    onSaveToAlbumChange = ::changeSaveToAlbum,
                    albumPermissionGranted = albumPermissionGranted,
                    albumPermissionRequired = needsAlbumWritePermission(),
                    onRequestAlbumPermission = { requestAlbumPermission(enableAfterGrant = false) },
                    onRefreshCredits = {
                        refreshDirectState(showLoading = true)
                    },
                    onSave = {
                        scope.launch {
                            try {
                                saveDirectSettings(prefs, settingsDraft)
                                settingsHydrated = false
                                applyDirectState(
                                    serverState.copy(
                                        models = readDirectModels(prefs),
                                        defaultModel = readDefaultModelId(prefs),
                                        modelStatus = emptyMap(),
                                        settings = readDirectSettings(prefs),
                                        serverTime = "直连平台 API",
                                    )
                                )
                                settingsDraft = SettingsDraft.from(readDirectSettings(prefs))
                                show("设置已保存")
                            } catch (error: Exception) {
                                show(error.message ?: "保存失败")
                            }
                        }
                    },
                )
            }
        }
    }
}

@Composable
private fun BrandAvatar(size: Dp) {
    Image(
        painter = painterResource(id = R.drawable.app_icon),
        contentDescription = null,
        modifier = Modifier
            .size(size)
            .clip(RoundedCornerShape(10.dp)),
        contentScale = ContentScale.Crop,
    )
}

@Composable
private fun PlatformBottomBar(
    currentTab: AppTab,
    onTabChange: (AppTab) -> Unit,
    taskCount: Int,
    historyCount: Int,
    showDock: Boolean,
    submitLabel: String,
    submitMeta: String,
    canSubmit: Boolean,
    onSubmit: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 6.dp,
        shadowElevation = 10.dp,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding(),
        ) {
            if (showDock) {
                SubmitDock(
                    submitLabel = submitLabel,
                    submitMeta = submitMeta,
                    canSubmit = canSubmit,
                    onSubmit = onSubmit,
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                AppTab.entries.forEach { tab ->
                    val badgeCount = when (tab) {
                        AppTab.Tasks -> taskCount
                        AppTab.History -> historyCount
                        else -> 0
                    }
                    BottomTabItem(
                        tab = tab,
                        selected = currentTab == tab,
                        badgeCount = badgeCount,
                        onClick = { onTabChange(tab) },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

@Composable
private fun SubmitDock(
    submitLabel: String,
    submitMeta: String,
    canSubmit: Boolean,
    onSubmit: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text("创作任务", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(submitMeta, maxLines = 1, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.Bold)
        }
        Button(
            onClick = onSubmit,
            enabled = canSubmit,
            modifier = Modifier
                .height(50.dp)
                .widthIn(min = 118.dp),
        ) {
            Text(submitLabel, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun BottomTabItem(
    tab: AppTab,
    selected: Boolean,
    badgeCount: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val contentColor = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
    val chipColor = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        BadgedBox(
            badge = {
                if (badgeCount > 0) {
                    Badge { Text(if (badgeCount > 99) "99+" else badgeCount.toString()) }
                }
            },
        ) {
            Box(
                modifier = Modifier
                    .height(30.dp)
                    .widthIn(min = 48.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .background(chipColor),
                contentAlignment = Alignment.Center,
            ) {
                Text(tab.glyph, color = contentColor, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }
        }
        Text(
            tab.title,
            color = contentColor,
            style = MaterialTheme.typography.labelSmall,
            maxLines = 1,
            overflow = TextOverflow.Clip,
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CreateScreen(
    state: ServerState,
    models: List<ModelInfo>,
    modelStatus: Map<String, ModelStatusInfo>,
    selectedModel: ModelInfo?,
    selectedModelId: String,
    showCreditEstimate: Boolean,
    onModelChange: (String) -> Unit,
    aspectRatio: String,
    onAspectRatioChange: (String) -> Unit,
    imageSize: String,
    onImageSizeChange: (String) -> Unit,
    prompt: String,
    onPromptChange: (String) -> Unit,
    onAppendPrompt: (String) -> Unit,
    optimizingPrompt: Boolean,
    onOptimizePrompt: () -> Unit,
    invalidUrlCount: Int,
    networkUrls: String,
    onNetworkUrlsChange: (String) -> Unit,
    queueCount: Int,
    onQueueCountChange: (Int) -> Unit,
    localImages: List<LocalImage>,
    onAddImages: (List<LocalImage>) -> Unit,
    onClearImages: () -> Unit,
    onRemoveImage: (LocalImage) -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.GetMultipleContents()) { uris ->
        scope.launch {
            onAddImages(uris.mapNotNull { uri -> readUriAsDataUrl(context, uri) })
        }
    }
    LazyColumn(
        contentPadding = PaddingValues(14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            SummaryRow(state)
        }
        item {
            SectionTitle(
                "模型",
                selectedModel?.let { if (showCreditEstimate) "${it.price} 积分/张" else "" } ?: "未连接",
            )
            LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                items(models) { model ->
                    ModelOptionCard(
                        model = model,
                        status = modelStatus[model.id],
                        selected = selectedModelId == model.id,
                        showCreditEstimate = showCreditEstimate,
                        onClick = { onModelChange(model.id) },
                    )
                }
            }
        }
        item {
            SectionTitle("比例")
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                state.aspectRatios.forEach { ratio ->
                    FilterChip(selected = ratio == aspectRatio, onClick = { onAspectRatioChange(ratio) }, label = { Text(ratio) })
                }
            }
        }
        item {
            SectionTitle("分辨率")
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                state.imageSizes.forEach { size ->
                    FilterChip(
                        selected = size == imageSize,
                        enabled = selectedModel?.supportsImageSize != false,
                        onClick = { onImageSizeChange(size) },
                        label = { Text(size) },
                    )
                }
            }
        }
        item {
            OutlinedTextField(
                value = prompt,
                onValueChange = onPromptChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("提示词") },
                placeholder = { Text("描述你想生成的画面...") },
                minLines = 6,
            )
        }
        item {
            PromptTools(
                prompt = prompt,
                onPromptChange = onPromptChange,
                onAppendPrompt = onAppendPrompt,
                optimizing = optimizingPrompt,
                onOptimize = onOptimizePrompt,
            )
        }
        item {
            ElevatedCard {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                        Text("参考图 ${localImages.size} 张", fontWeight = FontWeight.Bold)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            TextButton(onClick = onClearImages, enabled = localImages.isNotEmpty()) { Text("清空") }
                            Button(onClick = { picker.launch("image/*") }) { Text("选择图片") }
                        }
                    }
                    if (localImages.isNotEmpty()) {
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(localImages) { image ->
                                ReferenceImageThumb(image, onRemove = { onRemoveImage(image) })
                            }
                        }
                    }
                }
            }
        }
        item {
            OutlinedTextField(
                value = networkUrls,
                onValueChange = onNetworkUrlsChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("网络图片链接") },
                placeholder = { Text("每行一个 http/https 图片链接") },
                minLines = 3,
                isError = invalidUrlCount > 0,
                supportingText = {
                    if (invalidUrlCount > 0) {
                        Text("有 $invalidUrlCount 个链接不是 http/https 图片地址")
                    }
                },
            )
        }
        item {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Text("队列数量", fontWeight = FontWeight.Bold)
                QuantityStepper(queueCount, onQueueCountChange)
            }
        }
        item {
            Text(
                text = when {
                    !state.settings.apiKeySet -> "底部提交栏已就绪，先到设置保存生成 Key。"
                    invalidUrlCount > 0 -> "修正图片链接后再提交。"
                    prompt.isBlank() -> "填写提示词后，可直接使用底部提交栏。"
                    else -> "底部提交栏会保持可见，滑动页面时也能随时提交。"
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PromptTools(
    prompt: String,
    onPromptChange: (String) -> Unit,
    onAppendPrompt: (String) -> Unit,
    optimizing: Boolean,
    onOptimize: () -> Unit,
) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        PROMPT_PRESETS.forEachIndexed { index, preset ->
            FilterChip(
                selected = false,
                onClick = { onPromptChange(preset) },
                label = { Text(listOf("头像", "产品", "像素")[index]) },
            )
        }
        FilledTonalButton(onClick = onOptimize, enabled = prompt.isNotBlank() && !optimizing) {
            Text(if (optimizing) "优化中" else "AI优化提示词")
        }
        AssistChip(onClick = { onAppendPrompt("高细节，干净构图，色彩协调，画面完整") }, label = { Text("增强") })
        AssistChip(onClick = { onPromptChange("") }, enabled = prompt.isNotBlank(), label = { Text("清空") })
        AssistChip(onClick = { onPromptChange(prompt.trim()) }, enabled = prompt != prompt.trim(), label = { Text("整理") })
    }
}

@Composable
private fun ReferenceImageThumb(image: LocalImage, onRemove: (() -> Unit)? = null) {
    val bitmap by produceState<Bitmap?>(initialValue = null, image.dataUrl) {
        value = withContext(Dispatchers.IO) { loadBitmap(image.dataUrl) }
    }
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(
            modifier = Modifier
                .width(76.dp)
                .padding(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.surface),
                contentAlignment = Alignment.Center,
            ) {
                if (bitmap == null) {
                    Text("图", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    Image(
                        bitmap = bitmap!!.asImageBitmap(),
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                    )
                }
                if (onRemove != null) {
                    Text(
                        "✕",
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .clip(RoundedCornerShape(bottomStart = 8.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.85f))
                            .clickable(onClick = onRemove)
                            .padding(horizontal = 5.dp, vertical = 2.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
            Text(image.name, maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.labelSmall)
        }
    }
}

@Composable
private fun ModelOptionCard(
    model: ModelInfo,
    status: ModelStatusInfo?,
    selected: Boolean,
    showCreditEstimate: Boolean,
    onClick: () -> Unit,
) {
    Surface(
        modifier = Modifier
            .widthIn(min = 168.dp, max = 220.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
        border = BorderStroke(
            width = if (selected) 1.5.dp else 1.dp,
            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
        ),
        tonalElevation = if (selected) 3.dp else 0.dp,
    ) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(model.name, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (showCreditEstimate) {
                Text("${model.price} 积分/张", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
            }
            ModelStatusPill(status)
            Text(
                if (model.supportsImageSize) "支持分辨率选择" else "自动分辨率",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun ModelStatusPill(status: ModelStatusInfo?) {
    val text = when {
        status == null || !status.checked -> "未检查"
        status.ok -> "可用"
        else -> "异常"
    }
    val color = when {
        status == null || !status.checked -> MaterialTheme.colorScheme.surfaceVariant
        status.ok -> MaterialTheme.colorScheme.tertiaryContainer
        else -> MaterialTheme.colorScheme.errorContainer
    }
    Surface(shape = RoundedCornerShape(999.dp), color = color) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
            style = MaterialTheme.typography.labelSmall,
            maxLines = 1,
        )
    }
}

@Composable
private fun QuantityStepper(value: Int, onChange: (Int) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedButton(
            onClick = { onChange(value - 1) },
            modifier = Modifier.size(44.dp),
            contentPadding = PaddingValues(0.dp),
        ) { Text("−", fontSize = 20.sp) }
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
        ) {
            Text(
                value.toString(),
                modifier = Modifier
                    .width(54.dp)
                    .padding(vertical = 10.dp),
                fontWeight = FontWeight.Bold,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            )
        }
        OutlinedButton(
            onClick = { onChange(value + 1) },
            modifier = Modifier.size(44.dp),
            contentPadding = PaddingValues(0.dp),
        ) { Text("+", fontSize = 20.sp) }
    }
}

@Composable
private fun OutputPreferenceCard(saveToAlbum: Boolean, onSaveToAlbumChange: (Boolean) -> Unit) {
    ElevatedCard {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onSaveToAlbumChange(!saveToAlbum) }
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("保存到手机相册", fontWeight = FontWeight.Bold)
                Text(
                    "开启后，新完成的图片会自动下载到系统图库；结果页也可手动保存。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Switch(checked = saveToAlbum, onCheckedChange = onSaveToAlbumChange)
        }
    }
}

@Composable
private fun AlbumPermissionCard(
    required: Boolean,
    granted: Boolean,
    onRequest: () -> Unit,
) {
    ElevatedCard {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("相册写入权限", fontWeight = FontWeight.Bold)
                Text(
                    when {
                        !required -> "当前系统使用系统相册接口保存图片，无需额外授权。"
                        granted -> "已授权，可以自动保存或手动保存到相册。"
                        else -> "当前系统需要授权后才能写入相册。"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (required && !granted) {
                Button(onClick = onRequest) { Text("授权") }
            } else {
                AssistChip(
                    onClick = {},
                    label = { Text(if (granted || !required) "可用" else "未授权") },
                )
            }
        }
    }
}

@Composable
private fun SummaryRow(state: ServerState) {
    val useCreditEstimate = state.settings.modelProvider != "custom"
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        if (useCreditEstimate) {
            SummaryCard("余额", state.credits.balance, Modifier.weight(1.2f))
        }
        SummaryCard("队列", (state.pending.size + state.active.size).toString(), Modifier.weight(1f))
        SummaryCard("生成", state.totalCount.toString(), Modifier.weight(1f))
    }
}

@Composable
private fun SummaryCard(label: String, value: String, modifier: Modifier = Modifier) {
    ElevatedCard(modifier = modifier) {
        Column(Modifier.padding(12.dp)) {
            Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun ResultScreen(
    baseUrl: String,
    item: ImageItem?,
    busyText: String,
    pinned: Boolean,
    onOpen: (ImageItem) -> Unit,
    onReusePrompt: (ImageItem) -> Unit,
    onUseAsReference: (ImageItem) -> Unit,
    onSaveToAlbum: (ImageItem) -> Unit,
    onShowLatest: () -> Unit,
) {
    LazyColumn(contentPadding = PaddingValues(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Row(horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                SectionTitle(if (item == null) "生成结果" else if (pinned) "查看历史" else "最新结果")
                if (pinned) TextButton(onClick = onShowLatest) { Text("显示最新") }
            }
        }
        item {
            ElevatedCard {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(420.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    if (item == null) {
                        Text(busyText, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    } else {
                        RemoteImage(absoluteUrl(baseUrl, item.displayUrl), Modifier.fillMaxSize())
                    }
                }
            }
        }
        if (item != null) {
            item {
                FlowMeta(listOf(item.modelName.ifBlank { item.model }, item.info, item.filename, item.createdAt).filter { it.isNotBlank() })
            }
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        Button(onClick = { onOpen(item) }, modifier = Modifier.weight(1f)) { Text("打开") }
                        FilledTonalButton(onClick = { onSaveToAlbum(item) }, modifier = Modifier.weight(1f)) { Text("存相册") }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        OutlinedButton(onClick = { onReusePrompt(item) }, modifier = Modifier.weight(1f)) { Text("复用提示词") }
                        OutlinedButton(onClick = { onUseAsReference(item) }, modifier = Modifier.weight(1f)) { Text("作为参考图") }
                    }
                }
            }
            item {
                if (item.prompt.isNotBlank()) {
                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                        Text(item.prompt, modifier = Modifier.padding(12.dp))
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun HistoryScreen(
    baseUrl: String,
    mode: String,
    onModeChange: (String) -> Unit,
    archiveDates: List<String>,
    selectedArchiveDate: String,
    onArchiveDateChange: (String) -> Unit,
    archiveLoading: Boolean,
    items: List<ImageItem>,
    onPick: (ImageItem) -> Unit,
    onClearCurrent: () -> Unit,
) {
    var filter by rememberSaveable { mutableStateOf("") }
    val filtered = if (filter.isBlank()) {
        items
    } else {
        items.filter {
            listOf(it.filename, it.prompt, it.modelName, it.info, it.createdAt)
                .joinToString(" ")
                .contains(filter, ignoreCase = true)
        }
    }
    LazyColumn(contentPadding = PaddingValues(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            SectionTitle("历史画廊", "${filtered.size}/${items.size} 张")
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = mode == "session", onClick = { onModeChange("session") }, label = { Text("当前记录") })
                FilterChip(selected = mode == "archive", onClick = { onModeChange("archive") }, label = { Text("保存归档") })
                OutlinedButton(onClick = onClearCurrent, enabled = mode == "session" && items.isNotEmpty()) { Text("清空当前") }
            }
        }
        if (mode == "archive") {
            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(archiveDates) { date ->
                        FilterChip(
                            selected = date == selectedArchiveDate,
                            onClick = { onArchiveDateChange(date) },
                            label = { Text(date) },
                        )
                    }
                }
            }
        }
        item {
            OutlinedTextField(value = filter, onValueChange = { filter = it }, modifier = Modifier.fillMaxWidth(), label = { Text("搜索") })
        }
        if (archiveLoading) {
            item { Text("正在加载归档...") }
        } else if (filtered.isEmpty()) {
            item { Text(if (mode == "archive") "该日期没有保存图片" else "暂无当前记录") }
        } else {
            items(filtered) { item ->
                HistoryCard(baseUrl, item, onPick)
            }
        }
    }
}

private sealed interface HistoryThumbnailState {
    data object Loading : HistoryThumbnailState
    data class Loaded(val bitmap: Bitmap) : HistoryThumbnailState
    data object Unavailable : HistoryThumbnailState
}

@Composable
private fun HistoryThumbnail(item: ImageItem, baseUrl: String, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val remoteUrl = absoluteUrl(baseUrl, item.displayUrl)
    val thumbnailState by produceState<HistoryThumbnailState>(
        initialValue = HistoryThumbnailState.Loading,
        item.savedImageUri,
        remoteUrl,
    ) {
        value = withContext(Dispatchers.IO) {
            val localBitmap = item.savedImageUri.takeIf { it.isNotBlank() }?.let { loadBitmap(context, it) }
            when {
                localBitmap != null -> HistoryThumbnailState.Loaded(localBitmap)
                remoteUrl.isBlank() -> HistoryThumbnailState.Unavailable
                else -> loadBitmap(remoteUrl)?.let { HistoryThumbnailState.Loaded(it) } ?: HistoryThumbnailState.Unavailable
            }
        }
    }
    when (val state = thumbnailState) {
        HistoryThumbnailState.Loading -> Box(
            modifier = modifier.background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center,
        ) {
            Text("加载中", style = MaterialTheme.typography.labelSmall)
        }
        is HistoryThumbnailState.Loaded -> Image(
            bitmap = state.bitmap.asImageBitmap(),
            contentDescription = null,
            modifier = modifier,
            contentScale = ContentScale.Crop,
        )
        HistoryThumbnailState.Unavailable -> Unit
    }
}

@Composable
private fun HistoryCard(baseUrl: String, item: ImageItem, onPick: (ImageItem) -> Unit) {
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onPick(item) },
    ) {
        Row(Modifier.padding(10.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            val hasThumbnailSource = item.savedImageUri.isNotBlank() || item.displayUrl.isNotBlank()
            if (hasThumbnailSource) {
                HistoryThumbnail(
                    item = item,
                    baseUrl = baseUrl,
                    modifier = Modifier
                        .size(92.dp)
                        .clip(RoundedCornerShape(10.dp)),
                )
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(item.filename.ifBlank { item.taskId.ifBlank { "保存图片" } }, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(item.modelName.ifBlank { item.model }, style = MaterialTheme.typography.bodySmall)
                Text(item.createdAt, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(item.prompt, maxLines = 2, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
private fun TasksScreen(state: ServerState, onCancel: (String) -> Unit) {
    LazyColumn(contentPadding = PaddingValues(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { SectionTitle("任务监控", "${state.active.size} 进行中 / ${state.pending.size} 待提交 / ${state.failed.size} 失败") }
        item { Text("进行中", fontWeight = FontWeight.Bold) }
        if (state.active.isEmpty()) item { Text("没有正在处理的任务") }
        items(state.active) { task ->
            ElevatedCard {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(task.modelName, fontWeight = FontWeight.Bold)
                    Text("ID ${task.id.take(8)} · ${task.status}")
                    LinearProgressIndicator(progress = { (task.progress / 100.0).toFloat().coerceIn(0f, 1f) }, modifier = Modifier.fillMaxWidth())
                    Text(task.prompt, maxLines = 2, overflow = TextOverflow.Ellipsis)
                }
            }
        }
        item { Text("待提交", fontWeight = FontWeight.Bold) }
        if (state.pending.isEmpty()) item { Text("没有等待提交的任务") }
        items(state.pending) { task ->
            ElevatedCard {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(task.modelName, fontWeight = FontWeight.Bold)
                    Text(if (task.cost >= 0) "${task.createdAt} · ${task.cost} 积分" else task.createdAt)
                    Text(task.prompt, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    OutlinedButton(onClick = { onCancel(task.id) }) { Text("取消") }
                }
            }
        }
        item { Text("失败", fontWeight = FontWeight.Bold) }
        if (state.failed.isEmpty()) item { Text("暂无失败记录") }
        items(state.failed) { task ->
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(task.id, fontWeight = FontWeight.Bold)
                    Text(task.reason)
                    Text(task.prompt, maxLines = 2, overflow = TextOverflow.Ellipsis)
                }
            }
        }
    }
}

private data class SettingsDraft(
    val modelProvider: String = "default",
    val apiHost: String = "",
    val apiKey: String = "",
    val activeCustomProviderId: String = DEFAULT_CUSTOM_PROVIDER_ID,
    val customProviders: List<CustomProviderDraft> = emptyList(),
    val platformApiHost: String = "",
    val platformApiKey: String = "",
    val platformToken: String = "",
    val creditQueryMode: String = "api_key",
    val llmApiUrl: String = "",
    val llmApiKey: String = "",
    val llmModel: String = "",
    val llmRolePrompt: String = DEFAULT_LLM_ROLE_PROMPT,
    val saveDir: String = "output",
    val maxConcurrency: String = "3",
    val maxRetries: String = "2",
    val apiPlatform: String = "grsai",
) {
    companion object {
        fun from(settings: SettingsInfo) = SettingsDraft(
            modelProvider = settings.modelProvider,
            apiHost = settings.apiHost,
            activeCustomProviderId = settings.activeCustomProviderId,
            customProviders = settings.customProviders
                .map {
                    CustomProviderDraft(
                        id = it.id,
                        name = it.name,
                        apiUrl = it.apiUrl,
                        apiKeySet = it.apiKeySet,
                        model = it.model,
                    )
                }
                .ifEmpty { listOf(blankCustomProviderDraft()) },
            platformApiHost = settings.platformApiHost,
            creditQueryMode = settings.creditQueryMode,
            llmApiUrl = settings.llmApiUrl,
            llmModel = settings.llmModel,
            llmRolePrompt = settings.llmRolePrompt.ifBlank { DEFAULT_LLM_ROLE_PROMPT },
            saveDir = settings.saveDir,
            maxConcurrency = settings.maxConcurrency.toString(),
            maxRetries = settings.maxRetries.toString(),
            apiPlatform = settings.apiPlatform,
        )
    }
}

private fun blankCustomProviderDraft(index: Int = 1): CustomProviderDraft =
    CustomProviderDraft(
        id = "custom-${UUID.randomUUID()}",
        name = "自定义平台 $index",
        apiUrl = "",
        model = "custom-image-model",
    )

private fun SettingsDraft.activeCustomProvider(): CustomProviderDraft =
    customProviders.firstOrNull { it.id == activeCustomProviderId }
        ?: customProviders.firstOrNull()
        ?: blankCustomProviderDraft()

private fun SettingsDraft.withCustomProvider(provider: CustomProviderDraft): SettingsDraft {
    val providers = customProviders.ifEmpty { listOf(provider) }
    val updated = if (providers.any { it.id == provider.id }) {
        providers.map { if (it.id == provider.id) provider else it }
    } else {
        providers + provider
    }
    return copy(customProviders = updated, activeCustomProviderId = provider.id)
}

private fun SettingsDraft.addCustomProvider(): SettingsDraft {
    val next = blankCustomProviderDraft(customProviders.size + 1)
    return copy(
        modelProvider = "custom",
        customProviders = customProviders + next,
        activeCustomProviderId = next.id,
    )
}

private fun SettingsDraft.removeActiveCustomProvider(): SettingsDraft {
    val remaining = customProviders.filterNot { it.id == activeCustomProviderId }
    val safeRemaining = remaining.ifEmpty { listOf(blankCustomProviderDraft()) }
    return copy(
        customProviders = safeRemaining,
        activeCustomProviderId = safeRemaining.first().id,
    )
}

private fun initialDirectState(prefs: SharedPreferences): ServerState = ServerState(
    models = readDirectModels(prefs),
    defaultModel = readDefaultModelId(prefs),
    aspectRatios = DIRECT_ASPECT_RATIOS,
    imageSizes = DIRECT_IMAGE_SIZES,
    settings = readDirectSettings(prefs),
    credits = CreditsInfo(status = "idle"),
    history = readDirectHistory(prefs),
    totalCount = readDirectHistory(prefs).size,
    serverTime = "直连平台 API",
)

private fun readDefaultModelId(prefs: SharedPreferences): String =
    if (prefs.getString("model_provider", "default") == "custom") {
        readActiveCustomProviderConfig(prefs).model.ifBlank { "custom-image-model" }
    } else {
        "nano-banana-pro"
    }

private fun readDirectModels(prefs: SharedPreferences): List<ModelInfo> {
    if (prefs.getString("model_provider", "default") != "custom") return DIRECT_MODELS
    val custom = readActiveCustomProviderConfig(prefs)
    val modelId = custom.model.ifBlank { "custom-image-model" }
    val providerName = custom.name.ifBlank { "自定义接口" }
    return listOf(
        ModelInfo(
            id = modelId,
            name = providerName,
            price = 0,
            enabled = true,
            deprecated = false,
            supportsImageSize = true,
            endpoint = custom.apiUrl,
        )
    )
}

private fun readActiveCustomProviderConfig(prefs: SharedPreferences): CustomProviderConfig {
    val providers = readCustomProviderConfigs(prefs)
    val activeId = prefs.getString(ACTIVE_CUSTOM_PROVIDER_PREF, "").orEmpty()
    return providers.firstOrNull { it.id == activeId }
        ?: providers.firstOrNull()
        ?: CustomProviderConfig(DEFAULT_CUSTOM_PROVIDER_ID, "自定义平台 1", "", "", "custom-image-model")
}

private fun readCustomProviderConfigs(prefs: SharedPreferences): List<CustomProviderConfig> {
    val raw = prefs.getString(CUSTOM_PROVIDERS_PREF, "").orEmpty()
    val parsed = runCatching {
        val array = JSONArray(raw)
        buildList {
            for (index in 0 until array.length()) {
                val item = array.optJSONObject(index) ?: continue
                val id = item.optString("id").ifBlank { "custom-${index + 1}" }
                add(
                    CustomProviderConfig(
                        id = id,
                        name = item.optString("name").ifBlank { "自定义平台 ${index + 1}" },
                        apiUrl = item.optString("api_url"),
                        apiKey = item.optString("api_key"),
                        model = item.optString("model").ifBlank { "custom-image-model" },
                    )
                )
            }
        }
    }.getOrDefault(emptyList())
    if (parsed.isNotEmpty()) return parsed

    val legacyUrl = prefs.getString("custom_api_url", "").orEmpty()
    val legacyKey = prefs.getString("custom_api_key", "").orEmpty()
    val legacyModel = prefs.getString("custom_model", "").orEmpty().ifBlank { "custom-image-model" }
    return listOf(
        CustomProviderConfig(
            id = DEFAULT_CUSTOM_PROVIDER_ID,
            name = "自定义平台 1",
            apiUrl = legacyUrl,
            apiKey = legacyKey,
            model = legacyModel,
        )
    )
}

private fun customProviderSummaries(configs: List<CustomProviderConfig>): List<CustomProviderSummary> =
    configs.map {
        CustomProviderSummary(
            id = it.id,
            name = it.name,
            apiUrl = it.apiUrl,
            model = it.model,
            apiKeySet = it.apiKey.isNotBlank(),
        )
    }

private fun readDirectHistory(prefs: SharedPreferences): List<ImageItem> {
    val raw = prefs.getString("direct_history", "[]").orEmpty()
    return runCatching {
        val array = JSONArray(raw)
        buildList {
            for (index in 0 until array.length()) {
                val json = array.optJSONObject(index) ?: continue
                val url = json.optString("display_url")
                if (url.isBlank()) continue
                add(
                    ImageItem(
                        displayUrl = url,
                        filename = json.optString("filename"),
                        prompt = json.optString("prompt"),
                        model = json.optString("model"),
                        modelName = json.optString("model_name"),
                        info = json.optString("info"),
                        createdAt = json.optString("created_at"),
                        taskId = json.optString("task_id"),
                        savedImageUri = json.optString("saved_image_uri"),
                    )
                )
            }
        }
    }.getOrDefault(emptyList())
}

private fun persistDirectHistory(prefs: SharedPreferences, history: List<ImageItem>) {
    val array = JSONArray()
    history.take(120).forEach { item ->
        array.put(
            JSONObject()
                .put("display_url", item.displayUrl)
                .put("filename", item.filename)
                .put("prompt", item.prompt)
                .put("model", item.model)
                .put("model_name", item.modelName)
                .put("info", item.info)
                .put("created_at", item.createdAt)
                .put("task_id", item.taskId)
                .put("saved_image_uri", item.savedImageUri)
        )
    }
    prefs.edit().putString("direct_history", array.toString()).apply()
}

private fun readDirectSettings(prefs: SharedPreferences): SettingsInfo {
    val provider = prefs.getString("model_provider", "default") ?: "default"
    val apiHost = normalizeRemoteBase(prefs.getString("api_host", DEFAULT_API_HOST) ?: DEFAULT_API_HOST)
    val defaultKeySet = !prefs.getString("api_key", "").isNullOrBlank()
    val customProviders = readCustomProviderConfigs(prefs)
    val activeCustomProvider = readActiveCustomProviderConfig(prefs)
    val customKeySet = activeCustomProvider.apiKey.isNotBlank()
    val platformHost = normalizeRemoteBase(prefs.getString("platform_api_host", apiHost) ?: apiHost)
    return SettingsInfo(
        apiHost = apiHost,
        apiKeySet = if (provider == "custom") customKeySet else defaultKeySet,
        defaultApiKeySet = defaultKeySet,
        modelProvider = provider,
        activeCustomProviderId = activeCustomProvider.id,
        customProviders = customProviderSummaries(customProviders),
        customApiUrl = activeCustomProvider.apiUrl,
        customApiKeySet = customKeySet,
        customModel = activeCustomProvider.model,
        platformApiHost = platformHost,
        platformApiKeySet = !prefs.getString("platform_api_key", "").isNullOrBlank(),
        platformTokenSet = !prefs.getString("platform_token", "").isNullOrBlank(),
        creditQueryMode = prefs.getString("credit_query_mode", "api_key") ?: "api_key",
        llmApiUrl = prefs.getString("llm_api_url", "").orEmpty(),
        llmApiKeySet = !prefs.getString("llm_api_key", "").isNullOrBlank(),
        llmModel = prefs.getString("llm_model", "").orEmpty(),
        llmRolePrompt = prefs.getString("llm_role_prompt", DEFAULT_LLM_ROLE_PROMPT).orEmpty().ifBlank { DEFAULT_LLM_ROLE_PROMPT },
        saveDir = "手机相册",
        maxConcurrency = prefs.getInt("max_concurrency", 3),
        maxRetries = prefs.getInt("max_retries", 2),
        apiPlatform = prefs.getString("api_platform", "grsai") ?: "grsai",
    )
}

private fun saveDirectSettings(prefs: SharedPreferences, draft: SettingsDraft) {
    val apiHost = normalizeRemoteBase(draft.apiHost.ifBlank { DEFAULT_API_HOST })
    val platformHost = normalizeRemoteBase(draft.platformApiHost.ifBlank { apiHost })
    val provider = if (draft.modelProvider == "custom") "custom" else "default"
    val customProviderDrafts = normalizeCustomProviderDrafts(draft.customProviders)
    val activeCustomDraft = customProviderDrafts.firstOrNull { it.id == draft.activeCustomProviderId }
        ?: customProviderDrafts.first()
    if (provider == "custom" && activeCustomDraft.apiUrl.isBlank()) error("自定义接口需要填写 URL")
    if (provider == "custom" && activeCustomDraft.model.isBlank()) error("自定义接口需要填写 Model")
    val previousProviders = readCustomProviderConfigs(prefs).associateBy { it.id }
    val customProviderConfigs = customProviderDrafts.map { item ->
        val retainedKey = previousProviders[item.id]?.apiKey.orEmpty()
        CustomProviderConfig(
            id = item.id,
            name = item.name,
            apiUrl = item.apiUrl,
            apiKey = item.apiKey.trim().ifBlank { retainedKey },
            model = item.model,
        )
    }
    val activeCustomConfig = customProviderConfigs.firstOrNull { it.id == activeCustomDraft.id }
        ?: customProviderConfigs.first()
    prefs.edit()
        .putString("model_provider", provider)
        .putString("api_host", apiHost)
        .putString(ACTIVE_CUSTOM_PROVIDER_PREF, activeCustomConfig.id)
        .putString(CUSTOM_PROVIDERS_PREF, customProviderConfigsJson(customProviderConfigs).toString())
        .putString("custom_api_url", activeCustomConfig.apiUrl)
        .putString("custom_model", activeCustomConfig.model)
        .putString("platform_api_host", platformHost)
        .putString("credit_query_mode", if (draft.creditQueryMode == "token") "token" else "api_key")
        .putString("llm_api_url", draft.llmApiUrl.trim())
        .putString("llm_model", draft.llmModel.trim())
        .putString("llm_role_prompt", draft.llmRolePrompt.trim().ifBlank { DEFAULT_LLM_ROLE_PROMPT })
        .putInt("max_concurrency", (draft.maxConcurrency.toIntOrNull() ?: 3).coerceIn(1, 10))
        .putInt("max_retries", (draft.maxRetries.toIntOrNull() ?: 2).coerceIn(0, 10))
        .putString("api_platform", draft.apiPlatform)
        .apply()
    if (draft.apiKey.isNotBlank()) prefs.edit().putString("api_key", draft.apiKey.trim()).apply()
    if (activeCustomConfig.apiKey.isNotBlank()) prefs.edit().putString("custom_api_key", activeCustomConfig.apiKey).apply()
    if (draft.platformApiKey.isNotBlank()) prefs.edit().putString("platform_api_key", draft.platformApiKey.trim()).apply()
    if (draft.platformToken.isNotBlank()) prefs.edit().putString("platform_token", draft.platformToken.trim()).apply()
    if (draft.llmApiKey.isNotBlank()) prefs.edit().putString("llm_api_key", draft.llmApiKey.trim()).apply()
}

private fun normalizeCustomProviderDrafts(providers: List<CustomProviderDraft>): List<CustomProviderDraft> {
    val safeProviders = providers.ifEmpty { listOf(blankCustomProviderDraft()) }
    return safeProviders.mapIndexed { index, item ->
        item.copy(
            id = item.id.ifBlank { "custom-${UUID.randomUUID()}" },
            name = item.name.trim().ifBlank { "自定义平台 ${index + 1}" },
            apiUrl = item.apiUrl.trim(),
            model = item.model.trim().ifBlank { "custom-image-model" },
        )
    }
}

private fun customProviderConfigsJson(configs: List<CustomProviderConfig>): JSONArray {
    val array = JSONArray()
    configs.forEach { item ->
        array.put(
            JSONObject()
                .put("id", item.id)
                .put("name", item.name)
                .put("api_url", item.apiUrl)
                .put("api_key", item.apiKey)
                .put("model", item.model)
        )
    }
    return array
}

private fun normalizeRemoteBase(value: String): String {
    val trimmed = value.trim().trimEnd('/')
    if (trimmed.isBlank()) return DEFAULT_API_HOST
    return if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) trimmed else "https://$trimmed"
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SettingsScreen(
    draft: SettingsDraft,
    onDraftChange: (SettingsDraft) -> Unit,
    defaultKeySet: Boolean,
    platformKeySet: Boolean,
    tokenSet: Boolean,
    llmKeySet: Boolean,
    darkMode: Boolean,
    onDarkModeChange: (Boolean) -> Unit,
    saveToAlbum: Boolean,
    onSaveToAlbumChange: (Boolean) -> Unit,
    albumPermissionGranted: Boolean,
    albumPermissionRequired: Boolean,
    onRequestAlbumPermission: () -> Unit,
    onRefreshCredits: () -> Unit,
    onSave: () -> Unit,
) {
    val usingCustomProvider = draft.modelProvider == "custom"
    val activeCustomProvider = draft.activeCustomProvider()
    val activeCustomKeySet = activeCustomProvider.apiKeySet || activeCustomProvider.apiKey.isNotBlank()
    val activeGenerationKeySet = if (usingCustomProvider) activeCustomKeySet else defaultKeySet
    val generationStatus = if (activeGenerationKeySet) "生成 Key 已配置" else "生成 Key 未配置"
    var modelMenuOpen by rememberSaveable { mutableStateOf(false) }
    var llmMenuOpen by rememberSaveable { mutableStateOf(false) }
    var creditMenuOpen by rememberSaveable { mutableStateOf(false) }
    var saveMenuOpen by rememberSaveable { mutableStateOf(false) }
    var aboutMenuOpen by rememberSaveable { mutableStateOf(false) }
    var showApiSitePage by rememberSaveable { mutableStateOf(false) }

    val context = LocalContext.current
    val currentVersion = remember {
        try { context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "" }
        catch (_: Exception) { "" }
    }
    var updateStatus by rememberSaveable { mutableStateOf("idle") }
    var latestVersion by rememberSaveable { mutableStateOf("") }
    var releaseUrl by rememberSaveable { mutableStateOf("") }
    var showUpdateDialog by rememberSaveable { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    fun triggerUpdateCheck() {
        scope.launch {
            updateStatus = "checking"
            try {
                val (status, ver, url) = withContext(Dispatchers.IO) { checkGitHubUpdate(currentVersion) }
                updateStatus = status
                latestVersion = ver
                releaseUrl = url
                if (status == "available") showUpdateDialog = true
            } catch (_: Exception) {
                updateStatus = "error"
            }
        }
    }

    LaunchedEffect(Unit) {
        if (updateStatus == "idle") {
            triggerUpdateCheck()
        }
    }

    val creditStatus = when (draft.creditQueryMode) {
        "token" -> if (tokenSet) "账户 Token 已配置" else "账户 Token 未配置"
        else -> if (platformKeySet) "余额 Key 已配置" else "余额 Key 未配置"
    }

    if (showApiSitePage) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextButton(onClick = { showApiSitePage = false }) {
                    Text("← 返回")
                }
                Spacer(Modifier.weight(1f))
                Text("API 网站", fontWeight = FontWeight.Bold)
                Spacer(Modifier.weight(1f))
                Spacer(Modifier.width(64.dp))
            }
            LazyColumn(contentPadding = PaddingValues(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(API_PLATFORMS) { platform ->
                    ElevatedCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { openUrl(context, platform.websiteUrl) },
                    ) {
                        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(platform.name, fontWeight = FontWeight.Bold)
                            Text(platform.websiteUrl, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                platform.description,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }
    } else {
    LazyColumn(contentPadding = PaddingValues(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { SectionTitle("直连平台设置", generationStatus) }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                Button(onClick = onSave, modifier = Modifier.weight(1f)) { Text("保存设置") }
                OutlinedButton(onClick = onRefreshCredits, modifier = Modifier.weight(1f)) { Text("刷新余额") }
            }
        }
        item {
            ElevatedCard {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp)
                        .clickable { onDarkModeChange(!darkMode) },
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("界面主题", fontWeight = FontWeight.Bold)
                        Text(
                            if (darkMode) "暗夜模式" else "白天模式",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Switch(checked = darkMode, onCheckedChange = onDarkModeChange)
                }
            }
        }
        item {
            ElevatedCard {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text("模型接入方", fontWeight = FontWeight.Bold)
                            Text(
                                if (usingCustomProvider) "自定义接口 · ${activeCustomProvider.name.ifBlank { activeCustomProvider.model }}" else "默认接口",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                generationStatus,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        TextButton(onClick = { modelMenuOpen = !modelMenuOpen }) {
                            Text(if (modelMenuOpen) "收起" else "展开")
                        }
                    }
                    if (modelMenuOpen) {
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilterChip(
                                selected = !usingCustomProvider,
                                onClick = { onDraftChange(draft.copy(modelProvider = "default")) },
                                label = { Text("默认接口") },
                            )
                            FilterChip(
                                selected = usingCustomProvider,
                                onClick = { onDraftChange(draft.copy(modelProvider = "custom")) },
                                label = { Text("自定义接口") },
                            )
                        }
                        if (usingCustomProvider) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Text("自定义平台", fontWeight = FontWeight.Bold)
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    TextButton(onClick = { onDraftChange(draft.removeActiveCustomProvider()) }) {
                                        Text("删除")
                                    }
                                    FilledTonalButton(onClick = { onDraftChange(draft.addCustomProvider()) }) {
                                        Text("新增")
                                    }
                                }
                            }
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                draft.customProviders.ifEmpty { listOf(activeCustomProvider) }.forEach { provider ->
                                    FilterChip(
                                        selected = provider.id == activeCustomProvider.id,
                                        onClick = { onDraftChange(draft.copy(modelProvider = "custom", activeCustomProviderId = provider.id)) },
                                        label = { Text(provider.name.ifBlank { provider.model.ifBlank { "自定义平台" } }) },
                                    )
                                }
                            }
                            OutlinedTextField(
                                value = activeCustomProvider.name,
                                onValueChange = { onDraftChange(draft.withCustomProvider(activeCustomProvider.copy(name = it))) },
                                modifier = Modifier.fillMaxWidth(),
                                label = { Text("平台名称") },
                                singleLine = true,
                            )
                            OutlinedTextField(
                                value = activeCustomProvider.apiUrl,
                                onValueChange = { onDraftChange(draft.withCustomProvider(activeCustomProvider.copy(apiUrl = it))) },
                                modifier = Modifier.fillMaxWidth(),
                                label = { Text("自定义 URL（Host 或提交地址）") },
                                singleLine = true,
                            )
                            SecretTextField(
                                value = activeCustomProvider.apiKey,
                                onValueChange = { onDraftChange(draft.withCustomProvider(activeCustomProvider.copy(apiKey = it))) },
                                label = if (activeCustomProvider.apiKeySet) "自定义 API Key（留空不覆盖）" else "自定义 API Key",
                            )
                            OutlinedTextField(
                                value = activeCustomProvider.model,
                                onValueChange = { onDraftChange(draft.withCustomProvider(activeCustomProvider.copy(model = it))) },
                                modifier = Modifier.fillMaxWidth(),
                                label = { Text("模型名") },
                                singleLine = true,
                            )
                        } else {
                            OutlinedTextField(
                                value = draft.apiHost,
                                onValueChange = { onDraftChange(draft.copy(apiHost = it)) },
                                modifier = Modifier.fillMaxWidth(),
                                label = { Text("生成 Host") },
                                singleLine = true,
                            )
                            SecretTextField(
                                value = draft.apiKey,
                                onValueChange = { onDraftChange(draft.copy(apiKey = it)) },
                                label = if (defaultKeySet) "生成 API Key（留空不覆盖）" else "生成 API Key",
                            )
                        }
                    }
                }
            }
        }
        item {
            ElevatedCard {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text("LLM 模型自定义", fontWeight = FontWeight.Bold)
                            Text(
                                if (llmKeySet || draft.llmApiKey.isNotBlank()) "LLM Key 已配置" else "LLM Key 未配置",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        TextButton(onClick = { llmMenuOpen = !llmMenuOpen }) {
                            Text(if (llmMenuOpen) "收起" else "展开")
                        }
                    }
                    if (llmMenuOpen) {
                        OutlinedTextField(
                            value = draft.llmApiUrl,
                            onValueChange = { onDraftChange(draft.copy(llmApiUrl = it)) },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("LLM URL（Host 或 Chat 地址）") },
                            singleLine = true,
                        )
                        SecretTextField(
                            value = draft.llmApiKey,
                            onValueChange = { onDraftChange(draft.copy(llmApiKey = it)) },
                            label = if (llmKeySet) "LLM API Key（留空不覆盖）" else "LLM API Key",
                        )
                        OutlinedTextField(
                            value = draft.llmModel,
                            onValueChange = { onDraftChange(draft.copy(llmModel = it)) },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("LLM Model") },
                            singleLine = true,
                        )
                        OutlinedTextField(
                            value = draft.llmRolePrompt,
                            onValueChange = { onDraftChange(draft.copy(llmRolePrompt = it)) },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("角色设定") },
                            minLines = 3,
                        )
                    }
                }
            }
        }
        item {
            ElevatedCard {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text("余额查询", fontWeight = FontWeight.Bold)
                            Text(
                                if (usingCustomProvider) "默认平台余额查询" else creditStatus,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                        TextButton(onClick = { creditMenuOpen = !creditMenuOpen }) {
                            Text(if (creditMenuOpen) "收起" else "展开")
                        }
                    }
                    if (creditMenuOpen) {
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilterChip(
                                selected = draft.creditQueryMode == "api_key",
                                onClick = { onDraftChange(draft.copy(creditQueryMode = "api_key")) },
                                label = { Text("APIKey 余额") },
                            )
                            FilterChip(
                                selected = draft.creditQueryMode == "token",
                                onClick = { onDraftChange(draft.copy(creditQueryMode = "token")) },
                                label = { Text("账户 Token 余额") },
                            )
                        }
                        OutlinedTextField(
                            value = draft.platformApiHost,
                            onValueChange = { onDraftChange(draft.copy(platformApiHost = it)) },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("余额/状态 Host") },
                            singleLine = true,
                        )
                        if (draft.creditQueryMode == "token") {
                            SecretTextField(
                                value = draft.platformToken,
                                onValueChange = { onDraftChange(draft.copy(platformToken = it)) },
                                label = if (tokenSet) "账户 Token（留空不覆盖）" else "账户 Token",
                            )
                        } else {
                            SecretTextField(
                                value = draft.platformApiKey,
                                onValueChange = { onDraftChange(draft.copy(platformApiKey = it)) },
                                label = if (platformKeySet) "余额 API Key（留空不覆盖）" else "余额 API Key",
                            )
                        }
                    }
                }
            }
        }
        item {
            ElevatedCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showApiSitePage = true },
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text("API 网站", fontWeight = FontWeight.Bold)
                    Text(">", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        item {
            ElevatedCard {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text("保存与权限", fontWeight = FontWeight.Bold)
                            Text(
                                "${if (saveToAlbum) "自动保存开启" else "自动保存关闭"} · ${if (albumPermissionGranted || !albumPermissionRequired) "相册可用" else "待授权"}",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        TextButton(onClick = { saveMenuOpen = !saveMenuOpen }) {
                            Text(if (saveMenuOpen) "收起" else "展开")
                        }
                    }
                    if (saveMenuOpen) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onSaveToAlbumChange(!saveToAlbum) },
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text("保存到手机相册", fontWeight = FontWeight.Bold)
                                Text(
                                    "开启后，新完成的图片会自动下载到系统图库；结果页也可手动保存。",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            Switch(checked = saveToAlbum, onCheckedChange = onSaveToAlbumChange)
                        }
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text("相册写入权限", fontWeight = FontWeight.Bold)
                                Text(
                                    when {
                                        !albumPermissionRequired -> "当前系统使用系统相册接口保存图片，无需额外授权。"
                                        albumPermissionGranted -> "已授权，可以自动保存或手动保存到相册。"
                                        else -> "当前系统需要授权后才能写入相册。"
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            if (albumPermissionRequired && !albumPermissionGranted) {
                                Button(onClick = onRequestAlbumPermission) { Text("授权") }
                            } else {
                                AssistChip(
                                    onClick = {},
                                    label = { Text(if (albumPermissionGranted || !albumPermissionRequired) "可用" else "未授权") },
                                )
                            }
                        }
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(draft.maxConcurrency, { onDraftChange(draft.copy(maxConcurrency = it)) }, modifier = Modifier.weight(1f), label = { Text("并发") })
                            OutlinedTextField(draft.maxRetries, { onDraftChange(draft.copy(maxRetries = it)) }, modifier = Modifier.weight(1f), label = { Text("重试") })
                        }
                    }
                }
            }
        }
        item {
            ElevatedCard {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text("关于", fontWeight = FontWeight.Bold)
                            Text(
                                "v$currentVersion · ${updateStatusText(updateStatus, latestVersion)}",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        TextButton(onClick = { aboutMenuOpen = !aboutMenuOpen }) {
                            Text(if (aboutMenuOpen) "收起" else "展开")
                        }
                    }
                    if (aboutMenuOpen) {
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text("当前版本", fontWeight = FontWeight.Bold)
                                    Text("v$currentVersion", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { triggerUpdateCheck() },
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text("更新状态", fontWeight = FontWeight.Bold)
                                    Text(
                                        when (updateStatus) {
                                            "checking" -> "检查中…"
                                            "upToDate" -> "已是最新版本"
                                            "available" -> "发现新版本 v$latestVersion"
                                            "error" -> "检查失败，点击重试"
                                            else -> "等待检测"
                                        },
                                        style = MaterialTheme.typography.bodySmall,
                                        color = when (updateStatus) {
                                            "available" -> MaterialTheme.colorScheme.primary
                                            "error" -> MaterialTheme.colorScheme.error
                                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                                        },
                                    )
                                }
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text("项目地址", fontWeight = FontWeight.Bold)
                                    Text(
                                        "github.com/$GITHUB_OWNER/$GITHUB_REPO",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.clickable {
                                            openUrl(context, "https://github.com/$GITHUB_OWNER/$GITHUB_REPO")
                                        },
                                    )
                                }
                            }
                            if (updateStatus == "available") {
                                Button(
                                    onClick = { showUpdateDialog = true },
                                    modifier = Modifier.fillMaxWidth(),
                                ) { Text("前往 GitHub 下载") }
                            }
                        }
                    }
                }
            }
        }
    }
    }

    if (showUpdateDialog) {
        AlertDialog(
            onDismissRequest = { showUpdateDialog = false },
            title = { Text("发现新版本") },
            text = { Text("当前版本：v$currentVersion\n最新版本：v$latestVersion\n\n前往 GitHub 下载最新版本？") },
            confirmButton = {
                Button(onClick = {
                    showUpdateDialog = false
                    openUrl(context, releaseUrl)
                }) { Text("前往下载") }
            },
            dismissButton = { TextButton(onClick = { showUpdateDialog = false }) { Text("稍后") } },
        )
    }
}

@Composable
private fun SecretTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
) {
    var visible by rememberSaveable { mutableStateOf(false) }
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier.fillMaxWidth(),
        label = { Text(label) },
        singleLine = true,
        visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
        trailingIcon = {
            TextButton(onClick = { visible = !visible }) {
                Text(if (visible) "隐藏" else "显示")
            }
        },
    )
}

@Composable
private fun SectionTitle(title: String, side: String = "") {
    Row(horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        if (side.isNotBlank()) Text(side, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FlowMeta(values: List<String>) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        values.forEach { value -> AssistChip(onClick = {}, label = { Text(value, maxLines = 1, overflow = TextOverflow.Ellipsis) }) }
    }
}

@Composable
private fun RemoteImage(url: String, modifier: Modifier = Modifier) {
    val bitmap by produceState<Bitmap?>(initialValue = null, url) {
        value = withContext(Dispatchers.IO) { loadBitmap(url) }
    }
    if (bitmap == null) {
        Box(modifier.background(MaterialTheme.colorScheme.surfaceVariant), contentAlignment = Alignment.Center) {
            Text("加载中", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    } else {
        Image(
            bitmap = bitmap!!.asImageBitmap(),
            contentDescription = null,
            modifier = modifier.background(MaterialTheme.colorScheme.surfaceVariant),
            contentScale = ContentScale.Fit,
        )
    }
}

private class DirectApiClient(
    private val apiHost: String,
    private val apiKey: String,
    private val modelProvider: String,
    private val customApiUrl: String,
    private val customApiKey: String,
    private val platformApiHost: String,
    private val platformApiKey: String,
    private val platformToken: String,
    private val creditQueryMode: String,
    private val llmApiUrl: String,
    private val llmApiKey: String,
    private val llmModel: String,
    private val llmRolePrompt: String,
) {
    private val usingCustomProvider: Boolean
        get() = modelProvider == "custom"

    companion object {
        fun fromPrefs(prefs: SharedPreferences): DirectApiClient {
            val provider = prefs.getString("model_provider", "default") ?: "default"
            val apiHost = normalizeRemoteBase(prefs.getString("api_host", DEFAULT_API_HOST) ?: DEFAULT_API_HOST)
            val platformHost = normalizeRemoteBase(prefs.getString("platform_api_host", apiHost) ?: apiHost)
            val customProvider = readActiveCustomProviderConfig(prefs)
            return DirectApiClient(
                apiHost = apiHost,
                apiKey = prefs.getString("api_key", "") ?: "",
                modelProvider = provider,
                customApiUrl = customProvider.apiUrl,
                customApiKey = customProvider.apiKey,
                platformApiHost = platformHost,
                platformApiKey = prefs.getString("platform_api_key", "") ?: "",
                platformToken = prefs.getString("platform_token", "") ?: "",
                creditQueryMode = prefs.getString("credit_query_mode", "api_key") ?: "api_key",
                llmApiUrl = prefs.getString("llm_api_url", "") ?: "",
                llmApiKey = prefs.getString("llm_api_key", "") ?: "",
                llmModel = prefs.getString("llm_model", "") ?: "",
                llmRolePrompt = prefs.getString("llm_role_prompt", DEFAULT_LLM_ROLE_PROMPT)?.ifBlank { DEFAULT_LLM_ROLE_PROMPT } ?: DEFAULT_LLM_ROLE_PROMPT,
            )
        }
    }

    suspend fun submitImage(
        model: ModelInfo,
        prompt: String,
        aspectRatio: String,
        imageSize: String,
        urls: List<String>,
    ): String = withContext(Dispatchers.IO) {
        val activeKey = generationApiKey()
        if (activeKey.isBlank()) error(if (usingCustomProvider) "缺少自定义 API Key" else "缺少生成 API Key")
        val submitUrl = submitUrl(model)
        val body = JSONObject()
            .put("model", model.id)
            .put("prompt", prompt)
            .put("aspectRatio", aspectRatio)
            .put("urls", JSONArray(urls))
            .put("webHook", "-1")
        if (model.supportsImageSize) body.put("imageSize", imageSize)
        val json = requestJson(
            url = submitUrl,
            method = "POST",
            body = body,
            bearer = activeKey,
            timeoutMs = 30000,
        )
        val data = json.optJSONObject("data")
        if (json.opt("code")?.toString() in listOf("0", null) && data?.optString("id").orEmpty().isNotBlank()) {
            data!!.optString("id")
        } else {
            error(json.optString("msg", "API 提交失败: $json"))
        }
    }

    suspend fun result(taskId: String): DirectResult = withContext(Dispatchers.IO) {
        val activeKey = generationApiKey()
        if (activeKey.isBlank()) error(if (usingCustomProvider) "缺少自定义 API Key" else "缺少生成 API Key")
        val json = requestJson(
            url = resultUrl(),
            method = "POST",
            body = JSONObject().put("id", taskId),
            bearer = activeKey,
            timeoutMs = 12000,
        )
        val data = json.optJSONObject("data") ?: json
        val results = data.optJSONArray("results")
        DirectResult(
            status = data.optString("status", json.optString("status", "submitted")),
            progress = data.optDouble("progress", 0.0),
            finalUrl = results?.optJSONObject(0)?.optString("url").orEmpty(),
            failureReason = data.optString("failure_reason", json.optString("msg")),
        )
    }

    suspend fun refreshCredits(): CreditsInfo = withContext(Dispatchers.IO) {
        val useToken = creditQueryMode == "token"
        val requestBody = if (useToken) {
            if (platformToken.isBlank()) return@withContext CreditsInfo(status = "missing_key", error = "缺少账户 Token")
            JSONObject().put("token", platformToken)
        } else {
            if (platformApiKey.isBlank()) return@withContext CreditsInfo(status = "missing_key", error = "缺少余额 API Key")
            JSONObject().put("apiKey", platformApiKey)
        }
        val path = if (useToken) "/client/openapi/getCredits" else "/client/openapi/getAPIKeyCredits"
        val json = requestJson(
            url = "${platformApiHost}${path}",
            method = "POST",
            body = requestBody,
            bearer = "",
            timeoutMs = 12000,
        )
        if (json.opt("code")?.toString() !in listOf("0", null)) {
            return@withContext CreditsInfo(status = "error", error = json.optString("msg", "余额查询失败"))
        }
        val balance = extractCredits(json)
        if (balance == null) {
            CreditsInfo(status = "error", error = "未识别 data.credits")
        } else {
            CreditsInfo(balance = balance, status = "ok", error = "")
        }
    }

    suspend fun modelStatuses(models: List<ModelInfo>): Map<String, ModelStatusInfo> = withContext(Dispatchers.IO) {
        if (usingCustomProvider) {
            return@withContext models.associate { model ->
                model.id to ModelStatusInfo(checked = true, ok = true, error = "自定义接口")
            }
        }
        models.associate { model ->
            val status = runCatching {
                val encoded = URLEncoder.encode(model.id, "UTF-8")
                val json = requestJson(
                    url = "${platformApiHost}/client/common/getModelStatus?model=$encoded",
                    method = "GET",
                    body = null,
                    bearer = "",
                    timeoutMs = 10000,
                )
                val data = json.optJSONObject("data") ?: JSONObject()
                ModelStatusInfo(
                    checked = true,
                    ok = data.optBoolean("status", false),
                    error = data.optString("error", json.optString("msg")),
                )
            }.getOrElse { error ->
                ModelStatusInfo(checked = true, ok = false, error = error.message ?: "状态检查失败")
            }
            model.id to status
        }
    }

    suspend fun optimizePrompt(prompt: String, imageModelName: String): String = withContext(Dispatchers.IO) {
        if (prompt.isBlank()) error("请先填写提示词")
        if (llmApiUrl.isBlank()) error("请先在设置里配置 LLM URL")
        if (llmApiKey.isBlank()) error("请先在设置里配置 LLM API Key")
        if (llmModel.isBlank()) error("请先在设置里配置 LLM Model")
        val systemPrompt = llmRolePrompt.ifBlank { DEFAULT_LLM_ROLE_PROMPT }
        val userPrompt = "当前生图模型：${imageModelName.ifBlank { "未指定" }}\n原始提示词：${prompt.trim()}\n请优化为一段可直接用于图像生成的提示词。"
        val messages = JSONArray()
            .put(JSONObject().put("role", "system").put("content", systemPrompt))
            .put(JSONObject().put("role", "user").put("content", userPrompt))
        val body = JSONObject()
            .put("model", llmModel)
            .put("messages", messages)
            .put("temperature", 0.6)
            .put("stream", false)
        val json = requestJson(
            url = llmChatUrl(),
            method = "POST",
            body = body,
            bearer = llmApiKey,
            timeoutMs = 30000,
        )
        val choice = json.optJSONArray("choices")?.optJSONObject(0)
        val content = choice?.optJSONObject("message")?.optString("content").orEmpty()
            .ifBlank { choice?.optString("text").orEmpty() }
            .ifBlank { json.optJSONObject("data")?.optString("content").orEmpty() }
            .ifBlank { json.optString("content") }
            .trim()
        cleanOptimizedPrompt(content).ifBlank { error("LLM 没有返回有效提示词") }
    }

    private fun generationApiKey(): String = if (usingCustomProvider) customApiKey else apiKey

    private fun submitUrl(model: ModelInfo): String {
        if (!usingCustomProvider) return "${apiHost}${model.endpoint}"
        return customSubmitUrl()
    }

    private fun customSubmitUrl(): String {
        if (customApiUrl.isBlank()) error("缺少自定义 URL")
        val normalized = normalizeRemoteBase(customApiUrl)
        val hasPath = runCatching { URL(normalized).path.trim('/').isNotBlank() }.getOrDefault(false)
        return if (hasPath) normalized else "$normalized/v1/draw/completions"
    }

    private fun resultUrl(): String {
        if (!usingCustomProvider) return "$apiHost/v1/draw/result"
        val submitUrl = customSubmitUrl()
        val base = if (submitUrl.contains("/v1/")) {
            submitUrl.substringBefore("/v1/").trimEnd('/')
        } else {
            runCatching {
                val parsed = URL(submitUrl)
                "${parsed.protocol}://${parsed.authority}"
            }.getOrDefault(submitUrl.trimEnd('/'))
        }
        return "$base/v1/draw/result"
    }

    private fun llmChatUrl(): String {
        val normalized = normalizeRemoteBase(llmApiUrl)
        val hasPath = runCatching { URL(normalized).path.trim('/').isNotBlank() }.getOrDefault(false)
        return if (hasPath) normalized else "$normalized/v1/chat/completions"
    }

    private fun cleanOptimizedPrompt(value: String): String =
        value.trim()
            .removePrefix("```")
            .removeSuffix("```")
            .trim()
            .trim('"', '“', '”')
            .trim()

    private fun requestJson(url: String, method: String, body: JSONObject?, bearer: String, timeoutMs: Int): JSONObject {
        val connection = URL(url).openConnection() as HttpURLConnection
        connection.requestMethod = method
        connection.connectTimeout = timeoutMs
        connection.readTimeout = timeoutMs
        connection.setRequestProperty("Content-Type", "application/json")
        if (bearer.isNotBlank()) connection.setRequestProperty("Authorization", "Bearer $bearer")
        if (body != null) {
            connection.doOutput = true
            connection.outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }
        }
        val stream = if (connection.responseCode in 200..299) connection.inputStream else connection.errorStream
        val text = stream?.let { BufferedReader(InputStreamReader(it, Charsets.UTF_8)).use { reader -> reader.readText() } }.orEmpty()
        val json = if (text.isBlank()) JSONObject() else JSONObject(text)
        if (connection.responseCode !in 200..299) error(json.optString("msg", "HTTP ${connection.responseCode}"))
        return json
    }
}

private fun parseNetworkUrls(value: String): List<String> = value
    .lineSequence()
    .map { it.trim() }
    .filter { it.isNotBlank() }
    .toList()

private fun invalidNetworkUrlCount(value: String): Int =
    parseNetworkUrls(value).count { !(it.startsWith("http://") || it.startsWith("https://")) }

private fun needsAlbumWritePermission(): Boolean =
    Build.VERSION.SDK_INT <= Build.VERSION_CODES.P

private fun hasAlbumWritePermission(context: Context): Boolean =
    !needsAlbumWritePermission() ||
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
        ) == PackageManager.PERMISSION_GRANTED

private fun extractCredits(json: JSONObject): String? {
    val data = json.optJSONObject("data")
    val value = data?.opt("credits") ?: json.opt("credits") ?: json.opt("balance")
    return value?.takeIf { it != JSONObject.NULL }?.toString()
}

private fun nowText(pattern: String = "yyyy-MM-dd HH:mm:ss"): String =
    SimpleDateFormat(pattern, Locale.CHINA).format(Date())

private fun Double.formatOne(): String = String.format(Locale.US, "%.1f", this)

private suspend fun readUriAsDataUrl(context: Context, uri: Uri): LocalImage? = withContext(Dispatchers.IO) {
    val resolver = context.contentResolver
    val mime = resolver.getType(uri) ?: "image/png"
    val bytes = resolver.openInputStream(uri)?.use { it.readBytes() } ?: return@withContext null
    val encoded = Base64.encodeToString(bytes, Base64.NO_WRAP)
    LocalImage(uri.lastPathSegment ?: "image", "data:$mime;base64,$encoded")
}

private fun normalizeBaseUrl(value: String): String {
    val trimmed = value.trim().trimEnd('/')
    return if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) trimmed else "http://$trimmed"
}

private fun absoluteUrl(baseUrl: String, path: String): String {
    if (path.startsWith("http://") || path.startsWith("https://") || path.startsWith("data:")) return path
    return normalizeBaseUrl(baseUrl) + if (path.startsWith("/")) path else "/$path"
}

private fun loadBitmap(url: String): Bitmap? = try {
    if (url.startsWith("data:")) {
        val encoded = url.substringAfter("base64,", "")
        val bytes = Base64.decode(encoded, Base64.DEFAULT)
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
    } else {
        val connection = URL(url).openConnection() as HttpURLConnection
        connection.connectTimeout = 12000
        connection.readTimeout = 20000
        connection.inputStream.use { BitmapFactory.decodeStream(it) }
    }
} catch (_: Exception) {
    null
}

private fun loadBitmap(context: Context, source: String): Bitmap? = try {
    when {
        source.startsWith("content://") -> context.contentResolver.openInputStream(Uri.parse(source))?.use { BitmapFactory.decodeStream(it) }
        else -> loadBitmap(source)
    }
} catch (_: Exception) {
    null
}

private suspend fun saveRemoteImageToGallery(context: Context, url: String, requestedName: String): Uri? =
    withContext(Dispatchers.IO) {
        val bytes = loadImageBytes(url) ?: return@withContext null
        val fileName = sanitizeImageFileName(requestedName)
        val mimeType = when (fileName.substringAfterLast('.', "").lowercase()) {
            "jpg", "jpeg" -> "image/jpeg"
            "webp" -> "image/webp"
            else -> "image/png"
        }
        val resolver = context.contentResolver
        val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        } else {
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        }
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
            put(MediaStore.Images.Media.MIME_TYPE, mimeType)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/Image box")
                put(MediaStore.Images.Media.IS_PENDING, 1)
            }
        }
        val itemUri = resolver.insert(collection, values) ?: return@withContext null
        try {
            resolver.openOutputStream(itemUri)?.use { it.write(bytes) } ?: error("无法写入相册")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                values.clear()
                values.put(MediaStore.Images.Media.IS_PENDING, 0)
                resolver.update(itemUri, values, null, null)
            }
            itemUri
        } catch (_: Exception) {
            resolver.delete(itemUri, null, null)
            null
        }
    }

private fun loadImageBytes(url: String): ByteArray? = try {
    if (url.startsWith("data:")) {
        val encoded = url.substringAfter(",", "")
        Base64.decode(encoded, Base64.DEFAULT)
    } else {
        val connection = URL(url).openConnection() as HttpURLConnection
        connection.connectTimeout = 12000
        connection.readTimeout = 30000
        connection.inputStream.use { it.readBytes() }
    }
} catch (_: Exception) {
    null
}

private fun sanitizeImageFileName(value: String): String {
    val cleaned = value
        .substringAfterLast('/')
        .substringBefore('?')
        .replace(Regex("""[\\/:*?"<>|]"""), "_")
        .ifBlank { "generated_${System.currentTimeMillis()}.png" }
    return if (cleaned.contains('.')) cleaned else "$cleaned.png"
}

private const val GITHUB_OWNER = "fanqiejiu"
private const val GITHUB_REPO = "ImageBox"

private fun updateStatusText(status: String, latest: String): String = when (status) {
    "checking" -> "检查中…"
    "upToDate" -> "已是最新"
    "available" -> "有新版本 v$latest"
    "error" -> "检测失败"
    else -> "等待检测"
}

private fun checkGitHubUpdate(currentVersion: String): Triple<String, String, String> {
    val url = URL("https://api.github.com/repos/$GITHUB_OWNER/$GITHUB_REPO/releases")
    val connection = url.openConnection() as HttpURLConnection
    connection.setRequestProperty("Accept", "application/vnd.github+json")
    connection.connectTimeout = 8000
    connection.readTimeout = 8000
    try {
        val body = connection.inputStream.bufferedReader().readText()
        val array = JSONArray(body)
        val latest = array.optJSONObject(0) ?: return Triple("error", "", "")
        val tagName = latest.optString("tag_name", "").trimStart('v')
        val htmlUrl = latest.optString("html_url", "")
        if (tagName.isBlank()) return Triple("error", "", "")
        return if (tagName != currentVersion.trimStart('v')) {
            Triple("available", tagName, htmlUrl)
        } else {
            Triple("upToDate", tagName, htmlUrl)
        }
    } finally {
        connection.disconnect()
    }
}

private fun openUrl(context: Context, url: String) {
    runCatching {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
    }
}
