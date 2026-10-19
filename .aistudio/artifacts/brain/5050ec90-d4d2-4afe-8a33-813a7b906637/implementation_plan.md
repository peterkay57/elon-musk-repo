### Implementation Plan: Secret Vault for Dialer

# Secret Vault Integration

This plan outlines the integration of a secure, local-only Secret Vault into the existing Dialer application, providing AES-256 encryption for photos, videos, audio, and documents.

### User Review & Critical Decisions

*   **Trigger Mechanism**: User will type the PIN in the dialer and then press the "Call" button to open the vault.
*   **Unhiding**: Files can be exported/unhidden back to phone storage at any time.
*   **PIN Recovery**: A security question will be configured during the initial PIN setup to allow recovery if the PIN is forgotten.

### 1. Overview & Core Concept

*   **What It Does**: A secure, local-only file vault integrated into the dialer app. Hidden files are encrypted using AES-256 (via Android Keystore) and moved from the device's public storage to the app's private, encrypted internal storage, making them invisible in standard gallery/file browser apps.
*   **Key Value**: Provides a high-security, privacy-focused storage solution within an existing utility, utilizing the dialer itself as a "secret" access gate.

### 2. User Experience & Visual Design

*   **Key User Flows**:
    *   **First Launch**: App requests `MANAGE_EXTERNAL_STORAGE` permission -> User performs PIN setup -> User proceeds to Dialer.
    *   **Accessing Vault**: User enters PIN -> Presses Call button -> Secure Vault UI appears.
    *   **Hiding Files**: User selects files (via Android Photo Picker) -> Files are encrypted, moved to private storage, and deleted from source.
    *   **Unhiding Files**: User selects file in Vault -> File is decrypted and exported back to public storage.
*   **Visual Identity**: Consistent with the current Dark/Electric Cyan dialer theme.
*   **Security UI**: Clean, minimal UI for PIN entry, security question setup, and file management within the vault.

### 3. Key Product Decisions & Trade-Offs

*   **Decision 1: Encryption Strategy**
    *   *Approach*: AES-256 with keys managed by Android Keystore.
    *   *Why*: Best practice for secure local storage, hardware-backed where available.
*   **Decision 2: File Storage**
    *   *Approach*: Internal app private storage (`context.filesDir`).
    *   *Why*: Ensures files are truly inaccessible outside the application, fulfilling the "disappear from phone" requirement.
*   **Decision 3: Permission Requirement**
    *   *Approach*: `MANAGE_EXTERNAL_STORAGE`.
    *   *Why*: Necessary to list and move files from across the device's storage.
    *   *Note*: This is a *highly restricted* permission.

### 4. Technical Architecture & Data Strategy

```
┌─────────────────┐      ┌───────────────────────────┐
│  Dialer App UI  │ ──>  │   VaultViewModel / Logic  │
└────────┬────────┘      └────────────┬──────────────┘
         │                            │
         └─────────────┬──────────────┘
                       │
             ┌─────────▼──────────────┐
             │    Encrypted Storage   │
             │ (AES-256 / Room / File)│
             └────────────────────────┘
```

*   **Database (Room)**: Stores metadata (original file path, original name, encrypted file location in private storage).
*   **Encryption**: `CipherOutputStream` for file encryption using keys generated and stored in Android Keystore.
*   **PIN/Security Question Storage**: EncryptedSharedPreferences.
*   **Workflow**:
    1.  **Hide**: Read bytes -> Encrypt -> Write to Internal Storage -> Delete original file -> Record in Room.
    2.  **Unhide**: Read from Internal Storage -> Decrypt -> Write to Original/Public Path -> Delete from Internal Storage -> Remove from Room.
