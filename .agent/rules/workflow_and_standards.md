---
trigger: always_on
---

# Workflow, Workspace & Coding Standards

1. **WORKSPACE HYGIENE**: Any temporary, one-off, or utility scripts created during AI coding sessions MUST be placed in the `scratch/` directory. Do NOT place temporary scripts in the project root.
2. **GIT OPERATIONS**: `git add` & `git commit` & `git restore` and other dangerous git commands can only be executed by the human user.
3. **BILINGUAL DOCUMENTATION**: When generating human-facing documents (such as Implementation Plans or Walkthroughs), the text must be in both Chinese and English.
4. **FILE ENCODING**: When modifying files, you must pay attention to the character set encoding (e.g. use UTF-8) to avoid generating garbled text.
5. **POWERSHELL COMMAND & SYNTAX COMPATIBILITY**: Never use `&&` to chain commands in terminal executions. Furthermore, never use Linux/Unix-specific utilities like `grep`, `sed`, `awk`, `cat`, `ls`, `rm`, `cp`, `mv`, `touch`, `export`, or `source` in terminal executions in Windows.
6. **KOTLIN TYPE SAFETY**: Strictly avoid using `Any` unnecessarily. Use sealed classes, enums, and generics to strictly model state and data types. Nullability must be strictly handled using `?` and safe calls (`?.`, `?:`), avoid using the `!!` operator unless absolutely guaranteed.
7. **GRADLE SYNC**: Whenever dependencies or Gradle build scripts are modified, assure that the changes are valid so that `gradlew sync` passes successfully.
8. **DOCUMENTATION SYNC (REQUIREMENTS/README)**: Whenever a task is completed, you MUST proactively update the `REQUIREMENTS.md` and/or `README.md` documents to accurately reflect the changes.
9. **DESIGN-PLAN-EXECUTE-VERIFY**: For any non-trivial task, you must strictly follow the 4-step process: 1. Design, 2. Plan, 3. Execute, 4. Verify.
10. **MAP SERVICE**: Use Tianditu (天地图) API or Baidu/Amap for all map-related features, avoiding foreign map services due to offset issues in China.
11. **RESOURCE MANAGEMENT**: Never hardcode string literals, dimensions (dp/sp), or colors in code. Always extract them to `res/values/strings.xml`, `dimens.xml`, and `colors.xml`.
12. **PROGUARD / R8**: Whenever adding data models that will be parsed from JSON (e.g., using Gson/Moshi/Kotlinx.serialization), ensure they are annotated properly (`@Keep` or `@Serializable`) to prevent R8 from obfuscating them and breaking the parser in release builds.