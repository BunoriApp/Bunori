package com.halovoid.bunori.data.preferences

import android.content.Context
import androidx.datastore.preferences.preferencesDataStore

val Context.appDataStore by preferencesDataStore(
    name = "app_preferences"
)