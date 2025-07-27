package com.mentos_koder.remote_lg_tv.util

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.tasks.Task
import com.google.android.material.snackbar.Snackbar
import com.google.android.play.core.appupdate.AppUpdateInfo
import com.google.android.play.core.appupdate.AppUpdateManager
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.install.InstallState
import com.google.android.play.core.install.InstallStateUpdatedListener
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.InstallStatus
import com.google.android.play.core.install.model.UpdateAvailability
import com.google.android.play.core.review.ReviewInfo
import com.google.android.play.core.review.ReviewManagerFactory
import com.mentos_koder.remote_lg_tv.R


class InApp(private val parentActivity: Activity) {
    private val appUpdateManager: AppUpdateManager? = AppUpdateManagerFactory.create(parentActivity)
    private val appUpdateType = AppUpdateType.IMMEDIATE
    private val MY_REQUEST_CODE = 500
    private var stateUpdatedListener = InstallStateUpdatedListener { installState: InstallState ->
            if (installState.installStatus() == InstallStatus.DOWNLOADED) {
                popupSnackBarForCompleteUpdate()
            }
        }

    fun checkForAppUpdate() {
        appUpdateManager!!.appUpdateInfo.addOnSuccessListener { info: AppUpdateInfo ->
            val isUpdateAvailable = info.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE
            val isUpdateAllowed = info.isUpdateTypeAllowed(appUpdateType)
            if (isUpdateAvailable && isUpdateAllowed) {
                showUpdateNotification()
//                try {
//                    appUpdateManager.startUpdateFlowForResult(
//                        info,
//                        appUpdateType,
//                        parentActivity,
//                        MY_REQUEST_CODE
//                    )
//                } catch (e: SendIntentException) {
//                    throw RuntimeException(e)
//                }
            }
        }
        appUpdateManager.registerListener(stateUpdatedListener)
    }

    fun onActivityResult(requestCode: Int, resultCode: Int) {
        if (requestCode == MY_REQUEST_CODE) {
            if (resultCode == Activity.RESULT_CANCELED) {
                println("Update canceled by user")
            } else if (resultCode != AppCompatActivity.RESULT_OK) {
                checkForAppUpdate()
            }
        }
    }

    private fun popupSnackBarForCompleteUpdate() {
        Snackbar.make(
            parentActivity.findViewById(R.id.fragment_container),
            "An update has just been downloaded.",
            Snackbar.LENGTH_INDEFINITE
        ).setAction("RESTART") { _: View? ->
            appUpdateManager?.completeUpdate()
        }.show()
    }

    fun onResume() {
        appUpdateManager?.appUpdateInfo?.addOnSuccessListener { info: AppUpdateInfo ->
            if (info.installStatus() == InstallStatus.DOWNLOADED) {
                popupSnackBarForCompleteUpdate()
            }
        }
    }

    fun onDestroy() {
        appUpdateManager?.unregisterListener(stateUpdatedListener)
    }

    fun requestReview(activity: Activity) {
        val reviewManager = ReviewManagerFactory.create(activity)
        val reviewInfoTask = reviewManager.requestReviewFlow()
        reviewInfoTask.addOnCompleteListener { task: Task<ReviewInfo?> ->
            try {
                if (task.isSuccessful) {
                    val reviewInfo = task.result
                    val reviewFlow =
                        reviewManager.launchReviewFlow(
                            activity,
                            reviewInfo!!
                        )
                    reviewFlow.addOnCompleteListener {
                        Toast.makeText(activity, "Thank you for rating!", Toast.LENGTH_SHORT).show()
                    }.addOnFailureListener { e1: Exception ->
                        Log.e("Rating", "error1: " + e1.message)
                    }
                } else {
                    val errorRating = task.exception!!.message
                    Log.e("Rating", "error1: $errorRating")
                }
            } catch (e: Exception) {
                Log.e("Rating", "error: $e")
            }
        }.addOnFailureListener { e: Exception ->
            Log.e(
                "Rating", "error: " + e.message
            )
        }
    }

    private fun showUpdateNotification() {
        val dialogBuilder = AlertDialog.Builder(parentActivity)
        val packageName = "com.mentos_koder.universalremote"

        dialogBuilder.setTitle("Update Available !")
        dialogBuilder.setMessage("New version of the app is available. Please update your app to use all of our amazing features.")

        dialogBuilder.setPositiveButton("Update") { _, _ ->
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$packageName"))
            parentActivity.startActivity(intent)
        }

        dialogBuilder.setNegativeButton("Cancel") { dialog, _ -> dialog.dismiss() }

        val alertDialog = dialogBuilder.create()

        alertDialog.window?.setBackgroundDrawableResource(R.drawable.background_gradient)
        alertDialog.show()

        alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(Color.BLACK)
        alertDialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(Color.BLACK)
    }


}

