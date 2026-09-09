package com.probusiness.intranet.ui.navigation

object Routes {
    const val LOGIN = "login"
    const val SUPPORT_LIST = "support_list"
    const val NEW_TICKET = "new_ticket"

    const val TICKET_CHAT_ARG = "solicitudId"
    const val TICKET_CHAT = "ticket_chat/{$TICKET_CHAT_ARG}"

    fun ticketChat(solicitudId: Int) = "ticket_chat/$solicitudId"
}
