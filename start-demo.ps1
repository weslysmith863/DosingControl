<#
.SYNOPSIS
    Brings up the full Adaptive Dosing Control Platform stack for a live demo:
    MySQL, the Spring Boot API, and the Python Modbus simulator, in the order
    each one depends on the last, then opens the Ignition Gateway.

    Assumes MySQL and Ignition are installed as local Windows services, and
    that DB_USERNAME / DB_PASSWORD are already set as environment variables
    on this machine (see README.md). This script never reads or sets either
    one directly, it just launches processes that inherit them.
#>

$ErrorActionPreference = "Stop"
$repoRoot = $PSScriptRoot

function Wait-ForPort {
    param(
        [string]$ComputerName = "localhost",
        [int]$Port,
        [string]$Label,
        [int]$TimeoutSeconds = 60
    )
    Write-Host "Waiting for $Label on port $Port..."
    $deadline = (Get-Date).AddSeconds($TimeoutSeconds)
    while ((Get-Date) -lt $deadline) {
        if (Test-NetConnection -ComputerName $ComputerName -Port $Port -InformationLevel Quiet -WarningAction SilentlyContinue) {
            Write-Host "$Label is up." -ForegroundColor Green
            return
        }
        Start-Sleep -Seconds 2
    }
    throw "$Label did not come up on port $Port within $TimeoutSeconds seconds."
}

function Start-ServiceIfStopped {
    param([string]$Name)
    $svc = Get-Service -Name $Name -ErrorAction SilentlyContinue
    if (-not $svc) {
        Write-Host "No service named '$Name' found, skipping." -ForegroundColor Yellow
        return
    }
    if ($svc.Status -ne "Running") {
        Write-Host "Starting service '$Name'..."
        Start-Service -Name $Name
    } else {
        Write-Host "Service '$Name' already running." -ForegroundColor Green
    }
}

# 1. MySQL
Start-ServiceIfStopped -Name "MySQL80"
Wait-ForPort -Port 3306 -Label "MySQL"

# 2. Spring Boot API (new window, inherits DB_USERNAME/DB_PASSWORD from the environment)
Write-Host "Starting Spring Boot API..."
Start-Process -FilePath "powershell" -WorkingDirectory $repoRoot `
    -ArgumentList "-NoExit", "-Command", ".\mvnw.cmd spring-boot:run"
Wait-ForPort -Port 8080 -Label "Spring Boot API" -TimeoutSeconds 120

# 3. Python Modbus simulator (new window)
Write-Host "Starting Modbus simulator..."
Start-Process -FilePath "powershell" -WorkingDirectory (Join-Path $repoRoot "simulator") `
    -ArgumentList "-NoExit", "-Command", "python simulator.py"
Wait-ForPort -Port 5020 -Label "Modbus simulator"

# 4. Ignition Gateway (normally already running as a service)
Start-ServiceIfStopped -Name "Ignition"
Wait-ForPort -Port 8088 -Label "Ignition Gateway"

Write-Host ""
Write-Host "Full stack is up: MySQL (3306), Spring Boot API (8080), Modbus simulator (5020), Ignition Gateway (8088)." -ForegroundColor Cyan
Start-Process "http://localhost:8088"
