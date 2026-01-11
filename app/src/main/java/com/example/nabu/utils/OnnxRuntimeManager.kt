package com.example.nabu.utils

import android.content.Context
import android.util.Log
import com.example.nabu.R
import com.example.nabu.kokoro.Downloader
import com.example.nabu.kokoro.KokoroBundle
import com.example.nabu.kokoro.KokoroLoader
import com.example.nabu.kokoro.Manifest
import com.example.nabu.kokoro.ManifestProvider
import com.example.nabu.kokoro.RunEp
import com.example.nabu.kokoro.KokoroEngine
import com.example.nabu.tts.TTSEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

object OnnxRuntimeManager {
    private const val TAG = "OnnxRuntimeManager"
    private val mutex = Mutex()

    @Volatile
    private var bundle: KokoroBundle? = null
    @Volatile
    private var manifest: Manifest = ManifestProvider.kokoroV1()
    @Volatile
    private var engine: KokoroEngine? = null
    @Volatile
    private var status: RuntimeStatus? = null

    suspend fun initialize(context: Context, preferred: RunEp? = null): Result<KokoroBundle> =
        mutex.withLock {
            val appContext = context.applicationContext
            manifest = ManifestProvider.kokoroV1()
            val choice = preferred ?: SettingsManager.getRuntimePreference(appContext)

            val fetchResult = Downloader.ensureModels(appContext, manifest)
            if (fetchResult.isFailure) {
                Log.w(TAG, "Failed to download Kokoro models", fetchResult.exceptionOrNull())
                ensureBundledFallback(appContext, manifest)
            }

            val loadResult = runCatching {
                KokoroLoader.load(appContext, manifest, choice)
            }

            loadResult.onSuccess { newBundle ->
                bundle?.session?.close()
                bundle = newBundle
                engine = KokoroEngine(newBundle, manifest)
                status = RuntimeStatus(newBundle.ep, newBundle.graphId, manifest.sampleRate)
                DebugLogger.log(
                    "Kokoro runtime ready ep=${newBundle.ep} graph=${newBundle.graphId} sampleRate=${manifest.sampleRate}"
                )
                
                // Perform warmup inference to reduce first-synthesis latency
                warmup()
            }.onFailure { error ->
                Log.e(TAG, "Unable to load Kokoro session", error)
                DebugLogger.log("Kokoro runtime failed: ${error.message}")
            }

            loadResult
        }

    /**
     * Perform a warmup inference with minimal dummy input to initialize the session.
     * This reduces latency for the first real synthesis by pre-loading the model.
     */
    private suspend fun warmup() {
        try {
            val startTime = System.currentTimeMillis()
            DebugLogger.log("OnnxRuntimeManager: Starting warmup inference...")
            
            val currentEngine = engine
            if (currentEngine != null) {
                // Create minimal valid input for warmup
                val dummyTokens = longArrayOf(0L, 1L, 0L)  // Minimal valid token sequence
                val dummyStyle = Array(1) { FloatArray(256) { 0f } }  // Zero-filled style vector
                
                // Run warmup synthesis and discard output
                currentEngine.synth(dummyTokens, dummyStyle, 1.0f)
                
                val elapsedMs = System.currentTimeMillis() - startTime
                DebugLogger.log("OnnxRuntimeManager: Warmup completed in ${elapsedMs}ms")
            }
        } catch (e: Exception) {
            // Warmup failure is not critical - log and continue
            DebugLogger.log("OnnxRuntimeManager: Warmup failed (non-critical): ${e.message}")
        }
    }

    fun getEngine(): KokoroEngine =
        requireNotNull(engine) { "Kokoro engine not initialized" }

    fun currentBundle(): KokoroBundle? = bundle

    fun currentManifest(): Manifest = manifest

    fun runtimeStatus(): RuntimeStatus? = status

    fun close() {
        bundle?.session?.close()
        bundle = null
        engine = null
        status = null
    }

    private suspend fun ensureBundledFallback(context: Context, manifest: Manifest) {
        withContext(Dispatchers.IO) {
            val fallbackFile = manifest.files.firstOrNull { it.id == "kokoro_int8" } ?: return@withContext
            val dest = File(context.filesDir, fallbackFile.dest)
            if (dest.exists()) return@withContext

            runCatching {
                context.resources.openRawResource(R.raw.kokoro).use { input ->
                    dest.parentFile?.mkdirs()
                    FileOutputStream(dest).use { output ->
                        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                        while (true) {
                            val read = input.read(buffer)
                            if (read == -1) break
                            output.write(buffer, 0, read)
                        }
                    }
                }
                Log.i(TAG, "Provisioned bundled INT8 fallback to ${dest.absolutePath}")
            }.onFailure { error ->
                Log.w(TAG, "Bundled INT8 fallback unavailable", error)
            }
        }
    }

    data class RuntimeStatus(
        val ep: RunEp,
        val graphId: String,
        val sampleRate: Int
    ) {
        override fun toString(): String = "${ep.name}/${graphId}"
    }
}
