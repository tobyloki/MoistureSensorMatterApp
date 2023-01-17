package com.iotgroup2.matterapp.Pages.Groups

import androidx.lifecycle.ViewModelProvider
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import com.iotgroup2.matterapp.Pages.MatterViewModel.MatterActivityViewModel
import com.iotgroup2.matterapp.databinding.FragmentGroupsBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class GroupsFragment : Fragment() {

    private lateinit var _binding: FragmentGroupsBinding

    private val viewModel: MatterActivityViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        val matterGroupsViewModel =
            ViewModelProvider(this).get(GroupsViewModel::class.java)
        lifecycle.addObserver(matterGroupsViewModel)

        _binding = FragmentGroupsBinding.inflate(inflater, container, false)

        val root: View = _binding.root

        return root
    }
}