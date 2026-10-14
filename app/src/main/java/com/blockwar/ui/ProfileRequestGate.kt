package com.blockwar.ui

import java.util.Locale

/** Coordinates explicit profile selection and background refreshes. No account data is stored here. */
internal class ProfileRequestGate {
    data class Ticket(val generation: Long, val owner: String, val selection: Boolean)
    private var generation = 0L
    private var active: Ticket? = null
    private fun owner(name: String) = name.trim().lowercase(Locale.ROOT)

    @Synchronized fun select(name: String): Ticket {
        return Ticket(++generation, owner(name), true).also { active = it }
    }

    @Synchronized fun refresh(name: String, currentName: String): Ticket? {
        val target = owner(name)
        if (target.isBlank() || target != owner(currentName) || active?.selection == true) return null
        return Ticket(++generation, target, false).also { active = it }
    }

    /** Each request has one terminal callback; stale successes, PIN errors and failures are ignored. */
    @Synchronized fun complete(ticket: Ticket, currentName: String): Boolean {
        if (active != ticket) return false
        active = null
        return ticket.selection || ticket.owner == owner(currentName)
    }

    @Synchronized fun invalidate() {
        generation++
        active = null
    }
}
