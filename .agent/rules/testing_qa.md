---
trigger: always_on
---

# Testing & Quality Assurance Rules

1. **ZERO HUMAN QA (AUTOMATION ONLY)**: Automated testing must be complete, effective, and perfectly accurate. Rely on automated testing exclusively as the ultimate gatekeeper for code quality.
2. **DETERMINISTIC UI TESTING (ESPRESSO / COMPOSE TEST RULE)**: 
   - Never use brittle sleep logic (e.g. `Thread.sleep()`).
   - Always rely on built-in idle synchronization in Espresso or Compose `waitUntil`.
   - Explicitly wait for specific UI elements to verify data loading rather than asserting on intermediate "Loading..." states.
3. **STRICT UNIT TESTING**: All non-UI tests must strictly adhere to the following deterministic standards:
   - **Isolated State**: Tests must use dedicated mock objects (e.g., MockK, Mockito) or fake repositories.
   - **Coroutine Testing**: Unit tests must strictly use `runTest` and `UnconfinedTestDispatcher` for Coroutine logic. Global timers (`delay`) should be bypassed using `advanceTimeBy` or `advanceUntilIdle`.
4. **TEST-DRIVEN WORKFLOW**: Whenever a feature is added or modified, you MUST create or update the corresponding test cases. 
5. **TEST COMMANDS**: You should use the following standard gradle commands for testing:
   - `gradlew testDebugUnitTest`: Runs local unit tests.
   - `gradlew connectedDebugAndroidTest`: Runs UI/instrumented tests on connected devices or emulators.
   - `gradlew lintDebug`: Runs static code analysis.
6. **COMPREHENSIVE COVERAGE**: Never ignore or skip writing Unit tests simply on the grounds that the feature is "already covered by UI tests".
7. **DEPENDENCY INJECTION IN TESTS**: Use your DI framework (like Hilt) for testing in instrumented tests when full integration is needed, and use constructor injection for unit tests to easily pass mocked dependencies.
