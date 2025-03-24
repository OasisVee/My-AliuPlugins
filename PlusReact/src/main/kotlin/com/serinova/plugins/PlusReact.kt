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
import com.discord.models.emoji.Emoji
import com.discord.stores.StoreMessages
import com.discord.models.guild.GuildEmoji
import com.discord.api.message.Message as ApiMessage

@AliucordPlugin
class BetterPlusReacts : Plugin() {
    override fun start(context: Context) {
        val regex = Regex("^\\+:{1,5}([^:]+):$")

        patcher.patch(
            MessageRequest::class.java.getDeclaredMethod("send", ApiMessage::class.java),
            PreHook { param ->
                val message = param.args[0] as? ApiMessage ?: return@PreHook
                val content = message.content
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
        val messagesHolder = StoreStream.getStore(StoreMessages::class.java).getMessages(channelId)
        val messages = messagesHolder
            ?.map { it.value } // Get the Message objects from the map
            ?.sortedByDescending { it.timestamp.toEpochMillis() }
            ?.toList()
        return messages?.getOrNull(plusCount - 1)
    }

    private fun reactWithEmote(message: Message, emoteName: String) {
        val emote = findEmote(emoteName)
        if (emote != null) {
            try {
                RestAPI.api.addReaction(
                    message.channelId,
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

    private fun findEmote(emoteName: String): Emoji? {
        return StoreStream.getGuilds().guilds.values
            .flatMap { it.emojis }
            .find { it.name.equals(emoteName, ignoreCase = true) }
            ?.let { Emoji.fromModel(it) }
    }
}
