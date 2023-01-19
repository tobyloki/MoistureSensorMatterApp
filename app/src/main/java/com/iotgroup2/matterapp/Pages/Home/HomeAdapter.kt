package com.iotgroup2.matterapp.Pages.Home

import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat.getColor
import androidx.recyclerview.widget.RecyclerView
import com.iotgroup2.matterapp.Pages.Home.Device.DeviceActivity
import com.iotgroup2.matterapp.R

class HomeAdapter(var context: Context, var devices : List<HomeViewModel.DevicesListItem>)
    : RecyclerView.Adapter<HomeAdapter.ViewHolder>() {

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val name : TextView
        val onlineIcon: TextView
        val online : TextView

        init {
            this.name = itemView.findViewById(R.id.device_label)
            this.onlineIcon = itemView.findViewById(R.id.onlineIcon)
            this.online = itemView.findViewById(R.id.onlineTxt)
        }
    }

    /** Adapter overriden functions **/
    override fun getItemCount(): Int {
        // Return the number of devices in the dynamic devices list
        return devices.size
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.device_list_item, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val device = devices[position]

        holder.name.text = device.label
        holder.onlineIcon.setTextColor(if (device.online) getColor(context, R.color.online) else getColor(context, R.color.offline))
        holder.online.text = if (device.online) "Online" else "Offline"

        // Setup onClick interaction to Devices Editor page
        holder.itemView.setOnClickListener {
            // launch DevicesEditor activity
            val intent = Intent(holder.itemView.context, DeviceActivity::class.java)
            intent.putExtra("deviceId", device.id)
            holder.itemView.context.startActivity(intent)
        }
    }
}