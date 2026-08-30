# 🌟 NowBrief (NovaPulse)
> **Trợ lý tóm tắt ngữ cảnh cá nhân hóa & AI On-Device chuẩn phong cách Samsung One UI 9 Liquid Glassmorphism**

[![Android](https://img.shields.io/badge/Android-8.0%20to%2016+-3DDC84.svg?logo=android)](https://android.com)
[![One UI 9](https://img.shields.io/badge/Design-Samsung%20One%20UI%209-6750A4.svg)](https://samsung.com)
[![Privacy First](https://img.shields.io/badge/Privacy-100%25%20On--Device-success.svg)](https://android.com)
[![APK Size](https://img.shields.io/badge/APK%20Size-~28.9%20MB-blue.svg)](https://android.com)

---

## 📖 Giới thiệu (Overview)

**NowBrief** là ứng dụng tóm tắt ngữ cảnh thông minh và quản lý nhịp sống thế hệ mới dành cho Android. Ứng dụng kết hợp giữa **Học máy cục bộ (On-Device Machine Learning - 100% bảo mật riêng tư)** và ngôn ngữ thiết kế **Samsung One UI 9 Liquid Glassmorphism** để mang đến trải nghiệm cá nhân hóa toàn diện, đồng hành cùng bạn suốt 24 giờ trong ngày.

```mermaid
graph LR
    %% Nguồn dữ liệu đầu vào
    subgraph Input_Sources ["Nguồn Dữ Liệu Đầu Vào"]
        A[Cảm biến & SDK: Thời tiết, Lịch trình, Sức khỏe]
        C[Notification Listener: Zalo, Banking, OTT]
        I[System Context: Màn hình khóa, PIN, Giờ hệ thống]
    end

    %% Tầng xử lý trung tâm
    subgraph Core_Engine ["NowBrief Core Engine"]
        B[Bộ máy AI & ML On-Device]
        R[Rule Engine & 7 Khung Giờ Briefing]
    end

    %% Tầng hiển thị giao diện
    subgraph UI_Outputs ["Giao diện Hiển thị One UI"]
        E[Dashboard thích ứng theo giờ]
        F[Dynamic Capsule / Mini Player]
        G[Now Nudge Gợi ý 1-chạm]
        H[Lockscreen Overlay & Glance Widget]
    end

    %% Luồng liên kết
    A --> Core_Engine
    C --> Core_Engine
    I --> Core_Engine
    
    B <--> R
    
    Core_Engine --> E
    Core_Engine --> F
    Core_Engine --> G
    Core_Engine --> H

    %% Styling màu sắc cho nổi bật
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



(** Hình ảnh chỉ mang tính minh họa có vài đặc điểm sẽ thay đổi để phù hợp hơn với một số thiết bị **)

## 📥 Hướng Dẫn Cài Đặt File APK (Installation Guide)

### 1. Tải về và cài đặt:
1. Tải file **`NowBrief-v1.1.2-release.apk`** về điện thoại Android của bạn.
2. Mở file APK vừa tải và bấm **Cài đặt (Install)**.
3. Nếu máy xuất hiện thông báo bảo mật:
   - **"Cài đặt từ nguồn không xác định"**: Chọn **Cho phép từ nguồn này (Allow from this source)**.
   - **Google Play Protect**: Do đây là bản cài trực tiếp (chưa đăng tải lên CH Play), bạn chỉ cần chọn **Vẫn cài đặt (Install anyway)**.

### 2. Thiết lập nhanh các tính năng:
- **Cấp quyền thông báo**: Cho phép NowBrief đọc thông báo để tự động nhận diện lịch hẹn & chi tiêu.
- **Cấp quyền vị trí**: Cập nhật thời tiết theo giờ chính xác từ Open-Meteo.
- **Thêm Widget**: Ra màn hình chính -> Giữ ngón tay -> Chọn **Widget** -> Chọn **NowBrief** để đưa widget kính mờ ra màn hình chính.

---

## ✨ Các Tính Năng Nổi Bật (Key Features)

### 🌅 1. Động cơ Nhịp Sinh Học 7 Khung Giờ (Diurnal Phase Engine)
- Tự động nhận diện và chuyển đổi gradient nền mờ, lời chào và màu sắc theo 7 buổi trong ngày:
  - **Bình minh (Dawn)** (05:00 - 07:00)
  - **Sáng sớm (Early Morning)** (07:00 - 09:00)
  - **Buổi trưa (Midday)** (11:30 - 13:30)
  - **Buổi chiều (Afternoon)** (14:00 - 17:00)
  - **Hoàng hôn (Golden Hour)** (17:00 - 18:30)
  - **Buổi tối (Evening)** (18:30 - 22:00)
  - **Đêm muộn (Late Night)** (22:00 - 05:00)

### 🧠 2. Trí Tuệ Nhân Tạo & Học Máy Cục Bộ (On-Device AI & ML)
- **Lifestyle Preference Learner**: Ứng dụng thuật toán **Multi-Armed Bandit (UCB1)** tự động tối ưu hóa và xếp hạng các gợi ý nội dung, ẩm thực, âm nhạc dựa trên thói quen bấm của người dùng theo từng khung giờ.
- **Task Procrastination Predictor**: Phân tích lịch sử hoàn thành công việc để cảnh báo nguy cơ trì hoãn và thúc đẩy năng suất.
- **Offline SLM (Small Language Model)**: Tự động trích xuất lịch hẹn, nhắc nhở và chi tiêu từ thông báo SMS Banking, Zalo, Telegram mà **không cần kết nối internet và không gửi dữ liệu lên đám mây**.

### 🎵 3. Thanh Capsule Mini Player (YouTube & Spotify)
- Tích hợp Mini Player dạng viên con nhộng (Capsule) chuẩn One UI.
- Thao tác **Play / Pause 1-chạm** với hiệu ứng nảy (spring physics) và rung phản hồi (Haptic Feedback).
- **Thanh sóng nhạc động (Dynamic Audio Waveform)** và biểu tượng đĩa vinyl xoay mượt mà chạy trên GPU layer, tiết kiệm pin tối đa.

### 🎯 4. Now Nudge - Đề Xuất Hành Động 1-Chạm
- Đưa ra các gợi ý hành động chủ động dựa trên bối cảnh hiện tại:
  - *Sáng*: Kiểm tra thời tiết, lộ trình di chuyển, nghe Audio Brief giọng đọc.
  - *Chiều*: Nhắc nhở uống nước, giãn cơ 2 phút, kích hoạt chế độ Deep Focus.
  - *Tối*: Thư giãn chánh niệm, tóm tắt chi tiêu trong ngày, hẹn giờ đi ngủ.

### 🏝️ 5. NowPill & Samsung Glance Widget
- **NowPill**: Thanh thông tin nổi thông minh trên đỉnh hay dưới hay màn hình tùy chọn hiển thị theme theo ngày giờ hoặc có thể các trạng thái khác trong cài đặt.
- **Glance App Widget**: Widget màn hình chính chuẩn Jetpack Glance đồng bộ tự động 15 phút/lần theo chu kỳ WorkManager.

---

## 🎨 Ngôn Ngữ Thiết Kế (Design System)

- **Liquid Glassmorphism**: Nền kính mờ cao cấp với Specular Highlight Border, đổ bóng đa tầng và Ambient Glow Orbs.
- **Micro-Animations Mượt mà**: Tối ưu hóa render 120 FPS trên màn hình tần số quét cao thông qua `graphicsLayer`.
- **Chế độ Sáng / Tối Thích Ứng (Dynamic Light / Dark Theme)**.

---

## 🛡️ Quyền Ứng Dụng (Permissions)

| Quyền | Mục đích sử dụng |
| :--- | :--- |
| `Truy cập Thông báo (Notification Access)` | Tự động tóm tắt sự kiện, mã vé, biến động số dư ngân hàng cục bộ. |
| `Lịch trình (Calendar)` | Hiển thị và quản lý lịch sự kiện cá nhân trên thẻ thông minh. |
| `Vị trí (Location)` | Cập nhật thời tiết địa phương từ Open-Meteo API. |
| `Cảm biến & Hoạt động (Sensors)` | Đếm số bước chân, theo dõi năng lượng hoạt động trong ngày. |
| `Hiển thị trên ứng dụng khác (Overlay)` | Hiển thị giao diện Dynamic Island(Pill Nowbrief) trên đỉnh màn hình hay phần dưới màn hình (tùy chọn). |

---

## 📱 Khả Năng Tương Thích & Hiệu Năng (Compatibility)

- **Thiết bị hỗ trợ**: Chạy tốt trên tất cả các dòng máy **Samsung Galaxy (One UI 5/6/7/8/9)**, **Xiaomi / POCO (HyperOS)**, **Google Pixel**, **Oppo / Realme (ColorOS)**, **Vivo (OriginOS/FuntouchOS)**.
- **Phiên bản Android**: Android 8.0 (Oreo) đến Android 16 mới nhất.
- **Hiệu năng & Pin**: Tối ưu hóa GPU, không chạy ngầm gây hao pin, bộ nhớ RAM chiếm dụng cực nhẹ (~260 MB).

---

## 📄 Bản Quyền & Thông Tin (License)
Dự án được phát triển phục vụ mục đích học tập, nghiên cứu và xây dựng ứng dụng AI On-Device cá nhân hóa.
Copyright © 2026 NowBrief / NovaPulse. All rights reserved.
