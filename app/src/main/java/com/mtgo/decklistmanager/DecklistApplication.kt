package com.mtgo.decklistmanager

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

/**
 * MTGO Decklist Manager Application
 * Custom Application class for Hilt dependency injection
 */
@HiltAndroidApp
class DecklistApplication : Application() {

    override fun onCreate() {
        super.onCreate()
    }
}
