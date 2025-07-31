package com.mentos_koder.remote_lg_tv.view.fragment

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import com.mentos_koder.remote_lg_tv.R

class GoToSettingFragment(private val name: String) : Fragment(){

    private lateinit var fragment: Fragment

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view: View = inflater.inflate(R.layout.fragment_settings_guide, container, false)

        val tvGoToSetting = view.findViewById<TextView>(R.id.tv_go_to_settings)
        val tvStep3 = view.findViewById<TextView>(R.id.tv_step_3)
        val tvStep4 = view.findViewById<TextView>(R.id.tv_step_4)
        val tvStepMicro = view.findViewById<TextView>(R.id.tv_step_micro)
        val backButton = view.findViewById<ImageView>(R.id.img_back)

        if(name == "micro"){
            tvStep3.visibility = View.GONE
            tvStep4.visibility = View.GONE
            tvStepMicro.visibility = View.VISIBLE
            fragment = homeFragment()
        }else if(name == "photo") {
            fragment = CastFragment()
        }

        tvGoToSetting.setOnClickListener{
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
            val uri: Uri = Uri.fromParts("package", requireActivity().packageName, null)
            intent.data = uri
            startActivity(intent)
        }

        backButton.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .setCustomAnimations(
                    R.anim.slide_in_right,
                    R.anim.slide_out_left
                )
                .replace(R.id.fragment_container, fragment, "findThisFragment")
                .commit()
        }

        return view
    }

}