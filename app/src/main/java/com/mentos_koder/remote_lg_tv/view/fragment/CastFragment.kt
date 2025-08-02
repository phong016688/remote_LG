package com.mentos_koder.remote_lg_tv.view.fragment

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.os.Bundle
import android.os.Vibrator
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager.widget.ViewPager
import com.mentos_koder.remote_lg_tv.R
import com.mentos_koder.remote_lg_tv.adapter.CastAdapter
import com.mentos_koder.remote_lg_tv.adapter.ViewPagerAdapter
import com.mentos_koder.remote_lg_tv.model.Cast
import com.mentos_koder.remote_lg_tv.util.PermissionUtils
import com.mentos_koder.remote_lg_tv.util.Singleton
import com.mentos_koder.remote_lg_tv.util.clicks
import com.mentos_koder.remote_lg_tv.util.restoreSwitchState
import com.mentos_koder.remote_lg_tv.util.showDialogDisconnect
import com.mentos_koder.remote_lg_tv.view.MainActivity

class CastFragment : Fragment()  {
    private lateinit var adapterCast : CastAdapter
    private lateinit var castRecyclerView : RecyclerView
    private lateinit var viewPagerContainer: LinearLayout
    private lateinit var linerCast: LinearLayout
    private lateinit var mActivity: Activity
    private var castButton: ImageView? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view: View = inflater.inflate(R.layout.fragment_cast, container, false)

        castRecyclerView = view.findViewById(R.id.recycler_cast)
        castButton = view.findViewById(R.id.img_cast)
        viewPagerContainer = view.findViewById(R.id.viewPagerContainer)
        linerCast = view.findViewById(R.id.liner_cast)
        castRecyclerView.setLayoutManager(GridLayoutManager(context, 2))
        adapterCast = CastAdapter()
        adapterCast.submitList(getDataCast())
        castRecyclerView.adapter = adapterCast

        adapterCast.clickItem = {
            if (PermissionUtils.checkAndRequestPermissions(mActivity)) {
                viewPagerContainer.visibility = View.VISIBLE
                linerCast.visibility = View.GONE
                setupViewPager(it.name)
            }
        }

        castButton?.clicks {
            performVibrateAction()
            if (isConnected) {
                context?.showDialogDisconnect {
                    val singleton: Singleton = Singleton.getInstance()
                    singleton.setConnected(false)
                    singleton.disconnect()
                }
            } else {
                (activity as? MainActivity)?.showFragmentDevice()
            }
        }

        return view
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is Activity) {
            mActivity = context
        }
    }

    private fun setupViewPager(typeCast: String) {
        val photoFragment = PhotoFragment()
        val videoFragment = VideoFragment()
        val audioFragment = AudioListFragment()
        when (typeCast) {
            "Photo" -> {
                requireActivity().supportFragmentManager.beginTransaction()
                    .replace(R.id.fragment_container, photoFragment)
                    .addToBackStack("photoFragment")
                    .commit()
            }
            "Video" -> {
                requireActivity().supportFragmentManager.beginTransaction()
                    .replace(R.id.fragment_container, videoFragment)
                    .addToBackStack("videoFragment")
                    .commit()
            }
            "Audio" -> {
                requireActivity().supportFragmentManager.beginTransaction()
                    .replace(R.id.fragment_container, audioFragment)
                    .addToBackStack("audioFragment")
                    .commit()
            }
        }
    }

    private val isConnected: Boolean
        get() {
            val singleton = Singleton.getInstance()
            return singleton.isConnectedCustom
        }

    private fun getDataCast(): List<Cast> {
        return listOf(
            Cast("Photo", R.drawable.ic_photo),
            Cast("Video", R.drawable.ic_video),
            Cast("Audio", R.drawable.ic_audio1)
        )
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
}