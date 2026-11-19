# Bliss Keyboard
Android Kotlin IME prototype with Material You-inspired rounded UI.

## Build APK on GitHub
1. Create a GitHub repository.
2. Upload all project files from this folder.
3. Open Actions -> Build Bliss Keyboard APK -> Run workflow.
4. Download the `BlissKeyboard-debug-apk` artifact.

## Enable on Android
Install APK -> Settings app -> Languages/Input -> On-screen keyboard -> enable Bliss Keyboard -> choose it in any text field.

Features: system IME service, QWERTY/Russian layout toggle, symbols, emoji insertion, clipboard paste, RGB accent setting, locale list, settings screen.

Note: this is an MVP. The locale list is generated from locales available in Android, but layouts are currently implemented for English and Russian; other languages need dedicated layouts. Autocorrect is a settings toggle placeholder and dictionary engine is not yet integrated.
