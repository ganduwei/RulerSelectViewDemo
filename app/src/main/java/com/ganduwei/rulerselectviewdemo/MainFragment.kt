package com.ganduwei.rulerselectviewdemo

import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import kotlinx.android.synthetic.main.fragment_main.view.*

class MainFragment : Fragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_main, container, false);
        view.rulerSelectView.setRulerRange(-20f, 20f)
        view.rulerSelectView.setRulerSelectChangeListener { rulerSelectView, selectedPosition, selectedValue ->
            view.textView.text = selectedValue.toString()
        }
        return view
    }
}