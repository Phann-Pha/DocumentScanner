# 📄 Document Scanner SDK — Structure & Algorithms

## 📌 Overview

The **Document Scanner SDK** is a modular document-scanning solution designed to capture multiple images and perform document cropping based on a visual reference displayed on the screen.

The SDK provides a structured and extensible architecture that separates camera control, image processing, configuration management, and result handling. This design allows developers to easily integrate custom document-scanning workflows into their applications while maintaining flexibility and scalability.

---

## ✨ Features

* 📷 Capture **multiple document images**
* 🧭 Crop images based on **on-screen reference view**
* ⚙️ Configurable scanning behavior
* 🧩 Modular and extensible architecture
* 🔄 Session-based camera management
* 🧠 Image preprocessing pipeline
* 📤 Callback-based result handling

---

## 🏗 Architecture Overview

```
DocumentCameraScannerManager
        │
        ├── DocumentCameraScannerSession
        │        └── Camera Configuration & Lifecycle
        │
        ├── DocumentImageProcessor
        │        └── Image Preprocessing
        │
        ├── DocumentScannerClientConfig
        │        └── Scanner Configuration
        │
        └── ResultHandler
                 └── Result Callbacks
```

---

## 🧱 Core Components

### 1️⃣ DocumentScannerClientConfig

Responsible for managing all scanner configurations.

**Responsibilities**

* Image capture settings
* Processing configuration
* Scanner behavior customization
* Reference view configuration

**Purpose**
Acts as the central configuration object shared across the SDK components.

---

### 2️⃣ DocumentCameraScannerSession

Handles camera-related operations and session lifecycle.

**Responsibilities**

* Camera initialization
* Camera configuration
* Session lifecycle management
* Frame capture handling

**Purpose**
Encapsulates all camera interactions to keep camera logic isolated from business logic.

---

### 3️⃣ DocumentImageProcessor

Performs preprocessing on captured document images.

**Responsibilities**

* Image normalization
* Perspective preparation
* Pre-cropping processing
* Preparing images for final output

**Purpose**
Ensures captured images are optimized before cropping and returning results.

---

### 4️⃣ DocumentCameraScannerManager

Acts as the main controller of the scanning workflow.

**Responsibilities**

* Managing scanner sessions
* Coordinating capture flow
* Connecting camera session with image processing
* Applying client configurations

**Purpose**
Provides a single entry point for developers integrating the SDK.

---

### 5️⃣ ResultHandler

Handles scanning results via callbacks.

**Responsibilities**

* Deliver captured images
* Return processed results
* Provide success/error callbacks

**Purpose**
Decouples SDK processing from application-level handling.

---

## 🔄 Scanning Flow

1. Initialize `DocumentScannerClientConfig`
2. Create `DocumentCameraScannerManager`
3. Start `DocumentCameraScannerSession`
4. Capture images
5. Process images using `DocumentImageProcessor`
6. Crop based on screen reference view
7. Return results through `ResultHandler`

---

## 🧠 Algorithms

### Multi-Image Capture

The SDK supports sequential image capturing within a single scanning session, allowing users to capture multiple pages or document sides.

### View-Based Cropping

Cropping is calculated using a reference frame displayed on screen:

* The reference view defines the target crop area
* Captured image coordinates are mapped to screen coordinates
* The final image is cropped according to the mapped region

### Image Preprocessing

Before returning results:

* Images are normalized
* Orientation is corrected
* Preprocessing prepares images for consistent output quality

---

## 🚀 Usage Concept (High-Level)

```kotlin
val config = DocumentScannerClientConfig(...)
val manager = DocumentCameraScannerManager(config)

manager.startSession()
manager.capture()
manager.stopSession()
```

---

## 🎯 Design Goals

* Clean separation of responsibilities
* Highly customizable scanning pipeline
* Easy integration into existing apps
* Replaceable processing components
* Scalable architecture

---

## 📱 Platform

**Android (Kotlin)**

---

## 📄 License

Huh??

---

## 👨‍💻 Author

PHANN PHA
