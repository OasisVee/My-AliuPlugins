package com.serinova.plugins

import android.content.Context
import com.aliucord.Utils
import com.aliucord.annotations.AliucordPlugin
import com.aliucord.entities.Plugin
import com.aliucord.patcher.PreHook
import com.discord.models.message.Message
import com.discord.stores.StoreStream
import com.discord.utilities.messagesend.MessageRequest
import com.discord.utilities.rest.RestAPI
import com.discord.models.domain.emoji.ModelEmoji
import com.discord.stores.StoreMessagesHolder

@AliucordPlugin
class PlusReact : Plugin() {
    override fun start(context: Context) {
        val regex = Regex("\\+:{1,5}([a-zA-Z0-9_]+):")

        patcher.patch(
            MessageRequest::class.java.getDeclaredMethod("send", Message::class.java),
            PreHook { param ->
                val message = param.args[0] as? Message ?: return@PreHook
                val content = message.content
                val match = regex.find(content)
                if (match != null) {
                    val emoteName = match.groupValues[1]
                    val plusCount = match.groupValues[0].count { it == '+' }
                    val channelId = message.channelId
                    val messages = getLastMessages(channelId, plusCount)
                    messages.forEach { msg ->
                        reactWithEmote(msg, emoteName)
                    }
                }
            }
        )
    }

    override fun stop(context: Context) {
        patcher.unpatchAll()
    }

    private fun getLastMessages(channelId: Long, count: Int): List<Message> {
        val messagesHolder = StoreStream.getMessages().getMessagesByChannelId(channelId)
        return messagesHolder?.messagesList
            ?.asReversed()
            ?.take(count.coerceAtMost(5))
            ?: emptyList()
    }

    private fun reactWithEmote(message: Message, emoteName: String) {
        val emote = findEmote(emoteName)
        if (emote != null) {
            try {
                RestAPI.api.addReaction(
                    message.channelId.toString(),
                    message.id,
                    emote.toReactionString()
                )
            } catch (e: Exception) {
                Utils.showToast("Failed to add reaction: ${e.message}")
            }
        } else {
            Utils.showToast("Emote not found: $emoteName")
        }
    }

    private fun findEmote(emoteName: String): ModelEmoji? {
        return StoreStream.getGuilds().guilds.values
            .flatMap { it.emojis }
            .find { it.getName().equals(emoteName, ignoreCase = true) }
            ?.let { ModelEmoji.fromEmoji(it) }
    }
}
