# Diamonds OPS — Enterprise Operations & Invoicing Suite

**Diamonds OPS** is an all-in-one Android business operations system, professional invoice maker, billing ledger manager, and daily email template generator built with modern Kotlin, Jetpack Compose, Material 3, and Room local persistence.

---

## 📖 Executive Summary: What, Why, When, Where & How

### 1. What is Diamonds OPS?
Diamonds OPS is an offline-first enterprise mobile application designed for independent operators, agency founders, consultants, and business operations teams. It consolidates three core workflows into a unified console:
1. **Invoice Maker & Billing Management**: Multi-layout PDF invoice creator, line-item calculator, signature capture, status tracking, and settlement ledger.
2. **Operations Hub & Task Console**: Kanban-style operational task organizer with priorities, deadlines, and progress states.
3. **Daily Email Template Generator & Studio**: Context-aware business email dispatcher with dynamic variable interpolation and Gemini AI draft generation.

### 2. Why was it created?
- **Eliminate Fragmented Tools**: Small businesses and operators often juggle disconnected apps for invoicing, task tracking, CRM, and email drafting.
- **Fast Interactive Workflows**: Native mobile gestures (swipe-to-delete with undo, tap-to-dial, tap-to-email) and real-time Room data streams make daily administrative tasks effortless.
- **Client Document Standards**: High-polish PDF rendering with 7 visual document layout themes, digital signatures, and customizable branding elevates client communications.
- **Reliable Local Persistence**: Offline-first Room architecture ensures full operational capability without constant server dependency.

### 3. When to use Diamonds OPS?
- **Billing Cycles & Milestones**: When issuing initial project quotes, retainers, sprint invoices, or overdue balance follow-ups.
- **Daily Standups & Operator Check-ins**: When tracking internal deliverables, dispatching operational tasks, or logging payment settlements.
- **Client Correspondence**: When composing professional status updates, payment reminders, contract follow-ups, or meeting recaps.

### 4. Where is data stored and processed?
- **On-Device Database**: All client profiles, company settings, invoices, billing records, tasks, and custom email templates are stored locally in an encrypted Room SQLite database (`bizops_database`).
- **Local PDF Engine**: High-resolution vector and bitmap PDFs are generated entirely on-device using Android's native `android.graphics.pdf.PdfDocument` API.
- **AI Processing**: Optional intelligent email drafting runs securely via Google Gemini API through server-side credentials.

### 5. How does it work?
- **Architecture**: Modern Android Clean MVVM (Model-View-ViewModel) architecture.
- **Data Pipeline**: Room DAOs emit reactive Kotlin `Flow`s, combined into `StateFlow`s inside `BizOpsViewModel`, consumed by Jetpack Compose UI with lifecycle awareness (`collectAsStateWithLifecycle`).
- **Interactive UI**: Material 3 Design System with dynamic color schemes, interactive elevation, swipe-to-dismiss boxes, filter chips, and animated dialogs.

---

## 🚀 Key Features

### 🧾 1. Professional Invoicing & Billing Ledger
- **7 Invoice Styles**: Modern Executive, Classic Slate, Emerald Minimal, Sapphire Modern, Sunset Warm, Monochromatic, and Compact Clean.
- **Live PDF Rendering & Export**: Export, view, share, or save high-fidelity PDF documents directly to device storage.
- **Line Items & Calculation**: Dynamic item rows with quantity, unit rate, tax percentage, and discount calculation.
- **Canvas Signature Capture**: Draw digital signatures with clear/save controls saved directly to the invoice record.
- **Status Lifecycle**: Track statuses across `DRAFT`, `SENT`, `PAID`, `OVERDUE`, and `CANCELLED`.
- **Swipe-to-Delete with Undo**: Swift swipe gesture (end-to-start) with destructive background and instant 1-tap undo capability.

### ⚡ 2. Operations Console & Task Tracker
- **Task Categorization**: Priority levels (Low, Medium, High, Critical) and operational statuses (Backlog, In Progress, Review, Completed).
- **Billing Velocity Analytics**: Interactive visual charts and summary metrics tracking active pipeline and cash collection.
- **Quick Action Triggers**: Instant invoice creation and task dispatch from top bar actions.

### ✉️ 3. Email Template Studio & AI Writer
- **Curated Template Library**: Ready-to-send templates across Invoicing, Payment Reminders, Follow-ups, Proposals, Status Updates, and Formal Notices.
- **Dynamic Variable Replacement**: Auto-injects `{client_name}`, `{invoice_number}`, `{amount}`, `{due_date}`, and `{company_name}`.
- **Gemini AI Integration**: Generate customized business emails tailored to specific tone (Formal, Friendly, Urgent, Persuasive) and context.
- **Direct Mail Intent**: One-tap dispatch to external email clients (Gmail, Outlook) pre-filled with subject and body.

### 👥 4. Client CRM & Business Branding
- **CRM Directory**: Maintain client contact names, companies, tax IDs, phone numbers, and addresses.
- **One-Tap Actions**: Direct phone dialing (`ACTION_DIAL`) and email launching (`ACTION_SENDTO`).
- **Company Profile**: Customize business name, tax ID/EIN, banking instructions (IBAN, SWIFT, Routing), address, and brand logo preset.

---

## 🛠️ Technical Stack & Architecture

| Layer | Technology |
|---|---|
| **Language** | Kotlin 2.0+ |
| **UI Framework** | Jetpack Compose (Material 3) |
| **Architecture** | MVVM + Clean Architecture |
| **Local Database** | Room 2.7.0 (KSP annotation processing) |
| **Asynchronous** | Kotlin Coroutines & Reactive Flow |
| **Image Loading** | Coil 3 (Compose integration) |
| **AI Integration** | Google Gemini API (Server-Side Proxy) |
| **PDF Generation** | Android Native `PdfDocument` |
| **Build System** | Gradle (Kotlin DSL - `.gradle.kts`) |

---

## 📂 Project Structure

```
app/src/main/java/com/example/bizops/
├── data/
│   ├── ai/
│   │   └── GeminiOpsService.kt          # Gemini API prompt client
│   ├── db/
│   │   ├── BizOpsDao.kt                 # Room DAOs (Invoices, Tasks, CRM, Billing, Templates)
│   │   ├── BizOpsDatabase.kt            # Room Database definition & initial seed data
│   │   └── Converters.kt                # TypeConverters for JSON lists and enums
│   ├── model/
│   │   ├── BillingRecord.kt             # Settled payment ledger entity
│   │   ├── Client.kt                    # Client CRM record entity
│   │   ├── CompanyProfile.kt            # Business entity profile
│   │   ├── EmailTemplate.kt             # Reusable email templates
│   │   ├── Invoice.kt                   # Invoice entity with line items & signatures
│   │   ├── InvoiceStyle.kt              # Invoice color schemes and layout configurations
│   │   └── OperationTask.kt             # Internal task entity
│   └── repository/
│       └── BizOpsRepository.kt          # Single source of truth abstracting Room DAOs
├── ui/
│   ├── clients/
│   │   └── ClientsAndSettingsScreen.kt  # CRM Directory & Company Profile settings
│   ├── components/
│   │   ├── EmptyStateView.kt            # Accessible empty-state visual placeholders
│   │   └── MonthlyInvoiceChart.kt       # Billing velocity and revenue charts
│   ├── emails/
│   │   └── EmailTemplatesScreen.kt      # Template library, editor & AI email studio
│   ├── invoices/
│   │   └── InvoicesScreen.kt            # Invoice manager, PDF preview & creation dialogs
│   ├── operations/
│   │   └── OperationsHubScreen.kt       # Primary executive dashboard & task console
│   └── viewmodel/
│       └── BizOpsViewModel.kt           # Centralized reactive state management
└── util/
    ├── InvoicePdfExporter.kt            # Multi-layout PDF generation and sharing engine
    └── LogoPresetManager.kt             # Dynamic logo and branding asset manager
```

---

## 🚦 Getting Started & Compilation

### Prerequisites
- Android Studio Ladybug / Meerkat or compatible CLI environment
- Android SDK 35 (Android 15) with `minSdk` 26 (Android 8.0 Oreo)
- Java 17 / 21 JDK

### Build & Run
To compile the application in this environment:
```bash
gradle assembleDebug
```

To run Robolectric unit tests:
```bash
gradle :app:testDebugUnitTest
```

---

## 🔒 Security & Offline Guarantee
- **Zero Cloud Storage Leakage**: Invoices, banking details, client lists, and signatures are stored strictly in local application sandboxed SQLite files.
- **Secure Credentials**: Server-side Gemini API keys are injected via `BuildConfig` environment variables rather than hardcoded source files.

---

## 📄 License & Attribution
Diamonds OPS Enterprise Suite is designed for internal and commercial business operational workflows.
All vector assets and UI components are built in compliance with Android Material Design 3 guidelines.
