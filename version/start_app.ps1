# Start Spring Boot app in background
$ErrorActionPreference = 'SilentlyContinue'
$proc = Start-Process -FilePath 'mvn' -ArgumentList 'spring-boot:run','-Dspring-boot.run.profiles=dev' -WorkingDirectory 'E:\Desktop\version' -NoNewWindow -PassThru -RedirectStandardOutput 'E:\Desktop\version\app.log' -RedirectStandardError 'E:\Desktop\version\app_err.log'
Write-Host "Started PID: $($proc.Id)"

# Wait for app to start (poll for port 8080)
$maxWait = 90
$elapsed = 0
while ($elapsed -lt $maxWait) {
    Start-Sleep -Seconds 2
    $elapsed += 2
    $listening = Get-NetTCPConnection -LocalPort 8080 -State Listen -ErrorAction SilentlyContinue
    if ($listening) {
        Write-Host "App ready on port 8080 after $elapsed seconds"
        break
    }
    Write-Host "Waiting... $elapsed sec"
}

if (-not $listening) {
    Write-Host "ERROR: App did not start within $maxWait seconds"
    exit 1
}

# Test login endpoint
try {
    $body = '{"username":"admin","password":"admin123"}'
    $r = Invoke-WebRequest -Uri 'http://localhost:8080/api/auth/login' -Method POST -ContentType 'application/json' -Body $body -UseBasicParsing -TimeoutSec 10
    Write-Host "HTTP Status: $($r.StatusCode)"
    Write-Host "Response: $($r.Content)"
} catch {
    Write-Host "Exception: $($_.Exception.Message)"
    $resp = $_.Exception.Response
    if ($resp) {
        Write-Host "HTTP Status: $($resp.StatusCode)"
    }
}
