package com.mentos_koder.remote_lg_tv.view.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.mentos_koder.remote_lg_tv.R


class BottomHDMIFragment : BottomSheetDialogFragment() {
    var btn_d_bottom_AV: TextView? = null
    var btn_d_bottom_HDMI4: TextView? = null
    var btn_d_bottom_HDMI3: TextView? = null
    var btn_c_bottom_HDMI2: TextView? = null
    var btn_b_bottom_HDMI1: TextView? = null
    var btn_a_bottom_tuner: TextView? = null
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view: View = inflater.inflate(R.layout.fragment_bottom_h_d_m_i, container, false)
        setupUI(view)
        return view
    }



    private fun setupUI(view: View) {
        btn_d_bottom_AV = view.findViewById(R.id.btn_d_bottom_AV)
        btn_d_bottom_HDMI4 = view.findViewById(R.id.btn_d_bottom_HDMI4)
        btn_d_bottom_HDMI3 = view.findViewById(R.id.btn_d_bottom_HDMI3)
        btn_c_bottom_HDMI2 = view.findViewById(R.id.btn_c_bottom_HDMI2)
        btn_b_bottom_HDMI1 = view.findViewById(R.id.btn_b_bottom_HDMI1)
        btn_a_bottom_tuner = view.findViewById(R.id.btn_a_bottom_tuner)
    }

}