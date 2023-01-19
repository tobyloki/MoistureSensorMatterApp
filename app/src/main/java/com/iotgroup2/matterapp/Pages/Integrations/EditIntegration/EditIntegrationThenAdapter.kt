package com.iotgroup2.matterapp.Pages.Integrations.EditIntegration

import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.iotgroup2.matterapp.Pages.Integrations.EditIntegration.EditIntegrationActuator.EditIntegrationActuatorActivity
import com.iotgroup2.matterapp.Pages.Integrations.EditIntegration.EditIntegrationSensor.EditIntegrationSensorActivity
import com.iotgroup2.matterapp.R

class EditIntegrationThenAdapter(var context: Context, var thenList : List<EditIntegrationViewModel.ThenListItem>)
    : RecyclerView.Adapter<EditIntegrationThenAdapter.ViewHolder>() {

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val name : TextView

        init {
            this.name = itemView.findViewById(R.id.nameTxt)
        }
    }

    /** Adapter overriden functions **/
    override fun getItemCount(): Int {
        // Return the number of devices in the dynamic devices list
        return thenList.size
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.edit_integration_then_list_item, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val thenItem = thenList[position]

        holder.name.text = thenItem.label

        // Setup onClick interaction to Devices Editor page
        holder.itemView.setOnClickListener {
            val intent = Intent(holder.itemView.context, EditIntegrationActuatorActivity::class.java)
            intent.putExtra("id", thenItem.id)
            holder.itemView.context.startActivity(intent)
        }
    }
}