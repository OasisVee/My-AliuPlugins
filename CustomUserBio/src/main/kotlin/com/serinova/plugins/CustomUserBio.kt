package com.serinova.plugins

import android.content.Context
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import com.aliucord.Utils
import com.aliucord.annotations.AliucordPlugin
import com.aliucord.entities.Plugin
import com.aliucord.fragments.InputDialog
import com.aliucord.patcher.Hook
import com.aliucord.views.Button
import com.aliucord.widgets.BottomSheet
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

    // Bottom sheet for managing user bios
    inner class UserBioBottomSheet : BottomSheet() {
        override fun onViewCreated(view: View, bundle: android.os.Bundle?) {
            super.onViewCreated(view, bundle)
            val context = requireContext()
            setPadding(20)

            // Title
            val title = TextView(context, null, 0, com.lytefast.flexinput.R.i.UiKit_Settings_Item_Header)
            title.text = "Custom User Bio"
            addView(title)

            // Current user ID
            val currentUserId = StoreStream.getUsers().me.id

            // Set Bio Button
            val setBioButton = Button(context)
            setBioButton.text = "Set Bio"
            setBioButton.setOnClickListener {
                val dialog = InputDialog()
                    .setTitle("Set Your Custom Bio")
                    .setDescription("Enter a bio that will be displayed on your profile")

                dialog.setOnOkListener { 
                    val bio = dialog.input
                    if (bio.isNotBlank()) {
                        setUserBio(currentUserId, bio)
                        Utils.showToast("Bio updated successfully")
                        dismiss()
                    } else {
                        Utils.showToast("Bio cannot be empty")
                    }
                }
                dialog.show(parentFragmentManager, "set_bio")
            }

            // Clear Bio Button
            val clearBioButton = Button(context)
            clearBioButton.text = "Clear Bio"
            clearBioButton.setOnClickListener {
                removeUserBio(currentUserId)
                Utils.showToast("Bio cleared successfully")
                dismiss()
            }

            addView(setBioButton)
            addView(clearBioButton)
        }
    }

    override fun start(context: Context) {
        // Load existing bios
        loadUserBios()

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

        // Add bottom sheet option to Discord user context menu
        patcher.patch(
            WidgetUserSheet::class.java,
            "configureOptionsMenu",
            Hook { cf ->
                val menuItems = cf.args[0] as MutableList<*>
                val context = (cf.thisObject as WidgetUserSheet).requireContext()
                
                menuItems.add(
                    mapOf(
                        "text" to "Manage Bio",
                        "icon" to context.resources.getIdentifier("ic_edit_24dp", "drawable", context.packageName),
                        "action" to Runnable { 
                            UserBioBottomSheet().show(
                                (cf.thisObject as WidgetUserSheet).parentFragmentManager, 
                                "custom_bio"
                            )
                        }
                    )
                )
            }
        )
    }

    override fun stop(context: Context) {
        patcher.unpatchAll()
    }

    // Set bio for a user
    private fun setUserBio(userId: Long, bio: String) {
        userBioMap[userId] = bio
        saveUserBios()
    }

    // Remove bio for a user
    private fun removeUserBio(userId: Long) {
        userBioMap.remove(userId)
        saveUserBios()
    }

    // Get bio for a user
    private fun getUserBio(userId: Long): String {
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
}
