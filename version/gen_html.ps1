[Console]::OutputEncoding = [System.Text.Encoding]::UTF8

$json = Get-Content 'E:\Desktop\version\db_data.json' -Raw | ConvertFrom-Json

$classes = $json.classes
$rules = $json.rules
$tasks = $json.tasks

Write-Host "Loaded $($classes.Count) classes, $($rules.Count) rules, $($tasks.Count) tasks"

$catMap = @{1='STYLE'; 2='SECURITY'; 3='PERFORMANCE'; 4='BEST_PRACTICE'; 5='CORRECTNESS'}
$sevMap = @{1='CRITICAL'; 2='HIGH'; 3='MEDIUM'; 4='LOW'}
$statMap = @{0='Pending'; 1='Scanning'; 2='Completed'; 3='Failed'}
$statClass = @{0='s-pending'; 1='s-scanning'; 2='s-done'; 3='s-failed'}

$css = @'
*{box-sizing:border-box;margin:0;padding:0}
body{font-family:-apple-system,BlinkMacSystemFont,'Segoe UI',Microsoft YaHei,sans-serif;background:linear-gradient(135deg,#667eea 0%,#764ba2 100%);min-height:100vh;padding:20px;color:#333}
.container{max-width:1400px;margin:0 auto;background:#fff;border-radius:16px;box-shadow:0 20px 60px rgba(0,0,0,.2);overflow:hidden}
.header{background:linear-gradient(135deg,#1e3c72 0%,#2a5298 100%);color:#fff;padding:40px}
.header h1{font-size:32px;margin-bottom:10px}
.header .sub{opacity:.9;font-size:14px;margin-bottom:20px}
.badges{display:flex;flex-wrap:wrap;gap:8px}
.badge{display:inline-block;padding:6px 14px;background:rgba(255,255,255,.2);border-radius:20px;font-size:13px;font-weight:500}
.stats{display:grid;grid-template-columns:repeat(4,1fr);gap:1px;background:#e9ecef}
.stat{background:#fff;padding:30px;text-align:center}
.stat .num{font-size:42px;font-weight:700;color:#1e3c72;line-height:1}
.stat .lbl{color:#6c757d;font-size:14px;margin-top:8px;text-transform:uppercase;letter-spacing:.5px}
.section{padding:40px;border-bottom:1px solid #e9ecef}
.section:last-child{border-bottom:none}
.section-title{color:#1e3c72;font-size:24px;margin-bottom:25px;display:flex;align-items:center;gap:12px}
.section-title::before{content:'';width:4px;height:24px;background:linear-gradient(180deg,#667eea,#764ba2);border-radius:2px}
.flow{display:flex;justify-content:space-between;align-items:center;padding:30px;background:#f8f9fa;flex-wrap:wrap;gap:15px}
.flow-step{flex:1;min-width:160px;background:#fff;padding:20px 15px;border-radius:12px;text-align:center;box-shadow:0 2px 8px rgba(0,0,0,.05);transition:transform .2s}
.flow-step:hover{transform:translateY(-3px);box-shadow:0 6px 15px rgba(0,0,0,.1)}
.flow-step .icon{font-size:32px}
.flow-step .name{font-weight:600;color:#1e3c72;margin-top:8px;font-size:15px}
.flow-step .desc{font-size:12px;color:#6c757d;margin-top:4px}
.flow-arrow{color:#1e3c72;font-size:24px;font-weight:700;opacity:.4}
.cards{display:grid;grid-template-columns:repeat(auto-fill,minmax(320px,1fr));gap:20px}
.card{background:#fff;border:1px solid #e9ecef;border-radius:12px;padding:20px;transition:all .2s;box-shadow:0 2px 4px rgba(0,0,0,.04)}
.card:hover{border-color:#667eea;box-shadow:0 8px 24px rgba(102,126,234,.15);transform:translateY(-2px)}
.card h3{color:#1e3c72;font-size:16px;margin-bottom:10px}
.card .meta{display:flex;gap:8px;margin-bottom:12px;flex-wrap:wrap}
.tag{display:inline-block;padding:3px 10px;border-radius:6px;font-size:11px;font-weight:600}
.cat-style{background:#d0ebff;color:#1971c2}
.cat-security{background:#ffe0e0;color:#c92a2a}
.cat-performance{background:#fff3bf;color:#e67700}
.cat-bestpractice{background:#d3f9d8;color:#2f9e44}
.cat-correctness{background:#e7d5ff;color:#6741d9}
.sev-critical{background:#fa5252;color:#fff}
.sev-high{background:#fd7e14;color:#fff}
.sev-medium{background:#fab005;color:#fff}
.sev-low{background:#74c0fc;color:#fff}
.lang{background:#e7f5ff;color:#1864ab;font-family:monospace}
.card .desc{color:#555;font-size:13px;line-height:1.6;margin-bottom:10px}
.card .pattern{background:#f1f3f5;padding:8px 10px;border-radius:6px;font-family:monospace;font-size:12px;color:#c92a2a;word-break:break-all}
table{width:100%;border-collapse:collapse;margin-top:15px}
th,td{padding:14px 16px;text-align:left;border-bottom:1px solid #e9ecef}
th{background:#f8f9fa;font-weight:600;color:#495057;font-size:13px;text-transform:uppercase;letter-spacing:.5px}
tbody tr{transition:background .15s}
tbody tr:hover{background:#f8f9ff}
.s-pending{color:#868e96}
.s-scanning{color:#1971c2;font-weight:600}
.s-scanning::before{content:'';display:inline-block;width:8px;height:8px;border-radius:50%;background:#1971c2;margin-right:6px;animation:pulse 1.5s infinite}
@keyframes pulse{0%,100%{opacity:1}50%{opacity:.4}}
.s-done{color:#2f9e44;font-weight:600}
.s-failed{color:#c92a2a;font-weight:600}
.id{font-family:monospace;color:#868e96;font-size:13px}
.footer{padding:30px;text-align:center;color:#868e96;background:#f8f9fa;font-size:13px}
'@

$rulesHtml = ""
foreach ($r in $rules) {
    $catKey = switch ([int]$r.category) { 1 {'style'} 2 {'security'} 3 {'performance'} 4 {'bestpractice'} 5 {'correctness'} default {'style'} }
    $sevKey = switch ([int]$r.severity) { 1 {'critical'} 2 {'high'} 3 {'medium'} 4 {'low'} default {'medium'} }
    $catName = $catMap[[int]$r.category]
    $sevName = $sevMap[[int]$r.severity]
    $rulesHtml += @"
<div class="card">
<h3>$($r.name)</h3>
<div class="meta">
<span class="tag cat-$catKey">$catName</span>
<span class="tag sev-$sevKey">$sevName</span>
<span class="tag lang">$($r.languages)</span>
<span class="tag" style="background:#f1f3f5;color:#495057">Priority $($r.priority)</span>
</div>
<div class="desc">$($r.description)</div>
<div class="pattern">$($r.pattern)</div>
<div style="margin-top:10px;font-size:11px;color:#868e96">Rule ID: $($r.rule_id)</div>
</div>
"@
}

$tasksHtml = ""
foreach ($t in $tasks) {
    $statusName = $statMap[[int]$t.status]
    $statusClass = $statClass[[int]$t.status]
    $tasksHtml += @"
<tr>
<td class="id">#$($t.id)</td>
<td><strong>$($t.title)</strong></td>
<td><span class="$statusClass">$statusName</span></td>
<td>Class #$($t.class_id)</td>
<td>User #$($t.submitter_id)</td>
<td>$($t.deadline)</td>
<td>$($t.create_time)</td>
</tr>
"@
}

$classesHtml = ""
foreach ($c in $classes) {
    $classesHtml += @"
<tr>
<td class="id">#$($c.id)</td>
<td><strong>$($c.name)</strong></td>
<td>User #$($c.teacher_id)</td>
<td>$($c.description)</td>
</tr>
"@
}

$time = Get-Date -Format 'HH:mm:ss'
$footerTime = Get-Date -Format 'yyyy-MM-dd HH:mm:ss'

$html = @"
<!DOCTYPE html>
<html><head><meta charset="UTF-8"><title>Code Review System - Running Demo</title>
<style>$css</style></head><body>
<div class="container">
<div class="header">
<h1>Code Review System - Live Demo</h1>
<div class="sub">Real-time data from running backend: Spring Boot 8080 + MySQL 3307 + Redis 6379</div>
<div class="badges">
<span class="badge">Spring Boot 3.2.5</span>
<span class="badge">MySQL 8.0</span>
<span class="badge">Redis 3.0</span>
<span class="badge">JWT Auth</span>
<span class="badge">MyBatis-Plus 3.5.5</span>
<span class="badge">Lombok</span>
</div>
</div>

<div class="stats">
<div class="stat"><div class="num">$($classes.Count)</div><div class="lbl">Classes</div></div>
<div class="stat"><div class="num">$($rules.Count)</div><div class="lbl">Review Rules</div></div>
<div class="stat"><div class="num">$($tasks.Count)</div><div class="lbl">Review Tasks</div></div>
<div class="stat"><div class="num">$time</div><div class="lbl">Server Time</div></div>
</div>

<div class="section">
<div class="section-title">System Architecture</div>
<div class="flow">
<div class="flow-step"><div class="icon">&#128187;</div><div class="name">Frontend</div><div class="desc">Postman / Web UI</div></div>
<div class="flow-arrow">&#10132;</div>
<div class="flow-step"><div class="icon">&#128274;</div><div class="name">JWT Filter</div><div class="desc">Authentication</div></div>
<div class="flow-arrow">&#10132;</div>
<div class="flow-step"><div class="icon">&#9889;</div><div class="name">Controllers</div><div class="desc">REST API (5)</div></div>
<div class="flow-arrow">&#10132;</div>
<div class="flow-step"><div class="icon">&#128295;</div><div class="name">Services</div><div class="desc">Business Logic</div></div>
<div class="flow-arrow">&#10132;</div>
<div class="flow-step"><div class="icon">&#128190;</div><div class="name">Mappers</div><div class="desc">MyBatis-Plus</div></div>
<div class="flow-arrow">&#10132;</div>
<div class="flow-step"><div class="icon">&#128190;</div><div class="name">MySQL + Redis</div><div class="desc">Persistence</div></div>
</div>
</div>

<div class="section">
<div class="section-title">Classes ($($classes.Count))</div>
<table>
<thead><tr><th>ID</th><th>Name</th><th>Teacher</th><th>Description</th></tr></thead>
<tbody>$classesHtml</tbody>
</table>
</div>

<div class="section">
<div class="section-title">Review Rules ($($rules.Count))</div>
<div class="cards">$rulesHtml</div>
</div>

<div class="section">
<div class="section-title">Review Tasks ($($tasks.Count))</div>
<table>
<thead><tr><th>ID</th><th>Title</th><th>Status</th><th>Class</th><th>Submitter</th><th>Deadline</th><th>Created</th></tr></thead>
<tbody>$tasksHtml</tbody>
</table>
</div>

<div class="footer">
Demo generated at $footerTime from running backend &middot; Real data from MySQL 3307
</div>
</div>
</body></html>
"@

[System.IO.File]::WriteAllText('E:\Desktop\version\demo.html', $html, [System.Text.Encoding]::UTF8)
Write-Host "HTML saved to E:\Desktop\version\demo.html" -ForegroundColor Green
Write-Host "Size: $((Get-Item 'E:\Desktop\version\demo.html').Length) bytes" -ForegroundColor Green