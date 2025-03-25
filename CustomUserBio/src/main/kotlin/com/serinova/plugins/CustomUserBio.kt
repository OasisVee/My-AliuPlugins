package com.example.plugins

import com.aliucord.annotations.AliucordPlugin
import com.aliucord.entities.Plugin
import com.aliucord.patcher.after
import com.discord.models.user.User
import com.discord.stores.StoreStream
import com.discord.widgets.user.profile.WidgetUserProfile
import android.content.Context
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

@AliucordPlugin
class CustomUserBio : Plugin() {
    // Storage for user custom information
    private val userCustomInfoMap = mutableMapOf<Long, String>()
    private val gson = Gson()
    private val PREFS_KEY = "custom_user_info"

    override fun start() {
        // Patch the user profile to add custom information
        patcher.after<WidgetUserProfile>("configureHeader") { param ->
            val profile = param.thisObject as WidgetUserProfile
            val user = StoreStream.getUsers().getUser(profile.userId)
            
            user?.let {
                addCustomUserInfo(profile, it)
            }
        }
    }

    private fun addCustomUserInfo(profile: WidgetUserProfile, user: User) {
        // Retrieve custom info for the user
        val customInfo = getUserCustomInfo(user.id)
        
        if (customInfo.isNotBlank()) {
            // Create a new TextView for custom info
            val customInfoView = TextView(profile.context).apply {
                text = customInfo
                setTextColor(0xFF7289DA.toInt()) // Discord blurple color
                textSize = 14f
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
            }

            // Find the header container and add the custom info
            val headerContainer = profile.view.findViewById<LinearLayout>(
                profile.context.resources.getIdentifier(
                    "profile_header_info_container", 
                    "id", 
                    profile.context.packageName
                )
            )
            
            headerContainer?.addView(customInfoView)
        }
    }

    // Set custom info for a user
    fun setCustomUserInfo(userId: Long, info: String) {
        userCustomInfoMap[userId] = info
        saveUserCustomInfo()
    }

    // Get custom info for a user
    fun getUserCustomInfo(userId: Long): String {
        return userCustomInfoMap[userId] ?: ""
    }

    // Save user custom info to persistent storage
    private fun saveUserCustomInfo() {
        val json = gson.toJson(userCustomInfoMap)
        settings.setString(PREFS_KEY, json)
    }

    // Load user custom info from persistent storage
    private fun loadUserCustomInfo() {
        val json = settings.getString(PREFS_KEY, "{}")
        val type = object : TypeToken<MutableMap<Long, String>>() {}.type
        userCustomInfoMap.clear()
        userCustomInfoMap.putAll(gson.fromJson(json, type))
    }

    override fun stop() {
        // Unpatch when plugin is stopped
        patcher.unpatchAll()
    }

    // Initialize plugin
    init {
        loadUserCustomInfo()
    }
}
