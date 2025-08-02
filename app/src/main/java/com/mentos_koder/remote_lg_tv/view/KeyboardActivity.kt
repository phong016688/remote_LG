package com.mentos_koder.remote_lg_tv.view

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.os.Vibrator
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.util.Log
import android.view.GestureDetector
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.tabs.TabLayout
import com.mentos_koder.remote_lg_tv.R
import com.mentos_koder.remote_lg_tv.util.KeycodeLG
import com.mentos_koder.remote_lg_tv.util.Singleton
import com.mentos_koder.remote_lg_tv.util.restoreSwitchState
import com.mentos_koder.remote_lg_tv.util.showDialogDisconnect
import com.mentos_koder.remote_lg_tv.view.fragment.DeviceFragment
import java.util.Locale
import kotlin.math.abs


class KeyboardActivity : AppCompatActivity(), GestureDetector.OnGestureListener {
    private lateinit var deviceNameEditText: EditText
    private lateinit var backButton: ImageView
    private lateinit var castButton: ImageView
    private lateinit var backLayout: LinearLayout
    private lateinit var homeLayout: LinearLayout
    private lateinit var powerLayout: LinearLayout
    private lateinit var tabLayout: TabLayout
    private lateinit var gestureDetector: GestureDetector

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_keyboard)
        initializeViews()
        setupEventListeners()
        gestureDetector = GestureDetector(this, this)
        deviceNameEditText.inputType =
            InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS

    }

    private fun checkRing(): Boolean {
        return let { restoreSwitchState(it) }
    }

    private fun performVibrateAction() {
        val vibrator = this.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        if (checkRing()) {
            vibrator.vibrate(100)
        }
    }

    override fun onResume() {
        super.onResume()
        deviceNameEditText.requestFocus()
        val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        imm.showSoftInput(deviceNameEditText, InputMethodManager.SHOW_IMPLICIT)
    }

    private fun initializeViews() {
        backButton = findViewById(R.id.img_back)
        castButton = findViewById(R.id.img_cast)
        backLayout = findViewById(R.id.liner_back)
        homeLayout = findViewById(R.id.liner_home)
        powerLayout = findViewById(R.id.liner_power)
        tabLayout = findViewById(R.id.tabLayout)
        deviceNameEditText = findViewById(R.id.edt_handel_device)
    }

    override fun onDown(motionEvent: MotionEvent): Boolean {
        return false
    }

    override fun onShowPress(motionEvent: MotionEvent) {}
    override fun onSingleTapUp(motionEvent: MotionEvent): Boolean {
        handleType(KeycodeLG.ENTER)
        return false
    }

    override fun onScroll(
        motionEvent: MotionEvent?, motionEvent1: MotionEvent, v: Float, v1: Float
    ): Boolean {
        return false
    }

    override fun onLongPress(motionEvent: MotionEvent) {}
    override fun onFling(e1: MotionEvent?, e2: MotionEvent, v: Float, v1: Float): Boolean {
        val deltaX = e2.x - e1!!.x
        val deltaY = e2.y - e1.y
        if (abs(deltaY.toDouble()) > abs(deltaX.toDouble())) {
            if (deltaY < 0) {
                performVibrateAction()
                handleType(KeycodeLG.UP)
            } else if (deltaY > 0) {
                performVibrateAction()
                handleType(KeycodeLG.DOWN)
            }
        } else {
            if (deltaX > 0) {
                performVibrateAction()
                handleType(KeycodeLG.RIGHT)
            } else if (deltaX < 0) {
                performVibrateAction()
                handleType(KeycodeLG.LEFT)
            }
        }
        return true
    }

    private val isConnected: Boolean
        get() {
            val singleton = Singleton.getInstance()
            return singleton.isConnectedCustom
        }

    private fun handleEvent(key: String, action: (String) -> Unit) {
        if (isConnected) {
            Singleton.getInstance()
            action(key)
        } else {
            showFragmentDevice()
        }
    }

    private fun handleEventButtonLG(key: KeycodeLG) {
        handleEvent(key.value) { eventName ->
            Singleton.getInstance().getService(eventName)
        }
    }

    private fun handleType(keyLG: KeycodeLG) {
        val type = Singleton.getInstance().getTypeDevice().lowercase(Locale.ROOT)
        when (type) {
            "lg" -> handleEventButtonLG(keyLG)
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    fun setupEventListeners() {
        tabLayout.setOnTouchListener { v: View?, event: MotionEvent? ->
            performVibrateAction()
            gestureDetector.onTouchEvent(
                event!!
            )
            true
        }
        backLayout.setOnClickListener {
            performVibrateAction()
            handleType(KeycodeLG.BACK)
        }
        homeLayout.setOnClickListener {
            performVibrateAction()
            handleType(KeycodeLG.HOME)
        }
        powerLayout.setOnClickListener {
            performVibrateAction()
            handleType(KeycodeLG.POWER)
        }
        deviceNameEditText.addTextChangedListener(object : TextWatcher {
            var previousText = ""
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {
                previousText = s.toString()
            }

            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
                try {
                    val key = deviceNameEditText.text.toString()
                    performVibrateAction()
                    if (key.isNotEmpty()) {
                        handleTextInput(key)
                    }
                } catch (_: Exception) {
                }
            }

            override fun afterTextChanged(s: Editable) {
                deviceNameEditText.setOnKeyListener { _: View?, keyCode: Int, event: KeyEvent ->
                    if (keyCode == KeyEvent.KEYCODE_DEL && event.action == KeyEvent.ACTION_DOWN) {
                        try {
                            handleTextDelete()
                        } catch (_: java.lang.Exception) {
                        }
                    }
                    false

                }

            }
        })
        castButton.setOnClickListener {
            showDialogDisconnect {
                val singleton: Singleton = Singleton.getInstance()
                singleton.setConnected(false)
                singleton.disconnect()
            }
        }
        backButton.setOnClickListener { finish() }
    }

    fun handleTextInput(key: String) {
        val type = Singleton.getInstance().getTypeDevice().lowercase(Locale.ROOT)
        when (type) {
            "lg" -> {
                textInput(key)
                deviceNameEditText.setText("")
            }
        }
    }

    fun handleTextDelete() {
        val type = Singleton.getInstance().getTypeDevice().lowercase(Locale.ROOT)
        when (type) {
            "lg" -> {
                Log.d("sendDelete", "handleTextDelete: ")
                Singleton.getInstance().deleteText()
            }
        }
    }

    private fun textInput(text: String) {
        val singleton = Singleton.getInstance()
        singleton.sendText(text)
    }

    private fun showFragmentDevice() {
        val deviceFrag = DeviceFragment()
        this.supportFragmentManager.beginTransaction().setCustomAnimations(
            R.anim.slide_in_right,  // enter
            R.anim.slide_out_left // exit
        ).replace(R.id.fragment_container, deviceFrag, "findThisFragment")
            .addToBackStack("findThisFragment").commit()
    }
}