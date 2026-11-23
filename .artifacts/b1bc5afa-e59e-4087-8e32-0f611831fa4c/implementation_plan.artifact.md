# Implementation Plan: Comprehensive Feature Enhancements & Bug Fixes

This plan outlines the implementation for 15 key features and fixes requested for BlissChat.

## User Review Required

- **Python Bot Execution & Editor**: We will add a Python bot code editor / configuration interface in bot creation and settings, allowing users to define custom bot logic or scripts.
- **E2EE Key Stability**: We will fix key persistence and rotation in [`E2EE.kt`](file:///C:/Users/%D0%90%D0%B4%D0%BC%D0%B8%D0%BD/Desktop/BlissAi-main%20(3)/BlissAi-main/app/src/main/java/com/bliss/blisschat/data/crypto/E2EE.kt) to ensure encryption keys remain consistent and messages are always readable.
- **Glassmorphism & Floating Shapes**: We will enhance [`Theme.kt`](file:///C:/Users/%D0%90%D0%B4%D0%BC%D0%B8%D0%BD/Desktop/BlissAi-main%20(3)/BlissAi-main/app/src/main/java/com/bliss/blisschat/ui/theme/Theme.kt) and chat screens with animated Material You floating blurred shapes and WhatsApp-style watermark background.

## Open Questions

- *None at this stage; all requirements are clearly defined.*

## Proposed Changes

### 1. Plugins & UI Enhancement
#### [MODIFY] [PluginRepository.kt](file:///C:/Users/%D0%90%D0%B4%D0%BC%D0%B8%D0%BD/Desktop/BlissAi-main%20(3)/BlissAi-main/app/src/main/java/com/bliss/blisschat/data/plugins/PluginRepository.kt)
- Support code-based plugin definitions to dynamically enhance the UI.

### 2. Bots & Python Integration
#### [MODIFY] [BotRepository.kt](file:///C:/Users/%D0%90%D0%B4%D0%BC%D0%B8%D0%BD/Desktop/BlissAi-main%20(3)/BlissAi-main/app/src/main/java/com/bliss/blisschat/data/BotRepository.kt)
- Add storage and editing support for Python bot scripts.
#### [MODIFY] [SettingsScreen.kt](file:///C:/Users/%D0%90%D0%B4%D0%BC%D0%B8%D0%BD/Desktop/BlissAi-main%20(3)/BlissAi-main/app/src/main/java/com/bliss/blisschat/ui/settings/SettingsScreen.kt)
- Add "Bots" section to view, create, and edit custom bot scripts.

### 3. Telegram-style Inline Buttons & Mini-Apps
#### [MODIFY] [Models.kt](file:///C:/Users/%D0%90%D0%B4%D0%BC%D0%B8%D0%BD/Desktop/BlissAi-main%20(3)/BlissAi-main/app/src/main/java/com/bliss/blisschat/data/Models.kt)
- Add models for inline buttons and mini-app URLs.
#### [MODIFY] [ChatScreen.kt](file:///C:/Users/%D0%90%D0%B4%D0%BC%D0%B8%D0%BD/Desktop/BlissAi-main%20(3)/BlissAi-main/app/src/main/java/com/bliss/blisschat/ui/chat/ChatScreen.kt)
- Render clickable inline buttons under bot messages.
- Add support for embedding mini-app websites when a URL is attached.

### 4. Voice Messages, Attachments & Stickers
#### [MODIFY] [ChatScreen.kt](file:///C:/Users/%D0%90%D0%B4%D0%BC%D0%B8%D0%BD/Desktop/BlissAi-main%20(3)/BlissAi-main/app/src/main/java/com/bliss/blisschat/ui/chat/ChatScreen.kt)
- Voice message button: replace send button with voice recording when text input is empty.
- Attachment menu on paperclip (Photos, Camera, Poll, Music, File).
- Sticker keyboard integration inside the keyboard area (persistent, pack selection, saving sticker packs on click).

### 5. End-to-End Encryption (E2EE)
#### [MODIFY] [E2EE.kt](file:///C:/Users/%D0%90%D0%B4%D0%BC%D0%B8%D0%BD/Desktop/BlissAi-main%20(3)/BlissAi-main/app/src/main/java/com/bliss/blisschat/data/crypto/E2EE.kt)
- Stabilize key generation and persistence so keys do not change unexpectedly.

### 6. Channels, Groups & Global Search
#### [MODIFY] [ChatRepository.kt](file:///C:/Users/%D0%90%D0%B4%D0%BC%D0%B8%D0%BD/Desktop/BlissAi-main%20(3)/BlissAi-main/app/src/main/java/com/bliss/blisschat/data/ChatRepository.kt)
- Support joining channels/groups via links and finding channels in global search.
- Restrict channels from auto-adding users; add linked channel discussion comments / toggleable channel chats.
#### [MODIFY] [HomeScreen.kt](file:///C:/Users/%D0%90%D0%B4%D0%BC%D0%B8%D0%BD/Desktop/BlissAi-main%20(3)/BlissAi-main/app/src/main/java/com/bliss/blisschat/ui/home/HomeScreen.kt)
- Global search across all app chats/channels.
- Category selector button (All, Groups, Channels, Unread).

### 7. Reactions & Swipe-to-Reply
#### [MODIFY] [ChatScreen.kt](file:///C:/Users/%D0%90%D0%B4%D0%BC%D0%B8%D0%BD/Desktop/BlissAi-main%20(3)/BlissAi-main/app/src/main/java/com/bliss/blisschat/ui/chat/ChatScreen.kt)
- Fix reactions and add "Show more" to pick any emoji.
- Implement swipe right gesture on messages to reply.

### 8. Push Notifications & UI Styling (Glassmorphism, Floating Shapes, Chat Wallpaper)
#### [MODIFY] [PushService.kt](file:///C:/Users/%D0%90%D0%B4%D0%BC%D0%B8%D0%BD/Desktop/BlissAi-main%20(3)/BlissAi-main/app/src/main/java/com/bliss/blisschat/data/push/PushService.kt) & [Notifications.kt](file:///C:/Users/%D0%90%D0%B4%D0%BC%D0%B8%D0%BD/Desktop/BlissAi-main%20(3)/BlissAi-main/app/src/main/java/com/bliss/blisschat/data/push/Notifications.kt)
- Fix push notification delivery and channel registration.
#### [MODIFY] [Theme.kt](file:///C:/Users/%D0%90%D0%B4%D0%BC%D0%B8%D0%BD/Desktop/BlissAi-main%20(3)/BlissAi-main/app/src/main/java/com/bliss/blisschat/ui/theme/Theme.kt) & [ChatScreen.kt](file:///C:/Users/%D0%90%D0%B4%D0%BC%D0%B8%D0%BD/Desktop/BlissAi-main%20(3)/BlissAi-main/app/src/main/java/com/bliss/blisschat/ui/chat/ChatScreen.kt)
- Add animated floating blurred Material You shapes behind glassmorphism background.
- WhatsApp-style chat background pattern with icons and prominent white/black app icon watermark.

## Verification Plan

### Automated Tests
- Build verification via `gradle_build("app:assembleDebug")`.

### Manual Verification
- Verify all UI features, bot script editor, inline buttons, voice messages, attachment picker, stickers, E2EE persistence, global search, categories, reactions, swipe-to-reply, glassmorphic floating shapes, and WhatsApp-style chat wallpaper.
