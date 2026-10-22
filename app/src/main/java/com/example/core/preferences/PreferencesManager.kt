package com.example.core.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "novapulse_prefs")

class PreferencesManager(private val context: Context) {

    companion object {
        val KEY_ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")
        val KEY_ACTIVE_MODEL = stringPreferencesKey("active_model")
        val KEY_REGEX_PREFILTER = booleanPreferencesKey("regex_prefilter_enabled")
        val KEY_BLACKLISTED_APPS = stringSetPreferencesKey("blacklisted_apps")
        val KEY_PROMPT_CUSTOMIZATION = stringPreferencesKey("custom_prompt_instructions")

        // Widget Customizer Keys
        val KEY_WIDGET_TRANSPARENCY = androidx.datastore.preferences.core.floatPreferencesKey("widget_transparency")
        val KEY_WIDGET_COLOR_PRESET = stringPreferencesKey("widget_color_preset")
        val KEY_WIDGET_SHAPE = stringPreferencesKey("widget_shape")
        val KEY_WIDGET_SHOW_SUBTEXT = booleanPreferencesKey("widget_show_subtext")
        val KEY_WIDGET_AI_BRIEFING = stringPreferencesKey("widget_ai_briefing")
        val KEY_WIDGET_AI_ENERGY_SCORE = androidx.datastore.preferences.core.intPreferencesKey("widget_ai_energy_score")
        val KEY_WIDGET_LAST_UPDATED = androidx.datastore.preferences.core.longPreferencesKey("widget_last_updated")

        // Dynamic Island Keys
        val KEY_DYNAMIC_ISLAND_ENABLED = booleanPreferencesKey("dynamic_island_enabled")
        val KEY_DYNAMIC_ISLAND_Y_OFFSET = androidx.datastore.preferences.core.intPreferencesKey("dynamic_island_y_offset")
        val KEY_DYNAMIC_ISLAND_LOCKSCREEN = booleanPreferencesKey("dynamic_island_lockscreen")
        val KEY_DYNAMIC_ISLAND_LOCKSCREEN_ONLY = booleanPreferencesKey("dynamic_island_lockscreen_only")
        val KEY_DYNAMIC_ISLAND_SCALE = androidx.datastore.preferences.core.floatPreferencesKey("dynamic_island_scale")
        val KEY_DYNAMIC_ISLAND_WIDTH = androidx.datastore.preferences.core.intPreferencesKey("dynamic_island_width")
        val KEY_DYNAMIC_ISLAND_HEIGHT = androidx.datastore.preferences.core.intPreferencesKey("dynamic_island_height")
        val KEY_DYNAMIC_ISLAND_OPACITY = androidx.datastore.preferences.core.floatPreferencesKey("dynamic_island_opacity")
        val KEY_DYNAMIC_ISLAND_FROSTED_GLASS = booleanPreferencesKey("dynamic_island_frosted_glass")
        val KEY_DYNAMIC_ISLAND_POSITION = stringPreferencesKey("dynamic_island_position")
        val KEY_DYNAMIC_ISLAND_COLOR_PRESET = stringPreferencesKey("dynamic_island_color_preset")

        // Content Display (Cards to include)
        val KEY_ENABLED_CONTENT_CARDS = stringSetPreferencesKey("enabled_content_cards")
        val DEFAULT_ENABLED_CARDS = setOf(
            "health_energy",     // Physical and mental health
            "calendar_schedule", // Events, tasks & schedule
            "now_nudge",         // Now Nudge AI & smart suggestions
            "weather",           // Weather & environment
            "habit_insights",    // Habit reports & peak hours
            "sleep_recovery",    // Sleep & recovery
            "smart_music",       // Music recommendations (Spotify)
            "digital_wellbeing", // Digital wellbeing & screen time
            "news_market"        // News & market
        )
    }

    val enabledContentCards: Flow<Set<String>> = context.dataStore.data.map { prefs ->
        prefs[KEY_ENABLED_CONTENT_CARDS] ?: DEFAULT_ENABLED_CARDS
    }

    val dynamicIslandColorPreset: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[KEY_DYNAMIC_ISLAND_COLOR_PRESET] ?: "DYNAMIC"
    }

    val dynamicIslandEnabled: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[KEY_DYNAMIC_ISLAND_ENABLED] ?: false
    }

    val dynamicIslandYOffset: Flow<Int> = context.dataStore.data.map { prefs ->
        prefs[KEY_DYNAMIC_ISLAND_Y_OFFSET] ?: 10
    }

    val dynamicIslandPosition: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[KEY_DYNAMIC_ISLAND_POSITION] ?: "BOTTOM"
    }

    val dynamicIslandLockscreen: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[KEY_DYNAMIC_ISLAND_LOCKSCREEN] ?: true
    }

    val dynamicIslandLockscreenOnly: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[KEY_DYNAMIC_ISLAND_LOCKSCREEN_ONLY] ?: true
    }

    val dynamicIslandScale: Flow<Float> = context.dataStore.data.map { prefs ->
        prefs[KEY_DYNAMIC_ISLAND_SCALE] ?: 0.9f
    }

    val dynamicIslandWidth: Flow<Int> = context.dataStore.data.map { prefs ->
        prefs[KEY_DYNAMIC_ISLAND_WIDTH] ?: 320
    }

    val dynamicIslandHeight: Flow<Int> = context.dataStore.data.map { prefs ->
        prefs[KEY_DYNAMIC_ISLAND_HEIGHT] ?: 42
    }

    val dynamicIslandOpacity: Flow<Float> = context.dataStore.data.map { prefs ->
        prefs[KEY_DYNAMIC_ISLAND_OPACITY] ?: 0.65f
    }

    val dynamicIslandFrostedGlass: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[KEY_DYNAMIC_ISLAND_FROSTED_GLASS] ?: true
    }

    val widgetAiBriefing: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[KEY_WIDGET_AI_BRIEFING] ?: ""
    }

    val widgetAiEnergyScore: Flow<Int> = context.dataStore.data.map { prefs ->
        prefs[KEY_WIDGET_AI_ENERGY_SCORE] ?: 92
    }

    val widgetLastUpdated: Flow<Long> = context.dataStore.data.map { prefs ->
        prefs[KEY_WIDGET_LAST_UPDATED] ?: 0L
    }

    val widgetTransparency: Flow<Float> = context.dataStore.data.map { prefs ->
        prefs[KEY_WIDGET_TRANSPARENCY] ?: 1.0f
    }

    val widgetColorPreset: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[KEY_WIDGET_COLOR_PRESET] ?: "AUTO"
    }

    val widgetShape: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[KEY_WIDGET_SHAPE] ?: "CAPSULE"
    }

    val widgetShowSubtext: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[KEY_WIDGET_SHOW_SUBTEXT] ?: true
    }

    val isOnboardingCompleted: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[KEY_ONBOARDING_COMPLETED] ?: false
    }

    val activeModel: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[KEY_ACTIVE_MODEL] ?: "qwen_0_5b"
    }

    val isRegexPrefilterEnabled: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[KEY_REGEX_PREFILTER] ?: true
    }

    val blacklistedApps: Flow<Set<String>> = context.dataStore.data.map { prefs ->
        prefs[KEY_BLACKLISTED_APPS] ?: setOf("com.google.android.youtube", "com.spotify.music")
    }

    val customPrompt: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[KEY_PROMPT_CUSTOMIZATION] ?: "Extract tasks and expenses with high precision and auto-tag priority."
    }

    suspend fun setOnboardingCompleted(completed: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[KEY_ONBOARDING_COMPLETED] = completed
        }
    }

    suspend fun setActiveModel(modelId: String) {
        context.dataStore.edit { prefs ->
            prefs[KEY_ACTIVE_MODEL] = modelId
        }
    }

    suspend fun setRegexPrefilter(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[KEY_REGEX_PREFILTER] = enabled
        }
    }

    suspend fun toggleBlacklistApp(packageName: String) {
        context.dataStore.edit { prefs ->
            val current = prefs[KEY_BLACKLISTED_APPS]?.toMutableSet() ?: mutableSetOf()
            if (current.contains(packageName)) {
                current.remove(packageName)
            } else {
                current.add(packageName)
            }
            prefs[KEY_BLACKLISTED_APPS] = current
        }
    }

    suspend fun setCustomPrompt(prompt: String) {
        context.dataStore.edit { prefs ->
            prefs[KEY_PROMPT_CUSTOMIZATION] = prompt
        }
    }

    suspend fun setWidgetConfig(
        transparency: Float,
        colorPreset: String,
        shape: String,
        showSubtext: Boolean
    ) {
        context.dataStore.edit { prefs ->
            prefs[KEY_WIDGET_TRANSPARENCY] = transparency
            prefs[KEY_WIDGET_COLOR_PRESET] = colorPreset
            prefs[KEY_WIDGET_SHAPE] = shape
            prefs[KEY_WIDGET_SHOW_SUBTEXT] = showSubtext
        }
    }

    suspend fun setWidgetAiBriefing(
        briefing: String,
        energyScore: Int,
        timestamp: Long = System.currentTimeMillis()
    ) {
        context.dataStore.edit { prefs ->
            prefs[KEY_WIDGET_AI_BRIEFING] = briefing
            prefs[KEY_WIDGET_AI_ENERGY_SCORE] = energyScore
            prefs[KEY_WIDGET_LAST_UPDATED] = timestamp
        }
    }

    suspend fun setDynamicIslandEnabled(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[KEY_DYNAMIC_ISLAND_ENABLED] = enabled
        }
    }

    suspend fun setDynamicIslandYOffset(offsetDp: Int) {
        context.dataStore.edit { prefs ->
            prefs[KEY_DYNAMIC_ISLAND_Y_OFFSET] = offsetDp
        }
    }

    suspend fun setDynamicIslandLockscreen(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[KEY_DYNAMIC_ISLAND_LOCKSCREEN] = enabled
        }
    }

    suspend fun setDynamicIslandLockscreenOnly(lockscreenOnly: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[KEY_DYNAMIC_ISLAND_LOCKSCREEN_ONLY] = lockscreenOnly
        }
    }

    suspend fun setDynamicIslandScale(scale: Float) {
        context.dataStore.edit { prefs ->
            prefs[KEY_DYNAMIC_ISLAND_SCALE] = scale
        }
    }

    suspend fun setDynamicIslandWidth(widthDp: Int) {
        context.dataStore.edit { prefs ->
            prefs[KEY_DYNAMIC_ISLAND_WIDTH] = widthDp
        }
    }

    suspend fun setDynamicIslandHeight(heightDp: Int) {
        context.dataStore.edit { prefs ->
            prefs[KEY_DYNAMIC_ISLAND_HEIGHT] = heightDp
        }
    }

    suspend fun setDynamicIslandOpacity(opacity: Float) {
        context.dataStore.edit { prefs ->
            prefs[KEY_DYNAMIC_ISLAND_OPACITY] = opacity
        }
    }

    suspend fun setDynamicIslandFrostedGlass(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[KEY_DYNAMIC_ISLAND_FROSTED_GLASS] = enabled
        }
    }

    suspend fun setDynamicIslandPosition(position: String) {
        context.dataStore.edit { prefs ->
            prefs[KEY_DYNAMIC_ISLAND_POSITION] = position
        }
    }

    suspend fun setDynamicIslandColorPreset(preset: String) {
        context.dataStore.edit { prefs ->
            prefs[KEY_DYNAMIC_ISLAND_COLOR_PRESET] = preset
        }
    }

    suspend fun setContentCardEnabled(cardId: String, isEnabled: Boolean) {
        context.dataStore.edit { prefs ->
            val current = (prefs[KEY_ENABLED_CONTENT_CARDS] ?: DEFAULT_ENABLED_CARDS).toMutableSet()
            if (isEnabled) {
                current.add(cardId)
            } else {
                current.remove(cardId)
            }
            prefs[KEY_ENABLED_CONTENT_CARDS] = current
        }
    }

    suspend fun toggleContentCard(cardId: String) {
        context.dataStore.edit { prefs ->
            val current = (prefs[KEY_ENABLED_CONTENT_CARDS] ?: DEFAULT_ENABLED_CARDS).toMutableSet()
            if (current.contains(cardId)) {
                current.remove(cardId)
            } else {
                current.add(cardId)
            }
            prefs[KEY_ENABLED_CONTENT_CARDS] = current
        }
    }

    suspend fun resetContentCardsToDefault() {
        context.dataStore.edit { prefs ->
            prefs[KEY_ENABLED_CONTENT_CARDS] = DEFAULT_ENABLED_CARDS
        }
    }
}
