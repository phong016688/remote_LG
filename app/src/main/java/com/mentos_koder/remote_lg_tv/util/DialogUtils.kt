package com.mentos_koder.remote_lg_tv.util

import android.app.ActionBar
import android.app.Dialog
import android.content.Context
import com.mentos_koder.remote_lg_tv.databinding.DialogDisconnectBinding
import com.mentos_koder.remote_lg_tv.databinding.DialogPairingBinding

fun Context.showDialogDisconnect(onDisconnect: () -> Unit) {
    val dialog = Dialog(this)
    dialog.window?.layoutInflater?.let {
        val binding = DialogDisconnectBinding.inflate(it, null, false)
        dialog.setContentView(binding.root)
        binding.btnCancel.clicks { dialog.dismiss() }
        binding.btnContinue.clicks {
            dialog.dismiss()
            onDisconnect()
        }
    }
    dialog.show()
    dialog.setCancelable(true)
    dialog.window?.setLayout(
        ActionBar.LayoutParams.MATCH_PARENT,
        ActionBar.LayoutParams.WRAP_CONTENT
    )
    dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
}

fun Context.showDialogPairing(onPairing: (String) -> Unit) {
    val dialog = Dialog(this)
    dialog.window?.layoutInflater?.let {
        val binding = DialogPairingBinding.inflate(it, null, false)
        dialog.setContentView(binding.root)
        binding.btnCancel.clicks { dialog.dismiss() }
        binding.btnContinue.clicks {
            dialog.dismiss()
            onPairing(binding.edtPin.toString().trim())
        }
    }
    dialog.show()
    dialog.setCancelable(true)
    dialog.window?.setLayout(
        ActionBar.LayoutParams.MATCH_PARENT,
        ActionBar.LayoutParams.WRAP_CONTENT
    )
    dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
}