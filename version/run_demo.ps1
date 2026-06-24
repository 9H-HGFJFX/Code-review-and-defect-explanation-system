#encoding UTF-8
[Console]::OutputEncoding = [System.Text.Encoding]::UTF8

Write-Host '[1] Login...' -ForegroundColor Cyan
$body = '{"username":"admin","password":"admin123"}'
$r = Invoke-WebRequest -Uri 'http://localhost:8080/api/auth/login' -Method POST -ContentType 'application/json' -Body $body -UseBasicParsing -TimeoutSec 10
$resp = $r.Content | ConvertFrom-Json
$token = $resp.data.accessToken
$user = $resp.data.user
$hdr = @{Authorization = "Bearer $token"}
Write-Host "  Logged in as $($user.username) ($($user.role))" -ForegroundColor Green

Write-Host '[2] Create class...' -ForegroundColor Cyan
$classBody = '{"name":"Software Engineering 2024-1","teacherId":1,"description":"Student code review class"}'
$r = Invoke-WebRequest -Uri 'http://localhost:8080/api/classes' -Method POST -Headers $hdr -ContentType 'application/json' -Body $classBody -UseBasicParsing -TimeoutSec 10
$classResp = $r.Content | ConvertFrom-Json
$classId = $classResp.data.id
Write-Host "  Created class ID=$classId" -ForegroundColor Green

Write-Host '[3] Create rules...' -ForegroundColor Cyan
$rules = @(
    @{name='R001-NoHardcodedPwd';category='SECURITY';severity='CRITICAL';pattern='password\s*=\s*["''][^"'']+["'']';enabled=$true},
    @{name='R002-NoSystemOut';category='PERFORMANCE';severity='MEDIUM';pattern='System\.out\.print';enabled=$true},
    @{name='R003-NoTabs';category='STYLE';severity='LOW';pattern='\\t';enabled=$true}
)
$createdRules = @()
foreach ($r0 in $rules) {
    $rb = $r0 | ConvertTo-Json -Compress
    $resp = Invoke-WebRequest -Uri 'http://localhost:8080/api/rules' -Method POST -Headers $hdr -ContentType 'application/json' -Body $rb -UseBasicParsing -TimeoutSec 10
    $rd = $resp.Content | ConvertFrom-Json
    $createdRules += $rd.data
    Write-Host "  Created rule $($rd.data.name) (ID=$($rd.data.id))" -ForegroundColor Green
}

Write-Host '[4] Create review task...' -ForegroundColor Cyan
$taskBody = @{
    title = 'Homework 1 - Code Review'
    description = 'Scan student Java code for security and style issues'
    classId = $classId
    deadline = '2026-07-01T23:59:59'
    ruleIds = $createdRules.id
    repoUrl = 'https://github.com/example/student-code.git'
} | ConvertTo-Json
$r = Invoke-WebRequest -Uri 'http://localhost:8080/api/tasks' -Method POST -Headers $hdr -ContentType 'application/json' -Body $taskBody -UseBasicParsing -TimeoutSec 10
$taskResp = $r.Content | ConvertFrom-Json
Write-Host "  Created task ID=$($taskResp.data.id)" -ForegroundColor Green

Write-Host '[5] Query everything...' -ForegroundColor Cyan
$rulesAll = (Invoke-WebRequest -Uri 'http://localhost:8080/api/rules' -Headers $hdr -UseBasicParsing -TimeoutSec 5).Content | ConvertFrom-Json
$classesAll = (Invoke-WebRequest -Uri 'http://localhost:8080/api/classes' -Headers $hdr -UseBasicParsing -TimeoutSec 5).Content | ConvertFrom-Json
$tasksAll = (Invoke-WebRequest -Uri 'http://localhost:8080/api/tasks' -Headers $hdr -UseBasicParsing -TimeoutSec 5).Content | ConvertFrom-Json
Write-Host "  Rules: $($rulesAll.data.list.Count)" -ForegroundColor Yellow
Write-Host "  Classes: $($classesAll.data.Count)" -ForegroundColor Yellow
Write-Host "  Tasks: $($tasksAll.data.list.Count)" -ForegroundColor Yellow

Write-Host '[6] Generate HTML...' -ForegroundColor Cyan
$rulesHtml = ""
foreach ($rd in $rulesAll.data.list) {
    $rulesHtml += "<tr><td>$($rd.id)</td><td>$($rd.name)</td><td><span class='badge cat-$($rd.category.ToLower())'>$($rd.category)</span></td><td><span class='badge sev-$($rd.severity.ToLower())'>$($rd.severity)</span></td><td><code>$($rd.pattern)</code></td><td>$($rd.description)</td></tr>"
}
$tasksHtml = ""
foreach ($td in $tasksAll.data.list) {
    $statusClass = switch ($td.status) { 0 {'status-pending'} 1 {'status-running'} 2 {'status-done'} 3 {'status-failed'} default {''} }
    $statusText = switch ($td.status) { 0 {'CREATED'} 1 {'SCANNING'} 2 {'COMPLETED'} 3 {'FAILED'} default {'UNKNOWN'} }
    $tasksHtml += "<tr><td>$($td.id)</td><td>$($td.title)</td><td><span class='$statusClass'>$statusText</span></td><td>$($td.classId)</td><td>$($td.submitterId)</td><td>$($td.createdAt)</td></tr>"
}
$classesHtml = ""
foreach ($cd in $classesAll.data) {
    $classesHtml += "<tr><td>$($cd.id)</td><td>$($cd.name)</td><td>$($cd.teacherId)</td><td>$($cd.description)</td></tr>"
}

$css = @'
body{font-family:-apple-system,Microsoft YaHei,sans-serif;background:linear-gradient(135deg,#667eea 0%,#764ba2 100%);min-height:100vh;margin:0;padding:20px}
.container{max-width:1200px;margin:0 auto;background:#fff;border-radius:16px;box-shadow:0 20px 60px rgba(0,0,0,.2);overflow:hidden}
.header{background:linear-gradient(135deg,#1e3c72,#2a5298);color:#fff;padding:30px}
.header h1{margin:0;font-size:28px}
.header .meta{margin-top:10px;opacity:.9;font-size:14px}
.header .pill{display:inline-block;padding:4px 12px;background:rgba(255,255,255,.2);border-radius:20px;margin-right:8px;font-size:13px}
.stats{display:grid;grid-template-columns:repeat(4,1fr);gap:20px;padding:30px;background:#f8f9fa}
.stat{background:#fff;border-radius:12px;padding:20px;text-align:center;box-shadow:0 2px 8px rgba(0,0,0,.06)}
.stat .num{font-size:36px;font-weight:700;color:#1e3c72}
.stat .lbl{color:#888;font-size:13px;margin-top:4px}
.section{padding:30px;border-top:1px solid #eee}
.section h2{color:#1e3c72;margin-top:0}
table{width:100%;border-collapse:collapse;margin-top:15px}
th,td{padding:12px 15px;text-align:left;border-bottom:1px solid #eee}
th{background:#f8f9fa;font-weight:600;color:#555}
tr:hover{background:#f0f4ff}
code{background:#f1f3f5;padding:2px 6px;border-radius:4px;font-size:12px;color:#d6336c}
.badge{display:inline-block;padding:3px 10px;border-radius:12px;font-size:12px;font-weight:600}
.cat-security{background:#ffe0e0;color:#c92a2a}
.cat-performance{background:#fff3bf;color:#e67700}
.cat-style{background:#d0ebff;color:#1971c2}
.sev-critical{background:#fa5252;color:#fff}
.sev-medium{background:#fab005;color:#fff}
.sev-low{background:#74c0fc;color:#fff}
.status-pending{color:#888}
.status-running{color:#1971c2}
.status-done{color:#2f9e44;font-weight:600}
.status-failed{color:#c92a2a}
.footer{padding:20px 30px;background:#f8f9fa;color:#888;font-size:12px;text-align:center}
.flow{display:flex;justify-content:space-between;align-items:center;padding:30px;background:#fafbfc;flex-wrap:wrap;gap:15px}
.flow-step{flex:1;min-width:140px;text-align:center;padding:15px;background:#fff;border-radius:10px;box-shadow:0 2px 6px rgba(0,0,0,.05);position:relative}
.flow-step .icon{font-size:28px}
.flow-step .name{font-weight:600;color:#1e3c72;margin-top:6px}
.flow-step .desc{font-size:11px;color:#888;margin-top:3px}
.flow-arrow{color:#1e3c72;font-size:20px;font-weight:700}
'@

$headerPills = "OK Spring Boot 8080  OK MySQL 3307  OK Redis 6379  OK JWT Auth  User: $($user.username)  Role: $($user.role)"
$time = (Get-Date).ToString('HH:mm:ss')
$footerTime = Get-Date -Format 'yyyy-MM-dd HH:mm:ss'

$html = @"
<!DOCTYPE html>
<html><head><meta charset="UTF-8"><title>Code Review System Demo</title>
<style>$css</style></head><body><div class="container">
<div class="header"><h1>Code Review System - Running Demo</h1>
<div class="meta"><span class="pill">Spring Boot 8080</span><span class="pill">MySQL 3307</span><span class="pill">Redis 6379</span><span class="pill">JWT Auth</span><span class="pill">$($user.username)</span><span class="pill">$($user.role)</span></div></div>
<div class="stats">
<div class="stat"><div class="num">$($rulesAll.data.list.Count)</div><div class="lbl">Review Rules</div></div>
<div class="stat"><div class="num">$($classesAll.data.Count)</div><div class="lbl">Classes</div></div>
<div class="stat"><div class="num">$($tasksAll.data.list.Count)</div><div class="lbl">Review Tasks</div></div>
<div class="stat"><div class="num">$time</div><div class="lbl">Server Time</div></div>
</div>
<div class="flow">
<div class="flow-step"><div class="icon">[1]</div><div class="name">Frontend</div><div class="desc">Postman / Web</div></div>
<div class="flow-arrow">-&gt;</div>
<div class="flow-step"><div class="icon">[2]</div><div class="name">Spring Boot</div><div class="desc">REST API + JWT</div></div>
<div class="flow-arrow">-&gt;</div>
<div class="flow-step"><div class="icon">[3]</div><div class="name">MyBatis Plus</div><div class="desc">ORM + Mapper</div></div>
<div class="flow-arrow">-&gt;</div>
<div class="flow-step"><div class="icon">[4]</div><div class="name">MySQL 3307</div><div class="desc">Data Storage</div></div>
<div class="flow-arrow">-&gt;</div>
<div class="flow-step"><div class="icon">[5]</div><div class="name">Redis 6379</div><div class="desc">Cache + Blacklist</div></div>
</div>
<div class="section"><h2>Classes</h2>
<table><thead><tr><th>ID</th><th>Name</th><th>Teacher</th><th>Description</th></tr></thead><tbody>$classesHtml</tbody></table></div>
<div class="section"><h2>Review Rules</h2>
<table><thead><tr><th>ID</th><th>Name</th><th>Category</th><th>Severity</th><th>Pattern</th><th>Message</th></tr></thead><tbody>$rulesHtml</tbody></table></div>
<div class="section"><h2>Review Tasks</h2>
<table><thead><tr><th>ID</th><th>Title</th><th>Status</th><th>Class</th><th>Submitter</th><th>Created</th></tr></thead><tbody>$tasksHtml</tbody></table></div>
<div class="footer">Code Review System Demo - generated at $footerTime</div>
</div></body></html>
"@

[System.IO.File]::WriteAllText('E:\Desktop\version\demo.html', $html, [System.Text.Encoding]::UTF8)
Write-Host "  HTML saved to E:\Desktop\version\demo.html" -ForegroundColor Green
Write-Host "DONE!" -ForegroundColor Green