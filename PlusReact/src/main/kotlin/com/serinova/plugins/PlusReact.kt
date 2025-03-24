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
import com.discord.stores.StoreMessages
import com.discord.api.message.Message as ApiMessage

@AliucordPlugin
class PlusReact : Plugin() {
    override fun start(context: Context) {
        val regex = Regex("^\\+:{1,5}([^:]+):$")

        patcher.patch(
            MessageRequest::class.java.getDeclaredMethod("send", ApiMessage::class.java),
            PreHook { param ->
                val message = param.args[0] as? ApiMessage ?: return@PreHook
                val content = message.content ?: return@PreHook
                val match = regex.find(content)
                if (match != null) {
                    val emoteName = match.groupValues[1]
                    val plusCount = content.takeWhile { it == '+' }.length
                    val channelId = message.channelId
                    val targetMessage = getTargetMessage(channelId, plusCount)
                    if (targetMessage != null) {
                        reactWithEmote(targetMessage, emoteName)
                        // Optionally delete the trigger message
                        // RestAPI.api.deleteMessage(channelId, message.id)
                    }
                }
            }
        )
    }

    override fun stop(context: Context) {
        patcher.unpatchAll()
    }

    private fun getTargetMessage(channelId: Long, plusCount: Int): Message? {
        val messagesStore = StoreStream.getMessages()
        val messages = messagesStore.getMessages(channelId)
            ?.map { it.value }
            ?.sortedByDescending { it.timestamp.toEpochMillis() }
            ?.toList()
        return messages?.getOrNull(plusCount - 1)
    }

    private fun reactWithEmote(message: Message, emoteName: String) {
        val emoteData = findEmoteData(emoteName)
        if (emoteData != null) {
            try {
                RestAPI.api.addReaction(
                    message.channelId,
                    message.id,
                    "<:${emoteData.name}:${emoteData.id}>" // Format for custom emojis
                )
            } catch (e: Exception) {
                Utils.showToast("Failed to add reaction: ${e.message}")
            }
        } else {
            Utils.showToast("Emote not found: $emoteName")
        }
    }

    private data class EmoteData(val id: String, val name: String)

    private fun findEmoteData(emoteName: String): EmoteData? {
        return StoreStream.getGuilds().guilds.values
            .flatMap { guild -> guild.emojis }
            .find { emoji -> emoji.name.equals(emoteName, ignoreCase = true) }
            ?.let { emoji -> 
                EmoteData(
                    emoji.id.toString(), 
                    emoji.name
                ) 
            }
    }
}
