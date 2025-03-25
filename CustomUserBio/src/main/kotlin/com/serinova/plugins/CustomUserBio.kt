package com.serinova.plugins

import android.content.Context
import android.widget.LinearLayout
import android.widget.TextView
import com.aliucord.annotations.AliucordPlugin
import com.aliucord.entities.Plugin
import com.aliucord.patcher.Hook
import com.discord.stores.StoreStream
import com.discord.widgets.user.usersheet.WidgetUserSheet
import com.discord.widgets.user.usersheet.WidgetUserSheetViewModel
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

@AliucordPlugin
class CustomUserBio : Plugin() {
    // Storage for user bios
    private val userBioMap = mutableMapOf<Long, String>()
    private val gson = Gson()
    private val PREFS_KEY = "user_bios"

    override fun start(context: Context) {
        // Patch the user sheet to add custom bio
        patcher.patch(
            WidgetUserSheet::class.java.getDeclaredMethod(
                "configureUI", 
                WidgetUserSheetViewModel.ViewState::class.java
            ), 
            Hook { cf ->
                val viewState = cf.args[0] as WidgetUserSheetViewModel.ViewState.Loaded
                val user = viewState.user
                val scrollView = WidgetUserSheet.access$getBinding$p((cf.thisObject as WidgetUserSheet)).root as LinearLayout

                // Create TextView for bio
                val bioTextView = TextView(context).apply {
                    val bio = getUserBio(user.id)
                    text = bio
                    textSize = 14f
                    setPadding(30, 10, 30, 10)
                }

                // Add bio to the user sheet if not empty
                if (bio.isNotBlank()) {
                    val contentLayout = scrollView.findViewById<LinearLayout>(
                        context.resources.getIdentifier(
                            "user_sheet_content", 
                            "id", 
                            context.packageName
                        )
                    )
                    contentLayout?.addView(bioTextView)
                }
            }
        )
    }

    override fun stop(context: Context) {
        patcher.unpatchAll()
    }

    // Set bio for a user
    fun setUserBio(userId: Long, bio: String) {
        userBioMap[userId] = bio
        saveUserBios()
    }

    // Get bio for a user
    fun getUserBio(userId: Long): String {
        return userBioMap[userId] ?: ""
    }

    // Save user bios to persistent storage
    private fun saveUserBios() {
        val json = gson.toJson(userBioMap)
        settings.setString(PREFS_KEY, json)
    }

    // Load user bios from persistent storage
    private fun loadUserBios() {
        val json = settings.getString(PREFS_KEY, "{}")
        val type = object : TypeToken<MutableMap<Long, String>>() {}.type
        userBioMap.clear()
        userBioMap.putAll(gson.fromJson(json, type))
    }

    // Initialize plugin
    init {
        loadUserBios()
    }
}
