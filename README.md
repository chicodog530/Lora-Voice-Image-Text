# LoRa Digital Voice, Image, Text & Security Camera

An off-grid, two-way communicator and remote security camera system built with ESP32 and UART LoRa modules. This project turns two ESP32s into autonomous walkie-talkies that connect to a custom Native Android App (or a built-in Web App), allowing you to send voice recordings, photos, text messages, and even set up an autonomous motion-detecting security camera over miles of distance without any internet or cellular connection.

## Features
- **Completely Off-Grid:** Generates its own Wi-Fi Access Point. No router or internet required.
- **Native Android App:** A fast, dark-mode Native Android App with a seamless UI for Voice, Text, and Images.
- **Digital Voice Walkie-Talkie:** Uses the incredible Codec2 compression algorithm to compress human speech down to 400 bytes per second, allowing live voice transmission over extremely low-bandwidth LoRa links.
- **Motion Detection (Security Camera Mode):** Leave a spare phone connected to the remote node. If it detects motion, it silently snaps a high-res photo and beams it over LoRa to your primary phone automatically!
- **Remote Camera Mode:** Manually request a photo from the remote node at any time.
- **High Resolution & Half-Size Modes:** Supports sending JPEGs up to 800x600 resolution, with an instant 50% downscaling toggle for faster transmission.
- **Infinite Text Messaging:** Send large text messages containing full Unicode Emojis.
- **Reliable Custom Protocol:** Built-in packet fragmentation, CRC32 checksums, ACKs, and missing-packet recovery to ensure your files arrive uncorrupted.

---

## Parts List
To build a complete two-way system, you need two of everything:
1. **2x ESP32 Development Boards** (Standard ESP32, NodeMCU ESP32, ESP32-WROOM, etc.)
2. **2x UART LoRa Modules**: e.g., Loongtrek SX1262 DX-LR32, Ebyte E220, or Reyax RYLR998.
3. **2x Antennas** (Usually included with the LoRa modules)
4. Jumper wires and USB power banks.

---

## Wiring Guide
The UART LoRa modules connect easily to the ESP32.

| LoRa Module | ESP32 Pin | Notes |
| :--- | :--- | :--- |
| **VCC** | **3.3V or 5V** | **CRITICAL:** Check your specific LoRa module's voltage requirements! Supplying 5V to a 3.3V module will destroy it. |
| **GND** | **GND** | Common ground. |
| **TX** | **Pin 16** | ESP32 RX receives from LoRa TX. |
| **RX** | **Pin 17** | ESP32 TX transmits to LoRa RX. |

*(Note: Depending on your specific ESP32 board, you can change the RX/TX pins at the top of the `transceiver_v4.ino` sketch).*

---

## Setup Instructions

### Part 1: Flashing the ESP32 Firmware
Both ESP32 devices run the exact same firmware.
1. Open `transceiver_v4.ino` in the Arduino IDE.
2. Connect your first ESP32 to your PC via USB.
3. Select the correct COM port and ESP32 board type.
4. Click **Upload**.
5. Repeat steps 2-4 for the second ESP32.

> **Wi-Fi Credentials:** Each ESP32 will broadcast its own Wi-Fi network named `LoRa_Chat_XXXX` (where XXXX is the last 4 characters of its MAC address). The password for the Wi-Fi network is `12345678`.

### Part 2: Installing the Android App (Main Phone)
This phone will be your primary handheld device for sending texts, voice messages, and requesting images.
1. If you downloaded the pre-compiled `LoRa_Digital_Voice_App.apk` from the GitHub release or the repository, simply transfer it to your Android Phone and tap it to install (sideload).
   - *(Optional: If you prefer to compile from source, open the `LoRa_Digital_Voice_App` folder in Android Studio and hit **Play** to install it via USB).*
2. Once installed, go to your phone's Wi-Fi settings and connect to the ESP32 network (e.g., `LoRa_Chat_1A2B`).
   - *Note: Android may warn you that the network has "No Internet Access". Tap **"Keep Connection"**.*
3. Open the **LoRa Voice** app. You should be able to navigate between the Voice, Text, and Image tabs!

### Part 3: Setting Up the Security Camera (Remote Phone)
Instead of the app, this phone will use the built-in Web App served directly by the ESP32. This allows it to run automatic motion detection and camera capture in the background!
1. Go to your **Remote Phone's** Wi-Fi settings and connect to the second ESP32 network (e.g., `LoRa_Chat_9F8E`).
2. Open Google Chrome (or your preferred mobile browser).
3. Type `http://192.168.4.1` into the address bar and hit Go.
4. **Important Chrome Security Setting:** Because the ESP32 serves the site over HTTP instead of HTTPS, Chrome blocks access to the camera by default. To fix this:
   - In Chrome, open a new tab and go to `chrome://flags`
   - Search for **"Insecure origins treated as secure"**
   - Type `http://192.168.4.1` into the text box, change the dropdown to **Enabled**, and tap the **Relaunch** button at the bottom of the screen.
5. **To Arm the Security Camera:**
   - Scroll down and check the **"Remote Cam"** box. Your browser will ask for Camera Permissions. Tap **Allow**.
   - Check the **"Motion Detect"** box if you want the phone to automatically snap and send photos when it sees movement.
   - **Leave the phone plugged in, with the screen ON, resting on this webpage.** 

---

## Testing the System

### 🎙️ Voice & Text Chat
From your **Main Phone** (using the Android App), type a message in the **Text** tab or record audio in the **Voice** tab. It will instantly beam over LoRa to the Security Camera phone! (Max limit is 30 seconds of audio per transmission).

### 🖼️ Remote Image Request
From your **Main Phone**, go to the **Image** tab and press the **Request Remote** button.
1. The Android app sends a `REQUEST_IMAGE` packet over LoRa.
2. The Remote Phone receives it.
3. The Web Browser on the Remote Phone automatically snaps a photo using the live camera feed.
4. It compresses the photo into a highly efficient grayscale image and sends it back over LoRa.
5. Your Android App will automatically pop up a notification saying **"New Image Received!"** and display the photo!

> **WARNING:** LoRa bandwidth is extremely small. Do not try to send Voice and Images at the exact same time, as the packets will collide in the air! Wait for one transmission to finish before starting another.
