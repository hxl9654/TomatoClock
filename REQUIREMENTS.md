# 番茄钟需求文档 (TomatoClock Requirements)

## 1. 产品简介 (Product Introduction)
TomatoClock 是一款基于番茄工作法的辅助计时工具，旨在帮助用户通过设定固定的专注和休息时间片段来提升工作和学习效率。

## 2. 核心功能 (Core Features)

### 2.1 计时流转 (Timer Flow)
*   **专注阶段 (Focus Phase)**：用户进行深度工作的阶段，倒计时默认为 25 分钟。
*   **短休息 (Short Break)**：每个专注阶段结束后的短暂休息，倒计时默认为 5 分钟。
*   **长休息 (Long Break)**：连续完成多个专注阶段（默认为 4 个）后的长时休息，倒计时默认为 15 分钟。
*   **循环边界规则**：在“短休 → 专注”流转时循环次数递增。若因用户缩小设置导致当前循环次数超出或等于设定的总循环次数，将强制裁剪边界以确保下一次准确触发长休息。

*   **推迟提醒 (Postpone Reminder)**：在阶段结束时，允许用户推迟进入下个阶段，继续延时当前状态（默认为 5 分钟）。

### 2.2 计时器控制 (Timer Controls)
*   **开始 (Start)**：启动当前模式的倒计时。
*   **暂停 (Pause)**：暂时停止计时，随时可以**继续 (Resume)**。
*   **停止 (Stop)**：放弃当前进度，重置回“专注”模式及第 1 次循环的初始状态。
*   **手动跳转 (Next)**：在阶段结束后，手动触发进入下一阶段。
*   **快捷操作 (Quick Actions)**：在倒计时进行中，支持“加5分钟”与“跳过当前周期”功能。

### 2.3 设置选项 (Settings)
*   **时长自定义**：允许用户自定义专注、短休息、长休息的时长。专注、短休默认为 1..120 和 1..30，长休息为 1..60。+/- 按钮支持长按连续调节（500ms 刟起，80ms 间隔）。
*   **设置实时同步**：设置修改后，首页在计时器空闲状态下立即更新显示。
*   **循环次数**：设定在进入长休息前需要完成的“专注”循环次数。
*   **推迟提醒时长**：设定点击推迟提醒增加的时间。
*   **行为偏好与提醒**：

    *   倒计时结束时点亮屏幕开关 (Wake Screen)
    *   倒计时结束时界面呼吸闪烁开关 (Breathing UI Flash)
    *   独立铃声模式选择：持续响铃、响铃一次、关闭
    *   独立震动模式选择：持续震动、震动一次、关闭
    *   提示音选择：经典电子滴答、清脆风铃、柔和合成音、禅意颂钵、自然木琴 (支持下拉选择时自动试听)

### 2.4 后台状态恢复 (Background State Restoration)
*   **持久化保存**：当应用退到后台或被系统回收时，当前计时器状态（状态、模式、到期时间等）会被持久化保存。
*   **1小时失效规则**：如果应用在后台不活跃（被回收）超过 1 小时，重新打开时将丢弃原有进度，重置为空闲（IDLE）状态。
*   **10分钟补发规则**：在 1 小时内重新打开应用，若原本的倒计时已在后台结束：
    *   **超时10分钟以内**：界面恢复为完成状态并播放正常呼吸动画，同时**立刻补发**响铃和震动提醒。
    *   **超时10分钟以上**：界面恢复为完成状态并播放正常呼吸动画，但**强制静音**（不响铃、不震动），以免打扰用户。

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
*   **状态文字语义规范 (State Label Convention)**：
    *   `IDLE` 时不得显示正在进行的模式名称（如"专注中"），应按模式显示"准备专注 / 准备短休息 / 准备长休息"。
    *   `PAUSED` 时应按模式显示"专注已暂停 / 短休息已暂停 / 长休息已暂停"，而非笼统的"已暂停"。
    *   `RUNNING` 时显示当前模式名称："专注中 / 短休息 / 长休息"。
    *   `FINISHED` 时显示"专注结束！/ 休息结束！"。

## 4. 架构及技术要求 (Architecture & Technical Requirements)
*   必须使用 **Jetpack Compose** 搭建 UI。
*   必须使用 **MVVM 架构** 及 **Unidirectional Data Flow (单向数据流)**。
*   必须使用 **Hilt** 进行依赖注入。
*   数据存储层必须使用 **Jetpack DataStore (Preferences)**。
*   核心计时逻辑必须位于 **TimerRepository**，通过系统级别的闹钟调度（如 `AlarmScheduler` 接口）锚定时间，解决休眠带来的时间漂移。
*   **状态同步机制**：`TimerRepository` 内部状态变动必须使用 `Mutex` 进行线程安全的锁保护，防止 Main 与 Default Coroutine 调度器之间的竞态条件。
*   后台执行必须基于 **系统闹钟服务 (AlarmManager.setAlarmClock)** 配合 **全屏意图 (FullScreenIntent)**，彻底抛弃不可靠的 `PowerManager.PARTIAL_WAKE_LOCK`，保障应用切入后台或深度锁屏息屏时倒计时正常运行和强制唤醒亮屏。
*   铃声及震动等硬件副作用应隔离至独立的 `AlarmPlayer` 模块单例中。
*   禁止使用 `Thread.sleep` 阻塞主线程；状态更新通过 `StateFlow`。
*   **安全规范**：
    *   DataStore 读取枚举值时必须使用 `runCatching { }.getOrNull()` 防止 `IllegalArgumentException` 崩溃。
    *   DataStore 的 `Flow.first()` 不得在 Main dispatcher 下直接调用，应使用 `withContext(Dispatchers.IO)` 切换。
    *   `TimerRepository.init` 块中，状态恢复逻辑（Step 1）必须完全执行后才能启动设置监听协程（Step 2），防止竞态条件。
*   **持久化字段规范**：`SavedTimerState` 必须包含 `totalTimeInSeconds`，以正确恢复 `addTime()` 延长后的进度圆弧。

## 5. 质量保证 (Quality Assurance)
*   所有业务逻辑需经过 JUnit 单元测试覆盖，包含：
    *   `TimerRepository`：计时流转、状态恢复、addTime、saveCurrentState（RUNNING/PAUSED/IDLE 三种场景）
    *   `SettingsDataStore`：所有设置项的读写及默认值（含 ringtone、flashScreen）
    *   `AlarmPlayer`：play/preview/stop 的幂等性及 H-5 场景（preview → play 不误停）
    *   `SettingsViewModel`、`TimerViewModel`：各保存方法及预览调用
*   所有核心流程必须通过基于 Emulator 的端到端 (E2E) 测试验证，包括：
    *   完整计时流（IDLE → RUNNING → FINISHED → 下一阶段）
    *   暂停/继续/停止
    *   长休息触发流
    *   推迟提醒
*   视觉回归测试 (VRT)：覆盖 IDLE/RUNNING/PAUSED/FINISHED × FOCUS/SHORT_BREAK/LONG_BREAK 的关键组合快照。
*   针对测试环境，引入加速机制 (Time Multiplier) 确保 E2E 测试在合理时间内完成。
*   **测试隔离与稳定性 (Test Isolation & Determinism)**：
    *   **防止状态泄漏 (State Bleeding)**：在 E2E 测试销毁阶段，必须显式调用 `TimerRepository.destroyForTesting()` 强制取消全局协程作用域，彻底阻断后台心跳任务跨测试向 DataStore 写入脏数据。
    *   **消除 UI 测试抖动 (Flaky Tests)**：对异步发布的状态变更（如 `StateFlow` 的状态跳转），禁止使用瞬时的 `assertIsDisplayed()`，必须使用轮询重试机制的 `waitUntilTextExists()` 来安全等待 UI 响应。
    *   **Mock 防御编程**：对 `TimerAlarmReceiver` 等需要被手动触发的组件，必须做可空安全调用（如 `pendingResult?.finish()`）以兼容无原生上下文的测试环境。
*   CI 测试脚本必须解析 HTML 报告，在执行 0 个测试时立即失败（防止静默跳过）。
