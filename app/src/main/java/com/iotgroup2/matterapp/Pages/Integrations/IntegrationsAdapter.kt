package com.iotgroup2.matterapp.Pages.Integrations

import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.iotgroup2.matterapp.Pages.Integrations.EditIntegration.EditIntegrationActivity
import com.iotgroup2.matterapp.R

class IntegrationsAdapter(var context: Context, var integrations : List<IntegrationsViewModel.IntegrationListItem>)
    : RecyclerView.Adapter<IntegrationsAdapter.ViewHolder>() {

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val name : TextView

        init {
            this.name = itemView.findViewById(R.id.nameTxt)
        }
    }

    /** Adapter overriden functions **/
    override fun getItemCount(): Int {
        // Return the number of devices in the dynamic devices list
        return integrations.size
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.integration_list_item, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val integration = integrations[position]

        holder.name.text = integration.label

        // Setup onClick interaction to Devices Editor page
        holder.itemView.setOnClickListener {
            val intent = Intent(holder.itemView.context, EditIntegrationActivity::class.java)
            intent.putExtra("integrationId", integration.id)
            holder.itemView.context.startActivity(intent)
        }
    }
}