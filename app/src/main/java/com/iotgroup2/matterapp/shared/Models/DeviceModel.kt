package shared.Models

import android.util.Log
import com.google.gson.Gson
import org.json.JSONObject
import java.io.Serializable

class DeviceModel (
    data: JSONObject
) : Serializable {

    /************ Utilities ************/
    private val TAG = DeviceModel::class.java.simpleName

    /************ Properties ************/
    private lateinit var id: String
    private lateinit var name: String
    private var active: Boolean = false
    private lateinit var wifi: String
    private lateinit var deviceType: String

    /************ Initialize Class ************/
    init {
        try {
            // Fetch properties from the Json object to serialize and store as primitive types
            this.id = data.getString("id")
            this.name = data.getString("name")
            this.active = data.getBoolean("active")
            this.wifi = data.getString("wifi")
            this.deviceType = data.getString("deviceType")
        } catch (e: Exception) {
            Log.e(TAG, "Device Constructor Error: $e")
        }
    }

    /************ Getters ************/

    fun getDeviceId(): String {
        return this.id
    }

    fun getDeviceName(): String {
        return this.name
    }

    fun isActive(): Boolean {
        return this.active
    }

    fun getDeviceWiFi(): String {
        return this.wifi
    }

    fun getDeviceType(): String {
        return this.deviceType
    }

    /************ Setters ************/

    fun setDeviceName(renameTo: String) {
        this.name = renameTo
    }

    fun setActive(active: Boolean) {
        this.active = active
    }

    fun setDeviceWiFi(WiFiName: String) {
        this.wifi = WiFiName
    }

    fun setDeviceType(type: String) {
        this.deviceType = type
    }

    /************ Public Methods ************/
    fun deviceToJson(): String? {
        return Gson().toJson(this)
    }
}