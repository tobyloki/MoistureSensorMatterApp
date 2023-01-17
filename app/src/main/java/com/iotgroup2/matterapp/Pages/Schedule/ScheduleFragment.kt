package com.iotgroup2.matterapp.Pages.Schedule

import androidx.lifecycle.ViewModelProvider
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.GridLayoutManager
import com.iotgroup2.matterapp.databinding.FragmentScheduleBinding

class ScheduleFragment : Fragment() {

    private lateinit var _binding: FragmentScheduleBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        val matterScheduleViewModel =
            ViewModelProvider(this).get(ScheduleViewModel::class.java)
        lifecycle.addObserver(matterScheduleViewModel)

        _binding = FragmentScheduleBinding.inflate(inflater, container, false)

        val root: View = _binding.root

        return root
    }
}