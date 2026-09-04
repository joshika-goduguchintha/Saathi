# 🤝 Saathi – AI-Powered Multilingual Voice Livelihood Assistant

> **Smart India Hackathon (SIH) Prototype**  
> Empowering SC/ST beneficiaries through AI-driven skill training, job matching, and livelihood roadmaps — in their native language.

---

## 📌 Problem Statement

Millions of SC/ST beneficiaries under PM-DAKSH and NBCFDC government schemes lack digital literacy and face language barriers when accessing skill training and employment resources. Existing portals are English-only, form-heavy, and inaccessible to rural populations.

**Saathi** bridges this gap with a voice-first, multilingual AI assistant that meets beneficiaries where they are — via phone call, push-to-talk voice notes, or a simple form.

---

## ✨ Key Features

| Feature | Description |
|---|---|
| 🎤 **AI Voice Onboarding** | 10-step conversational intake via simulated IVR, push-to-talk, or direct form |
| 🌐 **Multilingual Support** | Telugu, Tamil, Hindi, Kannada & English — auto-detected from speech/text |
| 🤖 **Gemini AI Chat** | Google Gemini 2.0 Flash powers intelligent, context-aware responses |
| 🎓 **NSQF Training Recommendations** | Personalized skill training matched from 8 NSQF-aligned programmes |
| 💼 **Job & Livelihood Matching** | Score-based matching to 7 local wage/self-employment opportunities |
| 🗺️ **6-Step Livelihood Roadmap** | Personalized career pathway with skill gap analysis |
| 📊 **Officer Dashboard** | Real-time pipeline monitoring with HITL (Human-in-the-Loop) alerts |
| 💾 **SQLite Persistence** | Zero-dependency local database via `saathi.db` |

---

## 🏗️ Architecture

```
sih-1/
├── Main.java          # Core engine: data models, recommendation algorithms,
│                      #   skill gap analysis, DB persistence, CLI menu
├── WebServer.java     # Embedded REST API server (port 8080):
│                      #   GeminiEngine, AiEngine, onboarding session handlers
├── web/
│   ├── index.html     # Single-page application UI
│   ├── app.js         # Frontend controller (voice, chat, onboarding, i18n)
│   └── styles.css     # Dark glassmorphism UI design system
├── lib/
│   └── sqlite-jdbc-3.53.2.0.jar   # SQLite JDBC driver (only dependency)
├── recordings/        # Runtime: audio recordings saved here
└── saathi.db          # Runtime: SQLite database (auto-created)
```

### Technology Stack

- **Backend** — Pure Java 21, zero frameworks, `com.sun.net.httpserver` embedded HTTP server
- **Database** — SQLite via JDBC (`saathi.db`)
- **AI** — Google Gemini 2.0 Flash API (via `java.net.http.HttpClient`), rule-based fallback
- **Frontend** — Vanilla HTML5 / CSS3 / JavaScript, Web Speech API (STT + TTS)
- **Voice** — Browser-native `SpeechRecognition` & `SpeechSynthesis` APIs

---

## 🚀 Quick Start

### Prerequisites

- **Java 21+** (JDK) — [Download](https://adoptium.net/)
- A `GEMINI_API_KEY` from [Google AI Studio](https://aistudio.google.com/)

### 1. Clone / Download

```bash
git clone https://github.com/your-username/sih-1.git
cd sih-1
```

### 2. Set your Gemini API Key

**Windows (PowerShell):**
```powershell
$env:GEMINI_API_KEY = "YOUR_API_KEY_HERE"
```

**Windows (persistent):**
```powershell
[System.Environment]::SetEnvironmentVariable("GEMINI_API_KEY", "YOUR_KEY", "User")
```

**Linux / macOS:**
```bash
export GEMINI_API_KEY="YOUR_API_KEY_HERE"
```

### 3. Compile

```powershell
javac -cp "lib/sqlite-jdbc-3.53.2.0.jar" Main.java WebServer.java
```

### 4. Run (Web Server Mode)

```powershell
# Windows
java -cp ".;lib/sqlite-jdbc-3.53.2.0.jar" Main

# Linux / macOS
java -cp ".:lib/sqlite-jdbc-3.53.2.0.jar" Main
```

The server starts at **http://localhost:8080** and opens your browser automatically.

### 5. Run (CLI Mode — optional)

```powershell
java -cp ".;lib/sqlite-jdbc-3.53.2.0.jar" Main --cli
```

---

## 🌐 REST API Reference

| Method | Endpoint | Description |
|---|---|---|
| `GET` | `/api/beneficiaries` | List all beneficiaries |
| `GET` | `/api/beneficiary?id=&lang=` | Beneficiary profile + localized labels |
| `POST` | `/api/register` | Register beneficiary (form data) |
| `POST` | `/api/beneficiary/direct` | Direct AI-voice registration |
| `GET` | `/api/beneficiary/{id}/roadmap` | Full 6-step roadmap JSON |
| `GET` | `/api/recommendations/training?id=` | NSQF training recommendations |
| `GET` | `/api/recommendations/jobs?id=` | Job opportunity recommendations |
| `GET` | `/api/roadmap?id=` | Roadmap summary |
| `POST` | `/api/update-status` | Update pipeline status |
| `GET` | `/api/dashboard` | Aggregate pipeline metrics |
| `GET` | `/api/officer/metrics` | Officer metrics + full pipeline array |
| `GET` | `/api/catalogs` | Training programmes + opportunities catalog |
| `POST` | `/api/ai-chat` | AI chat (Gemini-powered) |
| `POST` | `/api/onboarding/start` | Start voice onboarding session |
| `POST` | `/api/onboarding/transcribe-and-reply` | Advance session with answer |
| `POST` | `/api/onboarding/complete` | Finalize session → register beneficiary |
| `POST` | `/api/onboarding/session` | Unified start-or-advance endpoint |
| `POST` | `/api/recordings` | Upload audio recording |
| `GET` | `/api/recordings?file=` | Retrieve audio recording |

---

## 🎓 NSQF Training Programmes

| ID | Programme | NSQF Level | Duration | Region |
|---|---|---|---|---|
| TP101 | Digital Skills & Data Entry | 4 | 3 months | All |
| TP102 | Tailoring & Garment Entrepreneurship | 3 | 4 months | Rural |
| TP103 | Electrical Technician & Solar Maintenance | 4 | 6 months | Urban |
| TP104 | Food Processing & Small Enterprise | 3 | 3 months | Rural |
| TP105 | Computer Hardware Technician | 4 | 6 months | Urban |
| TP106 | Beauty & Wellness Entrepreneurship | 3 | 3 months | All |
| TP107 | Organic Farming & Agritech Management | 4 | 4 months | Rural |
| TP108 | Handicraft & Artisan Skills | 3 | 3 months | Rural |

---

## 📊 Beneficiary Pipeline Stages

```
Profile Created
    → Recommendation Generated
        → Enrolled
            → Training In Progress
                → Training Completed
                    → Placed  |  Self-Employment Started
                    
(Any stage) → Needs Officer Support  ← HITL Alert triggered
```

---

## 🗣️ Supported Languages

| Language | BCP-47 Code | Script |
|---|---|---|
| English | `en-IN` | Latin |
| Hindi | `hi-IN` | देवनागरी |
| Telugu | `te-IN` | తెలుగు |
| Tamil | `ta-IN` | தமிழ் |
| Kannada | `kn-IN` | ಕನ್ನಡ |

---

## 🔧 Configuration

| Variable | Default | Description |
|---|---|---|
| `GEMINI_API_KEY` | *(none)* | Google Gemini API key — falls back to rule-based AI if unset |
| Port | `8080` | Hardcoded in `WebServer.java` (`PORT` constant) |
| DB path | `saathi.db` | SQLite file in working directory |

---

## 🤖 AI Engine

Saathi uses a **dual-engine architecture**:

1. **GeminiEngine** (primary) — Calls `gemini-2.0-flash` with a rich system prompt containing the full knowledge base (training programmes, opportunities, beneficiary profile context). Responds in the user's detected language.

2. **AiEngine** (fallback) — Rule-based NLP with keyword intent detection and hand-crafted responses in 5 Indian languages. Used automatically when `GEMINI_API_KEY` is not set or the API call fails.

---

## 📁 Database Schema

```sql
beneficiaries        -- beneficiary profiles
training_programs    -- NSQF-aligned training catalogue
opportunities        -- local job & livelihood opportunities
onboarding_sessions  -- conversational intake session state
```

---

## 🛠️ Development Notes

- **No build tool required** — compile with a single `javac` command
- **No external frameworks** — only the SQLite JDBC driver (bundled in `lib/`)
- **Hot-reload** — stop the server, recompile, restart
- **CLI mode** — use `--cli` flag for terminal-only testing without a browser

---

## 📜 License

This project was built for the **Smart India Hackathon (SIH)**. All rights reserved by the development team.

---

## 👥 Team

> Built with ❤️ for rural SC/ST communities of India.  
> Aligned with PM-DAKSH, NBCFDC, and National Skills Qualification Framework (NSQF) guidelines.
