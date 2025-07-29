package com.mentos_koder.remote_lg_tv.util

import android.view.View
import com.muratozturk.click_shrink_effect.applyClickShrink

inline fun View.clicks(coolDown: Long = 300L, crossinline action: (view: View) -> Unit) {
    applyClickShrink()
    setOnClickListener(object : View.OnClickListener {
        var lastTime = 0L
        override fun onClick(v: View) {
            val now = System.currentTimeMillis()
            if (now - lastTime > coolDown) {
                action(v)
                lastTime = now
            }
        }
    })
}