package com.example.core.ai

import android.app.ActivityManager
import android.content.Context
import android.os.Build
import android.os.Process
import java.io.RandomAccessFile

data class DeviceSpecs(
    val totalRamMb: Long,
    val availableRamMb: Long,
    val cpuCores: Int,
    val isCapableForLocalSlm: Boolean,
    val recommendedModel: String,
    val recommendedQuantization: String,
    val tierDescription: String,
    val optimalThreadCount: Int
)

object DeviceCapabilityDetector {

    fun detect(context: Context): DeviceSpecs {
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
        val memoryInfo = ActivityManager.MemoryInfo()
        activityManager?.getMemoryInfo(memoryInfo)

        val totalRamMb = memoryInfo.totalMem / (1024 * 1024)
        val availableRamMb = memoryInfo.availMem / (1024 * 1024)
        val cpuCores = Runtime.getRuntime().availableProcessors()

        val isCapable = totalRamMb >= 3500 // 4GB tier device or above
        val optimalThreads = when {
            cpuCores >= 8 -> 4
            cpuCores >= 4 -> 3
            else -> 2
        }

        val (recommendedModel, quant, tier) = when {
            totalRamMb >= 7000 -> Triple(
                "Qwen2.5-0.5B-Instruct",
                "INT4 (GGUF/MediaPipe Task)",
                "Tier 1 (High Performance) - Full Offline SLM Real-Time Inference"
            )
            totalRamMb >= 3500 -> Triple(
                "SmolLM2-360M-Instruct",
                "INT4 Compact",
                "Tier 2 (Balanced) - Low Memory Offline SLM with Regex Pre-Gating"
            )
            else -> Triple(
                "NovaPulse Edge-Core (Hybrid Engine)",
                "Rule + Regex Engine",
                "Tier 3 (Battery Saver) - Memory < 4GB, using Zero-Latency Engine"
            )
        }

        return DeviceSpecs(
            totalRamMb = totalRamMb,
            availableRamMb = availableRamMb,
            cpuCores = cpuCores,
            isCapableForLocalSlm = isCapable,
            recommendedModel = recommendedModel,
            recommendedQuantization = quant,
            tierDescription = tier,
            optimalThreadCount = optimalThreads
        )
    }
}
