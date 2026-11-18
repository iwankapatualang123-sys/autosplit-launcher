package com.autosplit.launcher

import android.os.SystemClock

/** Jam bersama untuk animasi bingkai berjalan: sudut dihitung dari waktu, jadi gerakan tetap halus. */
object Ambient {
    @Volatile
    var running = false

    @Volatile
    var periodMs = 9000L

    fun angle(): Float = (SystemClock.uptimeMillis() % periodMs) * 360f / periodMs
}
