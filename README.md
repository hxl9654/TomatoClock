# TomatoClock (番茄钟)

A modern Android Pomodoro Timer application built with Kotlin, Jetpack Compose, and Hilt. 
一款使用 Kotlin、Jetpack Compose 和 Hilt 构建的现代 Android 番茄钟应用。

## 🎯 Features / 功能

*   **Customizable Timers (自定义时长)**: Set custom durations for Focus (专注), Short Break (短休息), and Long Break (长休息).
*   **Cycles (循环)**: Automatically track your pomodoro cycles and trigger a long break after a set number of focuses.
*   **Auto-Transitions (自动流转)**: Optional settings to automatically start breaks or focus sessions.
*   **Snooze (稍后提醒)**: Snooze a completed session to continue for a few more minutes.
*   **Data Persistence (数据持久化)**: Settings are saved using Android DataStore.
*   **Pure Chinese UI (纯中文界面)**: The user interface is completely in Simplified Chinese.

## 🛠️ Tech Stack / 技术栈

*   **UI**: Jetpack Compose, Material 3
*   **Architecture**: MVVM (Model-View-ViewModel) + Unidirectional Data Flow
*   **Dependency Injection**: Dagger Hilt
*   **Storage**: Jetpack DataStore (Preferences)
*   **Concurrency**: Kotlin Coroutines & Flow
*   **Testing**:
    *   Unit Tests: JUnit 4, MockK, Coroutines Test
    *   E2E/Instrumented Tests: Compose UI Test, Hilt Android Testing

## 🧪 Testing / 测试

The project relies heavily on automated testing as the primary gatekeeper for quality. 
本项目深度依赖自动化测试作为质量控制的网关。

To run tests / 运行测试命令：

*   **Local Unit Tests (本地单元测试)**:
    ```bash
    ./gradlew testDebugUnitTest
    ```
*   **Instrumented E2E Tests (仪器化端到端测试)**:
    ```bash
    ./gradlew connectedDebugAndroidTest
    ```

## 🏗️ Architecture Rules / 架构规则

*   **Thin UI Controllers**: Activities strictly act as thin UI controllers.
*   **Single Source of Truth**: The `TimerRepository` coordinates the state, while `TimerViewModel` acts as the bridge for `TimerScreen`.
*   **No Hardcoded Colors**: Colors and typography rely on `MaterialTheme` and `colors.xml`.
*   **Strict UI Purity**: No side effects during the Compose render phase.

## 🚀 Getting Started / 快速开始

1.  Clone the repository.
2.  Open in Android Studio (Jellyfish or newer recommended).
3.  Ensure you have JDK 17 configured (`jvmToolchain(17)`).
4.  Sync Gradle and run the `app` configuration on an emulator or device.
