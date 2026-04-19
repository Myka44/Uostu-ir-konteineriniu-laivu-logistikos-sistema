<#
run-all.ps1

Starts the backend (using the included mvnw.cmd) and the frontend (npm dev server),
monitors both, and restarts both if either process exits. Designed for Windows PowerShell.

Usage: .\run-all.ps1
#>

param(
    [int]$RestartDelay = 1
)

function Start-Processes {
    Write-Host "Starting backend..."
    # Start backend in a new PowerShell window and keep it open while the backend runs
    $backendCmd = "cd '$((Get-Location).Path)'; .\mvnw.cmd spring-boot:run"
    $backend = Start-Process -FilePath "powershell.exe" -ArgumentList "-NoExit","-Command", $backendCmd -PassThru

    Write-Host "Starting frontend..."
    # Start frontend in a separate PowerShell window and keep it open while dev server runs
    $frontendDir = (Join-Path (Get-Location).Path "frontend")
    $frontendCmd = "cd '$frontendDir'; npm install; npm run dev"
    $frontend = Start-Process -FilePath "powershell.exe" -ArgumentList "-NoExit","-Command", $frontendCmd -PassThru
    return @($backend, $frontend)
}
# Start both processes and exit this script immediately.
$procs = Start-Processes
Write-Host "Started backend (PID: $($procs[0].Id)) and frontend (PID: $($procs[1].Id)). Exiting run-all script." -ForegroundColor Green
