package com.wickmoth.lakekeeps.game.mail

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.mutableStateListOf
import com.wickmoth.lakekeeps.game.Owner

/**
 * An email. [body] is plain text with paragraphs separated by blank lines. It may carry a
 * [picture] (shown as a placeholder image with that caption) and an [attachment] (a file name).
 * It arrived [daysAgo] days before the game's "now", at [minutes] past midnight. [unread] is how
 * the phone's owner left it.
 */
@Immutable
data class Email(
    val id: String,
    val sender: String,
    val address: String,
    val subject: String,
    val body: String,
    val daysAgo: Int,
    val minutes: Int,
    val picture: String? = null,
    val attachment: String? = null,
    val unread: Boolean = false,
)

/** Which emails the player has opened, saved with the game state. */
@Stable
class MailBox(initial: Collection<String> = emptyList()) {
    private val opened = mutableStateListOf<String>().apply { addAll(initial.distinct()) }

    fun isUnread(mail: Email): Boolean = mail.unread && mail.id !in opened

    fun open(mail: Email) {
        if (mail.id !in opened) opened += mail.id
    }

    fun unread(owner: Owner): Int = Inboxes.of(owner).count(::isUnread)

    fun clear() = opened.clear()

    fun encode(): String = opened.joinToString("\n")

    companion object {
        fun decode(saved: String?): MailBox =
            MailBox(saved.orEmpty().lineSequence().filter { Inboxes.byId(it) != null }.toList())
    }
}
