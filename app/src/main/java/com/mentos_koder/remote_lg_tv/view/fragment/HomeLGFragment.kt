package com.mentos_koder.remote_lg_tv.view.fragment

import android.annotation.SuppressLint
import android.app.Activity
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
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Toast
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
import com.mentos_koder.remote_lg_tv.util.Constants
import com.mentos_koder.remote_lg_tv.util.showDialogDisconnect
import com.mentos_koder.remote_lg_tv.view.MainActivity


class HomeFragment : Fragment(), GestureDetector.OnGestureListener {

    private lateinit var dpadView: DPadView
    private lateinit var powerButton: FrameLayout
    private lateinit var castButton: FrameLayout
    private lateinit var volumeUpButton: ImageView
    private lateinit var volumeDownButton: ImageView
    private lateinit var channelListButton: LinearLayout
    private lateinit var homeButton: LinearLayout
    private lateinit var muteButton: LinearLayout
    private lateinit var infoButton: LinearLayout
    private lateinit var channelUpButton: ImageView
    private lateinit var channelDownButton: ImageView
    private lateinit var menuButton: LinearLayout
    private lateinit var hdmiButton: LinearLayout
    private lateinit var micButton: LinearLayout
    private lateinit var keyboardButton: LinearLayout
    private lateinit var rewindButton: LinearLayout
    private lateinit var playButton: LinearLayout
    private lateinit var stopButton: LinearLayout
    private lateinit var forwardButton: LinearLayout
    private lateinit var youtubeButton: FrameLayout
    private lateinit var netflixButton: FrameLayout
    private lateinit var primeVideoButton: FrameLayout
    private lateinit var controlTab: CardView
    private lateinit var numberTab: CardView
    private lateinit var touchpadTab: CardView
    private lateinit var controlTabIcon: ImageView
    private lateinit var numberTabIcon: ImageView
    private lateinit var touchpadTabIcon: ImageView
    private lateinit var backButton: LinearLayout
    private lateinit var exitButton: LinearLayout
    private lateinit var settingsButton: LinearLayout
    private lateinit var tvButton: LinearLayout
    private lateinit var buttonA: CardView
    private lateinit var buttonB: CardView
    private lateinit var buttonC: CardView
    private lateinit var buttonD: CardView
    private lateinit var buttonOne: LinearLayout
    private lateinit var buttonTwo: LinearLayout
    private lateinit var buttonThree: LinearLayout
    private lateinit var buttonFour: LinearLayout
    private lateinit var buttonFive: LinearLayout
    private lateinit var buttonSix: LinearLayout
    private lateinit var buttonSeven: LinearLayout
    private lateinit var buttonEight: LinearLayout
    private lateinit var buttonNine: LinearLayout
    private lateinit var buttonZero: LinearLayout
    private lateinit var controlLayout: ConstraintLayout
    private lateinit var numberLayout: ConstraintLayout
    private lateinit var touchpadLayout: ConstraintLayout
    private lateinit var gestureDetector: GestureDetector

    private var isAutoConnectEnabled = true
    private val PRIME_VIDEO_ID = "amazon"
    private val NETFLIX_ID = "netflix"
    private val YOUTUBE_ID = "youtube.leanback.v4"

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        val view: View = inflater.inflate(R.layout.fragment_home, container, false)
        initializeViews(view)
        setupEventListeners()
        setupEventListenersControl()
        if (getTypeDevice() == "samsung" && isAutoConnectEnabled) {
            autoConnect()
            isAutoConnectEnabled = false
        }
        gestureDetector = GestureDetector(context, this)
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
                singleton.autoConnectURI(name, ip, token)
                val sharedPref = requireActivity().getSharedPreferences(
                    Constants.PREFERENCE_NAME, Context.MODE_PRIVATE
                )
                sharedPref.edit {
                    putString(Constants.DEVICE_NAME, device.name)
                }
                singleton.setConnected(true)
            } else {
                singleton.setConnected(false)
            }
        } catch (e: Exception) {

        }
    }

    private fun initializeViews(view: View) {

        powerButton = view.findViewById(R.id.cv_power)
        castButton = view.findViewById(R.id.cv_cast)
        volumeUpButton = view.findViewById(R.id.img_volume_up)
        volumeDownButton = view.findViewById(R.id.img_volume_down)
        channelListButton = view.findViewById(R.id.linear_ch_list)
        homeButton = view.findViewById(R.id.linear_home)
        muteButton = view.findViewById(R.id.linear_mute)
        infoButton = view.findViewById(R.id.linear_info)
        channelUpButton = view.findViewById(R.id.img_ch_up)
        channelDownButton = view.findViewById(R.id.img_ch_down)
        menuButton = view.findViewById(R.id.linear_menu)
        hdmiButton = view.findViewById(R.id.linear_hdmi)
        micButton = view.findViewById(R.id.linear_mic)
        keyboardButton = view.findViewById(R.id.linear_keyboard)

        rewindButton = view.findViewById(R.id.linear_rewind)
        playButton = view.findViewById(R.id.linear_play)
        stopButton = view.findViewById(R.id.linear_stop)
        forwardButton = view.findViewById(R.id.linear_forward)

        youtubeButton = view.findViewById(R.id.cv_youtube)
        netflixButton = view.findViewById(R.id.cv_netflix)
        primeVideoButton = view.findViewById(R.id.cv_prime_video)

        controlTab = view.findViewById(R.id.cv_tab_control)
        numberTab = view.findViewById(R.id.cv_tab_number)
        touchpadTab = view.findViewById(R.id.cv_tab_touch_pad)
        controlTabIcon = view.findViewById(R.id.img_tab_control)
        numberTabIcon = view.findViewById(R.id.img_tab_number)
        touchpadTabIcon = view.findViewById(R.id.img_tab_touch_pad)

        backButton = view.findViewById(R.id.linear_back)
        exitButton = view.findViewById(R.id.linear_exit)
        settingsButton = view.findViewById(R.id.linear_setting)
        tvButton = view.findViewById(R.id.linear_tv)

        buttonA = view.findViewById(R.id.cv_a)
        buttonB = view.findViewById(R.id.cv_b)
        buttonC = view.findViewById(R.id.cv_c)
        buttonD = view.findViewById(R.id.cv_d)
        buttonOne = view.findViewById(R.id.linear_one)
        buttonTwo = view.findViewById(R.id.linear_two)
        buttonThree = view.findViewById(R.id.linear_three)
        buttonFour = view.findViewById(R.id.linear_four)
        buttonFive = view.findViewById(R.id.linear_five)
        buttonSix = view.findViewById(R.id.linear_six)
        buttonSeven = view.findViewById(R.id.linear_seven)
        buttonEight = view.findViewById(R.id.linear_eight)
        buttonNine = view.findViewById(R.id.linear_nine)
        buttonZero = view.findViewById(R.id.linear_zero)

        controlLayout = view.findViewById(R.id.constraint_control)
        numberLayout = view.findViewById(R.id.constraint_number)
        touchpadLayout = view.findViewById(R.id.constraint_touch_pad)

        dpadView = view.findViewById(R.id.dPadView_remote_control)
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setupEventListeners() {
        touchpadLayout.setOnTouchListener { v: View?, event: MotionEvent? ->
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

        setViewClickListener(powerButton, KeycodeLG.POWER)
        setViewClickListener(menuButton, KeycodeLG.MENU)
        setViewClickListener(homeButton, KeycodeLG.HOME)
        setViewClickListener(backButton, KeycodeLG.BACK)
        setViewClickListener(infoButton, KeycodeLG.INFO)
        setViewClickListener(hdmiButton, KeycodeLG.HDMI)
        setViewClickListener(volumeUpButton, KeycodeLG.VOLUMEUP)
        setViewClickListener(volumeDownButton, KeycodeLG.VOLUMEDOWN)
        setViewClickListener(exitButton, KeycodeLG.EXIT)
        setViewClickListener(muteButton, KeycodeLG.MUTE)
        setViewClickListener(channelUpButton, KeycodeLG.CHANNELUP)
        setViewClickListener(channelDownButton, KeycodeLG.CHANNELDOWN)
        setViewClickListener(channelListButton, KeycodeLG.LIST)
        setViewClickListener(rewindButton, KeycodeLG.LEFT)
        setViewClickListener(forwardButton, KeycodeLG.RIGHT)
        setViewClickListener(playButton, KeycodeLG.PLAY)
        setViewClickListener(stopButton, KeycodeLG.PAUSE)
        setViewClickListener(settingsButton, KeycodeLG.SETTING)
        setViewClickListener(buttonA, KeycodeLG.RED)
        setViewClickListener(buttonB, KeycodeLG.GREEN)
        setViewClickListener(buttonC, KeycodeLG.YELLOW)
        setViewClickListener(buttonD, KeycodeLG.BLUE)
        setViewClickListener(buttonOne, KeycodeLG.ONE)
        setViewClickListener(buttonTwo, KeycodeLG.TWO)
        setViewClickListener(buttonThree, KeycodeLG.THREE)
        setViewClickListener(buttonFour, KeycodeLG.FOUR)
        setViewClickListener(buttonFive, KeycodeLG.FIVE)
        setViewClickListener(buttonSix, KeycodeLG.SIX)
        setViewClickListener(buttonSeven, KeycodeLG.SEVEN)
        setViewClickListener(buttonEight, KeycodeLG.EIGHT)
        setViewClickListener(buttonNine, KeycodeLG.NINE)
        setViewClickListener(buttonZero, KeycodeLG.ZERO)
        micButton.clicks {
            performVibrateAction()
            if (isConnected) {
                handleMicro()
            } else {
                showFragmentDevice()
            }
        }

        youtubeButton.clicks {
            performVibrateAction()
            if (isConnected) {
                handleApp(YOUTUBE_ID)
            } else {
                showFragmentDevice()
            }
        }
        primeVideoButton.clicks {
            performVibrateAction()
            if (isConnected) {
                handleApp(PRIME_VIDEO_ID)
            } else {
                showFragmentDevice()
            }
        }

        netflixButton.clicks {
            performVibrateAction()
            if (isConnected) {
                handleApp(NETFLIX_ID)
            } else {
                showFragmentDevice()
            }
        }

        castButton.clicks {
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

        keyboardButton.clicks {
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
                getString(R.string.no_internet_connection_please_try_again_later),
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
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            val speechResult = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            if (!speechResult.isNullOrEmpty()) {
                val singleton = Singleton.getInstance()
                singleton.sendText(speechResult[0])
            }
        }
    }

    private fun setupEventListenersControl() {
        controlTab.clicks {
            controlLayout.visibility = View.VISIBLE
            numberLayout.visibility = View.GONE
            touchpadLayout.visibility = View.GONE

            controlTab.setCardBackgroundColor(
                ContextCompat.getColor(
                    requireContext(), R.color.white
                )
            )
            controlTabIcon.setColorFilter(ContextCompat.getColor(requireContext(), R.color.pink))

            numberTab.setCardBackgroundColor(
                ContextCompat.getColor(
                    requireContext(), R.color.bgr_btn_lg
                )
            )
            numberTabIcon.setColorFilter(ContextCompat.getColor(requireContext(), R.color.white))

            touchpadTab.setCardBackgroundColor(
                ContextCompat.getColor(
                    requireContext(), R.color.bgr_btn_lg
                )
            )
            touchpadTabIcon.setColorFilter(ContextCompat.getColor(requireContext(), R.color.white))
        }
        numberTab.clicks {
            controlLayout.visibility = View.GONE
            numberLayout.visibility = View.VISIBLE
            touchpadLayout.visibility = View.GONE

            controlTab.setCardBackgroundColor(
                ContextCompat.getColor(
                    requireContext(), R.color.bgr_btn_lg
                )
            )
            controlTabIcon.setColorFilter(ContextCompat.getColor(requireContext(), R.color.white))

            numberTab.setCardBackgroundColor(
                ContextCompat.getColor(
                    requireContext(), R.color.white
                )
            )
            numberTabIcon.setColorFilter(ContextCompat.getColor(requireContext(), R.color.pink))

            touchpadTab.setCardBackgroundColor(
                ContextCompat.getColor(
                    requireContext(), R.color.bgr_btn_lg
                )
            )
            touchpadTabIcon.setColorFilter(ContextCompat.getColor(requireContext(), R.color.white))
        }
        touchpadTab.clicks {
            controlLayout.visibility = View.GONE
            numberLayout.visibility = View.GONE
            touchpadLayout.visibility = View.VISIBLE

            controlTab.setCardBackgroundColor(
                ContextCompat.getColor(
                    requireContext(), R.color.bgr_btn_lg
                )
            )
            controlTabIcon.setColorFilter(ContextCompat.getColor(requireContext(), R.color.white))

            numberTab.setCardBackgroundColor(
                ContextCompat.getColor(
                    requireContext(), R.color.bgr_btn_lg
                )
            )
            numberTabIcon.setColorFilter(ContextCompat.getColor(requireContext(), R.color.white))

            touchpadTab.setCardBackgroundColor(
                ContextCompat.getColor(
                    requireContext(), R.color.white
                )
            )
            touchpadTabIcon.setColorFilter(ContextCompat.getColor(requireContext(), R.color.pink))
        }
    }

    private fun showFragmentDevice() {
        (activity as? MainActivity)?.showFragmentDevice()
    }

    override fun onDown(p0: MotionEvent): Boolean {
        return false
    }

    override fun onShowPress(p0: MotionEvent) {
    }

    override fun onSingleTapUp(p0: MotionEvent): Boolean {
        handleEventButton(KeycodeLG.ENTER)
        return true
    }

    override fun onScroll(p0: MotionEvent?, p1: MotionEvent, p2: Float, p3: Float): Boolean {
        return false
    }

    override fun onLongPress(p0: MotionEvent) {
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