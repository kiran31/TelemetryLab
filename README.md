# Telemetry Lab - HIPER Android Performance Mini-Assignment

This project is an implementation of the HIPER Android Performance Mini-Assignment. It's an Android application designed to simulate and monitor a compute-intensive background task, built with a focus on performance, battery awareness, and modern Android development practices.

## Core Technologies

- **UI:** 100% Jetpack Compose for a declarative and modern user interface.
- **Architecture:** MVVM (Model-View-ViewModel) on the UI layer.
- **Dependency Injection:** Dagger Hilt for managing dependencies and decoupling components.
- **Concurrency:** Kotlin Coroutines for asynchronous operations and managing the background computation loop.
- **Background Execution:** Foreground Service for managing the long-running, user-initiated computation task.
- **Performance Monitoring:** `androidx.metrics:metrics-performance` (JankStats) for real-time UI performance and jank analysis.

## Architectural Decisions

### 1. Threading & Backpressure Approach

**Threading Model:**

The core principle is to keep the main (UI) thread completely free from heavy work.

1.  **UI Thread:** This thread is exclusively used by Jetpack Compose for rendering the UI, handling user input, and running animations. No blocking or compute-intensive operations are ever performed here.
2.  **Default Dispatcher for Computation:** The compute-intensive convolution task is executed within a coroutine launched on `Dispatchers.Default`. This is a thread pool optimized for CPU-bound work, ensuring the main thread remains unblocked and the UI stays responsive.
3.  **JankStats Listener:** The JankStats listener, which processes performance data for every frame, is configured to run on a background thread by default, preventing any potential overhead on the main thread.

**Backpressure & Data Flow:**

The background service produces data at a fixed rate (20Hz or 10Hz), but the UI needs to be updated with performance metrics that are calculated per frame (e.g., at 60Hz or 120Hz). A naive approach of spamming the UI with updates would cause excessive recompositions and hurt performance.

The chosen approach is a decoupled, flow-based model using a singleton `TelemetryDataBus`:

1.  **Service -> DataBus:** The `ComputeService` does not directly update the UI. Its only job is to update its running status (`isServiceRunning`) on the central `TelemetryDataBus`.
2.  **JankStats -> ViewModel:** The `JankStats` listener in `MainActivity` receives frame data and forwards it to the `TelemetryViewModel`.
3.  **ViewModel (Processing):** The ViewModel maintains a 30-second rolling window of frame data. On each new frame, it recalculates the performance metrics (jank %, average frame time).
4.  **ViewModel -> DataBus -> UI:** The ViewModel posts the newly calculated `PerformanceMetrics` to the `TelemetryDataBus`. The UI (specifically, the `TelemetryScreen` Composable) collects this data as a `StateFlow` from the ViewModel, which in turn collects it from the bus. This ensures the UI only recomposes when the state actually changes, providing a natural form of backpressure.

### 2. Justification for Foreground Service (FGS) vs. WorkManager

For this assignment's requirements, a **Foreground Service (FGS) was the clear choice** over WorkManager.

**Why FGS is appropriate here:**

* **User-Initiated, Long-Running Task:** The computation is started and stopped directly by the user via a toggle in the UI. It's an immediate task that should run for as long as the user wants it to. FGS is the designated API for this pattern.
* **Real-time Nature:** The task simulates a real-time process (producing "frames" at 20Hz). It's not a deferrable or background sync task.
* **User Awareness:** An FGS requires a persistent notification, which is crucial for this use case. It keeps the user aware that a potentially battery-intensive process is running, which is a key principle of modern Android development.

**Why WorkManager is inappropriate here:**

* **Not for Immediate Tasks:** WorkManager is designed for deferrable and guaranteed background work. It is explicitly not designed for immediate tasks that need to run continuously from the moment they are started. The system can (and will) delay WorkManager jobs based on system health, battery, and other constraints.
* **No Real-time Guarantees:** WorkManager does not provide the real-time execution guarantees needed to maintain a consistent 20Hz processing loop.
* **Lifecycle Mismatch:** The lifecycle of the task is tied to the user's interaction with the toggle switch, not to system-level triggers or constraints, which is the domain of WorkManager.

In summary, FGS provides the correct execution model, lifecycle, and user transparency for a task that is immediate, long-running, and user-initiated.

---

### How to Run

1.  Open the project in Android Studio.
2.  Let Gradle sync the dependencies.
3.  Run the `app` configuration on an emulator or a physical device (recommended for accurate performance metrics).
