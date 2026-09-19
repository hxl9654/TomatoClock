param (
    [switch]$Help
)

if ($Help) {
    Write-Host "用法: .\run_tests.ps1"
    Write-Host "此脚本将依次执行: Lint 静态分析 → 本地单元测试 → 仪器化 UI 测试。"
    Write-Host "注意：如果本地运行的是 JDK 25，Robolectric 测试可能会报错。"
    exit
}

Write-Host "=========================================" -ForegroundColor Cyan
Write-Host "      TomatoClock 自动化测试执行器       " -ForegroundColor Cyan
Write-Host "=========================================" -ForegroundColor Cyan
Write-Host ""

Write-Host "[1/3] 开始执行 Lint 静态代码分析..." -ForegroundColor Yellow
.\gradlew lintDebug

$LintExit = $LASTEXITCODE

Write-Host ""
Write-Host "[2/3] 开始执行本地单元测试 (Unit Tests)..." -ForegroundColor Yellow
# 使用 --continue 确保即使部分测试(如Robolectric在JDK25下的异常)失败，也会继续执行真机测试
.\gradlew testDebugUnitTest --continue

$UnitTestExit = $LASTEXITCODE

Write-Host ""
Write-Host "[3/3] 开始执行仪器化测试 (Instrumented Tests)..." -ForegroundColor Yellow
Write-Host "注意：请确保已经连接了 Android 手机或者启动了模拟器，否则此步骤会失败。" -ForegroundColor DarkGray
.\gradlew connectedDebugAndroidTest --continue

$InstrumentedTestExit = $LASTEXITCODE

Write-Host ""
Write-Host "=========================================" -ForegroundColor Cyan
Write-Host "                测试完成                 " -ForegroundColor Cyan
Write-Host "=========================================" -ForegroundColor Cyan

function Confirm-TestCount {
    param([string]$ReportPath, [string]$TestType)
    if (Test-Path $ReportPath) {
        $content = Get-Content $ReportPath -Raw
        if ($content -match '<div class="infoBox" id="tests">\s*<div class="counter">0</div>') {
            Write-Host "❌ $TestType 运行了 0 个测试！(可能测试未被识别、配置错误或无设备)" -ForegroundColor Red
            return $false
        }
    } else {
        Write-Host "⚠️ 找不到 $TestType 的测试报告，无法验证测试数量。" -ForegroundColor Yellow
    }
    return $true
}

if ($LintExit -ne 0) {
    Write-Host "❌ Lint 静态分析发现错误，请查看 app/build/reports/lint-results-debug.html。" -ForegroundColor Red
} else {
    Write-Host "✅ Lint 静态分析通过！" -ForegroundColor Green
}

$UnitTestValid = $true
if ($UnitTestExit -ne 0) {
    Write-Host "❌ 本地单元测试有失败项 (注意: 如果您使用的是 JDK 25，Robolectric 本地测试可能不兼容)。" -ForegroundColor Red
} else {
    $UnitTestValid = Confirm-TestCount ".\app\build\reports\tests\testDebugUnitTest\index.html" "本地单元测试"
    if ($UnitTestValid) {
        Write-Host "✅ 本地单元测试全部通过！" -ForegroundColor Green
    }
}

$InstrumentedTestValid = $true
if ($InstrumentedTestExit -ne 0) {
    Write-Host "❌ 仪器化测试有失败项 (请检查是否已连接设备或代码逻辑报错)。" -ForegroundColor Red
} else {
    $InstrumentedTestValid = Confirm-TestCount ".\app\build\reports\androidTests\connected\debug\index.html" "仪器化测试"
    if ($InstrumentedTestValid) {
        Write-Host "✅ 仪器化测试全部通过！" -ForegroundColor Green
    }
}

if ($LintExit -eq 0 -and $UnitTestExit -eq 0 -and $InstrumentedTestExit -eq 0 -and $UnitTestValid -and $InstrumentedTestValid) {
    Write-Host ""
    Write-Host "🎉 恭喜！Lint + 所有测试均已成功通过！" -ForegroundColor Green
} else {
    Write-Host ""
    Write-Host "⚠️ 部分检查未通过（或执行了 0 个测试），请查看上方日志。" -ForegroundColor Yellow
    exit 1
}
