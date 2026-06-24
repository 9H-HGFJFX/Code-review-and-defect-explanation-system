$ErrorActionPreference = 'SilentlyContinue'
$proc = Start-Process -FilePath 'mvn' -ArgumentList 'spring-boot:run','-Dspring-boot.run.profiles=dev' -WorkingDirectory 'E:\Desktop\version' -NoNewWindow -PassThru
Write-Host "Maven PID:" $proc.Id

# Poll for port 8080 up to 120 seconds
$maxWait = 120
$elapsed = 0
while ($elapsed -lt $maxWait) {
    Start-Sleep -Seconds 3
    $elapsed += 3
    $listening = Get-NetTCPConnection -LocalPort 8080 -State Listen -ErrorAction SilentlyContinue
    if ($listening) {
        Write-Host "PORT 8080 UP after $elapsed sec"
        break
    }
}
if (-not $listening) {
    Write-Host "TIMEOUT: port 8080 not up after $maxWait sec"
    exit 1
}

# Give it 5 more seconds to fully init
Start-Sleep -Seconds 5

# Test login
try {
    $body = '{"username":"admin","password":"admin123"}'
    $r = Invoke-WebRequest -Uri 'http://localhost:8080/api/auth/login' -Method POST -ContentType 'application/json' -Body $body -UseBasicParsing -TimeoutSec 10
    Write-Host "HTTP:" $r.StatusCode
    Write-Host "Body:" $r.Content.Substring(0, [Math]::Min(500, $r.Content.Length))
} catch {
    $e = $_
    Write-Host "HTTP:" $e.Exception.Response.StatusCode
    Write-Host "Msg:" $e.Exception.Message
}
