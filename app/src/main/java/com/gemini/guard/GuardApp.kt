package com.gemini.guard

import android.app.Application

/** Nap ngon ngu giao dien cho moi thanh phan (Activity, dich vu, receiver). */
class GuardApp : Application() {
    override fun onCreate() {
        super.onCreate()
        L.load(this)
    }
}
