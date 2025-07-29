package com.mentos_koder.remote_lg_tv.view.fragment

import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Vibrator
import android.speech.RecognizerIntent
import android.util.Log
import android.view.GestureDetector
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResultCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.cardview.widget.CardView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.codertainment.dpadview.DPadView
import com.mentos_koder.remote_lg_tv.R
import com.mentos_koder.remote_lg_tv.database.AppDatabase
import com.mentos_koder.remote_lg_tv.util.KeycodeLG
import com.mentos_koder.remote_lg_tv.util.PermissionUtils
import com.mentos_koder.remote_lg_tv.util.Singleton
import com.mentos_koder.remote_lg_tv.util.clicks
import com.mentos_koder.remote_lg_tv.util.restoreSwitchState
import com.mentos_koder.remote_lg_tv.view.KeyboardActivity
import java.util.Locale
import kotlin.math.abs
import androidx.core.content.edit


class HomeLGFragment : Fragment(), GestureDetector.OnGestureListener {

    private lateinit var dpadView: DPadView
    private lateinit var cvPower: FrameLayout
    private lateinit var cvCast: FrameLayout
    private lateinit var imgVolumeUp: ImageView
    private lateinit var imgVolumeDown: ImageView
    private lateinit var linearChList: LinearLayout
    private lateinit var linearHome: LinearLayout
    private lateinit var linearMute: LinearLayout
    private lateinit var linearInfo: LinearLayout
    private lateinit var imgChUp: ImageView
    private lateinit var imgChDown: ImageView
    private lateinit var linearMenu: LinearLayout
    private lateinit var linearHdmi: LinearLayout
    private lateinit var linearMic: LinearLayout
    private lateinit var linearKeyboard: LinearLayout
    private lateinit var linearRewind: LinearLayout
    private lateinit var linearPlay: LinearLayout
    private lateinit var linearStop: LinearLayout
    private lateinit var linearForward: LinearLayout
    private lateinit var cvYoutube: FrameLayout
    private lateinit var cvNetflix: FrameLayout
    private lateinit var cvPrimeVideo: FrameLayout
    private lateinit var cvTabControl: CardView
    private lateinit var cvTabNumber: CardView
    private lateinit var cvTabTouchPad: CardView
    private lateinit var imgTabControl: ImageView
    private lateinit var imgTabNumber: ImageView
    private lateinit var imgTabTouchPad: ImageView
    private lateinit var linearBack: LinearLayout
    private lateinit var linearExit: LinearLayout
    private lateinit var linearSetting: LinearLayout
    private lateinit var linearTv: LinearLayout
    private lateinit var cvA: CardView
    private lateinit var cvB: CardView
    private lateinit var cvC: CardView
    private lateinit var cvD: CardView
    private lateinit var linearOne: LinearLayout
    private lateinit var linearTwo: LinearLayout
    private lateinit var linearThree: LinearLayout
    private lateinit var linearFour: LinearLayout
    private lateinit var linearFive: LinearLayout
    private lateinit var linearSix: LinearLayout
    private lateinit var linearSeven: LinearLayout
    private lateinit var linearEight: LinearLayout
    private lateinit var linearNine: LinearLayout
    private lateinit var linearZero: LinearLayout
    private lateinit var constraintControl: ConstraintLayout
    private lateinit var constraintNumber: ConstraintLayout
    private lateinit var constraintTouchPad: ConstraintLayout
    private lateinit var gestureDetector: GestureDetector

    private var isAutoConnectCalled = true
    private val primeVideoIdString = "amazon"
    private val netflixIdString = "netflix"
    private val youtubeIdString = "youtube.leanback.v4"

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        val view: View = inflater.inflate(R.layout.fragment_home_lg, container, false)
        setupUI(view)
        setEvenListenVisibility()
        if (getTypeDevice() == "samsung" && isAutoConnectCalled) {
            autoConnect()
            isAutoConnectCalled = false
        }
        gestureDetector = GestureDetector(context, this)
        setUpListener()
        return view
    }

    private fun checkRing(): Boolean? {
        return context?.let { restoreSwitchState(it) }
    }

    private fun performVibrateAction() {
        val vibrator = requireContext().getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        if (checkRing() == true) {
            vibrator.vibrate(100)
        }
    }

    private fun getTypeDevice(): String {
        val type = Singleton.getInstance().getTypeDevice().lowercase(Locale.ROOT)
        return type
    }

    private fun handleApp(keyLG: String) {
        val type = getTypeDevice()
        when (type) {
            "lg" -> {
                Singleton.getInstance().openAppOnTV(keyLG)
            }
        }
    }

    private fun handleEventButton(key: KeycodeLG) {
        if (isConnected) {
            val singleton = Singleton.getInstance()
            val eventName = key.value
            singleton.getService(eventName)
        } else {
            showFragmentDevice()
        }
    }

    private val isConnected: Boolean
        get() {
            val singleton = Singleton.getInstance()
            return singleton.isConnectedCustom
        }

    @SuppressLint("UseKtx")
    private fun autoConnect() {
        try {
            val deviceDao = AppDatabase.getDatabase(requireActivity()).deviceDao()
            val device = deviceDao.lastDateConnect
            val singleton = Singleton.getInstance()
            if (!isConnected && device != null && device.token != null) {
                val ip = device.address
                val name = device.typeDevice
                val token = device.token
                singleton.AutoConnectURI(name, ip, token)
                val sharedPref = requireActivity().getSharedPreferences("MyPreferences", Context.MODE_PRIVATE)
                sharedPref.edit {
                    putString("nameDevice", device.name)
                }
                singleton.setConnected(true)
            } else {
                singleton.setConnected(false)
            }
        } catch (e: Exception) {
            Log.e("####", "autoConnect: " + e.message)
        }
    }

    private fun setupUI(view: View) {

        cvPower = view.findViewById(R.id.cv_power)
        cvCast = view.findViewById(R.id.cv_cast)
        imgVolumeUp = view.findViewById(R.id.img_volume_up)
        imgVolumeDown = view.findViewById(R.id.img_volume_down)
        linearChList = view.findViewById(R.id.linear_ch_list)
        linearHome = view.findViewById(R.id.linear_home)
        linearMute = view.findViewById(R.id.linear_mute)
        linearInfo = view.findViewById(R.id.linear_info)
        imgChUp = view.findViewById(R.id.img_ch_up)
        imgChDown = view.findViewById(R.id.img_ch_down)
        linearMenu = view.findViewById(R.id.linear_menu)
        linearHdmi = view.findViewById(R.id.linear_hdmi)
        linearMic = view.findViewById(R.id.linear_mic)
        linearKeyboard = view.findViewById(R.id.linear_keyboard)

        linearRewind = view.findViewById(R.id.linear_rewind)
        linearPlay = view.findViewById(R.id.linear_play)
        linearStop = view.findViewById(R.id.linear_stop)
        linearForward = view.findViewById(R.id.linear_forward)

        cvYoutube = view.findViewById(R.id.cv_youtube)
        cvNetflix = view.findViewById(R.id.cv_netflix)
        cvPrimeVideo = view.findViewById(R.id.cv_prime_video)

        cvTabControl = view.findViewById(R.id.cv_tab_control)
        cvTabNumber = view.findViewById(R.id.cv_tab_number)
        cvTabTouchPad = view.findViewById(R.id.cv_tab_touch_pad)
        imgTabControl = view.findViewById(R.id.img_tab_control)
        imgTabNumber = view.findViewById(R.id.img_tab_number)
        imgTabTouchPad = view.findViewById(R.id.img_tab_touch_pad)

        linearBack = view.findViewById(R.id.linear_back)
        linearExit = view.findViewById(R.id.linear_exit)
        linearSetting = view.findViewById(R.id.linear_setting)
        linearTv = view.findViewById(R.id.linear_tv)

        cvA = view.findViewById(R.id.cv_a)
        cvB = view.findViewById(R.id.cv_b)
        cvC = view.findViewById(R.id.cv_c)
        cvD = view.findViewById(R.id.cv_d)
        linearOne = view.findViewById(R.id.linear_one)
        linearTwo = view.findViewById(R.id.linear_two)
        linearThree = view.findViewById(R.id.linear_three)
        linearFour = view.findViewById(R.id.linear_four)
        linearFive = view.findViewById(R.id.linear_five)
        linearSix = view.findViewById(R.id.linear_six)
        linearSeven = view.findViewById(R.id.linear_seven)
        linearEight = view.findViewById(R.id.linear_eight)
        linearNine = view.findViewById(R.id.linear_nine)
        linearZero = view.findViewById(R.id.linear_zero)

        constraintControl = view.findViewById(R.id.constraint_control)
        constraintNumber = view.findViewById(R.id.constraint_number)
        constraintTouchPad = view.findViewById(R.id.constraint_touch_pad)

        dpadView = view.findViewById(R.id.dPadView_remote_control)
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setUpListener() {
        constraintTouchPad.setOnTouchListener { v: View?, event: MotionEvent? ->
            performVibrateAction()
            gestureDetector.onTouchEvent(
                event!!
            )
            true
        }

        dpadView.isHapticFeedbackEnabled = false
        dpadView.onDirectionPressListener = { direction, action ->
            when (action) {
                MotionEvent.ACTION_DOWN -> {
                    when (direction?.name) {
                        "LEFT" -> {
                            dpadView.setBackgroundResource(R.drawable.ic_pad_right)
                            performVibrateAction()
                            handleEventButton(KeycodeLG.LEFT)
                        }

                        "UP" -> {
                            dpadView.setBackgroundResource(R.drawable.ic_pad_top)
                            performVibrateAction()
                            handleEventButton(KeycodeLG.UP)
                        }

                        "RIGHT" -> {
                            dpadView.setBackgroundResource(R.drawable.ic_pad_left)
                            performVibrateAction()
                            handleEventButton(KeycodeLG.RIGHT)
                        }

                        "DOWN" -> {
                            dpadView.setBackgroundResource(R.drawable.ic_pad_bottom)
                            performVibrateAction()
                            handleEventButton(KeycodeLG.DOWN)
                        }

                        "CENTER" -> {
                            performVibrateAction()
                            handleEventButton(KeycodeLG.ENTER)
                        }
                    }
                }

                MotionEvent.ACTION_UP -> {
                    dpadView.setBackgroundResource(R.drawable.ic_pad)
                }
            }
        }

        setViewClickListener(cvPower, KeycodeLG.POWER)
        setViewClickListener(linearMenu, KeycodeLG.MENU)
        setViewClickListener(linearHome, KeycodeLG.HOME)
        setViewClickListener(linearBack, KeycodeLG.BACK)
        setViewClickListener(linearInfo, KeycodeLG.INFO)
        setViewClickListener(linearHdmi, KeycodeLG.HDMI)
        setViewClickListener(imgVolumeUp, KeycodeLG.VOLUMEUP)
        setViewClickListener(imgVolumeDown, KeycodeLG.VOLUMEDOWN)
        setViewClickListener(linearExit, KeycodeLG.EXIT)
        setViewClickListener(linearMute, KeycodeLG.MUTE)
        setViewClickListener(imgChUp, KeycodeLG.CHANNELUP)
        setViewClickListener(imgChDown, KeycodeLG.CHANNELDOWN)
        setViewClickListener(linearChList, KeycodeLG.LIST)
        setViewClickListener(linearRewind, KeycodeLG.LEFT)
        setViewClickListener(linearForward, KeycodeLG.RIGHT)
        setViewClickListener(linearPlay, KeycodeLG.PLAY)
        setViewClickListener(linearStop, KeycodeLG.PAUSE)
        setViewClickListener(linearSetting, KeycodeLG.SETTING)
        setViewClickListener(cvA, KeycodeLG.RED)
        setViewClickListener(cvB, KeycodeLG.GREEN)
        setViewClickListener(cvC, KeycodeLG.YELLOW)
        setViewClickListener(cvD, KeycodeLG.BLUE)
        setViewClickListener(linearOne, KeycodeLG.ONE)
        setViewClickListener(linearTwo, KeycodeLG.TWO)
        setViewClickListener(linearThree, KeycodeLG.THREE)
        setViewClickListener(linearFour, KeycodeLG.FOUR)
        setViewClickListener(linearFive, KeycodeLG.FIVE)
        setViewClickListener(linearSix, KeycodeLG.SIX)
        setViewClickListener(linearSeven, KeycodeLG.SEVEN)
        setViewClickListener(linearEight, KeycodeLG.EIGHT)
        setViewClickListener(linearNine, KeycodeLG.NINE)
        setViewClickListener(linearZero, KeycodeLG.ZERO)
        linearMic.clicks {
            performVibrateAction()
            if (isConnected) {
                handleMicro()
            } else {
                showFragmentDevice()
            }
        }

        cvYoutube.clicks {
            performVibrateAction()
            if (isConnected) {
                handleApp(youtubeIdString)
            } else {
                showFragmentDevice()
            }
        }
        cvPrimeVideo.clicks {
            performVibrateAction()
            if (isConnected) {
                handleApp(primeVideoIdString)
            } else {
                showFragmentDevice()
            }
        }

        cvNetflix.clicks {
            performVibrateAction()
            if (isConnected) {
                handleApp(netflixIdString)
            } else {
                showFragmentDevice()
            }
        }

        cvCast.clicks {
            performVibrateAction()
            if (isConnected) {
                showAlertDialogDisconnected("LG")
            } else {
                showFragmentDevice()
            }
        }

        linearKeyboard.clicks {
            performVibrateAction()
            if (isConnected) {
                val intent = Intent(activity, KeyboardActivity::class.java)
                requireActivity().startActivity(intent)
            } else {
                showFragmentDevice()
            }
        }
    }

    private fun setViewClickListener(view: View, key: KeycodeLG) {
        view.clicks {
            performVibrateAction()
            handleEventButton(key)
        }
    }

    private fun handleMicro() {
        if (!PermissionUtils.isNetworkAvailable(requireContext())) {
            Toast.makeText(
                requireContext(),
                "No internet connection. Please try again later.",
                Toast.LENGTH_SHORT
            ).show()
        }
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(
                RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
            )
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak to text")
        }
        try {
            speechResultLauncher.launch(intent)
        } catch (e: Exception) {
            Toast.makeText(requireContext(), " " + e.message, Toast.LENGTH_SHORT).show()
        }
    }

    private val speechResultLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult(), ActivityResultCallback { result ->
            if (result.resultCode == Activity.RESULT_OK && result.data != null) {
                val speechResult =
                    result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
                if (!speechResult.isNullOrEmpty()) {
                    val singleton = Singleton.getInstance()
                    singleton.sendText(speechResult[0])
                }
            }
        })

    private fun setEvenListenVisibility() {
        cvTabControl.clicks {
            constraintControl.visibility = View.VISIBLE
            constraintNumber.visibility = View.GONE
            constraintTouchPad.visibility = View.GONE

            cvTabControl.setCardBackgroundColor(
                ContextCompat.getColor(
                    requireContext(), R.color.white
                )
            )
            imgTabControl.setColorFilter(ContextCompat.getColor(requireContext(), R.color.pink))

            cvTabNumber.setCardBackgroundColor(
                ContextCompat.getColor(
                    requireContext(), R.color.bgr_btn_lg
                )
            )
            imgTabNumber.setColorFilter(ContextCompat.getColor(requireContext(), R.color.white))

            cvTabTouchPad.setCardBackgroundColor(
                ContextCompat.getColor(
                    requireContext(), R.color.bgr_btn_lg
                )
            )
            imgTabTouchPad.setColorFilter(ContextCompat.getColor(requireContext(), R.color.white))
        }
        cvTabNumber.clicks {
            constraintControl.visibility = View.GONE
            constraintNumber.visibility = View.VISIBLE
            constraintTouchPad.visibility = View.GONE

            cvTabControl.setCardBackgroundColor(
                ContextCompat.getColor(
                    requireContext(), R.color.bgr_btn_lg
                )
            )
            imgTabControl.setColorFilter(ContextCompat.getColor(requireContext(), R.color.white))

            cvTabNumber.setCardBackgroundColor(
                ContextCompat.getColor(
                    requireContext(), R.color.white
                )
            )
            imgTabNumber.setColorFilter(ContextCompat.getColor(requireContext(), R.color.pink))

            cvTabTouchPad.setCardBackgroundColor(
                ContextCompat.getColor(
                    requireContext(), R.color.bgr_btn_lg
                )
            )
            imgTabTouchPad.setColorFilter(ContextCompat.getColor(requireContext(), R.color.white))
        }
        cvTabTouchPad.clicks {
            constraintControl.visibility = View.GONE
            constraintNumber.visibility = View.GONE
            constraintTouchPad.visibility = View.VISIBLE

            cvTabControl.setCardBackgroundColor(
                ContextCompat.getColor(
                    requireContext(), R.color.bgr_btn_lg
                )
            )
            imgTabControl.setColorFilter(ContextCompat.getColor(requireContext(), R.color.white))

            cvTabNumber.setCardBackgroundColor(
                ContextCompat.getColor(
                    requireContext(), R.color.bgr_btn_lg
                )
            )
            imgTabNumber.setColorFilter(ContextCompat.getColor(requireContext(), R.color.white))

            cvTabTouchPad.setCardBackgroundColor(
                ContextCompat.getColor(
                    requireContext(), R.color.white
                )
            )
            imgTabTouchPad.setColorFilter(ContextCompat.getColor(requireContext(), R.color.pink))
        }
    }

    private fun showFragmentDevice() {
        val deviceFrag = DeviceFragment()
        requireActivity().supportFragmentManager.beginTransaction().setCustomAnimations(
                R.anim.slide_in_right, R.anim.slide_out_left
            ).replace(R.id.fragment_container, deviceFrag, "findThisFragment")
            .addToBackStack("findThisFragment").commit()
    }

    private fun showAlertDialogDisconnected(txtDevice: String): AlertDialog {
        val alertDialogBuilder = AlertDialog.Builder(
            activity
        )
        val view = getLayoutInflater().inflate(R.layout.item_cast, null)
        alertDialogBuilder.setView(view)
        val textName = view.findViewById<TextView>(R.id.textNameDevice)
        val btnDisconnect = view.findViewById<Button>(R.id.btnDisconnect)
        val btnCancel = view.findViewById<Button>(R.id.btn_cancel)
        textName.text = txtDevice
        val alertDialog = alertDialogBuilder.create()
        btnDisconnect.clicks {
            val singleton = Singleton.getInstance()
            singleton.setConnected(false)
            singleton.disconnect()
            //txtDevice.text = ""
            alertDialog.dismiss()
        }
        btnCancel.clicks { alertDialog.dismiss() }
        alertDialog.show()
        return alertDialog
    }

    override fun onDown(p0: MotionEvent): Boolean {
        Log.d("TAG", "onDown: ")
        return false
    }

    override fun onShowPress(p0: MotionEvent) {
        Log.d("TAG", "onShowPress: ")
    }

    override fun onSingleTapUp(p0: MotionEvent): Boolean {
        handleEventButton(KeycodeLG.ENTER)
        return true
    }

    override fun onScroll(p0: MotionEvent?, p1: MotionEvent, p2: Float, p3: Float): Boolean {
        return false
    }

    override fun onLongPress(p0: MotionEvent) {
        Log.d("TAG", "onLongPress: ")
    }

    override fun onFling(p0: MotionEvent?, p1: MotionEvent, p2: Float, p3: Float): Boolean {
        val deltaX: Float = p1.x - p0!!.x
        val deltaY: Float = p1.y - p0.y

        if (abs(deltaY.toDouble()) > abs(deltaX.toDouble())) {
            if (deltaY < 0) {
                handleEventButton(KeycodeLG.UP)
            } else if (deltaY > 0) {
                handleEventButton(KeycodeLG.DOWN)
            }
        } else {
            if (deltaX > 0) {
                handleEventButton(KeycodeLG.RIGHT)
            } else if (deltaX < 0) {
                handleEventButton(KeycodeLG.LEFT)
            }
        }

        return true
    }
}