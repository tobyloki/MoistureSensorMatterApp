package com.iotgroup2.matterapp.Pages.Integrations.EditIntegration.SelectDeviceToAdd

import android.app.Activity
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.iotgroup2.matterapp.Device
import com.iotgroup2.matterapp.Pages.Integrations.EditIntegration.EditIntegrationActuator.EditIntegrationActuatorActivity
import com.iotgroup2.matterapp.Pages.Integrations.EditIntegration.EditIntegrationActuator.EditIntegrationViewModel
import com.iotgroup2.matterapp.Pages.Integrations.EditIntegration.EditIntegrationActuator.Method
import com.iotgroup2.matterapp.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import okhttp3.MediaType
import okhttp3.RequestBody
import org.json.JSONObject
import shared.Utility.HTTP
import timber.log.Timber

class SelectDeviceToAddAdapter(var activity: Activity, var list : List<SelectDeviceToAddViewModel.SelectDeviceItem>, var deviceType : Int, var integrationId : String)
    : RecyclerView.Adapter<SelectDeviceToAddAdapter.ViewHolder>() {
    private val viewModelJob = Job()
    private var coroutineScope = CoroutineScope(viewModelJob + Dispatchers.Main)

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
            if (deviceType == Device.DeviceType.TYPE_LIGHT_VALUE) {
                addSensor(ifItem.id, ifItem._version)
            } else {
                val intent = Intent(activity, EditIntegrationActuatorActivity::class.java)
                intent.putExtra("method", Method.ADD)
                intent.putExtra("integrationId", integrationId)
                intent.putExtra("deviceId", ifItem.id)
                intent.putExtra("name", ifItem.label)
                intent.putExtra("_version", ifItem._version)
                holder.itemView.context.startActivity(intent)
                activity.finish()
            }
        }
    }

    private fun addSensor(deviceId: String, _version: Int) {
        coroutineScope.launch {
            try {
                Timber.i("Sending http request to graphql endpoint...")

                val json = JSONObject()
                json.put("query", "mutation MyMutation {\n" +
                        "  updateSensor(input: {id: \"$deviceId\", integrationID: \"$integrationId\", _version: $_version}) {\n" +
                        "    id\n" +
                        "  }\n" +
                        "}")
                val body: RequestBody = RequestBody.create(MediaType.parse("application/json"), json.toString())
                val httpResponse = HTTP.retrofitService.query(body).await()
                Timber.i("data: $httpResponse")

                activity.finish()
            } catch (e: Exception) {
                Timber.e(e)
            }
        }
    }
}