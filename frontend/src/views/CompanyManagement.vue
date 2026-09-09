<template>
  <div class="company-management">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>上市公司基本信息管理</span>
          <el-button type="primary" @click="handleAdd">新增企业</el-button>
        </div>
      </template>

      <div class="search-bar">
        <el-input
          v-model="searchQuery"
          placeholder="搜索股票代码或简称"
          style="width: 300px; margin-bottom: 20px"
          clearable
          @clear="fetchCompanies"
          @input="handleSearch"
        >
          <template #prefix>
            <el-icon><Search /></el-icon>
          </template>
        </el-input>
      </div>

      <el-table :data="filteredCompanies" stripe style="width: 100%" v-loading="loading">
        <el-table-column prop="stkcd" label="股票代码" width="120" sortable />
        <el-table-column prop="shortName" label="股票简称" width="180" />
        <el-table-column prop="industry" label="所属行业" />
        <el-table-column prop="isSt" label="ST状态" width="120">
          <template #default="scope">
            <el-tag :type="scope.row.isSt ? 'danger' : 'success'">
              {{ scope.row.isSt ? '是 (ST)' : '否' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="updateTime" label="更新时间" width="200">
          <template #default="scope">
            {{ formatDate(scope.row.updateTime) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="250">
          <template #default="scope">
            <el-button size="small" type="primary" @click="goToPredict(scope.row)">风险预测</el-button>
            <el-button size="small" type="warning" @click="handleEdit(scope.row)">编辑</el-button>
            <el-popconfirm title="确定删除该企业信息吗？" @confirm="handleDelete(scope.row.stkcd)">
              <template #reference>
                <el-button size="small" type="danger">删除</el-button>
              </template>
            </el-popconfirm>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 新增/编辑对话框 -->
    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑企业' : '新增企业'" width="500px">
      <el-form :model="companyForm" label-width="100px">
        <el-form-item label="股票代码">
          <el-input v-model="companyForm.stkcd" :disabled="isEdit" placeholder="如: 000001" />
        </el-form-item>
        <el-form-item label="股票简称">
          <el-input v-model="companyForm.shortName" placeholder="如: 平安银行" />
        </el-form-item>
        <el-form-item label="所属行业">
          <el-input v-model="companyForm.industry" placeholder="如: 金融业" />
        </el-form-item>
        <el-form-item label="是否ST">
          <el-switch v-model="companyForm.isSt" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitForm">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import axios from 'axios'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search } from '@element-plus/icons-vue'
import { useRouter } from 'vue-router'

const router = useRouter()
const loading = ref(false)
const companies = ref([])
const searchQuery = ref('')
const dialogVisible = ref(false)
const isEdit = ref(false)

const companyForm = reactive({
  stkcd: '',
  shortName: '',
  industry: '',
  isSt: false
})

const fetchCompanies = async () => {
  loading.value = true
  try {
    const response = await axios.get('http://localhost:8080/api/companies')
    companies.value = response.data
  } catch (error) {
    ElMessage.error('获取企业列表失败')
  } finally {
    loading.value = false
  }
}

const filteredCompanies = computed(() => {
  if (!searchQuery.value) return companies.value
  const q = searchQuery.value.toLowerCase()
  return companies.value.filter(c => 
    c.stkcd.toLowerCase().includes(q) || 
    c.shortName.toLowerCase().includes(q)
  )
})

const handleSearch = () => {
  // 已经在 computed 中处理了
}

const formatDate = (dateStr) => {
  if (!dateStr) return '-'
  return new Date(dateStr).toLocaleString()
}

const handleAdd = () => {
  isEdit.value = false
  Object.assign(companyForm, { stkcd: '', shortName: '', industry: '', isSt: false })
  dialogVisible.value = true
}

const handleEdit = (row) => {
  isEdit.value = true
  Object.assign(companyForm, { ...row })
  dialogVisible.value = true
}

const submitForm = async () => {
  if (!companyForm.stkcd || !companyForm.shortName) {
    ElMessage.warning('请填写股票代码和简称')
    return
  }
  try {
    await axios.post('http://localhost:8080/api/companies', companyForm)
    ElMessage.success(isEdit.value ? '更新成功' : '新增成功')
    dialogVisible.value = false
    fetchCompanies()
  } catch (error) {
    ElMessage.error('保存失败')
  }
}

const handleDelete = async (stkcd) => {
  try {
    await axios.delete(`http://localhost:8080/api/companies/${stkcd}`)
    ElMessage.success('删除成功')
    fetchCompanies()
  } catch (error) {
    ElMessage.error('删除失败')
  }
}

const goToPredict = (row) => {
  // 跳转到预测页面，并携带股票代码
  router.push({
    path: '/predict',
    query: { stkcd: row.stkcd }
  })
}

onMounted(() => {
  fetchCompanies()
})
</script>

<style scoped>
.company-management {
  padding: 20px;
}
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.search-bar {
  margin-top: 10px;
}
</style>
