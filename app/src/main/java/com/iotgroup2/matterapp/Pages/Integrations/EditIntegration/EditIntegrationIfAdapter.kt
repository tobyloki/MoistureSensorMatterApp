package com.iotgroup2.matterapp.Pages.Integrations.EditIntegration

import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.iotgroup2.matterapp.Pages.Integrations.EditIntegration.EditIntegrationSensor.EditIntegrationSensorActivity
import com.iotgroup2.matterapp.R

class EditIntegrationIfAdapter(var context: Context, var ifList : List<EditIntegrationViewModel.IfListItem>)
    : RecyclerView.Adapter<EditIntegrationIfAdapter.ViewHolder>() {

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val name : TextView

        init {
            this.name = itemView.findViewById(R.id.nameTxt)
        }
    }

    /** Adapter overriden functions **/
    override fun getItemCount(): Int {
        // Return the number of devices in the dynamic devices list
        return ifList.size
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.edit_integration_if_list_item, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val ifItem = ifList[position]

        holder.name.text = ifItem.label

        // Setup onClick interaction to Devices Editor page
        holder.itemView.setOnClickListener {
            val intent = Intent(holder.itemView.context, EditIntegrationSensorActivity::class.java)
            intent.putExtra("id", ifItem.id)
            holder.itemView.context.startActivity(intent)
        }
    }
}