# INTERNAL AUDITING SUPPORT & STATUTORY REPORTING SUBMISSION PLATFORM (IAA APP)

> **COMMERCIAL IN CONFIDENCE • PROPRIETARY GOVTECH PLATFORM**  
> **Author & Lead Software Engineer:** Clement Kofi Acheampong  
> **Entity:** Bibinii Tech  
> **Legal Copyright:** © 2024–2026 Clement Kofi Acheampong. All Rights Reserved.  
> **Statutory Jurisdiction:** Republic of Ghana (Protected under Ghana Copyright Act, 2005 - Act 690)  
> **Official Demonstration / Staging:** [https://iaaapp.vercel.app](https://iaaapp.vercel.app)  

---

## 1. Executive Summary & Legal Ownership

The **IAA App** is an enterprise cross-platform compliance ecosystem designed to modernize statutory internal audit reporting for the **Internal Audit Agency (IAA) of Ghana** and its **466 Covered Public Entities** (comprising 147 MDAs, 42 SOEs, 16 RCCs, and 261 MMDAs) in strict alignment with:
* **Public Financial Management Act, 2016 (Act 921)**
* **Internal Audit Agency Act, 2003 (Act 658)**
* **Public Financial Management Regulations, 2019 (L.I. 2378)**
* **Ghana Data Protection Act, 2012 (Act 843)**
* **Cybersecurity Act, 2020 (Act 1038)**

All source code, data schemas, compliance algorithms, security sentinels, and curated knowledge bases contained in this repository are the proprietary trade secrets of **Clement Kofi Acheampong**. Review of this code is permitted exclusively under executed Non-Disclosure and Proprietary Intellectual Property terms (see [`PROPRIETARY_NOTICE.md`](PROPRIETARY_NOTICE.md) and [`LICENSE.md`](LICENSE.md)).

---

## 2. Cross-Platform System Architecture

The ecosystem couples a high-performance **Native Android Application** with an enterprise **Next.js 16 Web Portal**, synchronized via a sub-second serverless cloud layer:

| Architectural Component | Android Native Client | Enterprise Web Portal |
| :--- | :--- | :--- |
| **Language & Framework** | Kotlin 2.0+ / Jetpack Compose / Material 3 | Next.js 16.2 / React 19 / TypeScript 5+ |
| **Architecture Pattern** | MVVM / Coroutines StateFlow & SharedFlow | Server Components + Client SWR Hooks |
| **Local Persistence** | Room SQLite (`iaa_app_database`) + DataStore | LocalStorage + IndexedDB Offline Drafts |
| **Offline Resilience** | Jetpack WorkManager (`SubmissionWorker`) | ServiceWorker Draft Sync Queue |
| **Asset Security** | AES-128 Encrypted Binary Datasets (`AssetCryptoUtils`) | Server-side Protected Edge Endpoints |
| **Hardware Sentinel** | Biometrics + Root/Emulator Sentinel (`SecurityUtils`) | Subresource Integrity (SRI) + CSRF Defense |
| **Cloud Synchronization** | Cloud Firestore via `syncToken` micro-listener | Edge API SDK via `syncToken` micro-listener |
| **Transactional Relay** | Brevo REST API v3 Relay (`EmailSender`) | Brevo REST API v3 Relay Serverless Routes |

---

## 3. Core Functional Capabilities

1. **Multi-Stream Submissions Filing Portal**: Supports 6 statutory reporting categories (Quarterly IA Reports, Annual Workplans, CCC Review Reports, Status of Implementation [SOI], Performance Reports, and Special Charters/Handover notes).
2. **Automated Submitter Legal Receipt**: Automatic email loopback dispatches verified filing packages with timestamped audit trails back to the submitting auditor as legal proof of filing.
3. **Resilient Background Queue (`SubmissionWorker`)**: Built on Android Jetpack `CoroutineWorker` with persistent SQLite job scheduling and exponential retry backoff when field networks drop.
4. **Client-Side File Compression (`FileCompressionUtils`)**: Analyzes byte streams on the fly to compress PDF attachments and scans before upload, minimizing field cellular bandwidth.
5. **Automated National Defaulters' List**: Real-time compliance registry monitoring 466 institutions with tri-state status tracking (On-Time, Late with exact days overdue, Defaulter) and date-elapse gating.
6. **Natural Language TF-IDF Criteria Engine**: Offline search engine with inverse document frequency scoring, synonym expansion, and fuzzy matching over 780+ KB of Ghanaian audit criteria (`audit_findings.enc` and `law_details.enc`), auto-formatting into 5-element findings (*Condition, Criteria, Cause, Consequence, Recommendation*).
7. **National PFM Compliance League Table**: Benchmarks and ranks MDAs, SOEs, and MMDAs on objective compliance metrics, fulfilling the Ministry of Finance National Budget Statement mandate.
8. **Interactive PFM Tax Calculators**: Built-in computation engines for GRA Graduated PAYE (with Tier 1 SSNIT deduction), Withholding Tax (WHT), and composite VAT/Levies (NHIL, GETFund, COVID).
9. **Zero-Coding Administrator Console**: Non-technical administrative portal to update submission recipient emails, modify deadlines, broadcast circulars, and monitor filings in real time.

---

## 4. Security & Anti-Tamper Sentinel

The application features comprehensive hardware-level and runtime integrity checks implemented in [`SecurityUtils.kt`](app/src/main/java/com/bibiniitech/iaaapp/SecurityUtils.kt):
* **Anti-Root Sentinel**: Inspects system partitions (`/system/xbin/su`, `/system/app/Superuser.apk`, Magisk).
* **Debugger Attachment Blocker**: Detects active JDWP and ptrace dynamic debugger attachments.
* **Emulator & Hypervisor Sandboxing Guard**: Inspects hardware drivers (QEMU, BlueStacks, Nox, Genymotion) to halt execution in virtualized testing environments.
* **Cryptographic Signature Verification**: Computes the Base64 SHA-256 hash of the release signing certificate to terminate patched or resigned APKs.
* **Biometric Authentication**: Enforces Class 3 BiometricPrompt (Fingerprint / 3D Face) before accessing local submission drafts.

---

## 5. Development & Build Setup

### Prerequisites
* **Android Studio Ladybug / Meerkat (2024.2+)**
* **JDK 17 or higher**
* **Android SDK 36 (target) / SDK 28 (minimum)**

### Local Configuration
1. Clone the repository into your private development workspace.
2. Ensure your `local.properties` contains your Android SDK path and local development fallback keys:
   ```properties
   sdk.dir=C\:\\Users\\<Username>\\AppData\\Local\\Android\\Sdk
   BREVO_API_KEY_MOBILE=your_private_brevo_key_here
   ```
3. Sync Gradle and build the project:
   ```bash
   ./gradlew clean assembleDebug
   ```
4. For release builds with R8 obfuscation and resource shrinking:
   ```bash
   ./gradlew assembleRelease
   ```

---

## 6. Intellectual Property & Commercial Contact

For government procurement, institutional deployment, and commercial partnership inquiries:

* **Clement Kofi Acheampong**  
  *Internal Auditor & Lead Software Architect*  
* **Telephone / WhatsApp:** +233 544 73 78 70 / +233 257 63 41 00  
* **Email:** [bibiniitech@gmail.com](mailto:bibiniitech@gmail.com) / [clemzy93@gmail.com](mailto:clemzy93@gmail.com)  
* **Staging Demonstration Portal:** [https://iaaapp.vercel.app](https://iaaapp.vercel.app)  
* **Commercial Prospectus Document:** [`IAA_App_Proposal_and_Technical_Prospectus.docx`](IAA_App_Proposal_and_Technical_Prospectus.docx)
