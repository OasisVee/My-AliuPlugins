package com.serinova.plugins

import android.content.Context
import com.aliucord.Utils
import com.aliucord.annotations.AliucordPlugin
import com.aliucord.entities.Plugin
import com.discord.api.commands.ApplicationCommandType
import com.discord.models.message.Message
import com.discord.stores.StoreStream
import com.discord.utilities.message.MessageUtils
import com.discord.models.guild.Guild
import com.discord.models.message.MessageReference
import com.discord.stores.StoreMessageReactions
import com.discord.models.guild.Emoji

@AliucordPlugin
class PlusReact : Plugin() {
    override fun start(context: Context) {
        val regex = Regex("\\+:{1,5}([a-zA-Z0-9_]+):")

        patcher.before<MessageUtils>("handleMessage") { param ->
            val message = param.args[0] as Message
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
    }

    override fun stop(context: Context) {
        patcher.unpatchAll()
    }

    private fun getLastMessages(channelId: Long, count: Int): List<Message> {
        return StoreStream.getMessages().getMessage(channelId)
            ?.takeIf { it.isNotEmpty() }
            ?.takeLast(count.coerceAtMost(5))
            ?: emptyList()
    }

    private fun reactWithEmote(message: Message?, emoteName: String) {
        if (message == null) return
        val emote = getEmoteByName(emoteName)
        if (emote != null) {
            val reactionKey = StoreMessageReactions.ReactionKey(
                messageId = message.id,
                channelId = message.channelId,
                emojiId = emote.id,
                emojiName = emote.name
            )
            StoreStream.getMessageReactions().handleMessageReactionAdd(reactionKey)
        } else {
            Utils.showToast("Emote not found: $emoteName")
        }
    }

    private fun getEmoteByName(emoteName: String): Emoji? {
        return StoreStream.getGuilds().guilds.values
            .flatMap { it.emojis }
            .find { emoji -> emoji.name == emoteName }
    }
}
