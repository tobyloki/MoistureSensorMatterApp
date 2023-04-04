package com.iotgroup2.matterapp.Pages.Integrations.EditIntegration

import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.iotgroup2.matterapp.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import okhttp3.MediaType
import okhttp3.RequestBody
import org.json.JSONObject
import shared.Utility.HTTPGraphQL
import timber.log.Timber

class EditIntegrationIfAdapter(var activity: Activity, var ifList : List<EditIntegrationViewModel.IfListItem>)
    : RecyclerView.Adapter<EditIntegrationIfAdapter.ViewHolder>() {

    private val viewModelJob = Job()
    private var coroutineScope = CoroutineScope(viewModelJob + Dispatchers.Main)

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val name : TextView
        val deleteBtn: ImageButton

        init {
            this.name = itemView.findViewById(R.id.nameTxt)
            this.deleteBtn = itemView.findViewById(R.id.deleteBtn)
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

        holder.deleteBtn.setOnClickListener {
            // ask for confirmation
            AlertDialog.Builder(activity, R.style.MyAlertDialogStyle)
                .setTitle("Delete ${ifItem.label}")
                .setMessage("Are you sure you want to delete this sensor?")
                .setPositiveButton(activity.getString(R.string.confirm)) { dialog, which ->
                    deleteItem(ifItem.id, ifItem._version)
                }
                .setNegativeButton(android.R.string.no) { dialog, which ->
                    // do nothing
                }
                .setIcon(R.drawable.baseline_warning_24)
                .show()
        }

        // Setup onClick interaction to Devices Editor page
        holder.itemView.setOnClickListener {

        }
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun deleteItem(id: String, _version: Int) {
        coroutineScope.launch {
            try {
                Timber.i("Sending http request to graphql endpoint...")

                val json = JSONObject()
                json.put("query", "mutation MyMutation {\n" +
                        "  updateSensor(input: {id: \"$id\", integrationID: null, _version: $_version}) {\n" +
                        "    id\n" +
                        "  }\n" +
                        "}")
                val body: RequestBody = RequestBody.create(MediaType.parse("application/json"), json.toString())
                val httpResponse = HTTPGraphQL.retrofitService.query(body).await()
                Timber.i("data: $httpResponse")

                // remove item from list
                val newList = ifList.toMutableList()
                newList.removeAt(ifList.indexOfFirst { it.id == id })
                ifList = newList.toList()
                notifyDataSetChanged()
            } catch (e: Exception) {
                Timber.e(e)
            }
        }
    }
}