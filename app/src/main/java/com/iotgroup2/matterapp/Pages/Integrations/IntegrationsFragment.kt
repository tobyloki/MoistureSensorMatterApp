package com.iotgroup2.matterapp.Pages.Integrations

import android.content.Intent
import androidx.lifecycle.ViewModelProvider
import android.os.Bundle
import android.view.*
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.GridLayoutManager
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.iotgroup2.matterapp.Pages.Integrations.EditIntegration.EditIntegrationActivity
import com.iotgroup2.matterapp.Pages.Units.UnitsActivity
import com.iotgroup2.matterapp.R
import com.iotgroup2.matterapp.shared.MatterViewModel.MatterActivityViewModel
import com.iotgroup2.matterapp.databinding.FragmentIntegrationsBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class IntegrationsFragment : Fragment() {

    private lateinit var _binding: FragmentIntegrationsBinding

    private val matterViewModel: MatterActivityViewModel by viewModels()

    private lateinit var addIntegrationBtn: FloatingActionButton

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        setHasOptionsMenu(true)

        val viewModel = ViewModelProvider(this).get(IntegrationsViewModel::class.java)
        lifecycle.addObserver(viewModel)

        _binding = FragmentIntegrationsBinding.inflate(inflater, container, false)

        val root: View = _binding.root

        addIntegrationBtn = _binding.addIntegrationBtn

        addIntegrationBtn.setOnClickListener {
            val intent = Intent(activity, EditIntegrationActivity::class.java)
            startActivity(intent)
        }

        val list = _binding.recyclerView
        list.layoutManager = GridLayoutManager(context, 1)

        viewModel.integrations.observe(viewLifecycleOwner) {
            list.adapter = IntegrationsAdapter(requireContext(), it)
        }

        return root
    }

    @Deprecated("Deprecated in Java")
    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.home_integrations_menu, menu)
        super.onCreateOptionsMenu(menu, inflater)
    }

    @Deprecated("Deprecated in Java")
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle item selection
        return when (item.itemId) {
            R.id.changeUnits -> {
                val intent = Intent(requireActivity(), UnitsActivity::class.java)
                startActivity(intent)
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}