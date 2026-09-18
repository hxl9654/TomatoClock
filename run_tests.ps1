param (
    [switch]$Help
)

if ($Help) {
    Write-Host "用法: .\run_tests.ps1"
    Write-Host "此脚本将一键运行所有测试，包括本地单元测试和模拟器/真机的 UI 仪器测试。"
    Write-Host "注意：如果本地运行的是 JDK 25，Robolectric 测试可能会报错。"
    exit
}

Write-Host "=========================================" -ForegroundColor Cyan
Write-Host "      TomatoClock 自动化测试执行器       " -ForegroundColor Cyan
Write-Host "=========================================" -ForegroundColor Cyan
Write-Host ""

Write-Host "[1/2] 开始执行本地单元测试 (Unit Tests)..." -ForegroundColor Yellow
# 使用 --continue 确保即使部分测试(如Robolectric在JDK25下的异常)失败，也会继续执行真机测试
.\gradlew testDebugUnitTest --continue

$UnitTestExit = $LASTEXITCODE

Write-Host ""
Write-Host "[2/2] 开始执行仪器化测试 (Instrumented Tests)..." -ForegroundColor Yellow
Write-Host "注意：请确保已经连接了 Android 手机或者启动了模拟器，否则此步骤会失败。" -ForegroundColor DarkGray
.\gradlew connectedDebugAndroidTest --continue

$InstrumentedTestExit = $LASTEXITCODE

Write-Host ""
Write-Host "=========================================" -ForegroundColor Cyan
Write-Host "                测试完成                 " -ForegroundColor Cyan
Write-Host "=========================================" -ForegroundColor Cyan

if ($UnitTestExit -ne 0) {
    Write-Host "❌ 本地单元测试有失败项 (注意: 如果您使用的是 JDK 25，Robolectric 本地测试可能不兼容)。" -ForegroundColor Red
} else {
    Write-Host "✅ 本地单元测试全部通过！" -ForegroundColor Green
}

if ($InstrumentedTestExit -ne 0) {
    Write-Host "❌ 仪器化测试有失败项 (请检查是否已连接设备或代码逻辑报错)。" -ForegroundColor Red
} else {
    Write-Host "✅ 仪器化测试全部通过！" -ForegroundColor Green
}

if ($UnitTestExit -eq 0 -and $InstrumentedTestExit -eq 0) {
    Write-Host ""
    Write-Host "🎉 恭喜！所有测试均已成功通过！" -ForegroundColor Green
} else {
    Write-Host ""
    Write-Host "⚠️ 部分测试未通过，请查看上方日志或 build/reports/tests 目录下的报告。" -ForegroundColor Yellow
    exit 1
}
