# 番茄钟需求文档 (TomatoClock Requirements)

## 1. 产品简介 (Product Introduction)
TomatoClock 是一款基于番茄工作法的辅助计时工具，旨在帮助用户通过设定固定的专注和休息时间片段来提升工作和学习效率。

## 2. 核心功能 (Core Features)

### 2.1 计时流转 (Timer Flow)
*   **专注阶段 (Focus Phase)**：用户进行深度工作的阶段，倒计时默认为 25 分钟。
*   **短休息 (Short Break)**：每个专注阶段结束后的短暂休息，倒计时默认为 5 分钟。
*   **长休息 (Long Break)**：连续完成多个专注阶段（默认为 4 个）后的长时休息，倒计时默认为 15 分钟。
*   **自动流转 (Auto-Transitions)**：
    *   可选：专注结束后自动进入休息。
    *   可选：休息结束后自动进入专注。
*   **推迟提醒 (Postpone Reminder)**：在阶段结束时，允许用户推迟进入下个阶段，继续延时当前状态（默认为 5 分钟）。

### 2.2 计时器控制 (Timer Controls)
*   **开始 (Start)**：启动当前模式的倒计时。
*   **暂停 (Pause)**：暂时停止计时，随时可以**继续 (Resume)**。
*   **停止 (Stop)**：放弃当前进度，重置回“专注”模式及第 1 次循环的初始状态。
*   **手动跳转 (Next)**：在阶段结束后，手动触发进入下一阶段。

### 2.3 设置选项 (Settings)
*   **时长自定义**：允许用户自定义专注、短休息、长休息的时长。专注、短休默认为 1..120 和 1..30，长休息为 1..60。+/- 按钮支持长按连续调节（500ms 刟起，80ms 间隔）。
*   **设置实时同步**：设置修改后，首页在计时器空闲状态下立即更新显示。
*   **循环次数**：设定在进入长休息前需要完成的“专注”循环次数。
*   **推迟提醒时长**：设定点击推迟提醒增加的时间。
*   **行为偏好与提醒**：
    *   自动进入休息开关
    *   自动进入专注开关
    *   倒计时结束时点亮屏幕开关 (Wake Screen)
    *   倒计时结束时界面呼吸闪烁开关 (Breathing UI Flash)
    *   提醒模式选择：闹铃+震动、仅闹铃、仅震动、单次提示音、静音
    *   提示音选择：经典电子滴答、清脆风铃、柔和合成音、禅意颂钵、自然木琴 (支持下拉选择时自动试听)

## 3. UI/UX 规范 (UI/UX Guidelines)
*   必须为**纯中文界面 (Chinese UI Only)**。
*   通过颜色直观区分当前状态：
    *   专注：Primary Color
    *   短休：Secondary Color
    *   长休：Tertiary Color
*   界面主要元素：
    *   带有圆角 (StrokeCap.Round) 的环形进度条显示流逝比例。
    *   中心以 `MM:SS` 大字号显示剩余时间。
    *   展示当前位于第几次循环（如：第 1 / 4 次循环）。
*   控制按钮组根据当前状态 (`IDLE`, `RUNNING`, `PAUSED`, `FINISHED`) 动态变化。

## 4. 架构及技术要求 (Architecture & Technical Requirements)
*   必须使用 **Jetpack Compose** 搭建 UI。
*   必须使用 **MVVM 架构** 及 **Unidirectional Data Flow (单向数据流)**。
*   必须使用 **Hilt** 进行依赖注入。
*   数据存储层必须使用 **Jetpack DataStore (Preferences)**。
*   核心计时逻辑必须位于 **TimerRepository**，通过 `SystemClock.elapsedRealtime()` 锚定时间，解决休眠带来的时间漂移。
*   后台执行必须基于 **Foreground Service (前台服务)** 配合 `PowerManager.PARTIAL_WAKE_LOCK`，保障应用切入后台或锁屏时倒计时正常运行。
*   铃声及震动等硬件副作用应隔离至独立的 `AlarmPlayer` 模块单例中。
*   禁止使用 `Thread.sleep` 阻塞主线程；状态更新通过 `StateFlow`。

## 5. 质量保证 (Quality Assurance)
*   所有业务逻辑需经过 JUnit 单元测试覆盖。
*   所有核心流程必须通过基于 Emulator 的端到端 (E2E) 测试验证。
*   针对测试环境，引入加速机制 (Time Multiplier) 确保 E2E 测试在合理时间内完成。
