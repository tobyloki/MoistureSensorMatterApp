package shared.Utility

import com.google.android.gms.common.internal.safeparcel.SafeParcelable.Param
import com.jakewharton.retrofit2.adapter.kotlin.coroutines.CoroutineCallAdapterFactory
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Deferred
import okhttp3.RequestBody
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.converter.scalars.ScalarsConverterFactory
import retrofit2.http.*

private const val BASE_URL = "https://lcwdhzcciwo3d5amt623pobsxq0xuwwb.lambda-url.us-west-2.on.aws/"

private val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()

private val retrofit =
    Retrofit.Builder()
        .addConverterFactory(ScalarsConverterFactory.create())
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .addCallAdapterFactory(CoroutineCallAdapterFactory())
        .baseUrl(BASE_URL)
        .build()

interface RestApiService {
    @GET("fetch-data/{deviceId}")
    fun fetchSensorData(@Path("deviceId") deviceId: String): Deferred<String>;
}

object HTTPRest {
    val retrofitService: RestApiService by lazy { retrofit.create(RestApiService::class.java) }
}