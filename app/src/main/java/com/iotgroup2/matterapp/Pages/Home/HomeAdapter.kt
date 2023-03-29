package com.iotgroup2.matterapp.Pages.Home

import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat.getColor
import androidx.recyclerview.widget.RecyclerView
import com.iotgroup2.matterapp.Device
import com.iotgroup2.matterapp.Pages.Home.Device.Actuator.ActuatorActivity
import com.iotgroup2.matterapp.Pages.Home.Device.Sensor.SensorActivity
import com.iotgroup2.matterapp.R

class HomeAdapter(var context: Context, var devices : List<HomeViewModel.DevicesListItem>)
    : RecyclerView.Adapter<HomeAdapter.ViewHolder>() {

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val image : ImageView
        val backgroundContainer: ConstraintLayout
        val name : TextView
        val onlineIcon: TextView
        val online : TextView

        init {
            this.image = itemView.findViewById(R.id.device_image)
            this.backgroundContainer = itemView.findViewById(R.id.backgroundContainer)
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
        val view = LayoutInflater.from(parent.context).inflate(R.layout.home_list_item, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val device = devices[position]

        if(device.type == Device.DeviceType.TYPE_HUMIDITY_SENSOR_VALUE) {
            holder.image.setImageResource(R.drawable.soil_moisture)
            holder.backgroundContainer.setBackgroundResource(R.drawable.sensor_device_card)
            // set scale of image to 0.8
            holder.image.scaleX = 1f
            holder.image.scaleY = 1f
        } else {
            holder.image.setImageResource(R.drawable.sprinkler)
            holder.backgroundContainer.setBackgroundResource(R.drawable.actuator_device_card)
            // set scale of image to 0.6
            holder.image.scaleX = 0.6f
            holder.image.scaleY = 0.6f
        }

        holder.name.text = device.name
        holder.onlineIcon.setTextColor(if (device.online) getColor(context, R.color.online) else getColor(context, R.color.offline))
        holder.online.text = if (device.online) "Online" else "Offline"

        // Setup onClick interaction to Devices Editor page
        holder.itemView.setOnClickListener {
            if (device.type == Device.DeviceType.TYPE_HUMIDITY_SENSOR_VALUE) {
                val intent = Intent(holder.itemView.context, SensorActivity::class.java)
                intent.putExtra("deviceId", device.id)
                intent.putExtra("deviceType", device.type)
                intent.putExtra("deviceName", device.name)
                intent.putExtra("deviceOnline", device.online)
                holder.itemView.context.startActivity(intent)
            } else {
                val intent = Intent(holder.itemView.context, ActuatorActivity::class.java)
                intent.putExtra("deviceId", device.id)
                intent.putExtra("deviceType", device.type)
                intent.putExtra("deviceName", device.name)
                intent.putExtra("deviceOnline", device.online)
                holder.itemView.context.startActivity(intent)
            }
        }
    }
}