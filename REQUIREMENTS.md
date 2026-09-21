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
*   **快捷操作 (Quick Actions)**：在倒计时进行中，支持“加5分钟”与“跳过当前周期”功能。附加时长必须受到合理上限的约束（如 24 小时），以防止溢出错误和 UI 异常渲染。

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
*   **初始化同步机制**：`TimerRepository` 必须使用 `CompletableDeferred` 等机制阻塞并等待底层 DataStore 状态恢复完毕后，再允许响应外部的公共方法调用（如 `startTimer` 等），避免因异步恢复导致竞态条件及状态异常。
*   后台执行必须基于 **系统闹钟服务 (AlarmManager.setAlarmClock)** 配合 **全屏意图 (FullScreenIntent)**，彻底抛弃不可靠的 `PowerManager.PARTIAL_WAKE_LOCK`，保障应用切入后台或深度锁屏息屏时倒计时正常运行和强制唤醒亮屏。
*   **后台与保活**：`TimerAlarmReceiver` 在被系统准时唤醒时，**会 startForegroundService 启动 TimerService**，这是接管状态并播放声音、震动的唯一手段，不可被移除。
*   **开机自启动恢复 (Boot Completed)**：设备重启后，依靠 `BootCompletedReceiver` 自动拉起进程，并通过 `TimerRepository` 的初始化逻辑恢复之前因重启而丢失的定时闹钟。
*   **音频焦点 (Audio Focus)**：闹钟播放时通过 `AudioManager` 抢占系统音频焦点 (`AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK`)，停止时必须释放，确保不与媒体应用冲突。
*   **前台服务终止 (Task Removed)**：当用户从多任务列表中划掉应用时，`TimerService` 必须重写 `onTaskRemoved` 来停止自身，防止内存泄漏和电量浪费。
*   不能使用 `Thread.sleep` 阻塞线程，状态流转必须通过 `StateFlow`。

## 5. 质量保证 (Quality Assurance)
*   核心业务逻辑必须有 JUnit 单元测试覆盖：
    *   `TimerRepository`：计时流转、状态恢复、addTime、saveCurrentState、RUNNING/PAUSED/IDLE 分支。
    *   `SettingsDataStore`：读取与写入，默认值，如 ringtone、flashScreen。
    *   `AlarmPlayer`：play/preview/stop 行为契约（包含 H-5 修复的 preview 与 play 互斥）。
    *   `SettingsViewModel`、`TimerViewModel`：防抖行为预期。
*   必须有仪器化跑通 Emulator 的端到端 (E2E) 验证：
    *   计时从 IDLE 到 RUNNING 到 FINISHED 到 下一阶段。
    *   暂停/继续/停止
    *   快进跳过
    *   进程死亡与后台恢复 (Process Death and Restore) 测试。
    *   字体缩放 (Font Scale) 兼容性测试。
*   视觉回归测试 (VRT)：覆盖 IDLE/RUNNING/PAUSED/FINISHED 在 FOCUS/SHORT_BREAK/LONG_BREAK 的关键帧快照，包括**深色模式 (Dark Theme)**。
*   针对测试环境，引入加速机制 (Time Multiplier) 确保 E2E 测试在合理时间内完成。
*   **测试隔离与稳定性 (Test Isolation & Determinism)**：
    *   **防止状态泄漏 (State Bleeding)**：在 E2E 测试销毁阶段，必须显式调用 `TimerRepository.destroyForTesting()` 强制取消全局协程作用域，彻底阻断后台心跳任务跨测试向 DataStore 写入脏数据。
    *   **消除 UI 测试抖动 (Flaky Tests)**：对异步发布的状态变更（如 `StateFlow` 的状态跳转），禁止使用瞬时的 `assertIsDisplayed()`，必须使用轮询重试机制的 `waitUntilTextExists()` 来安全等待 UI 响应。
    *   **Mock 防御编程**：对 `TimerAlarmReceiver` 等需要被手动触发的组件，必须做可空安全调用（如 `pendingResult?.finish()`）以兼容无原生上下文的测试环境。

## 6. 技术栈 (Tech Stack)
*   **UI**: Jetpack Compose, Material 3
*   **Architecture**: MVVM (Model-View-ViewModel) + Unidirectional Data Flow
*   **Dependency Injection**: Dagger Hilt
*   **Storage**: Jetpack DataStore (Preferences)
*   **Concurrency**: Kotlin Coroutines & Flow
*   **Background Execution / 后台执行**:
    *   AlarmManager.setAlarmClock() — 系统级精确闹钟，绕过 Doze 模式，保障深度休眠下准时触发
    *   TimerAlarmReceiver — 接收系统闹钟广播，采用 startForegroundService 主动拉起 Service 防止进程死亡时漏报
    *   TimerService — 独立前台服务，利用 StateFlow 内存热缓存 (Eagerly caching) 偏好设置，实现 O(1) 的零延迟 (Zero-Latency) 响铃
    *   FullScreenIntent — 在锁屏/息屏状态下弹出全屏通知唤醒用户

## 7. 权限说明 (Permissions)
*   POST_NOTIFICATIONS：用于在状态栏显示倒计时进度和前台服务通知。
*   USE_EXACT_ALARM / SCHEDULE_EXACT_ALARM：用于设定精确的倒计时结束时间，确保息屏状态下准时提醒。**注意：在 Android 14 (API 34)+ 及 Google Play 政策中，此类权限受到严格审查，本应用符合 "Clock / Timer" 豁免类目。**
*   FOREGROUND_SERVICE_SPECIAL_USE：在 Android 14+ 中声明特殊前台服务，并附带了具体的业务用途（计时与闹钟）。
*   WAKE_LOCK / USE_FULL_SCREEN_INTENT：用于在倒计时结束时点亮屏幕并弹出提醒界面。
*   VIBRATE：用于倒计时结束时的震动提醒。
*   RECEIVE_BOOT_COMPLETED：用于设备重启后自动恢复因关机被清除的倒计时精准闹钟。

## 8. 已知限制 (Known Limitations)
*   **System Time Manipulation (系统时间篡改)**: The app persists absolute wall-clock time (System.currentTimeMillis()) for active timers to survive reboots. If the user manually changes the system time or timezone while a timer is running in the background, the timer behavior will be undefined (it may finish instantly or take much longer).
*   **Test Environment (E2E测试环境)**: When running E2E Instrumented Tests (e.g., TomatoClockE2ETest), ensure that device animations are completely disabled in Developer Options (Window animation scale, Transition animation scale, Animator duration scale all set to Off) to prevent flaky test execution and timeout errors.
