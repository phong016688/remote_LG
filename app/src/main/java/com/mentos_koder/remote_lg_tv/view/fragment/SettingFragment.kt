package com.mentos_koder.remote_lg_tv.view.fragment

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.Vibrator
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.widget.SwitchCompat
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import androidx.core.net.toUri
import androidx.fragment.app.Fragment
import com.mentos_koder.remote_lg_tv.R
import com.mentos_koder.remote_lg_tv.util.Constants
import com.mentos_koder.remote_lg_tv.util.Singleton
import com.mentos_koder.remote_lg_tv.util.restoreSwitchState
import com.mentos_koder.remote_lg_tv.util.showDialogDisconnect
import com.mentos_koder.remote_lg_tv.view.HelpActivity
import com.mentos_koder.remote_lg_tv.view.PrivatePolicyActivity


class SettingsFragment : Fragment() {
    private var castSettingButton: ImageButton? = null
    private var linearShareApp: LinearLayout? = null
    private var linearFeedback: LinearLayout? = null
    private var linearHelp: LinearLayout? = null
    private var linearPolicy: LinearLayout? = null
    private var linearRate: LinearLayout? = null
    private var versionName: String? = null
    private var tvVersion: TextView? = null
    private lateinit var ring: SwitchCompat
    private val VIBRATE_PERMISSION_REQUEST_CODE = 1001

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        val view: View = inflater.inflate(R.layout.fragment_config, container, false)
        initializeViews(view)
        ring.isChecked = context?.let { restoreSwitchState(it) } == true
        val sharedPreferences =
            requireContext().getSharedPreferences(Constants.PREFERENCE_NAME, Context.MODE_PRIVATE)
        val switchState = sharedPreferences.getBoolean(Constants.SWITCH_STATE, true)
        ring.isChecked = switchState
        versionName()
        setupEventListeners()
        return view
    }

    private fun initializeViews(rootView: View) {
        ring = rootView.findViewById(R.id.switchRing)
        castSettingButton = rootView.findViewById(R.id.btn_cast_setting)
        linearShareApp = rootView.findViewById(R.id.linear_shareAppLayout)
        linearRate = rootView.findViewById(R.id.linear_rateLayout)
        linearFeedback = rootView.findViewById(R.id.linear_feedbackLayout)
        linearPolicy = rootView.findViewById(R.id.linear_policyLayout)
        linearHelp = rootView.findViewById(R.id.linear_helpLayout)
        tvVersion = rootView.findViewById(R.id.version)
    }

    private fun performVibrateAction() {
        val vibrator = requireContext().getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        if (ring.isChecked) {
            vibrator.vibrate(100)
        }
    }

    private fun saveSwitchState(isChecked: Boolean) {
        val sharedPreferences =
            requireContext().getSharedPreferences(Constants.PREFERENCE_NAME, Context.MODE_PRIVATE)
        sharedPreferences.edit { putBoolean(Constants.SWITCH_STATE, isChecked) }
    }


    private fun setupEventListeners() {

        ring.setOnCheckedChangeListener { _, isChecked ->
            saveSwitchState(isChecked)
            if (isChecked) {
                requestVibratePermissionIfNeeded()
            } else {
                revokeVibratePermission()
            }
        }

        castSettingButton!!.setOnClickListener {
            performVibrateAction()
            if (isConnected) {
                context?.showDialogDisconnect {
                    val singleton: Singleton = Singleton.getInstance()
                    singleton.setConnected(false)
                    singleton.disconnect()
                }
            } else {
                showFragmentDevice()
            }
        }
        linearFeedback!!.setOnClickListener {
            performVibrateAction()
            sendEmail()
        }
        linearRate!!.setOnClickListener {
            performVibrateAction()
            openPlayStore()
        }
        linearShareApp!!.setOnClickListener {
            performVibrateAction()
            shareApp()
        }
        linearPolicy!!.setOnClickListener {
            performVibrateAction()
            startActivity(
                Intent(
                    activity, PrivatePolicyActivity::class.java
                )
            )
        }
        linearHelp!!.setOnClickListener {
            performVibrateAction()
            startActivity(
                Intent(
                    activity, HelpActivity::class.java
                )
            )
        }
    }

    private fun requestVibratePermissionIfNeeded() {
        if (ContextCompat.checkSelfPermission(
                requireContext(), Manifest.permission.VIBRATE
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(
                arrayOf(Manifest.permission.VIBRATE), VIBRATE_PERMISSION_REQUEST_CODE
            )
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray
    ) {
        if (requestCode == VIBRATE_PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(
                    activity, getString(R.string.app_need_vibrate_permission), Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun revokeVibratePermission() {
        if (ContextCompat.checkSelfPermission(
                requireContext(), Manifest.permission.VIBRATE
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                requireActivity(),
                arrayOf(Manifest.permission.VIBRATE),
                VIBRATE_PERMISSION_REQUEST_CODE
            )
        }
    }


    private fun versionName() {
        try {
            val packageInfo =
                requireActivity().packageManager.getPackageInfo(requireContext().packageName, 0)
            versionName = packageInfo.versionName
            tvVersion!!.text = "${getString(R.string.version_app)}: $versionName"
        } catch (e: PackageManager.NameNotFoundException) {
            e.printStackTrace()
        }
    }

    private fun showFragmentDevice() {
        val deviceFrag = DeviceFragment()
        requireActivity().supportFragmentManager.beginTransaction().setCustomAnimations(
            R.anim.slide_in_right,  // enter
            R.anim.slide_out_left // exit
        ).replace(R.id.fragment_container, deviceFrag, "findThisFragment")
            .addToBackStack("findThisFragment").commit()
    }

    private val isConnected: Boolean
        get() {
            val singleton = Singleton.getInstance()
            return singleton.isConnectedCustom
        }

    private fun sendEmail() {
        val appName = getString(R.string.app_name)
        val emailSubject = "Report Bug issue & suggested - Version: $versionName $appName"
        val emailBody = "Please describe your issue or suggested here"
        val uri =
            ("mailto:personpick11@gmail.com" + "?subject=" + Uri.encode(emailSubject) + "&body=" + Uri.encode(
                emailBody
            )).toUri()
        val emailIntent = Intent(Intent.ACTION_SENDTO, uri)
        try {
            startActivity(emailIntent)
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(activity, "Gmail app not found", Toast.LENGTH_SHORT).show()
        }
    }

    private fun shareApp() {
        val packageName = "com.mentos_koder.remote_lg_tv"
        val appName = getString(R.string.app_name)
        val emailBody =
            ("I am using " + appName + "to plan my day, It's really a convenient to-do list. Share it with you now." + "Download it here:" + " https://play.google.com/store/apps/details?id=" + packageName)
        val emailIntent = Intent(Intent.ACTION_SEND)
        emailIntent.setType("message/rfc822")
        emailIntent.putExtra(Intent.EXTRA_EMAIL, arrayOf(" "))
        emailIntent.putExtra(Intent.EXTRA_SUBJECT, appName)
        emailIntent.putExtra(Intent.EXTRA_TEXT, emailBody)
        startActivity(Intent.createChooser(emailIntent, "Send email..."))
    }

    private fun openPlayStore() {
        val packageName = "com.mentos_koder.remote_lg_tv"
        try {
            startActivity(Intent(Intent.ACTION_VIEW, "market://details?id=$packageName".toUri()))
        } catch (e: ActivityNotFoundException) {
            startActivity(
                Intent(
                    Intent.ACTION_VIEW,
                    "https://play.google.com/store/apps/details?id=$packageName".toUri()
                )
            )
        }
    }
}