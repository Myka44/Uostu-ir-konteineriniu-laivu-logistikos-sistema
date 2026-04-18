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
    Start-Sleep -Milliseconds 500
    Write-Host "Starting frontend..."
    # Start frontend in a separate PowerShell window and keep it open while dev server runs
    $frontendDir = (Join-Path (Get-Location).Path "frontend")
    $frontendCmd = "cd '$frontendDir'; npm install; npm run dev"
    $frontend = Start-Process -FilePath "powershell.exe" -ArgumentList "-NoExit","-Command", $frontendCmd -PassThru
    return @($backend, $frontend)
}

while ($true) {
    $procs = Start-Processes
    Write-Host "Both processes started. Monitoring..."
    $exited = $false
    while (-not $exited) {
        foreach ($p in $procs) {
            if ($p.HasExited) {
                Write-Host "Process $($p.Id) exited with code $($p.ExitCode). Restarting both..." -ForegroundColor Yellow
                $exited = $true
                break
            }
        }
        Start-Sleep -Seconds 1
    }

    Start-Sleep -Seconds 1
    foreach ($p in $procs) {
        if (-not $p.HasExited) {
            try {
                Write-Host "Stopping process $($p.Id)..." -ForegroundColor Cyan
                Stop-Process -Id $p.Id -Force -ErrorAction SilentlyContinue
            } catch {}
        }
    }

    Write-Host "Restarting in $RestartDelay seconds..." -ForegroundColor Green
    Start-Sleep -Seconds $RestartDelay
}
