[Console]::OutputEncoding = [System.Text.Encoding]::UTF8

$baseUrl = 'http://localhost:8080/api'
$report = New-Object System.Collections.ArrayList

function Log($step, $desc, $status, $detail = '') {
    $icon = switch ($status) {
        'PASS' { '[PASS]' }
        'FAIL' { '[FAIL]' }
        'INFO' { '[INFO]' }
        default { '[----]' }
    }
    Write-Host "$icon $step - $desc" -ForegroundColor $(if($status -eq 'PASS'){'Green'}elseif($status -eq 'FAIL'){'Red'}else{'Cyan'})
    if ($detail) { Write-Host "       $detail" -ForegroundColor Gray }
    $script:report += [PSCustomObject]@{Step=$step; Desc=$desc; Status=$status; Detail=$detail}
}

# Step 1: Login
Write-Host "`n=== Test Case 1: User Login ===" -ForegroundColor Yellow
try {
    $body = '{"username":"admin","password":"admin123"}'
    $r = Invoke-WebRequest -Uri "$baseUrl/auth/login" -Method POST -ContentType 'application/json' -Body $body -UseBasicParsing -TimeoutSec 10
    $resp = $r.Content | ConvertFrom-Json
    if ($resp.code -eq 200 -and $resp.data.accessToken) {
        $global:token = $resp.data.accessToken
        $global:user = $resp.data.user
        Log '1.1' 'Login with admin/admin123' 'PASS' "User: $($global:user.username), Role: $($global:user.role), Token: $($global:token.Substring(0,30))..."
    } else {
        Log '1.1' 'Login response' 'FAIL' $r.Content
        exit
    }
} catch {
    Log '1.1' 'Login request' 'FAIL' $_.Exception.Message
    exit
}

$hdr = @{Authorization = "Bearer $global:token"}

# Step 2: Query existing classes
Write-Host "`n=== Test Case 2: Browse Classes ===" -ForegroundColor Yellow
try {
    $r = Invoke-WebRequest -Uri "$baseUrl/classes" -Headers $hdr -UseBasicParsing -TimeoutSec 5
    $resp = $r.Content | ConvertFrom-Json
    Log '2.1' 'GET /api/classes' 'PASS' "Found $($resp.data.Count) classes"
    $global:classId = $resp.data[0].classId
    Log '2.2' 'Use first class for testing' 'INFO' "Class ID: $global:classId, Name: $($resp.data[0].name)"
} catch {
    Log '2.1' 'List classes' 'FAIL' $_.Exception.Message
}

# Step 3: Browse rules
Write-Host "`n=== Test Case 3: Browse Review Rules ===" -ForegroundColor Yellow
try {
    $r = Invoke-WebRequest -Uri "$baseUrl/rules?pageSize=20" -Headers $hdr -UseBasicParsing -TimeoutSec 5
    $resp = $r.Content | ConvertFrom-Json
    Log '3.1' 'GET /api/rules' 'PASS' "Found $($resp.data.pagination.total) rules total"
    if ($resp.data.list.Count -gt 0) {
        $global:ruleIds = ($resp.data.list | Select-Object -First 3 -ExpandProperty id) -join ','
        Log '3.2' 'Pick first 3 rules' 'INFO' "Rule IDs: $global:ruleIds"
    }
} catch {
    Log '3.1' 'List rules' 'FAIL' $_.Exception.Message
}

# Step 4: Submit code for review
Write-Host "`n=== Test Case 4: Submit Code Review Task ===" -ForegroundColor Yellow
try {
    $code = @'
public class LoginService {
    public boolean login(String user, String pwd) {
        String password = "admin123";
        if (user == null || user == "") return false;
        if (pwd.equals(password)) {
            System.out.println("Login success for " + user);
            return true;
        }
        return false;
    }
}
'@
    $codeBase64 = [Convert]::ToBase64String([System.Text.Encoding]::UTF8.GetBytes($code))
    $taskBody = @{
        title = 'Lab1 - Student Code Review Demo'
        description = 'Auto-generated test submission'
        classId = $global:classId
        deadline = '2026-12-31T23:59:59'
        ruleIds = @()
        repoUrl = 'inline-submission'
        sourceCode = $codeBase64
    } | ConvertTo-Json -Depth 5
    $r = Invoke-WebRequest -Uri "$baseUrl/tasks" -Method POST -Headers $hdr -ContentType 'application/json' -Body $taskBody -UseBasicParsing -TimeoutSec 10
    $resp = $r.Content | ConvertFrom-Json
    if ($resp.code -eq 200) {
        $global:taskId = $resp.data.id
        Log '4.1' 'POST /api/tasks (create review task)' 'PASS' "Task ID: $global:taskId, Title: $($resp.data.title)"
    } else {
        Log '4.1' 'Create task' 'FAIL' $r.Content
    }
} catch {
    Log '4.1' 'Create task' 'FAIL' $_.Exception.Message
}

# Step 5: Check task in list
Write-Host "`n=== Test Case 5: Verify Task Created ===" -ForegroundColor Yellow
try {
    $r = Invoke-WebRequest -Uri "$baseUrl/tasks?pageSize=20" -Headers $hdr -UseBasicParsing -TimeoutSec 5
    $resp = $r.Content | ConvertFrom-Json
    $found = $resp.data.list | Where-Object { $_.id -eq $global:taskId }
    if ($found) {
        Log '5.1' 'GET /api/tasks contains new task' 'PASS' "Title: $($found.title), Status: $($found.status)"
    } else {
        Log '5.1' 'GET /api/tasks contains new task' 'FAIL' 'Task not found'
    }
} catch {
    Log '5.1' 'Verify task' 'FAIL' $_.Exception.Message
}

# Step 6: Token refresh
Write-Host "`n=== Test Case 6: Token Refresh ===" -ForegroundColor Yellow
try {
    $loginResp = Invoke-WebRequest -Uri "$baseUrl/auth/login" -Method POST -ContentType 'application/json' -Body '{"username":"admin","password":"admin123"}' -UseBasicParsing -TimeoutSec 10
    $refreshToken = ($loginResp.Content | ConvertFrom-Json).data.refreshToken
    $r = Invoke-WebRequest -Uri "$baseUrl/auth/refresh" -Method POST -Headers $hdr -ContentType 'application/json' -Body (@{refreshToken=$refreshToken} | ConvertTo-Json) -UseBasicParsing -TimeoutSec 5
    $resp = $r.Content | ConvertFrom-Json
    if ($resp.code -eq 200 -and $resp.data.accessToken) {
        Log '6.1' 'POST /api/auth/refresh' 'PASS' "New token issued, expires in: $($resp.data.expiresIn)s"
    } else {
        Log '6.1' 'Token refresh' 'FAIL' $r.Content
    }
} catch {
    Log '6.1' 'Token refresh' 'FAIL' $_.Exception.Message
}

# Step 7: Auth failure - wrong password
Write-Host "`n=== Test Case 7: Auth Security Check ===" -ForegroundColor Yellow
try {
    $r = Invoke-WebRequest -Uri "$baseUrl/auth/login" -Method POST -ContentType 'application/json' -Body '{"username":"admin","password":"wrongpwd"}' -UseBasicParsing -TimeoutSec 5
    $resp = $r.Content | ConvertFrom-Json
    if ($resp.code -eq 401 -or $resp.code -eq 1000 -or $resp.code -eq 1004) {
        Log '7.1' 'Reject wrong password' 'PASS' "Response code: $($resp.code), message: $($resp.message)"
    } else {
        Log '7.1' 'Wrong password rejected' 'FAIL' "Got code: $($resp.code)"
    }
} catch {
    if ($_.Exception.Response.StatusCode -eq 401) {
        Log '7.1' 'Reject wrong password (HTTP 401)' 'PASS' 'Correctly rejected'
    } else {
        Log '7.1' 'Auth check' 'FAIL' $_.Exception.Message
    }
}

# Summary
Write-Host "`n=== TEST SUMMARY ===" -ForegroundColor Yellow
$pass = ($report | Where-Object Status -eq 'PASS').Count
$fail = ($report | Where-Object Status -eq 'FAIL').Count
$info = ($report | Where-Object Status -eq 'INFO').Count
Write-Host "PASS: $pass  FAIL: $fail  INFO: $info" -ForegroundColor $(if($fail -eq 0){'Green'}else{'Red'})

# Generate HTML report
$reportHtml = ""
foreach ($r in $report) {
    $color = switch ($r.Status) { 'PASS' {'#2f9e44'} 'FAIL' {'#c92a2a'} default {'#1971c2'} }
    $icon = switch ($r.Status) { 'PASS' {'[PASS]'} 'FAIL' {'[FAIL]'} default {'[INFO]'} }
    $reportHtml += "<tr><td><span style='color:$color;font-weight:600'>$icon</span></td><td>$($r.Step)</td><td>$($r.Desc)</td><td style='font-size:12px;color:#666'>$($r.Detail)</td></tr>"
}

$passColor = if($fail -eq 0){'#2f9e44'}else{'#c92a2a'}

$css = @'
body{font-family:-apple-system,Microsoft YaHei,sans-serif;background:linear-gradient(135deg,#667eea,#764ba2);min-height:100vh;margin:0;padding:30px;color:#333}
.container{max-width:1200px;margin:0 auto;background:#fff;border-radius:16px;box-shadow:0 20px 60px rgba(0,0,0,.2);overflow:hidden}
.header{padding:40px;background:linear-gradient(135deg,#1e3c72,#2a5298);color:#fff}
.header h1{margin:0;font-size:28px}
.header .sub{opacity:.9;margin-top:8px;font-size:14px}
.summary{display:grid;grid-template-columns:repeat(3,1fr);gap:1px;background:#e9ecef}
.summary .cell{padding:25px;background:#fff;text-align:center}
.summary .num{font-size:36px;font-weight:700}
.summary .lbl{color:#6c757d;font-size:13px;margin-top:6px;text-transform:uppercase}
.section{padding:30px}
.section-title{font-size:20px;color:#1e3c72;margin-bottom:20px}
table{width:100%;border-collapse:collapse}
th,td{padding:12px;text-align:left;border-bottom:1px solid #e9ecef}
th{background:#f8f9fa;color:#495057;font-weight:600;font-size:13px}
tr:hover{background:#f8f9ff}
.footer{padding:20px;text-align:center;color:#868e96;background:#f8f9fa;font-size:13px}
'@

$html = @"
<!DOCTYPE html><html><head><meta charset="UTF-8"><title>Test Report</title>
<style>$css</style></head><body>
<div class="container">
<div class="header">
<h1>Test Execution Report</h1>
<div class="sub">Code Review System - Functional Test Suite</div>
</div>
<div class="summary">
<div class="cell"><div class="num" style="color:$passColor">$pass</div><div class="lbl">Passed</div></div>
<div class="cell"><div class="num" style="color:#c92a2a">$fail</div><div class="lbl">Failed</div></div>
<div class="cell"><div class="num" style="color:#1971c2">$info</div><div class="lbl">Info</div></div>
</div>
<div class="section">
<div class="section-title">Test Results</div>
<table>
<thead><tr><th>Status</th><th>Step</th><th>Description</th><th>Details</th></tr></thead>
<tbody>$reportHtml</tbody>
</table>
</div>
<div class="footer">Generated at $(Get-Date -Format 'yyyy-MM-dd HH:mm:ss') &middot; Backend: Spring Boot 8080</div>
</div></body></html>
"@

[System.IO.File]::WriteAllText('E:\Desktop\version\test_report.html', $html, [System.Text.Encoding]::UTF8)
Write-Host "`nReport saved: E:\Desktop\version\test_report.html" -ForegroundColor Green