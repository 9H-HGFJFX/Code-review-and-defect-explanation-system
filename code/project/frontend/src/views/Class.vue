<template>
  <div class="class-container">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>班级管理</span>
          <el-button type="primary" @click="openCreateDialog">
            创建班级
          </el-button>
        </div>
      </template>
      
      <el-table :data="classList" stripe v-loading="loading">
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="className" label="班级名称" />
        <el-table-column prop="description" label="描述" />
        <el-table-column label="操作" width="300" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" size="small" @click="viewMembers(row)">
              成员管理
            </el-button>
            <el-button type="warning" size="small" @click="openEditDialog(row)">
              编辑
            </el-button>
            <el-button type="danger" size="small" @click="deleteClass(row)">
              删除
            </el-button>
          </template>
        </el-table-column>
      </el-table>
      
      <el-pagination
        v-model:current-page="pagination.current"
        v-model:page-size="pagination.size"
        :total="pagination.total"
        :page-sizes="[10, 20, 50]"
        layout="total, sizes, prev, pager, next"
        style="margin-top: 20px; justify-content: flex-end;"
        @size-change="loadData"
        @current-change="loadData"
      />
    </el-card>
    
    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="500px">
      <el-form ref="formRef" :model="classForm" :rules="formRules" label-width="100px">
        <el-form-item label="班级名称" prop="className">
          <el-input v-model="classForm.className" placeholder="请输入班级名称" />
        </el-form-item>
        <el-form-item label="班级描述">
          <el-input v-model="classForm.description" type="textarea" rows="3" />
        </el-form-item>
      </el-form>
      
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitForm" :loading="submitting">
          确定
        </el-button>
      </template>
    </el-dialog>
    
    <el-dialog v-model="memberDialogVisible" title="成员管理" width="600px">
      <div class="member-actions">
        <el-button type="primary" size="small" @click="openAddMemberDialog">
          添加成员
        </el-button>
      </div>
      
      <el-table :data="memberList" stripe style="margin-top: 15px;">
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="username" label="用户名" />
        <el-table-column prop="email" label="邮箱" />
        <el-table-column label="操作" width="100">
          <template #default="{ row }">
            <el-button type="danger" size="small" @click="removeMember(row)">
              移除
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, computed } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getClassList, createClass, updateClass, deleteClass as apiDeleteClass, getClassMembers, addClassMembers, removeClassMembers } from '@/api/class'

const loading = ref(false)
const submitting = ref(false)
const dialogVisible = ref(false)
const memberDialogVisible = ref(false)
const isEdit = ref(false)
const classList = ref([])
const memberList = ref([])
const currentClassId = ref(null)

const pagination = reactive({
  current: 1,
  size: 10,
  total: 0
})

const classForm = reactive({
  className: '',
  description: ''
})

const formRules = {
  className: [{ required: true, message: '请输入班级名称', trigger: 'blur' }]
}

onMounted(() => {
  loadData()
})

const loadData = async () => {
  loading.value = true
  try {
    const res = await getClassList({
      current: pagination.current,
      size: pagination.size
    })
    classList.value = res.data.records
    pagination.total = res.data.total
  } catch (error) {
    console.error('加载班级列表失败', error)
  } finally {
    loading.value = false
  }
}

const openCreateDialog = () => {
  isEdit.value = false
  Object.assign(classForm, { className: '', description: '' })
  dialogVisible.value = true
}

const openEditDialog = (row) => {
  isEdit.value = true
  Object.assign(classForm, {
    id: row.id,
    className: row.className,
    description: row.description
  })
  dialogVisible.value = true
}

const submitForm = async () => {
  submitting.value = true
  try {
    if (isEdit.value) {
      await updateClass(classForm.id, classForm)
      ElMessage.success('更新成功')
    } else {
      await createClass(classForm)
      ElMessage.success('创建成功')
    }
    dialogVisible.value = false
    loadData()
  } catch (error) {
    ElMessage.error(error.message || '操作失败')
  } finally {
    submitting.value = false
  }
}

const deleteClass = async (row) => {
  try {
    await ElMessageBox.confirm('确定要删除该班级吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    await apiDeleteClass(row.id)
    ElMessage.success('删除成功')
    loadData()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('删除失败')
    }
  }
}

const viewMembers = async (row) => {
  currentClassId.value = row.id
  memberDialogVisible.value = true
  await loadMembers()
}

const loadMembers = async () => {
  try {
    const res = await getClassMembers(currentClassId.value)
    memberList.value = res.data
  } catch (error) {
    console.error('加载成员列表失败', error)
  }
}

const openAddMemberDialog = () => {
  ElMessage.info('请输入学生用户ID进行添加（演示功能）')
}

const removeMember = async (row) => {
  try {
    await ElMessageBox.confirm('确定要移除该成员吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    await removeClassMembers(currentClassId.value, { userIds: [row.id] })
    ElMessage.success('移除成功')
    loadMembers()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('移除失败')
    }
  }
}

const dialogTitle = computed(() => isEdit.value ? '编辑班级' : '创建班级')
</script>

<style scoped lang="scss">
.class-container {
  .card-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
  }
}

.member-actions {
  padding-bottom: 10px;
  border-bottom: 1px solid #eee;
}
</style>
