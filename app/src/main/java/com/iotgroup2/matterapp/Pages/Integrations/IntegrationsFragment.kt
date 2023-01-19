package com.iotgroup2.matterapp.Pages.Integrations

import androidx.lifecycle.ViewModelProvider
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import com.iotgroup2.matterapp.shared.MatterViewModel.MatterActivityViewModel
import com.iotgroup2.matterapp.databinding.FragmentIntegrationsBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class IntegrationsFragment : Fragment() {

    private lateinit var _binding: FragmentIntegrationsBinding

    private val viewModel: MatterActivityViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        val matterGroupsViewModel =
            ViewModelProvider(this).get(IntegrationsViewModel::class.java)
        lifecycle.addObserver(matterGroupsViewModel)

        _binding = FragmentIntegrationsBinding.inflate(inflater, container, false)

        val root: View = _binding.root

        return root
    }
}