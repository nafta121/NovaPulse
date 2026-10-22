# 🌟 NowBrief (NovaPulse)
> **Personalized Context Briefing Assistant & On-Device AI styled after Samsung One UI**

[![Android](https://img.shields.io/badge/Android-8.0%20to%2016+-3DDC84.svg?logo=android)](https://android.com)
[![One UI 9](https://img.shields.io/badge/Design-Samsung%20One%20UI%209-6750A4.svg)](https://samsung.com)
[![Privacy First](https://img.shields.io/badge/Privacy-100%25%20On--Device-success.svg)](https://android.com)
[![APK Size](https://img.shields.io/badge/APK%20Size-~28.9%20MB-blue.svg)](https://android.com)

---

## 📖 Overview

**NowBrief** is a next-generation smart context briefing and daily rhythm management app for Android. It combines **On-Device Machine Learning (100% privacy-first)** with the **Samsung One UI** design language to deliver a comprehensive, personalized experience that accompanies you 24 hours a day.

```mermaid
graph LR
    %% Input Data Sources
    subgraph Input_Sources ["Input Data Sources"]
        A[Sensors & SDKs: Weather, Calendar, Health]
        C[Notification Listener: Messaging, Banking, OTT]
        I[System Context: Lock Screen, Battery, System Time]
    end

    %% Central Processing Layer
    subgraph Core_Engine ["NowBrief Core Engine"]
        B[On-Device AI & ML Engine]
        R[Rule Engine & 7 Briefing Sessions]
    end

    %% UI Presentation Layer
    subgraph UI_Outputs ["One UI Presentation Layer"]
        E[Time-Adaptive Dashboard]
        F[Dynamic Capsule / Mini Player]
        G[Now Nudge 1-Tap Actions]
        H[Lockscreen Overlay & Glance Widget]
    end

    %% Data Flow Connections
    A --> Core_Engine
    C --> Core_Engine
    I --> Core_Engine
    
    B <--> R
    
    Core_Engine --> E
    Core_Engine --> F
    Core_Engine --> G
    Core_Engine --> H

    %% Highlight Color Styling
    classDef inputStyle fill:#e0f2fe,stroke:#0284c7,stroke-width:1px,color:#0369a1;
    classDef coreStyle fill:#ede9fe,stroke:#7c3aed,stroke-width:2px,color:#5b21b6;
    classDef uiStyle fill:#f0fdf4,stroke:#16a34a,stroke-width:1px,color:#15803d;

    class A,C,I inputStyle;
    class B,R coreStyle;
    class E,F,G,H uiStyle;
```

---


<img width="200" height="400" alt="image" src="https://github.com/user-attachments/assets/afce790b-eca3-43b7-8d02-4f6e87d4d579" />
<img width="200" height="400" alt="image" src="https://github.com/user-attachments/assets/b7a99a57-0e53-4d28-805f-8a367b3e6787" />
<img width="200" height="400" alt="image" src="https://github.com/user-attachments/assets/47e53456-788d-45db-ab8c-dfc081dd85d4" />
<img width="200" height="400" alt="image" src="https://github.com/user-attachments/assets/1d23aae3-6b31-48e7-abf8-86e221ffa1f6" />
<img width="200" height="400" alt="image" src="https://github.com/user-attachments/assets/04cc468a-1883-4fd2-9c97-a14a28f4f82f" />
<img width="200" height="400" alt="image" src="https://github.com/user-attachments/assets/c7a32a26-9c3d-43f8-942c-169b0a37c545" />


<br>

## Widget

<img width="200" height="100" alt="image" src="https://github.com/user-attachments/assets/ed9706a0-f74d-46a6-afef-e46a6a4e0f64" />
<img width="200" height="200" alt="image" src="https://github.com/user-attachments/assets/a04ee3ad-32db-4b61-ac61-26df41168b2f" />
<img width="300" height="200" alt="image" src="https://github.com/user-attachments/assets/9dd14b34-4eb3-44f0-90b5-320ef1bf308d" />

<br>

## Pill

<img width="340" height="100" alt="image" src="https://github.com/user-attachments/assets/90854814-329c-4ee2-8caf-17ad0a28d7f8" />



(** Images are for illustration purposes only; certain visual details may adapt depending on the target device **)

## 📥 APK Installation Guide

### 1. Download and Install:
1. Download **`NowBrief-v1.3.0-release.apk`** to your Android phone.
2. Open the downloaded APK file and tap **Install**.
3. If a security prompt appears:
   - **"Install unknown apps"**: Select **Allow from this source**.
   - **Google Play Protect**: Since this is a direct sideloaded build (not yet published on Google Play), simply select **Install anyway**.

### 2. Quick Feature Setup:
- **Grant Notification Access**: Allow NowBrief to read notifications to automatically detect schedules and expenses.
- **Grant Location Permission**: Fetch accurate hourly local weather from Open-Meteo.
- **Add Widget**: Go to your Home Screen -> Long press -> Select **Widgets** -> Choose **NowBrief** to place the frosted-glass widget on your home screen.

---

## ✨ Key Features

### 🌅 1. 7-Session Diurnal Phase Engine
- Automatically detects and transitions blurred background gradients, greetings, and color palettes across 7 distinct phases of the day:
  - **Dawn** (05:00 - 07:00)
  - **Early Morning** (07:00 - 09:00)
  - **Midday** (11:30 - 13:30)
  - **Afternoon** (14:00 - 17:00)
  - **Golden Hour** (17:00 - 18:30)
  - **Evening** (18:30 - 22:00)
  - **Late Night** (22:00 - 05:00)

### 🧠 2. On-Device AI & Machine Learning
- **Lifestyle Preference Learner**: Applies the **Multi-Armed Bandit (UCB1)** algorithm to automatically optimize and rank content, dining, and music recommendations based on user interaction habits for each time slot.
- **Task Procrastination Predictor**: Analyzes task completion history to warn against procrastination risks and boost productivity.
- **Offline SLM (Small Language Model)**: Automatically extracts appointments, reminders, and expenses from SMS Banking, messaging, and Telegram notifications **without an internet connection and without sending data to the cloud**.

### 🎵 3. Capsule Mini Player (YouTube & Spotify)
- Integrated One UI-style Capsule Mini Player.
- **1-Tap Play / Pause** controls with spring physics and Haptic Feedback.
- **Dynamic Audio Waveform** and smooth spinning vinyl disc rendered on the GPU layer for maximum battery efficiency.

### 🎯 4. Now Nudge - 1-Tap Proactive Actions
- Delivers proactive action suggestions tailored to your current context:
  - *Morning*: Check weather, commute route, listen to voice Audio Brief.
  - *Afternoon*: Hydration reminders, 2-minute desk stretches, activate Deep Focus mode.
  - *Evening*: Mindful wind-down, daily spending recap, bedtime alarm setup.

### 🏝️ 5. NowPill & Samsung Glance Widget
- **NowPill**: Smart floating information pill at the top or bottom of the screen with customizable time-of-day themes and status indicators in Settings.
- **Glance App Widget**: Jetpack Glance home screen widget that automatically syncs every 15 minutes via WorkManager.

---

## 🎨 Design System

- **Liquid Glassmorphism**: Premium frosted-glass surfaces with Specular Highlight Borders, multi-layered elevation shadows, and Ambient Glow Orbs.
- **Smooth Micro-Animations**: Optimized for 120 FPS rendering on high-refresh-rate displays via `graphicsLayer`.
- **Dynamic Light / Dark Theme**.

---

## 🛡️ App Permissions

| Permission | Purpose |
| :--- | :--- |
| `Notification Access` | Locally summarize events, ticket codes, and bank balance updates. |
| `Calendar` | Display and manage personal calendar events on smart cards. |
| `Location` | Fetch local weather updates from the Open-Meteo API. |
| `Sensors & Activity` | Count daily steps and track active energy throughout the day. |
| `Display Over Other Apps (Overlay)` | Render the Dynamic Island (NowBrief Pill) at the top or bottom of the screen (optional). |

---

## 📱 Compatibility & Performance

- **Supported Devices**: Runs smoothly across **Samsung Galaxy (One UI 5/6/7/8/9)**, **Xiaomi / POCO (HyperOS)**, **Google Pixel**, **Oppo / Realme (ColorOS)**, and **Vivo (OriginOS/FuntouchOS)**.
- **Android Versions**: Android 8.0 (Oreo) through the latest Android 16.
- **Performance & Battery**: GPU-accelerated rendering, zero background battery drain, and ultra-lightweight RAM footprint (~260 MB).

---

## 📄 License
Developed for educational, research, and personalized On-Device AI application development purposes.
Copyright © 2026 NowBrief / NovaPulse. All rights reserved.
