# 📱 Fleet Manager (ফ্লিট ম্যানেজার) 🚀

> **Enterprise Device Diagnostics, Real-time Telemetry, Security Logs & Fleet Monitoring App**  
> এন্টারপ্রাইজ ফ্লিট ম্যানেজমেন্ট, ডিভাইস ডায়াগনস্টিকস, রিয়েল-টাইম টেলিমেট্রি এবং সিকিউরিটি মনিটরিংয়ের জন্য আধুনিক অ্যান্ড্রয়েড অ্যাপ্লিকেশন।

---

![Android](https://img.shields.io/badge/Platform-Android-3DDC84?style=for-the-badge&logo=android&logoColor=white)
![Kotlin](https://img.shields.io/badge/Language-Kotlin_2.0-7F52FF?style=for-the-badge&logo=kotlin&logoColor=white)
![Jetpack Compose](https://img.shields.io/badge/UI-Jetpack_Compose_M3-4285F4?style=for-the-badge&logo=jetpackcompose&logoColor=white)
![Firebase](https://img.shields.io/badge/Cloud-Firebase_Firestore_&_Auth-FFCA28?style=for-the-badge&logo=firebase&logoColor=black)
![Socket.IO](https://img.shields.io/badge/Network-Socket.io_Client-010101?style=for-the-badge&logo=socketdotio&logoColor=white)

---

## 📖 বিষয়বস্তু (Table of Contents)

1. [ভূমিকা (Overview)](#-ভূমিকা-overview)
2. [মূল বৈশিষ্ট্যসমূহ (Key Features)](#-মূল-বৈশিষ্ট্যসমূহ-key-features)
3. [সিস্টেম আর্কিটেকচার (System Architecture)](#-সিস্টেম-আর্কিটেকচার-system-architecture)
4. [ব্যবহৃত প্রযুক্তি ও লাইব্রেরি (Tech Stack)](#-ব্যবহৃত-প্রযুক্তি-ও-লাইব্রেরি-tech-stack)
5. [প্রজেক্টের ফাইল স্ট্রাকচার (Project Structure)](#-প্রজেক্টের-ফাইল-স্ট্রাকচার-project-structure)
6. [ইনস্টলেশন ও রান করার নিয়ম (Getting Started)](#-ইনস্টলেশন-ও-রান-করার-নিয়ম-getting-started)
7. [ফায়ারবেস ও সিকিউরিটি (Firebase & Security)](#-ফায়ারবেস-ও-সিকিউরিটি-firebase--security)
8. [টেস্টিং গাইড (Testing & Verification)](#-টেস্টিং-গাইড-testing--verification)
9. [গিটহাব অ্যাকশন্স থেকে এপিকে তৈরি (GitHub Actions CI/CD & APK)](#-গিটহাব-অ্যাকশন্স-থেকে-এপিকে-তৈরি-github-actions-cicd--apk)
10. [পারমিশন ও গোপনীয়তা (Permissions & Privacy)](#-পারমিশন-ও-গোপনীয়তা-permissions--privacy)
11. [লাইসেন্স ও অবদান (License & Contributing)](#-লাইসেন্স-ও-অবদান-license--contributing)

---

## 🌟 ভূমিকা (Overview)

**Fleet Manager** হলো একটি আধুনিক, স্কেলেবল এবং এন্টারপ্রাইজ গ্রেডের অ্যান্ড্রয়েড অ্যাপ্লিকেশন। এটি বিভিন্ন ডিভাইসের লাইভ স্ট্যাটাস, হার্ডওয়্যার ডায়াগনস্টিকস (ব্যাটারি, র‍্যাম, সিপিইউ ব্যবহার), জিপিএস ট্র্যাকিং, জিওফেন্সিং পলিসি এবং নিরাপত্তা সংক্রান্ত অডিট লগ পর্যবেক্ষণ করার জন্য নির্মিত। 

অ্যাপটি গুগল ক্লাউডের আধুনিক প্রযুক্তি **Firebase Firestore**-এর মাধ্যমে রিয়েল-টাইম স্ন্যাপশট ডেটা সিঙ্ক করে এবং **Jetpack Credential Manager**-এর মাধ্যমে নির্ভরযোগ্য ও নিরাপদ গুগল সাইন-ইন সমর্থন করে।

---

## ⚡ মূল বৈশিষ্ট্যসমূহ (Key Features)

### 1. 🔐 নিরাপদ গুগল প্রমাণীকরণ (Secure Google Sign-In)
- অ্যান্ড্রয়েডের লেটেস্ট **Jetpack Credential Manager** (`GetSignInWithGoogleOption`) ব্যবহার করে তৈরি।
- লাইফসাইকেল-অ্যাওয়ার করুটিন স্কোপিং (`rememberCoroutineScope` ও `authStateFlow`) এর মাধ্যমে নিরবচ্ছিন্ন সেশন হ্যান্ডলিং।
- অ্যাডমিনিস্ট্রেটিভ প্রিভিলেজ ও রোল-বেসড অ্যাক্সেস কন্ট্রোল।

### 2. 📊 রিয়েল-টাইম ফ্লিট মনিটরিং (Live Device Telemetry)
- প্রতিটি ডিভাইসের লাইভ স্ট্যাটাস: `Online`, `Offline`, `Warning`।
- হার্ডওয়্যার রিসোর্স মনিটরিং:
  - 🔋 ব্যাটারি লেভেল এবং চার্জিং স্টেট
  - ⚙️ সিপিইউ ইউটিলাইজেশন শতাংশ (CPU Load %)
  - 💾 র‍্যাম মেমোরি ব্যবহার এবং ক্যাপাসিটি (RAM Usage / Total MB)
  - 📍 সর্বশেষ অবস্থান (Latitude, Longitude)
  - 🛡️ এন্টারপ্রাইজ কমপ্লায়েন্স স্ট্যাটাস (`compliant`, `violation`, `pending`)

### 3. 🛡️ সিকিউরিটি অডিট ও অ্যালার্টস (Security Audit Logs)
- সিস্টেমে অননুমোদিত অ্যাক্সেস বা নিয়মলঙ্ঘনের সম্পূর্ণ হিস্ট্রি লগ।
- সেভিয়ারিটি ফিল্টার: `Critical`, `Warning`, `Info`।
- সার্চ এবং ইনস্ট্যান্ট রিয়েল-টাইম স্ন্যাপশট ফিল্টারিং।

### 4. 📍 জিওফেন্সিং জোন ব্যবস্থাপনা (Geofence Zones)
- ভার্চুয়াল ভৌগোলিক সীমানা নির্ধারণ এবং মনিটরিং।
- সক্রিয় ও নিষ্ক্রিয় জোন ম্যানেজমেন্ট।
- রেডিয়াস (মিটার) এবং কোঅর্ডিনেট অনুযায়ী ডিভাইস ট্র্যাকিং।

### 5. 📡 বাইডাইরেকশনাল রিয়েল-টাইম সিগন্যালিং (Socket.IO Integration)
- ডেডিকেটেড `SocketManager` যা টোকেন-বেসড হ্যান্ডশেক ও রিকানেক্ট সাপোর্ট করে।
- ব্যাকগ্রাউন্ড নেটওয়ার্ক ইভেন্ট পর্যবেক্ষণ করতে রিঅ্যাক্টিভ `Flow` স্ট্রিম (`onEventFlow`)।
- লো-লেটেন্সি কমান্ডিং ও ডায়াগনস্টিক সিগন্যাল।

### 6. 📷 রিমোট ক্যামেরা ও নজরদারি (Remote Camera & Optical Surveillance)
- ফ্লিট ডিভাইসে দূরবর্তী ক্যামেরা ফ্রেম ক্যাপচার (`Remote Snapshot Trigger`) কমান্ড।
- ফ্রন্ট ও ব্যাক লেন্স স্যুইচিং (`CameraSelector`) এবং ফ্ল্যাশলাইট/টর্চ কন্ট্রোল (`toggleTorch`)।
- লাইভ ভিউফাইন্ডার প্রিভিউ (`CameraX PreviewView` + `AndroidView`) এবং ফ্রেম অডিট ডায়াগনস্টিকস।
- সারভেইল্যান্স স্ন্যাপশট গ্যালারি, ফুল-প্রিভিউ ডায়ালগ এবং টাইমস্ট্যাম্পযুক্ত অডিট লগ রেকর্ড।

### 7. 🎨 মেটেরিয়াল ৩ ইন্টারফেস (Material 3 UI)
- ডায়নামিক লাইট ও ডার্ক থিম সাপোর্ট।
- কাস্টম ডায়ালগ (`DeviceDetailDialog`, `GeofenceDialog`, `SnapshotDetailDialog`)।
- ফ্লুইড ট্রানজিশন ও অ্যাক্সেসিবিলিটি ফ্রেন্ডলি টাচ-টার্গেট।

---

## 🏗️ সিস্টেম আর্কিটেকচার (System Architecture)

অ্যাপ্লিকেশনটি গুগল প্রস্তাবিত **MVVM (Model-View-ViewModel)** এবং **Clean Architecture** নীতি অনুসরণ করে তৈরি:

```text
       ┌─────────────────────────────────────────────────────┐
       │                Jetpack Compose UI                   │
       │    (FleetHomeScreen, AuthScreen, M3 Dialogs)       │
       └─────────────────────────▲───────────────────────────┘
                                 │ StateFlow / UI Actions
       ┌─────────────────────────▼───────────────────────────┐
       │                     ViewModels                      │
       │          (FleetViewModel, AuthViewModel)            │
       └─────────────────────────▲───────────────────────────┘
                                 │ Coroutines / Kotlin Flow
       ┌─────────────────────────▼───────────────────────────┐
       │                    Repositories                     │
       │    (FleetRepository, DeviceRepository, AuthManager) │
       └─────────────────────────▲───────────────────────────┘
                                 │
              ┌──────────────────┴──────────────────┐
              ▼                                     ▼
   ┌───────────────────────┐             ┌───────────────────────┐
   │   Firebase Firestore  │             │   Socket.IO Server    │
   │  & Cloud Auth System  │             │  (Signaling Engine)   │
   └───────────────────────┘             └───────────────────────┘
```

---

## 🛠️ ব্যবহৃত প্রযুক্তি ও লাইব্রেরি (Tech Stack)

| কম্পোনেন্ট | প্রযুক্তি / লাইব্রেরি | সংস্করণ / বিবরণ |
|---|---|---|
| **Programming Language** | Kotlin | আধুনিক Coroutines ও StateFlow সহ |
| **UI Framework** | Jetpack Compose | Material 3 (M3) ডিজাইন সিস্টেম |
| **Authentication** | Jetpack Credential Manager + Firebase Auth | বায়োমেট্রিক ও ওয়ান-ট্যাপ গুগল সাইন-ইন |
| **Cloud Database** | Firebase Cloud Firestore | রিয়েল-টাইম স্ন্যাপশট লিসেনার ও জিরো-ট্রাস্ট রুলস |
| **Networking** | Socket.io-client, Retrofit, OkHttp | রিয়েল-টাইম ইভেন্ট ব্রডকাস্টিং |
| **Location Services** | Google Play Services Location | ফাইন ও কোরস জিপিএস ট্র্যাকিং |
| **Testing** | Robolectric, JUnit 4, Compose UI Test | লোকাল JVM টেস্ট ও CUJ ভেরিফিকেশন |
| **Build System** | Gradle (Kotlin DSL `.gradle.kts`) | Version Catalog (`libs.versions.toml`) সহ |

---

## 📁 প্রজেক্টের ফাইল স্ট্রাকচার (Project Structure)

```text
├── app/
│   ├── src/
│   │   ├── main/
│   │   │   ├── AndroidManifest.xml          # অ্যাপ পারমিশন ও কনফিগারেশন
│   │   │   ├── java/com/example/
│   │   │   │   ├── MainActivity.kt          # প্রধান অ্যাক্টিভিটি ও নেভিগেশন কন্ট্রোল
│   │   │   │   ├── auth/
│   │   │   │   │   └── AuthenticationManager.kt # গুগল সাইন-ইন ও অথ স্টেট
│   │   │   │   ├── data/
│   │   │   │   │   ├── model/               # ডেটা মডেল (Device, Geofence, SecurityLog)
│   │   │   │   │   ├── repository/          # ফায়ারবেস ডেটা রিপোজিটরি
│   │   │   │   │   └── util/
│   │   │   │   ├── location/
│   │   │   │   │   └── LocationPermissionHelper.kt # জিপিএস পারমিশন চেকার
│   │   │   │   ├── network/
│   │   │   │   │   └── SocketManager.kt     # Socket.io লাইফসাইকেল ও ইভেন্ট হ্যান্ডলার
│   │   │   │   └── ui/
│   │   │   │       ├── auth/                # সাইন-ইন স্ক্রিন কম্পোনেন্টস
│   │   │   │       ├── fleet/               # ফ্লিট ড্যাশবোর্ড, ফিল্টার ও ডায়ালগ
│   │   │   │       └── theme/               # কালার স্কিম ও টাইপোগ্রাফি
│   │   │   └── res/                         # লেআউট, ড্রয়েবল ও স্ট্রিংস রিসোর্স
│   │   └── test/                            # Robolectric ইউনিট ও ইন্টিগ্রেশন টেস্ট
│   ├── build.gradle.kts                     # অ্যাপ-লেভেল ডিপেন্ডেন্সি ও বিল্ড স্ক্রিপ্ট
│   └── firestore.rules                      # ফায়ারস্টোর ক্লাউড সিকিউরিটি রুলস
├── build.gradle.kts                         # প্রজেক্ট-লেভেল বিল্ড স্ক্রিপ্ট
├── metadata.json                            # AI Studio প্ল্যাটফর্ম মেটাডেটা
├── settings.gradle.kts                      # মডিউল সেটিংস
└── README.md                                # প্রজেক্ট ডকুমেন্টেশন
```

---

## 🚀 ইনস্টলেশন ও রান করার নিয়ম (Getting Started)

### পূর্বশর্ত (Prerequisites)
- **Android Studio Ladybug (2024.2.1)** বা তার পরবর্তী সংস্করণ
- **JDK 17** বা **JDK 21**
- অ্যান্ড্রয়েড ডিভাইস বা এমুলেটর (API 24+)

### প্রজেক্ট সেটআপ ধাপসমূহ:

1. **রিপোজিটরি ক্লোন করুন:**
   ```bash
   git clone <repository-url>
   cd <project-folder>
   ```

2. **এনভায়রনমেন্ট কনফিগারেশন (`.env`):**
   রুট ডিরেক্টরিতে `.env` ফাইল প্রস্তুত করুন (প্রয়োজনে `.env.example` ফলো করুন):
   ```env
   # API কী এবং কনফিগারেশন (প্রযোজ্য ক্ষেত্রে)
   ```

3. **প্রজেক্ট বিল্ড করুন:**
   ```bash
   gradle assembleDebug
   ```

4. **ডিভাইসে ইনস্টল ও রান করুন:**
   অ্যান্ড্রয়েড স্টুডিও থেকে `Run 'app'` বাটনে ক্লিক করুন অথবা কমান্ড লাইনে চালান।

---

## 🔒 ফায়ারবেস ও সিকিউরিটি (Firebase & Security)

অ্যাপ্লিকেশনটিতে ব্যবহৃত প্রতিটি ডেটা অ্যাক্সেসের নিরাপত্তা নিশ্চিত করতে জিরো-ট্রাস্ট পলিসি প্রয়োগ করা হয়েছে:

- **Security Rules (`firestore.rules`):**
  - শুধুমাত্র অথেনটিকেটেড ইউজাররা তাদের অধীনস্থ ফ্লিট ডিভাইসের রিড ও রাইট করতে পারেন।
  - প্রতিটি ডিভাইস অবজেক্টে `createdAt` এবং `updatedAt` অখণ্ডতা বজায় রাখা হয়।
  - ডিভাইস ও অডিট লগ ভ্যালিডেশন স্কিমার মাধ্যমে কঠোরভাবে নিয়ন্ত্রিত।

---

## 🧪 টেস্টিং গাইড (Testing & Verification)

প্রজেক্টে নির্ভরযোগ্যতা নিশ্চিত করতে লোকাল জেভিএম টেস্ট এবং রোবোলেক্ট্রিক সুইট অন্তর্ভুক্ত রয়েছে:

### টেস্ট রান করার কমান্ড:
```bash
# সব ইউনিট এবং রোবোলেক্ট্রিক টেস্ট রান করতে:
gradle :app:testDebugUnitTest
```

### প্রধান টেস্ট ফাইলসমূহ:
- `ExampleRobolectricTest.kt` - মূল ফ্রেমওয়ার্ক ইন্টিগ্রেশন টেস্ট
- `AuthViewModelTest.kt` - প্রমাণীকরণ এবং ইউজার স্টেট টেস্ট
---

## 🤖 গিটহাব অ্যাকশন্স থেকে এপিকে তৈরি (GitHub Actions CI/CD & APK)

এই রিপোজিটরিতে **GitHub Actions** ওয়ার্কফ্লো (`.github/workflows/build-apk.yml`) কনফিগার করা আছে, যার মাধ্যমে কোড পুশ করলেই স্বয়ংক্রিয়ভাবে ক্লাউডে অ্যান্ড্রয়েড এপিকে (APK) তৈরি হয়ে যায়।

### 🛠️ যেভাবে এপিকে তৈরি ও ডাউনলোড করবেন:

#### পদ্ধতি ১: অটোমেটিক বিল্ড (Push বা Pull Request)
1. আপনার কোড `main` বা `master` ব্রাঞ্চে `git push` করুন।
2. গিটহাব স্বয়ংক্রিয়ভাবে অ্যাকশন টাস্ক রান করে `app-debug.apk` তৈরি করবে।

#### পদ্ধতি ২: ম্যানুয়াল রান (GitHub Actions UI থেকে এক ক্লিকে)
1. আপনার GitHub রিপোজিটরি ব্রাউজারে খুলুন।
2. উপরের মেনু থেকে **"Actions"** ট্যাবে ক্লিক করুন।
3. বাম পাশের তালিকায় **"Build Android APK"** ওয়ার্কফ্লো নির্বাচন করুন।
4. ডানপাশে **"Run workflow"** ড্রপডাউনে ক্লিক করে বিল্ড টাইপ (`debug` / `release` / `all`) সিলেক্ট করুন এবং **"Run workflow"** বোতামে চাপুন।

#### 📥 এপিকে ডাউনলোড করার নিয়ম:
1. সম্পন্ন হওয়া বিল্ড রানে (সবুজ টিকচিহ্ন ✅) ক্লিক করুন।
2. পেজের নিচের দিকে স্ক্রোল করে **Artifacts** সেকশনে যান।
3. **`app-debug-apk`** (বা `app-release-apk`)-এ ক্লিক করলেই জিপ ফাইলে তৈরি করা এপিকে ডাউনলোড শুরু হবে। জিপটি আনজিপ করে আপনার অ্যান্ড্রয়েড ফোনে ইনস্টল করতে পারবেন!

#### 🏷️ রিলিজ এপিকে ও ট্যাগিং (Releases):
- যখনই আপনি কোনো গিট ট্যাগ পুশ করবেন (যেমন `git tag v1.0.0 && git push origin v1.0.0`), ওয়ার্কফ্লো সরাসরি গিটহাব **Releases** সেকশনে এপিকে ফাইল আপলোড করে প্রকাশ করবে।

---

## 🛡️ পারমিশন ও গোপনীয়তা (Permissions & Privacy)

অ্যাপ্লিকেশনের নির্বিঘ্ন সেবার জন্য নিম্নলিখিত অ্যান্ড্রয়েড পারমিশনসমূহ প্রয়োজন:
- `INTERNET` - ক্লাউড ডাটাবেস ও সার্ভার যোগাযোগের জন্য।
- `ACCESS_NETWORK_STATE` - নেটওয়ার্ক কানেক্টিভিটি পর্যবেক্ষণের জন্য।
- `ACCESS_FINE_LOCATION` & `ACCESS_COARSE_LOCATION` - ডিভাইসের সুনির্দিষ্ট জিপিএস অবস্থান নির্ণয় ও জিওফেন্সিংয়ের জন্য।

---

## 👥 অবদান ও সহায়তা (Contributing & Support)

যদি কোনো বাগ খুঁজে পান বা নতুন কোনো ফিচার যোগ করতে চান:
1. রিপোজিটরিতে একটি Issue খুলুন।
2. নতুন ফিচার ব্রাঞ্চ তৈরি করুন (`feature/amazing-feature`)।
3. কোড পুশ করে Pull Request তৈরি করুন।

---

<p align="center">
  <b>Fleet Manager</b> — Built with ❤️ using modern Android standards and Jetpack Compose.
</p>
