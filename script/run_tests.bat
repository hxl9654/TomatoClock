@echo off
chcp 65001 >nul
echo =========================================
echo       TomatoClock 自动化测试执行器       
echo =========================================
echo.

echo [1/3] 开始执行 Lint 静态代码分析...
echo.
call gradlew.bat lintDebug

set LINT_EXIT=%ERRORLEVEL%

echo.
echo [2/3] 开始执行本地单元测试 (Unit Tests)...
echo.
call gradlew.bat testDebugUnitTest --continue

set UNIT_TEST_EXIT=%ERRORLEVEL%

echo.
echo [3/3] 开始执行仪器化测试 (Instrumented Tests)...
echo 注意：请确保已经连接了 Android 手机或者启动了模拟器，否则此步骤会失败。
echo.
call gradlew.bat connectedDebugAndroidTest --continue

set INST_TEST_EXIT=%ERRORLEVEL%

echo.
echo =========================================
echo                 测试完成                 
echo =========================================
echo.

if %LINT_EXIT% neq 0 (
    echo [X] Lint 静态分析发现错误，请查看 app/build/reports/lint-results-debug.html。
) else (
    echo [v] Lint 静态分析通过！
)

if %UNIT_TEST_EXIT% neq 0 (
    echo [X] 本地单元测试有失败项 (注意: 如果您使用的是 JDK 25，Robolectric 本地测试可能不兼容)。
) else (
    echo [v] 本地单元测试全部通过！
)

if %INST_TEST_EXIT% neq 0 (
    echo [X] 仪器化测试有失败项 (请检查是否已连接设备或代码逻辑报错)。
) else (
    echo [v] 仪器化测试全部通过！
)

if %LINT_EXIT% equ 0 if %UNIT_TEST_EXIT% equ 0 if %INST_TEST_EXIT% equ 0 (
    echo.
    echo 恭喜！Lint + 所有测试均已成功通过！
    exit /b 0
) else (
    echo.
    echo 部分检查未通过，请查看上方日志或 app/build/reports 目录下的报告。
    exit /b 1
)
