package com.gemini.guard

import android.app.Activity
import android.os.Bundle
import android.widget.Toast

/** Activity trong suot: bam la bat/tat Tro nang roi dong ngay. Dung cho phim tat khi giu icon app. */
class AccToggleActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val c = applicationContext
        Toast.makeText(c, AccToggle.toggle(c), Toast.LENGTH_SHORT).show()
        finish()
    }
}
