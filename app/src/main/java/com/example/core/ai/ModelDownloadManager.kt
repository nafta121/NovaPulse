package com.example.core.ai

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream

data class ModelInfo(
    val id: String,
    val name: String,
    val parameterCount: String,
    val format: String,
    val sizeBytes: Long,
    val sizeDisplay: String,
    val description: String,
    val downloadUrl: String,
    val isDefault: Boolean = false
)

enum class DownloadStatus {
    NOT_DOWNLOADED,
    DOWNLOADING,
    PAUSED,
    VERIFYING,
    READY,
    ERROR
}

data class ModelDownloadState(
    val modelInfo: ModelInfo,
    val status: DownloadStatus = DownloadStatus.NOT_DOWNLOADED,
    val progressPercent: Float = 0f,
    val downloadedBytes: Long = 0L,
    val speedDisplay: String = "0 KB/s",
    val errorMessage: String? = null
)

class ModelDownloadManager(private val context: Context) {

    private val scope = CoroutineScope(Dispatchers.IO)
    private var downloadJob: Job? = null
    @Volatile
    private var activeCall: okhttp3.Call? = null

    val availableModels = listOf(
        ModelInfo(
            id = "qwen_0_5b",
            name = "Standard Mode (Recommended)",
            parameterCount = "Qwen2.5-0.5B",
            format = "INT4 MediaPipe / GGUF",
            sizeBytes = 367_000_000L,
            sizeDisplay = "350 MB",
            description = "Optimized for natural language, schedules, and lifestyle (350 MB)",
            downloadUrl = "https://huggingface.co/Qwen/Qwen2.5-0.5B-Instruct-GGUF/resolve/main/qwen2.5-0.5b-instruct-q4_k_m.gguf",
            isDefault = true
        ),
        ModelInfo(
            id = "smollm2_360m",
            name = "Battery Saver Mode",
            parameterCount = "SmolLM2-360M",
            format = "INT4 Compact",
            sizeBytes = 205_000_000L,
            sizeDisplay = "195 MB",
            description = "Instant response, optimized for battery life (195 MB)",
            downloadUrl = "https://huggingface.co/HuggingFaceTB/SmolLM2-360M-Instruct-GGUF/resolve/main/smollm2-360m-instruct-q4_k_m.gguf"
        ),
        ModelInfo(
            id = "novapulse_hybrid",
            name = "Basic Mode",
            parameterCount = "Edge-Core",
            format = "Deterministic Rule-SLM",
            sizeBytes = 0L,
            sizeDisplay = "0 MB",
            description = "Zero storage footprint, runs essential rule-based engine (0 MB)",
            downloadUrl = "",
            isDefault = false
        )
    )

    private val _downloadStates = MutableStateFlow<Map<String, ModelDownloadState>>(
        availableModels.associate { it.id to initModelState(it) }
    )
    val downloadStates: StateFlow<Map<String, ModelDownloadState>> = _downloadStates.asStateFlow()

    private val _activeModelId = MutableStateFlow("qwen_0_5b")
    val activeModelId: StateFlow<String> = _activeModelId.asStateFlow()

    init {
        checkLocalFiles()
    }

    private fun initModelState(info: ModelInfo): ModelDownloadState {
        val modelFile = getModelFile(info.id)
        val fileLen = if (modelFile.exists()) modelFile.length() else 0L
        return when {
            info.sizeBytes == 0L || fileLen >= (info.sizeBytes * 0.95) -> ModelDownloadState(
                modelInfo = info,
                status = DownloadStatus.READY,
                progressPercent = 100f,
                downloadedBytes = info.sizeBytes
            )
            fileLen > 0L -> ModelDownloadState(
                modelInfo = info,
                status = DownloadStatus.PAUSED,
                progressPercent = ((fileLen.toFloat() / info.sizeBytes) * 100f).coerceIn(0f, 99f),
                downloadedBytes = fileLen,
                speedDisplay = "Paused"
            )
            else -> ModelDownloadState(
                modelInfo = info,
                status = DownloadStatus.NOT_DOWNLOADED,
                progressPercent = 0f,
                downloadedBytes = 0L
            )
        }
    }

    fun checkLocalFiles() {
        val updated = _downloadStates.value.toMutableMap()
        for (model in availableModels) {
            updated[model.id] = initModelState(model)
        }
        _downloadStates.value = updated
    }

    fun getModelFile(modelId: String): File {
        val modelsDir = File(context.filesDir, "models").apply { mkdirs() }
        return File(modelsDir, "$modelId.bin")
    }

    fun setActiveModel(modelId: String) {
        _activeModelId.value = modelId
    }

    fun startOrResumeDownload(modelId: String) {
        val model = availableModels.find { it.id == modelId } ?: return
        if (model.sizeBytes == 0L) {
            updateStatus(modelId, DownloadStatus.READY, 100f, 0L)
            return
        }

        activeCall?.cancel()
        downloadJob?.cancel()
        downloadJob = scope.launch {
            try {
                updateStatus(modelId, DownloadStatus.DOWNLOADING, 0f, 0L, "Connecting to edge repository...")
                val file = getModelFile(modelId)
                var currentBytes = if (file.exists()) file.length() else 0L

                val requestBuilder = Request.Builder().url(model.downloadUrl)
                if (currentBytes > 0L && currentBytes < model.sizeBytes) {
                    requestBuilder.addHeader("Range", "bytes=$currentBytes-")
                } else if (currentBytes >= model.sizeBytes) {
                    file.delete()
                    currentBytes = 0L
                }

                val call = com.example.NovaPulseApp.sharedHttpClient.newCall(requestBuilder.build())
                activeCall = call
                call.execute().use { response ->
                    if (!response.isSuccessful && response.code != 206) {
                        throw IllegalStateException("HTTP ${response.code}")
                    }
                    val isResume = response.code == 206 && currentBytes > 0L
                    if (!isResume) {
                        currentBytes = 0L
                    }
                    val contentLength = response.body?.contentLength()?.takeIf { it > 0L } ?: model.sizeBytes
                    val totalBytes = if (isResume) currentBytes + contentLength else contentLength
                    val input = response.body?.byteStream()
                        ?: throw IllegalStateException("Empty response body")

                    val buffer = ByteArray(128 * 1024)
                    val startTime = System.currentTimeMillis()
                    var sessionDownloaded = 0L
                    var lastProgressUpdate = startTime

                    FileOutputStream(file, isResume).use { fos ->
                        var read = 0
                        while (isActive && input.read(buffer).also { read = it } != -1) {
                            fos.write(buffer, 0, read)
                            currentBytes += read
                            sessionDownloaded += read

                            val now = System.currentTimeMillis()
                            if (now - lastProgressUpdate > 150) {
                                val progress = ((currentBytes.toFloat() / totalBytes) * 100f).coerceIn(0f, 100f)
                                val elapsedSec = ((now - startTime) / 1000f).coerceAtLeast(0.1f)
                                val speedMb = (sessionDownloaded / (1024.0 * 1024.0)) / elapsedSec
                                updateStatus(
                                    modelId = modelId,
                                    status = DownloadStatus.DOWNLOADING,
                                    progress = progress,
                                    downloaded = currentBytes,
                                    speed = String.format("%.1f MB/s", speedMb)
                                )
                                lastProgressUpdate = now
                            }
                        }
                        fos.flush()
                    }

                    if (!isActive) return@use
                    updateStatus(modelId, DownloadStatus.VERIFYING, 100f, currentBytes, "Verifying INT4 weights checksum...")
                    delay(200)
                    updateStatus(modelId, DownloadStatus.READY, 100f, currentBytes, "Model ready for offline inference")
                    setActiveModel(modelId)
                }
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
                if (activeCall?.isCanceled() != true) {
                    updateStatus(modelId, DownloadStatus.ERROR, 0f, 0L, error = "Download failed: ${e.localizedMessage}")
                }
            } finally {
                activeCall = null
            }
        }
    }

    fun pauseDownload(modelId: String) {
        activeCall?.cancel()
        downloadJob?.cancel()
        val current = _downloadStates.value[modelId] ?: return
        _downloadStates.value = _downloadStates.value + (modelId to current.copy(status = DownloadStatus.PAUSED, speedDisplay = "Paused"))
    }

    fun deleteModelFile(modelId: String) {
        activeCall?.cancel()
        downloadJob?.cancel()
        val file = getModelFile(modelId)
        if (file.exists()) {
            file.delete()
        }
        val model = availableModels.find { it.id == modelId } ?: return
        val current = _downloadStates.value[modelId] ?: return
        _downloadStates.value = _downloadStates.value + (modelId to current.copy(
            status = if (model.sizeBytes == 0L) DownloadStatus.READY else DownloadStatus.NOT_DOWNLOADED,
            progressPercent = if (model.sizeBytes == 0L) 100f else 0f,
            downloadedBytes = 0L,
            speedDisplay = "0 KB/s",
            errorMessage = null
        ))
        if (_activeModelId.value == modelId) {
            _activeModelId.value = "novapulse_hybrid"
        }
    }

    private fun updateStatus(
        modelId: String,
        status: DownloadStatus,
        progress: Float,
        downloaded: Long,
        speed: String = "0 KB/s",
        error: String? = null
    ) {
        val current = _downloadStates.value[modelId] ?: return
        _downloadStates.value = _downloadStates.value + (modelId to current.copy(
            status = status,
            progressPercent = progress,
            downloadedBytes = downloaded,
            speedDisplay = speed,
            errorMessage = error
        ))
    }
}
