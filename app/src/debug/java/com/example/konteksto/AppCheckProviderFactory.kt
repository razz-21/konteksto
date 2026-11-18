package com.example.konteksto

import com.google.firebase.appcheck.AppCheckProviderFactory
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory

/**
 * Debug builds can't pass Play Integrity, so they use a debug token. It is logged on first launch
 * (or read from the `firebaseAppCheckDebugSecret` test argument) and must be registered under
 * App Check > Manage debug tokens in the Firebase console.
 */
internal fun appCheckProviderFactory(): AppCheckProviderFactory = DebugAppCheckProviderFactory.getInstance()
