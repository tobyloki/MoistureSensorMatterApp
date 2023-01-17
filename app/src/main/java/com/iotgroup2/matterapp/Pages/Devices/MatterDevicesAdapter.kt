package com.iotgroup2.matterapp.Pages.Devices

import com.iotgroup2.matterapp.Pages.DevicesEditor.DevicesEditorActivity
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView
import com.iotgroup2.matterapp.R

class MatterDevicesAdapter(var devices : List<MatterDeviceViewModel.DevicesListItem>)
    : RecyclerView.Adapter<MatterDevicesAdapter.ViewHolder>() {

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val cardView: CardView
        val label : TextView
        val state : TextView
        val image : ImageView

        init {
            this.cardView = itemView.findViewById(R.id.cardView)
            this.label = itemView.findViewById(R.id.device_label)
            this.state = itemView.findViewById(R.id.device_state)
            this.image = itemView.findViewById(R.id.device_image)
        }
    }

    /** Adapter overriden functions **/
    override fun getItemCount(): Int {
        // Return the number of devices in the dynamic devices list
        return devices.size
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.devices_list_item, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val device = devices[position]

//        Timber.i("device is online: ${device.online}")

        // set background tint color for itemView
        holder.cardView.setCardBackgroundColor(if (device.online) {
            // set hex color
            android.graphics.Color.parseColor("#7BE0FF")
        } else {
            android.graphics.Color.parseColor("#B0B1B1")
        })
        holder.label.text = device.label
        holder.state.text = if (device.state) "ON" else "OFF"
        holder.image.setImageResource(device.image)

        // Setup onClick interaction to Devices Editor page
        holder.itemView.setOnClickListener {
            // launch DevicesEditor activity
            val intent = Intent(holder.itemView.context, DevicesEditorActivity::class.java)
            intent.putExtra("deviceId", device.id)
            holder.itemView.context.startActivity(intent)
        }
    }
}