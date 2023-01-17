package shared.Utility

import com.jakewharton.retrofit2.adapter.kotlin.coroutines.CoroutineCallAdapterFactory
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Deferred
import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.converter.scalars.ScalarsConverterFactory
import retrofit2.http.*

private const val BASE_URL = "https://hu5nnerv6b.execute-api.us-west-2.amazonaws.com/Prod/"

private val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()

private val retrofit =
    Retrofit.Builder()
        .addConverterFactory(ScalarsConverterFactory.create())
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .addCallAdapterFactory(CoroutineCallAdapterFactory())
        .baseUrl(BASE_URL)
        .build()

interface CatApiService {
    @GET("devices")
    fun getDevices(): Deferred<String>;

    @POST("devices")
    @Headers("Content-Type: application/json")
    fun addDevice(@Body device: RequestBody): Deferred<String>;

    @GET("devices/{deviceid}")
    fun getDeviceById(@Path("deviceid") id: String): Deferred<String>;

    @PUT("devices/{deviceid}")
    @Headers("Content-Type: application/json")
    fun updateDeviceById(@Path("deviceid") id: String, @Body device: RequestBody): Deferred<Response<Unit>>;

    @DELETE("devices/{deviceid}")
    fun deleteDeviceById(@Path("deviceid") id: String): Deferred<String>;
}

object CatApi {
    val retrofitService: CatApiService by lazy { retrofit.create(CatApiService::class.java) }
}