package com.probusiness.intranet.data.remote.realtime

import javax.inject.Inject
import javax.inject.Singleton

/**
 * Registra qué ticket de Soporte TI tiene el usuario abierto ahora mismo. Se usa para no mostrar
 * la notificación push de un mensaje si el chat de ese ticket ya está abierto en pantalla — ahí
 * el WebSocket (RealtimeService) ya lo actualiza en vivo, la notificación sería redundante.
 */
@Singleton
class ActiveChatTracker @Inject constructor() {
    @Volatile
    var activeSolicitudId: Int? = null
        private set

    fun onChatOpened(solicitudId: Int) {
        activeSolicitudId = solicitudId
    }

    fun onChatClosed(solicitudId: Int) {
        if (activeSolicitudId == solicitudId) {
            activeSolicitudId = null
        }
    }
}
