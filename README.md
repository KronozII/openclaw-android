# OpenClaw Android

**On-device AI assistant · No cloud · Sovereign permissions · Sentinel security**

---

## Architecture Overview

```
┌─────────────────────────────────────────────────┐
│                    UI LAYER                      │
│  ChatScreen  PermissionsScreen  SandboxScreen    │
└─────────────────────────────────────────────────┘
         │                │               │
┌────────▼────────┐  ┌────▼───────┐  ┌───▼────────────┐
│  PrimaryAgent   │  │PermVault   │  │SandboxBuilder  │
│  (MediaPipe LLM)│  │(AllowList) │  │Agent           │
└────────┬────────┘  └────────────┘  └────────────────┘
         │
┌────────▼─────────────────────────────────────────┐
│              SENTINEL AGENT (isolated)            │
│  Reads audit log only. Cannot be instructed by   │
│  Primary. Freezes agent on threat detection.     │
└──────────────────────────────────────────────────┘
         │
┌────────▼─────────────────────────────────────────┐
│           STORAGE LAYER (SQLCipher)               │
│  scope_tokens  audit_log  chat_messages  sandbox  │
└──────────────────────────────────────────────────┘
         │
┌────────▼─────────────────────────────────────────┐
│       NETWORK LAYER (AllowList enforced)          │
│  OkHttp NetworkInterceptor blocks all requests   │
│  not in PermissionVault. No exceptions.          │
└──────────────────────────────────────────────────┘
```

## Key Design Decisions

### 1. Cryptographic ScopeTokens
Every permission grant is HMAC-signed via Android Keystore. Agent code **cannot forge tokens**. 
The only way to get a token is for the user to tap "Allow" in the permission sheet.

### 2. Isolated Sentinel Agent
Runs on a separate thread pool (`newSingleThreadContext("sentinel-monitor")`).
Has no shared channels with PrimaryAgent — it reads **only** the audit log stream.
On threat: FREEZE → REVOKE → SURFACE → WAIT.

### 3. AllowList Network Interceptor
Installed as an OkHttp `NetworkInterceptor` (fires before any caching).
Every outgoing request must have a valid `ScopeToken` for its host, or it returns 403.
This is OS-level enforcement, not advisory.

### 4. Zero Egress Default
The app ships with all network blocked. Users must grant per-domain permission.
The model runs entirely on-device via MediaPipe LLM Inference API.

### 5. Sandbox Isolation
The creation sandbox runs in a WebView with:
- `domStorageEnabled = false`
- `databaseEnabled = false`
- `allowFileAccess = false`
- No cookies, no cross-origin
- Its own sandbox-scoped permission sub-layer

---

## Setup

### Prerequisites
- Android Studio Ladybug (2024.2.1) or newer
- Android device with 4GB+ RAM (for Gemma 2B)
- Android 9 (API 28) minimum

### Model Download
Download one of these models and place in `/data/data/com.openclaw.android/files/models/`:

| Model | Size | RAM | Speed |
|-------|------|-----|-------|
| gemma-2b-it-gpu-int4.bin | ~1.5GB | 2GB | Fast (GPU) |
| gemma-2b-it-cpu-int4.bin | ~1.5GB | 2GB | Slower (CPU) |
| phi-3-mini-4k-instruct.bin | ~1.8GB | 1GB | Medium |

Models: https://www.kaggle.com/models/google/gemma/frameworks/tfLite

### Build
```bash
./gradlew assembleDebug
adb install app/build/outputs/apk/debug/app-debug.apk
```

---

## File Structure

```
app/src/main/java/com/openclaw/android/
├── agent/
│   ├── primary/PrimaryAgent.kt        # On-device inference, freeze/unfreeze
│   ├── sentinel/SentinelAgent.kt      # Isolated security monitor
│   └── sandbox/SandboxBuilderAgent.kt # IDE + code generation
├── permission/
│   ├── ScopeToken.kt                  # Token model + HMAC signer
│   └── PermissionVault.kt             # Central permission store
├── network/
│   └── AllowListInterceptor.kt        # Blocks unauthorized requests
├── storage/
│   ├── db/                            # Room DAOs + SQLCipher DB
│   └── models/                        # Entities
├── ui/
│   ├── screens/                       # Chat, Permissions, Sandbox, Settings
│   ├── components/                    # PermissionSheet, ThreatBanner
│   ├── theme/                         # OpenClaw design system
│   └── viewmodel/                     # MVVM layer
├── AppModule.kt                       # Hilt DI
├── OpenClawApplication.kt             # App entry
└── ui/MainActivity.kt                 # Navigation host
```

---

## Sentinel Threat Taxonomy

| Threat | Detection | Response |
|--------|-----------|----------|
| Scope Creep | Request to ungranated domain | FREEZE + ALERT |
| Prompt Injection | Regex patterns in input | BLOCK + LOG |
| Exfiltration | Payload > 50KB outbound | FREEZE + REVOKE ALL |
| Background Op | Action without active session | DENY + LOG |
| Permission Replay | Expired/revoked token used | DENY + ALERT |
| Autonomous Social | Social platform access | FREEZE + ALERT |
| Rapid Requests | > 10 permission requests/min | FREEZE + ALERT |

---

## Phase Roadmap

- [x] Phase 1: Skeleton (Kotlin + Compose + MediaPipe + Room + OkHttp AllowList)
- [x] Phase 2: Permission Layer (ScopeTokens + Vault + Dashboard)
- [x] Phase 3: Sentinel Agent (Isolated monitor + threat protocol)
- [x] Phase 4: Sandbox IDE (WebView + CodeMirror + AI assist + export)
- [ ] Phase 5: Model downloader UI + progress
- [ ] Phase 6: llama.cpp JNI integration for power users
- [ ] Phase 7: Capacitor APK export pipeline
- [ ] Phase 8: Sandbox CodeMirror 6 WebView bridge (replace TextField editor)

---

## Privacy Guarantee

This app:
- Makes **zero network calls** without explicit user permission
- Stores all data encrypted with SQLCipher (key in Android Keystore)
- Has **no telemetry**, no crash reporting, no analytics
- **Never backs up** to cloud (data extraction rules enforce this)
- Cannot be silently updated (no auto-update mechanism)
