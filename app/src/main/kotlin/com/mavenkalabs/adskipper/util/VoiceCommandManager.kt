package com.mavenkalabs.adskipper.util

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.media.audiofx.AcousticEchoCanceler
import android.os.Process
import android.util.Log
import androidx.annotation.RequiresPermission
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import org.vosk.Model
import org.vosk.Recognizer

enum class DetectionState {
    WAKE_WORD,
    COMMAND,
    DISABLED
}

enum class VoiceCommand (val phrases: List<String>) {
    SWIPE_UP (listOf("swipe up", "swip up")),
    SWIPE_DOWN(listOf("swipe down", "swip down"))
}

object VoiceCommandManager {
    private val TAG = VoiceCommandManager::class.java.name
    private const val SAMPLE_RATE = 16000
    private const val CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO
    private const val AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT
    private val BUFFER_SIZE = AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT) * 2

    private var audioRecord: AudioRecord? = null
    private var echoCanceler: AcousticEchoCanceler? = null

    @Volatile private var currentState = DetectionState.DISABLED
    private var loopThread: Thread? = null

    private val engineScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private var timeoutJob: Job? = null
    private const val TIMEOUT_DURATION_MS = 5000L

    private val assistantNames = listOf("miri", "mary")
    private val wakePhrases =
        assistantNames.flatMap { listOf("ok $it", "okay $it", "oh $it", "o $it") }
    private val stopPhrases =
        assistantNames.map{ "stop $it" }
    private val commandPhrases = VoiceCommand.entries.flatMap { it.phrases }

    @RequiresPermission(Manifest.permission.RECORD_AUDIO)
    fun startEngine(model: Model, onCommandDetected: (VoiceCommand) -> Unit) {
        if (currentState != DetectionState.DISABLED) return

        // 1. Configure VOICE_COMMUNICATION to request system-level AEC pipelines
        audioRecord = AudioRecord(
            MediaRecorder.AudioSource.VOICE_COMMUNICATION,
            SAMPLE_RATE,
            CHANNEL_CONFIG,
            AUDIO_FORMAT,
            BUFFER_SIZE
        )

        if (audioRecord?.state != AudioRecord.STATE_INITIALIZED) {
            AppLog.d(TAG, "Failed to initialize AudioRecord pipeline.")
            return
        }

        // 2. Explicitly bind and force-enable AcousticEchoCanceler if available
        val sessionId = audioRecord!!.audioSessionId
        if (AcousticEchoCanceler.isAvailable()) {
            echoCanceler = AcousticEchoCanceler.create(sessionId).apply {
                enabled = true
            }
            AppLog.d(TAG, "Hardware/System Acoustic Echo Cancellation successfully active: ${echoCanceler?.enabled}")
        } else {
            AppLog.d(TAG, "System AEC not supported on this specific hardware layout.")
        }


        currentState = DetectionState.WAKE_WORD
        audioRecord?.startRecording()

        loopThread = Thread({ processAudioStream(model, onCommandDetected) }, "VoiceStateStreamingThread").apply {
            start()
        }
    }

    private fun processAudioStream(model: Model, onCommandDetected: (VoiceCommand) -> Unit) {
        Process.setThreadPriority(Process.THREAD_PRIORITY_AUDIO)
        val buffer = ShortArray(BUFFER_SIZE)

        // Initialize our two separate recognizers
        var currentRecognizer = Recognizer(model, SAMPLE_RATE.toFloat(),
            JSONArray(wakePhrases.toMutableList().apply { add("[unk]") }).toString())
        val wakeRecognizer = currentRecognizer
        val commandRecognizer = Recognizer(model, SAMPLE_RATE.toFloat(),
            JSONArray(commandPhrases.toMutableList().apply { add("[unk]") }).toString())

        var previousState = DetectionState.WAKE_WORD

        while (currentState != DetectionState.DISABLED) {
            // Context swap execution if the state shifted on the main/callback thread
            if (currentState != previousState) {
                currentRecognizer = when (currentState) {
                    DetectionState.WAKE_WORD -> {
                        cancelTimeoutTimer()
                        wakeRecognizer.reset()
                        wakeRecognizer
                    }
                    DetectionState.COMMAND -> {
                        commandRecognizer.reset()

                        val drainBuffer = ShortArray(BUFFER_SIZE)
                        var bytesDrained: Int
                        do {
                            bytesDrained = audioRecord?.read(drainBuffer, 0, drainBuffer.size, AudioRecord.READ_NON_BLOCKING) ?: 0
                        } while (bytesDrained > 0)

                        resetTimeoutTimer()
                        commandRecognizer
                    }
                    else -> currentRecognizer
                }
                previousState = currentState
            }

            val readBytes = audioRecord?.read(buffer, 0, buffer.size) ?: -1
            if (readBytes > 0) {
                val isFinal = currentRecognizer.acceptWaveForm(buffer, readBytes)

                // Parse instant partial responses for faster interaction handling
                val resultJson = if (isFinal) currentRecognizer.result else currentRecognizer.partialResult
                val success = handleSpeechResult(resultJson, isFinal, onCommandDetected)
                if (success) {
                    currentRecognizer.reset()

                    val drainBuffer = ShortArray(BUFFER_SIZE)
                    var bytesDrained: Int
                    do {
                        bytesDrained = audioRecord?.read(drainBuffer, 0, drainBuffer.size, AudioRecord.READ_NON_BLOCKING) ?: 0
                    } while (bytesDrained > 0)
                }
            }
        }

        cancelTimeoutTimer()
        wakeRecognizer.close()
        commandRecognizer.close()
    }

    private fun handleSpeechResult(jsonString: String, isFinal: Boolean, onCommandDetected: (VoiceCommand) -> Unit) : Boolean {
        try {
            val json = JSONObject(jsonString)
            var handled = false
            when (currentState) {
                DetectionState.WAKE_WORD -> {
                    val text =
                        json.optString("partial", "").lowercase()

                    if (wakePhrases.any { text.contains(it, ignoreCase = true) }) {
                        Log.d(TAG, "Wake word matched ${text}! Swapping engine directly to COMMAND state.")

                        // Audio Ducking point: Trigger media volume lower here before command capture
                        currentState = DetectionState.COMMAND
                        handled = true
                    }
                }

                DetectionState.COMMAND -> {
                    val text = if (isFinal) {
                        json.optString("text", "").lowercase()
                    } else {
                        json.optString("partial", "").lowercase()
                    }

                    if (text.isEmpty() || text == "[unk]") return false

                    if (stopPhrases.any { text.contains(it, ignoreCase = true) }) {
                        Log.d(TAG, "Command canceled by user. Falling back to WAKE_WORD loop.")
                        currentState = DetectionState.WAKE_WORD
                    } else {
                        val detectedCommand = VoiceCommand.entries.firstOrNull { it.phrases.contains(
                            text.lowercase()
                        ) }
                        if (detectedCommand != null) {
                            // Deliver isolated matched command string
                            onCommandDetected(detectedCommand)

                            resetTimeoutTimer()
                            handled = true
                            //framesToSkip = 1
                        }
                    }
                }
                else -> {}
            }

            return handled
        } catch (e: Exception) {
            Log.e(TAG, "Error handling state machine logic mapping", e)
            return false
        }
    }

    private fun resetTimeoutTimer() {
        timeoutJob?.cancel() // Invalidate the previous countdown instantly
        timeoutJob = engineScope.launch {
            delay(TIMEOUT_DURATION_MS)

            // If the delay completes without cancellation, target state shifts back
            if (currentState == DetectionState.COMMAND) {
                Log.d(TAG, "5-second command window expired. Reverting to WAKE_WORD mode.")
                currentState = DetectionState.WAKE_WORD
            }
        }
    }

    private fun cancelTimeoutTimer() {
        timeoutJob?.cancel()
        timeoutJob = null
    }

    fun stopEngine() {
        currentState = DetectionState.DISABLED
        cancelTimeoutTimer()
        engineScope.cancel() // Tear down coroutine structural bindings safely
        try {
            audioRecord?.stop()
            audioRecord?.release()
            audioRecord = null

            echoCanceler?.release()
            echoCanceler = null

            loopThread?.join(500)
            loopThread = null
        } catch (e: Exception) {
            AppLog.e(TAG, "Teardown exception inside engine", e)
        }
    }
}