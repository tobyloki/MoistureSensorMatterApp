package com.iotgroup2.matterapp.Pages.Integrations.EditIntegration.SelectDeviceToAdd

import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.iotgroup2.matterapp.Device
import com.iotgroup2.matterapp.Pages.Integrations.EditIntegration.EditIntegrationActuator.EditIntegrationActuatorActivity
import com.iotgroup2.matterapp.Pages.Integrations.EditIntegration.EditIntegrationSensor.EditIntegrationSensorActivity
import com.iotgroup2.matterapp.R

class SelectDeviceToAddAdapter(var context: Context, var list : List<SelectDeviceToAddViewModel.SelectDeviceItem>, var deviceType : Int)
    : RecyclerView.Adapter<SelectDeviceToAddAdapter.ViewHolder>() {

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val image: ImageView
        val name : TextView

        init {
            this.image = itemView.findViewById(R.id.imageView)
            this.name = itemView.findViewById(R.id.nameTxt)
        }
    }

    /** Adapter overriden functions **/
    override fun getItemCount(): Int {
        // Return the number of devices in the dynamic devices list
        return list.size
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.select_device_list_item, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val ifItem = list[position]

        if (deviceType == Device.DeviceType.TYPE_LIGHT_VALUE) {
            holder.image.setImageResource(R.drawable.soil_moisture)
        } else {
            holder.image.setImageResource(R.drawable.sprinkler)
        }

        holder.name.text = ifItem.label

        // Setup onClick interaction to Devices Editor page
        holder.itemView.setOnClickListener {
            val intent : Intent = if (deviceType == Device.DeviceType.TYPE_LIGHT_VALUE) {
                Intent(context, EditIntegrationSensorActivity::class.java)
            } else {
                Intent(context, EditIntegrationActuatorActivity::class.java)
            }
            intent.putExtra("id", ifItem.id)
            holder.itemView.context.startActivity(intent)
        }
    }
}