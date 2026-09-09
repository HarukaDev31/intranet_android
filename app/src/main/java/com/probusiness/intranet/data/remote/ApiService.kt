package com.probusiness.intranet.data.remote

import com.probusiness.intranet.data.remote.dto.ActualizarComplejidadRequest
import com.probusiness.intranet.data.remote.dto.ActualizarEstadoRequest
import com.probusiness.intranet.data.remote.dto.ApiMessageResponse
import com.probusiness.intranet.data.remote.dto.DeviceTokenRequest
import com.probusiness.intranet.data.remote.dto.LoginRequest
import com.probusiness.intranet.data.remote.dto.LoginResponse
import com.probusiness.intranet.data.remote.dto.LogoutRequest
import com.probusiness.intranet.data.remote.dto.MarcarLeidosRequest
import com.probusiness.intranet.data.remote.dto.MeResponse
import com.probusiness.intranet.data.remote.dto.MensajeDetailResponse
import com.probusiness.intranet.data.remote.dto.MensajesResponse
import com.probusiness.intranet.data.remote.dto.RefreshResponse
import com.probusiness.intranet.data.remote.dto.SolicitudDetailResponse
import com.probusiness.intranet.data.remote.dto.SolicitudListResponse
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Path
import retrofit2.http.Query

interface ApiService {

    @POST("auth/login")
    suspend fun login(@Body request: LoginRequest): LoginResponse

    @POST("auth/logout")
    suspend fun logout(@Body request: LogoutRequest): ApiMessageResponse

    @POST("auth/refresh")
    suspend fun refresh(): RefreshResponse

    @GET("auth/me")
    suspend fun me(): MeResponse

    @POST("auth/device/token")
    suspend fun registerDeviceToken(@Body request: DeviceTokenRequest): ApiMessageResponse

    @DELETE("auth/device/token")
    suspend fun removeDeviceToken(@Query("fcm_token") fcmToken: String): ApiMessageResponse

    @GET("soporte-ti/solicitudes")
    suspend fun listarSolicitudes(): SolicitudListResponse

    @GET("soporte-ti/solicitudes/{id}")
    suspend fun obtenerSolicitud(@Path("id") id: Int): SolicitudDetailResponse

    @PATCH("soporte-ti/solicitudes/{id}/estado")
    suspend fun actualizarEstado(@Path("id") id: Int, @Body request: ActualizarEstadoRequest): SolicitudDetailResponse

    @PATCH("soporte-ti/solicitudes/{id}/complejidad")
    suspend fun actualizarComplejidad(@Path("id") id: Int, @Body request: ActualizarComplejidadRequest): SolicitudDetailResponse

    @Multipart
    @POST("soporte-ti/solicitudes")
    suspend fun crearSolicitud(
        @Part("tipo_solicitud") tipoSolicitud: RequestBody,
        @Part("subtipo_b") subtipoB: RequestBody?,
        @Part("titulo") titulo: RequestBody,
        @Part("area") area: RequestBody,
        @Part("seccion_ruta") seccionRuta: RequestBody?,
        @Part("descripcion") descripcion: RequestBody,
        @Part imagenes: List<MultipartBody.Part>,
    ): SolicitudDetailResponse

    @GET("soporte-ti/chats/{chatUuid}/mensajes")
    suspend fun listarMensajes(
        @Path("chatUuid") chatUuid: String,
        @Query("limit") limit: Int = 30,
        @Query("before_id") beforeId: Int? = null,
    ): MensajesResponse

    @Multipart
    @POST("soporte-ti/solicitudes/{id}/mensajes")
    suspend fun enviarMensaje(
        @Path("id") solicitudId: Int,
        @Part("texto") texto: RequestBody?,
        @Part("reply_to_id") replyToId: RequestBody?,
        @Part imagenes: List<MultipartBody.Part>,
    ): MensajeDetailResponse

    @POST("soporte-ti/chats/{chatUuid}/mensajes/leidos")
    suspend fun marcarLeidos(
        @Path("chatUuid") chatUuid: String,
        @Body request: MarcarLeidosRequest,
    ): ApiMessageResponse
}
