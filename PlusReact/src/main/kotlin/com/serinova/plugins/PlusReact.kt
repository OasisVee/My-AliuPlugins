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
import com.discord.api.message.Message as ApiMessage
import java.net.URLEncoder

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
        val emoteData = findEmoteInCurrentGuild(message.channelId, emoteName)
        if (emoteData != null) {
            try {
                // URL encode the emoji as per Discord API requirements
                val encodedEmoji = URLEncoder.encode("${emoteData.name}:${emoteData.id}", "UTF-8")
                RestAPI.api.addReaction(
                    message.channelId,
                    message.id,
                    encodedEmoji
                )
            } catch (e: Exception) {
                Utils.showToast("Failed to add reaction: ${e.message}")
            }
        } else {
            Utils.showToast("Emote not found in current server: $emoteName")
        }
    }

    private data class EmoteData(val id: String, val name: String)

    private fun findEmoteInCurrentGuild(channelId: Long, emoteName: String): EmoteData? {
        // Get the current guild for the channel
        val guildId = StoreStream.getChannels().getChannel(channelId)?.guildId
            ?: return null

        // Find the guild
        val guild = StoreStream.getGuilds().getGuild(guildId)
            ?: return null

        // Search for the emoji in the current guild, case-insensitive
        val emoji = guild.emojis.find { emoji -> 
            emoji.name.equals(emoteName, ignoreCase = true) 
        }
        
        return emoji?.let { 
            EmoteData(
                it.id.toString(), 
                it.name
            ) 
        }
    }
}
