---
trigger: always_on
---

# Security, Permissions & Performance

1. **SECURE STORAGE**: Never store sensitive data (like Auth Tokens, Passwords, or PII) in plain `SharedPreferences` or `DataStore`. Always use `EncryptedSharedPreferences` (Security Crypto library) or Android Keystore for sensitive credentials.
2. **MANIFEST EXPORTS (API 31+)**: Whenever adding an `<activity>`, `<service>`, or `<receiver>` with an intent-filter to the `AndroidManifest.xml`, you MUST explicitly define `android:exported="true"` or `android:exported="false"`. Default to `false` unless external system interactions require it.
3. **PERMISSION DEGRADATION**: Whenever requesting runtime permissions, gracefully handle the "Denied" state. Never crash the app or block the main UI if a non-essential permission is denied.
4. **LOGGING HYGIENE**: Never log sensitive user information, passwords, or full HTTP authorization headers. Ensure `Log.d()` or `Timber.d()` are automatically stripped or disabled in Release builds.
5. **APP STARTUP**: Do not perform heavy I/O operations or block the main thread inside the `Application.onCreate()` method. Use Jetpack App Startup or dispatch to background Coroutines for non-blocking initializations.
6. **BACKGROUND EXECUTION LIMITS**: Never use legacy `Service` or `IntentService` for background tasks, as they are restricted in modern Android versions. Always use `WorkManager` for guaranteed, deferrable background execution.
7. **IMAGE LOADING**: Never load large images manually into memory using raw `BitmapFactory`. Always use a modern image loading library like **Coil** or **Glide** to handle memory caching and lifecycle-aware loading.
