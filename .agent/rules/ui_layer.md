---
trigger: always_on
---

# UI Layer Rules

1. **CHINESE UI ONLY**: The user interface language facing end users must be exclusively in Chinese (Simplified). Ensure all UI text, labels, placeholders, and user-facing messages are written in Chinese. String resources (`strings.xml`) should be used for all texts.
2. **NO NATIVE POPUPS**: Never use generic Java `AlertDialog` indiscriminately. Prefer customized Material Design dialogs or Jetpack Compose Dialogs that match the application's aesthetic.
3. **STATE SYNCHRONIZATION (ViewModel vs UI)**: Never manage complex business state inside the UI component (Compose or Activity). UI should merely observe `StateFlow` or `LiveData` from the ViewModel and render accordingly (Unidirectional Data Flow).
4. **COMPOSE RENDER PURITY (IF USING COMPOSE)**: Never mutate state during the Compose render phase. Side effects must be wrapped in `LaunchedEffect` or `DisposableEffect`. State (`mutableStateOf`) must be used for any data that affects rendering logic.
5. **NO HARDCODED COLORS (Resources ONLY)**: Never use hardcoded colors (like `#FFFFFF` or `Color.White`) in UI components. Always use centralized color resources (`colors.xml`) or Compose `MaterialTheme.colorScheme` to guarantee seamless Light/Dark mode compatibility.
6. **LIFECYCLE AWARENESS**: Ensure that UI state collection is lifecycle-aware. Use `collectAsStateWithLifecycle()` in Compose or `repeatOnLifecycle` in XML/View-based fragments to avoid memory leaks and background crashes.
7. **COMPOSE NAVIGATION**: If using Jetpack Compose Navigation, use it for all internal app routing. Pass primitive IDs or strings as route arguments, and fetch complex data from the ViewModel based on the ID.
8. **PREVIEW SUPPORT**: All Compose UI components should include `@Preview` annotations with realistic mock data to facilitate UI iteration without deploying to a device.
