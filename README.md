# ByteVision 👁️🔐

**ByteVision** is a security-focused Android application built using **Kotlin** that combines data encryption, image steganography, pixel manipulation, and acoustic data transfer into a unified platform. 

It provides tools for hiding confidential messages inside lossless images, obscuring visual data using password-derived pixel shuffling, and transferring text data over sound waves.

---

## 📸 Key Features & Architecture

```
                                  BYTEVISION
                                      │
         ┌────────────────────────────┼────────────────────────────┐
         ▼                            ▼                            ▼
   Audio Transfer              Image Shuffle                 Steganography
  ────────────────             ─────────────                ───────────────
   Text ➔ Sound                 Pixel Permutation            Message Encryption
   Speaker ➔ Mic                Key-based Seed               AES-256 / PBKDF2
   Data Recovery                Reversible Matrix            LSB Embedding
```

---

## 🛠️ Technologies & Stack

ByteVision leverages modern Android APIs alongside industry-standard cryptographic primitives.

| Technology / Library | Purpose |
| :--- | :--- |
| **Kotlin** | Primary language for Android development and core business logic |
| **PBKDF2** | Key Derivation Function (KDF) to compute secure 256-bit keys from passwords |
| **AES-256** | Symmetric encryption for encoding secret messages prior to embedding |
| **LSB Steganography** | Least Significant Bit algorithm to embed ciphertext into image color channels |
| **PNG Format** | Lossless compression format ensuring pixel integrity for data retrieval |
| **Android Photo Picker / ContentResolver** | Modern, privacy-focused media selection APIs |
| **MediaStore API** | Safe export and persistence of processed images to public storage |

---

## 📑 Core Functionalities

### 1. Acoustic Data Transfer (Audio Message Transfer)

The **Audio Message Transfer** module enables air-gapped text communication using high-frequency audio waveforms.

#### Process Flow

```
+--------------+     +------------------+     +-----------------------+
| Text Message | --> | Convert to Bytes | --> | Encode Audio Pattern  |
+--------------+     +------------------+     +-----------------------+
                                                         |
+------------------+     +------------------+            v
| Air Medium (Mic) | <-- | Speaker Emission | <-- +--------------------+
+------------------+     +------------------+     | Generate Waveform  |
         |                                        +--------------------+
         v
+------------------+     +------------------+     +--------------------+
| Audio Sampling   | --> | Analyze Signal   | --> | Decoded Text Output|
+------------------+     +------------------+     +--------------------+
```

* **Workflow:**
  1. **Encoding:** Input string is serialized into raw bytes and modulated into predefined audio frequencies/waveforms.
  2. **Transmission:** Speaker emits acoustic signals across physical air gaps.
  3. **Reception:** Receiver microphone captures ambient audio, samples the signal, decodes pattern markers, and reconstructs the original string.

---

### 2. Password-Based Pixel Permutation (Image Shuffle)

The **Image Shuffle** feature distorts visual representations by algorithmically reordering pixels based on a user-provided password.

```
                  SHUFFLE
  [Selected Image] ➔ [Bitmap Conversion] ➔ [Extract Pixels]
                                                  │
                                                  ▼
   [Save as PNG] ◄─ [Obfuscated Image] ◄─ [shuffleBitmap(Key)]

------------------------------------------------------------------

                 UNSHUFFLE
  [Shuffled PNG] ➔ [Bitmap Conversion] ➔ [unshuffleBitmap(Key)]
                                                  │
                                                  ▼
                                         [Original Image]
```

* **Methodology:**
  * Converts the source image to a 2D `Bitmap` pixel matrix.
  * Generates a deterministic pseudo-random sequence using the input password as a seed.
  * Reorders pixel coordinates through `shuffleBitmap()`.
  * Re-applying the exact password via `unshuffleBitmap()` restores the original pixel distribution cleanly.

---

### 3. Cryptographic LSB Steganography

**Steganography** provides covert communication by hiding encrypted payloads directly within raw image pixels without altering visual quality.

#### Hiding Process

```
[Secret Message] + [Password]
        │
        ▼
   (PBKDF2 KDF) ➔ [Derived AES Key]
                        │
                        ▼
            (AES-256) ➔ [Ciphertext Bytes]
                             │
                             ▼
  [Source Image] ➔ (LSB Embedding in RGB) ➔ [Stego PNG Image]
```

#### Extraction Process

```
[Stego PNG Image] ➔ (Read RGB LSB Bits) ➔ [Extracted Ciphertext]
                                                    │
                                                    ▼
    [Password] ➔ (PBKDF2 KDF) ➔ [AES Key] ➔ (AES-256 Decrypt)
                                                    │
                                                    ▼
                                         [Original Secret Message]
```

* **Security Layers:**
  1. **Encryption:** Message text is encrypted using **AES-256** with a key derived via **PBKDF2**.
  2. **Embedding:** The resulting binary payload replaces the **Least Significant Bit (LSB)** of RGB color channels across image pixels.
  3. **Lossless Persistence:** Images are exported as **PNG** files to prevent image compression artifacts from damaging hidden data bits.

---

## 🚀 Getting Started

### Prerequisites

* Android Studio Ladybug (or newer)
* JDK 17+
* Android device or emulator running API 24 (Android 7.0) or higher

### Installation

1. Clone the repository:
   ```bash
   git clone https://github.com/your-username/ByteVision.git
   ```
2. Open the project in **Android Studio**.
3. Build and run on your target Android device.

---

## 📄 License

This project is open-source and available under the [MIT License](LICENSE).
