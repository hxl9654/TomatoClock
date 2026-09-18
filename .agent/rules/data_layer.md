---
trigger: always_on
---

# Data Layer & Architecture Rules

1. **NO FAT ACTIVITIES/FRAGMENTS (THIN UI CONTROLLERS)**: Android Activities and Fragments must act strictly as thin UI controllers. All business logic, complex data transformations, and state management must be extracted into a dedicated `ViewModel` to ensure separation of concerns and testability.
2. **DATABASE QUERY OPTIMIZATION (ROOM)**: Never execute sequential database queries on the main thread. Always use Kotlin Coroutines (`Dispatchers.IO`) or Kotlin `Flow` with Room Database to perform asynchronous database operations.
3. **Exception Visibility (NO Exception Swallowing)**: Indiscriminately swallowing errors (e.g., empty `catch {}` block) is strictly prohibited. Every `catch` block in Coroutines and ViewModels must include at least `Log.e()` or `Timber.e()` to ensure production troubleshooting visibility.
4. **SINGLE SOURCE OF TRUTH (REPOSITORY PATTERN)**: When managing application data, never fetch or manipulate data directly from the ViewModel. Always use the Repository pattern as the single source of truth to coordinate between local database (Room) and remote API (Retrofit/Ktor).
5. **OFFLINE RESILIENCE**: When making API calls that mutate data, consider providing offline fallbacks or queueing (e.g. using WorkManager) to ensure progress isn't lost during poor network conditions.
6. **DEPENDENCY INJECTION**: Never instantiate ViewModels, Repositories, or DataSources manually in Production if a DI framework is available. Prefer using a Dependency Injection framework like Hilt or Dagger to manage dependencies and lifecycles.
7. **NO CONTEXT LEAKS IN VIEWMODEL**: ViewModels must absolutely never hold references to `Activity`, `Fragment`, or `View` instances to prevent memory leaks. If a Context is absolutely required, use `ApplicationContext` or inherit from `AndroidViewModel`.
