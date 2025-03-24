package com.serinova.plugins

import android.content.Context
import com.aliucord.Utils
import com.aliucord.annotations.AliucordPlugin
import com.aliucord.entities.Plugin
import com.aliucord.patcher.before
import com.discord.api.message.Message
import com.discord.models.domain.ModelMessage
import com.discord.stores.StoreStream

@AliucordPlugin
class ReactWithEmote : Plugin() {
    override fun start(context: Context) {
        val regex = Regex("\\+:{1,5}([a-zA-Z0-9_]+):")

        StoreStream.getMessages().registerMessageObserver { message ->
            val content = message.content
            val match = regex.find(content)
            if (match != null) {
                val emoteName = match.groupValues[1]
                val plusCount = match.groupValues[0].count { it == '+' }
                val channelId = message.channelId
                val messages = getLastMessages(channelId, plusCount)
                messages.forEachIndexed { _, msg ->
                    reactWithEmote(msg, emoteName)
                }
            }
        }
    }

    override fun stop(context: Context) {
        patcher.unpatchAll()
    }

    private fun getLastMessages(channelId: Long, count: Int): List<Message> {
        val messages = StoreStream.getMessages().getMessagesForChannel(channelId)
        return messages?.takeLast(count) ?: emptyList()
    }

    private fun reactWithEmote(message: Message?, emoteName: String) {
        if (message == null) return
        val emote = getEmoteByName(emoteName)
        if (emote != null) {
            StoreStream.getMessages().addReaction(message.channelId, message.id, emote.id)
        } else {
            Utils.showToast("Emote not found: $emoteName")
        }
    }

    private fun getEmoteByName(emoteName: String): ModelMessage.Reaction? {
        val emotes = StoreStream.getGuilds().getEmotes()
        return emotes.find { it.name == emoteName }
    }
}
