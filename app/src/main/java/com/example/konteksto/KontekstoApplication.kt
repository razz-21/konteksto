package com.example.konteksto

import android.app.Application
import com.google.firebase.Firebase
import com.google.firebase.appcheck.appCheck

class KontekstoApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // Must be installed before any Firebase call that App Check protects, such as AI Logic.
        Firebase.appCheck.installAppCheckProviderFactory(appCheckProviderFactory())
    }
}
