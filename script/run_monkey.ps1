$ErrorActionPreference = "Stop"

Write-Host "Running Android UI Exerciser Monkey Test..."
Write-Host "Make sure an emulator or device is connected."

$PACKAGE = "com.hxlxz.tomatoclock"
$EVENTS = 5000

# Locate adb
$adb = "adb"
if (-not (Get-Command $adb -ErrorAction SilentlyContinue)) {
    if ($env:ANDROID_HOME) {
        $adb = Join-Path $env:ANDROID_HOME "platform-tools\adb.exe"
    } else {
        $adb = Join-Path $env:LOCALAPPDATA "Android\Sdk\platform-tools\adb.exe"
    }
}

if (-not (Test-Path $adb -ErrorAction SilentlyContinue) -and -not (Get-Command $adb -ErrorAction SilentlyContinue)) {
    Write-Error "Could not find adb. Please ensure Android SDK is installed or adb is in your PATH."
    exit 1
}

Write-Host "Installing the latest debug build..."
.\gradlew.bat installDebug
if ($LASTEXITCODE -ne 0) {
    Write-Error "Failed to install the app."
    exit $LASTEXITCODE
}

Write-Host "Starting app and pinning screen to prevent Monkey escapes..."
& $adb shell am start -W -n "$PACKAGE/.MainActivity" | Out-Null

$output = & $adb shell dumpsys activity activities
$taskId = $null
foreach ($line in $output) {
    if ($line -match "Task\{[a-f0-9]+\s+#(\d+)\s+.*$PACKAGE") {
        $taskId = $matches[1]
        break
    }
}

if ($taskId) {
    Write-Host "Locking task $taskId to screen..."
    & $adb shell am task lock $taskId | Out-Null
    Write-Host ""
    Write-Host "=========================================================" -ForegroundColor Yellow
    Write-Host ">>> PLEASE CLICK 'GOT IT' (我知道了) ON THE EMULATOR! <<<" -ForegroundColor Yellow
    Write-Host "=========================================================" -ForegroundColor Yellow
    Write-Host ""
    Write-Host "Waiting 5 seconds for you to confirm the dialog..."
    Start-Sleep -Seconds 5
} else {
    Write-Warning "Failed to find task ID. Monkey might escape."
}

Write-Host "Starting Monkey Test with $EVENTS events using $adb..."
& $adb shell monkey -p $PACKAGE --pct-syskeys 0 --pct-appswitch 0 --pct-anyevent 0 --ignore-crashes --ignore-timeouts -v $EVENTS
$monkeyExit = $LASTEXITCODE

Write-Host "Unpinning task..."
& $adb shell am task lock stop | Out-Null

if ($monkeyExit -ne 0) {
    Write-Error "Monkey test encountered an issue or crashed."
    exit $LASTEXITCODE
}

Write-Host "Monkey test completed successfully!" -ForegroundColor Green
