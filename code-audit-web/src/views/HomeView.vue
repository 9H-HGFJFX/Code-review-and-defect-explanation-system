<script setup lang="ts">
import { ref, computed, onMounted, watch } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Promotion, FolderOpened, View, Histogram, Setting, SwitchButton, UserFilled } from '@element-plus/icons-vue'
import CodeEditor from '@/components/CodeEditor.vue'
import { reviewApi } from '@/api/review'
import { useAuthStore } from '@/store/auth'

const router = useRouter()
const auth = useAuthStore()

const SAMPLE = `public class LoginService {
    private String password = "123456";

    public User findUser(String name) {
        String sql = "SELECT * FROM users WHERE name='" + name + "'";
        Connection conn = DriverManager.getConnection(url, "root", password);
        Statement stmt = conn.createStatement();
        ResultSet rs = stmt.executeQuery(sql);
        try {
            return new User(rs.getString(1), rs.getString(2));
        } catch (Exception e) {
        }
        return null;
    }
}`

const code = ref<string>(SAMPLE)
const fileName = ref<string>('LoginService.java')
const lineCount = ref(0)
const exceeded = ref(false)
const submitting = ref(false)
const result = ref<any>(null)

const isLoggedIn = computed(() => auth.isLoggedIn)

function onFileChange(e: Event) {
  const input = e.target as HTMLInputElement
  const f = input.files?.[0]
  if (!f) return
  if (!f.name.endsWith('.java')) {
    ElMessage.warning('仅支持 .java 文件')
    return
  }
  const reader = new FileReader()
  reader.onload = (ev) => {
    code.value = String(ev.target?.result || '')
    fileName.value = f.name
  }
  reader.readAsText(f, 'utf-8')
}

function loadSample() {
  code.value = SAMPLE
  fileName.value = 'LoginService.java'
}

async function submit() {
  if (!isLoggedIn.value) {
    ElMessage.warning('请先登录')
    router.push({ name: 'login' })
    return
  }
  if (exceeded.value) {
    ElMessage.error('代码超过 500 行上限，请精简后再提交')
    return
  }
  submitting.value = true
  try {
    const r = await reviewApi.submit({ code: code.value, fileName: fileName.value })
    result.value = r
    ElMessage.success(\`审查完成，发现 \${r.issueCount} 个问题\`)
    // 跳到详情页
    router.push({ name: 'review', params: { id: r.reviewId } })
  } catch (e: any) {
    ElMessage.error(e?.message || '审查失败')
  } finally {
    submitting.value = false
  }
}

function logout() {
  auth.logout()
  ElMessage.success('已退出登录')
  router.push({ name: 'login' })
}

onMounted(() => {
  if (!auth.user && auth.token) {
    // 拉一次用户信息
    import('@/api/auth').then(m => m.authApi.me().then((u: any) => {
      auth.user = u
    }).catch(() => {}))
  }
})
</script>

<template>
  <el-container class="layout">
    <el-header class="topbar">
      <div class="logo">
        <span style="font-size: 18px; font-weight: 700; color: var(--primary)">&lt;/&gt; CodeInsight</span>
        <span class="text-sub" style="margin-left: 8px">代码审查与缺陷解释系统</span>
      </div>
      <div class="nav">
        <el-button :type="$route.name === 'home' ? 'primary' : 'default'" text @click="$router.push('/')">
          <el-icon><Promotion /></el-icon> 提交审查
        </el-button>
        <el-button :type="$route.name === 'history' ? 'primary' : 'default'" text @click="$router.push('/history')">
          <el-icon><Histogram /></el-icon> 历史记录
        </el-button>
        <el-button v-if="auth.isTeacher" :type="$route.name === 'rules' ? 'primary' : 'default'" text @click="$router.push('/rules')">
          <el-icon><Setting /></el-icon> 规则管理
        </el-button>
        <el-button v-if="auth.isTeacher" :type="$route.name === 'stats' ? 'primary' : 'default'" text @click="$router.push('/stats')">
          <el-icon><View /></el-icon> 数据看板
        </el-button>
      </div>
      <div class="user">
        <template v-if="isLoggedIn">
          <el-dropdown>
            <span class="user-info">
              <el-icon><UserFilled /></el-icon>
              {{ auth.user?.username || '用户' }}
              <el-tag size="small" :type="auth.user?.role === 'ADMIN' ? 'danger' : auth.user?.role === 'TEACHER' ? 'warning' : ''">
                {{ auth.user?.role }}
              </el-tag>
            </span>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item @click="logout"><el-icon><SwitchButton /></el-icon> 退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </template>
        <template v-else>
          <el-button type="primary" link @click="$router.push('/login')">登录</el-button>
          <el-button link @click="$router.push('/register')">注册</el-button>
        </template>
      </div>
    </el-header>

    <el-main class="page-container">
      <el-card shadow="never">
        <template #header>
          <div class="flex items-center justify-between">
            <b>提交 Java 代码进行审查</b>
            <div class="text-sub">单次最大 {{ 500 }} 行 · 当前 {{ lineCount }} 行 <span v-if="exceeded" style="color: var(--danger)">（超出限制）</span></div>
          </div>
        </template>

        <el-row :gutter="12" style="margin-bottom: 12px">
          <el-col :span="12">
            <el-input v-model="fileName" placeholder="文件名（可选）" clearable>
              <template #prepend>File</template>
            </el-input>
          </el-col>
          <el-col :span="12" class="flex gap-12">
            <el-upload :show-file-list="false" :auto-upload="false" accept=".java" :on-change="(f: any) => onFileChange({ target: { files: [f.raw] } } as any)">
              <el-button><el-icon><FolderOpened /></el-icon> 上传 .java 文件</el-button>
            </el-upload>
            <el-button @click="loadSample">加载示例</el-button>
            <el-button type="primary" :loading="submitting" :disabled="exceeded || !code.trim()" @click="submit">
              <el-icon><Promotion /></el-icon> 开始审查
            </el-button>
          </el-col>
        </el-row>

        <CodeEditor v-model="code" language="java" :max-lines="500" :height="'500px'" @line-count="(n: number) => lineCount = n" @line-count-exceeded="(b: boolean) => exceeded = b" />
      </el-card>

      <el-card v-if="result" shadow="never" style="margin-top: 16px">
        <template #header><b>最近一次审查结果</b></template>
        <pre class="code-block">{{ JSON.stringify(result, null, 2) }}</pre>
      </el-card>
    </el-main>
  </el-container>
</template>

<style scoped>
.layout { min-height: 100vh; }
.topbar {
  display: flex; align-items: center; justify-content: space-between;
  background: #fff; border-bottom: 1px solid #e5e7eb; padding: 0 24px;
}
.logo { display: flex; align-items: center; }
.nav { display: flex; gap: 4px; }
.user-info { display: inline-flex; align-items: center; gap: 6px; cursor: pointer; }
</style>
