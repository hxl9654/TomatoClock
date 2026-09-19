# TomatoClock (番茄钟)

A modern Android Pomodoro Timer application built with Kotlin, Jetpack Compose, and Hilt. 
一款使用 Kotlin、Jetpack Compose 和 Hilt 构建的现代 Android 番茄钟应用。

## 🎯 Features / 功能

*   **Customizable Timers (自定义时长)**: Set custom durations for Focus (专注), Short Break (短休息), and Long Break (长休息). +/- buttons support long-press for fast adjustment.
*   **Quick Actions (快捷操作)**: Add 5 minutes to the current timer or skip the current phase entirely. (在倒计时进行中支持“加5分钟”与“跳过当前周期”功能)
*   **Independent Notifications (独立通知设置)**: Separately configure Sound Mode and Vibration Mode. (独立配置铃声与震动模式，支持持续、单次或关闭)
*   **Cycles (循环)**: Automatically track your pomodoro cycles and trigger a long break after a set number of focuses.
*   **Real-time Settings Sync (设置实时同步)**: Home screen instantly reflects changes made in Settings while the timer is idle.

*   **Postpone Reminder (推迟提醒)**: Postpone a completed session to continue for a few more minutes.
*   **Data Persistence (数据持久化)**: Settings are saved using Android DataStore.
*   **Pure Chinese UI (纯中文界面)**: The user interface is completely in Simplified Chinese.

## 🛠️ Tech Stack / 技术栈

*   **UI**: Jetpack Compose, Material 3
*   **Architecture**: MVVM (Model-View-ViewModel) + Unidirectional Data Flow
*   **Dependency Injection**: Dagger Hilt
*   **Storage**: Jetpack DataStore (Preferences)
*   **Concurrency**: Kotlin Coroutines & Flow
*   **Background Execution / 后台执行**:
    *   `AlarmManager.setAlarmClock()` — 系统级精确闹钟，绕过 Doze 模式，保障深度休眠下准时触发
    *   `TimerAlarmReceiver` — 接收系统闹钟广播，强制完成计时
    *   `FullScreenIntent` — 在锁屏/息屏状态下弹出全屏通知唤醒用户
    *   `PowerManager.PARTIAL_WAKE_LOCK` — 保持 CPU 在计时期间运行（已配合 AlarmManager 使用）
*   **Permissions / 权限说明**:
    *   `POST_NOTIFICATIONS`：用于在状态栏显示倒计时进度和前台服务通知。
    *   `USE_EXACT_ALARM` / `SCHEDULE_EXACT_ALARM`：用于设定精确的倒计时结束时间，确保息屏状态下准时提醒。
    *   `WAKE_LOCK` / `USE_FULL_SCREEN_INTENT`：用于在倒计时结束时点亮屏幕并弹出提醒界面。
    *   `VIBRATE`：用于倒计时结束时的震动提醒。
*   **Testing**:
    *   Unit Tests: JUnit 4, MockK, Coroutines Test, Robolectric
    *   Snapshot Tests: Roborazzi (5 snapshots covering all states/modes)
    *   E2E/Instrumented Tests: Compose UI Test, Hilt Android Testing

## 🧪 Testing / 测试

The project relies heavily on automated testing as the primary gatekeeper for quality. 
本项目深度依赖自动化测试作为质量控制的网关。

To run all checks at once / 一键运行所有检查:

```powershell
.\script\run_tests.ps1
# 依次执行: [1] Lint静态分析 → [2] 本地单元测试 → [3] 仪器化E2E测试
```

*   **Lint (静态代码分析)**:
    ```bash
    ./gradlew lintDebug
    ```
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
