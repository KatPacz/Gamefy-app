package com.example.notifications

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * Synthesizes and plays a gentle, warm offline gong chime when a timer or Pomodoro phase finishes.
 * Uses pure PCM synthesis via AudioTrack — no network access or external audio assets required.
 */
object TimerSoundHelper {
  internal const val SAMPLE_RATE_HZ = 44_100
  internal const val DURATION_SECONDS = 2.2

  private val cachedGongPcm: ShortArray by lazy { generateGentleGongPcm() }

  internal fun generateGentleGongPcm(): ShortArray {
    val totalSamples = (SAMPLE_RATE_HZ * DURATION_SECONDS).toInt()
    val pcm = ShortArray(totalSamples)
    val baseFreq = 220.0 // Warm A3 fundamental
    val twoPi = 2.0 * PI
    val fadeOutStart = DURATION_SECONDS - 0.18

    for (i in 0 until totalSamples) {
      val t = i.toDouble() / SAMPLE_RATE_HZ
      // Soft 35 ms attack eliminates harsh clicks; gentle tail fade prevents end-pop
      val attack = min(1.0, t / 0.035)
      val tailFade = if (t > fadeOutStart) {
        ((DURATION_SECONDS - t) / 0.18).coerceIn(0.0, 1.0)
      } else {
        1.0
      }

      // Singing-bowl / zen gong harmonic partials with natural staggered exponential decay
      val subHum = 0.14 * sin(twoPi * (baseFreq * 0.5) * t) * exp(-1.2 * t)
      val fundamental = 0.50 * sin(twoPi * baseFreq * t) * exp(-1.55 * t)
      val octave = 0.24 * sin(twoPi * (baseFreq * 2.0) * t) * exp(-2.3 * t)
      val bellPartial1 = 0.15 * sin(twoPi * (baseFreq * 2.76) * t) * exp(-3.1 * t)
      val bellPartial2 = 0.08 * sin(twoPi * (baseFreq * 4.07) * t) * exp(-4.2 * t)

      val sample = (subHum + fundamental + octave + bellPartial1 + bellPartial2) * attack * tailFade * 0.42
      val scaled = (sample * Short.MAX_VALUE).roundToInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt())
      pcm[i] = scaled.toShort()
    }
    return pcm
  }

  fun playGentleGong(context: Context) {
    runCatching {
      val audioManager = context.applicationContext.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
      if (audioManager != null && audioManager.ringerMode != AudioManager.RINGER_MODE_NORMAL) {
        return
      }
      val samples = cachedGongPcm
      val byteSize = samples.size * 2

      val thread = Thread {
        var track: AudioTrack? = null
        runCatching {
          val attributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_NOTIFICATION_EVENT)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()
          val format = AudioFormat.Builder()
            .setSampleRate(SAMPLE_RATE_HZ)
            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
            .build()
          track = AudioTrack.Builder()
            .setAudioAttributes(attributes)
            .setAudioFormat(format)
            .setBufferSizeInBytes(byteSize)
            .setTransferMode(AudioTrack.MODE_STATIC)
            .build()
          val activeTrack = track ?: return@runCatching
          if (activeTrack.state == AudioTrack.STATE_INITIALIZED) {
            activeTrack.write(samples, 0, samples.size)
            activeTrack.play()
            Thread.sleep((DURATION_SECONDS * 1000).toLong() + 120L)
          }
        }
        runCatching {
          track?.stop()
          track?.release()
        }
      }
      thread.isDaemon = true
      thread.name = "gamefy-gentle-gong"
      thread.start()
    }
  }
}
