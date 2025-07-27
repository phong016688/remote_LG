package com.mentos_koder.remote_lg_tv.util

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.mentos_koder.remote_lg_tv.R
import com.mentos_koder.remote_lg_tv.view.fragment.GoToSettingFragment

object PermissionUtils {
    private const val REQUEST_ID_MULTIPLE_PERMISSIONS = 1
    //private const val REQUEST_MICROPHONE_PERMISSION = 2

    fun checkAndRequestPermissions(activity: Activity): Boolean {
        val context: Context = activity.applicationContext
        val permissionReadExternalStorage: Int = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(context, Manifest.permission.READ_MEDIA_IMAGES)
        } else {
            ContextCompat.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE)
        }

        val permissionWriteExternalStorage: Int = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(context, Manifest.permission.READ_MEDIA_AUDIO)
        } else {
            ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE)
        }

        val listPermissionsNeeded = ArrayList<String>()
        if (permissionWriteExternalStorage != PackageManager.PERMISSION_GRANTED) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                listPermissionsNeeded.add(Manifest.permission.READ_MEDIA_AUDIO)
            } else {
                listPermissionsNeeded.add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            }
        }

        if (permissionReadExternalStorage != PackageManager.PERMISSION_GRANTED) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                listPermissionsNeeded.add(Manifest.permission.READ_MEDIA_IMAGES)
            } else {
                listPermissionsNeeded.add(Manifest.permission.READ_EXTERNAL_STORAGE)
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val permissionVideoStorage = ContextCompat.checkSelfPermission(context, Manifest.permission.READ_MEDIA_VIDEO)
            if (permissionVideoStorage != PackageManager.PERMISSION_GRANTED) {
                listPermissionsNeeded.add(Manifest.permission.READ_MEDIA_VIDEO)
            }
        }

        if (listPermissionsNeeded.isNotEmpty()) {
            ActivityCompat.requestPermissions(activity, listPermissionsNeeded.toTypedArray(), REQUEST_ID_MULTIPLE_PERMISSIONS)
            return false
        }
        return true
    }

//    fun checkAndRequestMicrophonePermission(activity: Activity): Boolean {
//        val context: Context = activity.applicationContext
//        val permissionRecordAudio = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO)
//
//        if (permissionRecordAudio != PackageManager.PERMISSION_GRANTED) {
//            ActivityCompat.requestPermissions(activity, arrayOf(Manifest.permission.RECORD_AUDIO), REQUEST_MICROPHONE_PERMISSION)
//            return false
//        }
//        return true
//    }

    fun isNetworkAvailable(context: Context): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val activeNetwork = connectivityManager.getNetworkCapabilities(network) ?: return false
        return when {
            activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> true
            activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> true
            else -> false
        }
    }

    fun checkAndRequestNotificationPermission(activity: Activity): Boolean {
        val notificationPermission = ContextCompat.checkSelfPermission(activity, Manifest.permission.POST_NOTIFICATIONS)
        if (notificationPermission != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(activity, arrayOf(Manifest.permission.POST_NOTIFICATIONS), 155)
            return false
        } else {
            return true
        }
    }

        fun onRequestPermissionsResult(activity: Activity, requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
            when (requestCode) {
                REQUEST_ID_MULTIPLE_PERMISSIONS -> {
                    val perms: MutableMap<String, Int> = HashMap()

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        perms[Manifest.permission.READ_MEDIA_IMAGES] = PackageManager.PERMISSION_GRANTED
                        perms[Manifest.permission.READ_MEDIA_AUDIO] = PackageManager.PERMISSION_GRANTED
                        perms[Manifest.permission.READ_MEDIA_VIDEO] = PackageManager.PERMISSION_GRANTED
                        perms[Manifest.permission.POST_NOTIFICATIONS] = PackageManager.PERMISSION_GRANTED
                    } else {
                        perms[Manifest.permission.WRITE_EXTERNAL_STORAGE] = PackageManager.PERMISSION_GRANTED
                        perms[Manifest.permission.READ_EXTERNAL_STORAGE] = PackageManager.PERMISSION_GRANTED
                    }

                    if (grantResults.isNotEmpty()) {
                        for (i in permissions.indices) {
                            perms[permissions[i]] = grantResults[i]
                        }

                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            if (perms[Manifest.permission.READ_MEDIA_IMAGES] == PackageManager.PERMISSION_GRANTED &&
                                perms[Manifest.permission.READ_MEDIA_AUDIO] == PackageManager.PERMISSION_GRANTED &&
                                perms[Manifest.permission.READ_MEDIA_VIDEO] == PackageManager.PERMISSION_GRANTED &&
                                perms[Manifest.permission.POST_NOTIFICATIONS] == PackageManager.PERMISSION_GRANTED
                            ) {
                                Toast.makeText(activity, "For Granting Permission.", Toast.LENGTH_LONG).show()
                            } else {
                                showFragmentGoToSettings(activity, "photo")

                            }
                        } else {
                            if (perms[Manifest.permission.WRITE_EXTERNAL_STORAGE] == PackageManager.PERMISSION_GRANTED &&
                                perms[Manifest.permission.READ_EXTERNAL_STORAGE] == PackageManager.PERMISSION_GRANTED
                            ) {
                                Toast.makeText(activity, "For Granting Permission.", Toast.LENGTH_LONG).show()
                            } else {
                                showFragmentGoToSettings(activity, "photo")
                            }
                        }
                    }
                }

//            REQUEST_MICROPHONE_PERMISSION -> {
//                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
//                    Toast.makeText(activity, "Microphone permission granted.", Toast.LENGTH_SHORT).show()
//                } else {
//                    showFragmentGoToSettings(activity,"micro")
//                }
//            }
            }

        }

        private fun showFragmentGoToSettings(activity: Activity, name: String) {
            val fragmentManager = (activity as AppCompatActivity).supportFragmentManager
            fragmentManager.beginTransaction()
                .replace(R.id.fragment_container, GoToSettingFragment(name))
                .commit()
        }
    }
