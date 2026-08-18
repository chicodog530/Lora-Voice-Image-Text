# Changelog

## [v5.0.0] - Digital Voice & Native Android App
- **Added:** Codec2 Digital Voice compression allowing up to 30 seconds of highly compressed voice to be transmitted over LoRa.
- **Added:** Native Android Application with distinct tabs for Voice, Text, and Images. Includes background polling, auto-tab switching on incoming data, and toast notifications.
- **Added:** Forced Dark Theme throughout the Android App to reduce eye strain.
- **Fixed (ESP32):** Addressed a severe bug where if a receiver successfully got a Voice or Image transfer and sent an ACK, but the transmitter's LoRa module missed the ACK due to RF noise or timing, the transmitter would retry sending the `END` packet for 2.5 minutes. The receiver was previously ignoring these retries because it was already in `IDLE` mode, causing the transmitter to completely lock up and reject all other attempts to communicate. The receiver will now properly re-acknowledge `END` packets even when idle.
- **Fixed (ESP32):** Fixed a bug where attempting to send an Image or Text immediately after Voice would allow the ESP32 to run multiple FreeRTOS transmission tasks simultaneously. This resulted in the UART stream being scrambled and crashed the ESP32. Strict mutual exclusion has been added to all upload handlers.
- **Fixed (Android):** Fixed an issue where the Android App would fail to upload Images after sending Voice. This was caused by an internal bug in the ESP32 `WebServer`'s multipart parser failing to handle consecutive uploads over the same Keep-Alive TCP connection. The Android App now forces `Connection: close` on all file uploads to bypass this ESP32 bug.
- **Fixed (Docs):** Rewrote the README with explicit instructions for sideloading the APK, clarifying peer roles, and added missing voltage warnings (3.3V vs 5V logic).
## [v4.0.0] - Motion Detection Security Camera
- **Added:** "Enable Motion Detection" feature. The web app now performs offline Javascript pixel analysis every 1 second.
- **Added:** Auto-trigger logic. If a >10% pixel change is detected, it automatically snaps a high-res photo, applies compression, and transmits it over LoRa.
- **Added:** 30-second cooldown timer for motion detection to prevent continuous radio spam.
- **Fixed:** Android Chrome aggressive video suspending issue by explicitly forcing `remoteVideo.play()` and adding a 500ms sensor wake-up delay before snapping photos.
- **Fixed:** Added a cache-buster timestamp parameter to the `/status` polling loop to prevent mobile browsers from aggressively caching the JSON response.

## [v3.0.0] - Remote Camera Control
- **Added:** "Request Remote Photo" feature. One node can now send a `REQUEST_IMAGE` packet over LoRa to the other node.
- **Added:** "Enable Remote Camera Mode" feature. The web app uses the `navigator.mediaDevices.getUserMedia` API to hook into the phone's rear camera.
- **Added:** Automatic photo snapping. The receiving phone polls the ESP32 `/status` endpoint, sees the request flag, snaps a frame from the live video feed, and automatically transmits it back over LoRa.

## [v2.0.0] - Text Messaging Integration
- **Added:** Two-way Text Messaging interface to the web app.
- **Added:** Dynamic string chunking. Javascript automatically splices long text messages into 96-byte chunks (LoRa Tweets) to comply with the LoRa module's hardware buffer limits.
- **Added:** Support for sending and displaying emojis over LoRa.
- **Changed:** Refactored the C++ backend state machine to safely handle concurrent HTTP requests and radio interrupts, resolving WDT (Watchdog Timer) panics.

## [v1.0.0] - Initial Release
- **Added:** Fast Mode (downscales to 400x300) and 2-Bit Grayscale compression algorithms running directly in the browser via Javascript `<canvas>`.
- **Added:** Custom CRC32, ACK, and MISSING packet protocol to ensure lossless image transmission over raw UART LoRa streams.
- **Added:** Real-time upload and receive progress bars in the web interface.
