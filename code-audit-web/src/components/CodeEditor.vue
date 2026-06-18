<script setup lang="ts">
import { ref, onMounted, onBeforeUnmount, watch } from 'vue'
import * as monaco from 'monaco-editor'

// 关闭 monaco 的 web worker（避免在简单部署中需要额外 worker 资源）
;(self as any).MonacoEnvironment = {
  getWorker: () => ({
    postMessage() { /* noop */ },
    terminate() { /* noop */ },
    addEventListener() { /* noop */ },
    removeEventListener() { /* noop */ }
  })
}

const props = defineProps<{
  modelValue: string
  language?: string
  readOnly?: boolean
  height?: string
  maxLines?: number
}>()

const emit = defineEmits<{
  (e: 'update:modelValue', value: string): void
  (e: 'lineCount', value: number): void
  (e: 'lineCountExceeded', value: boolean): void
}>()

const editorRef = ref<HTMLDivElement>()
let editor: monaco.editor.IStandaloneCodeEditor | null = null

onMounted(() => {
  if (!editorRef.value) return
  editor = monaco.editor.create(editorRef.value, {
    value: props.modelValue || '',
    language: props.language || 'java',
    readOnly: props.readOnly || false,
    theme: 'vs-dark',
    fontSize: 14,
    lineNumbers: 'on',
    minimap: { enabled: false },
    automaticLayout: true,
    scrollBeyondLastLine: false,
    tabSize: 4,
    wordWrap: 'on',
    renderLineHighlight: 'all',
    contextmenu: false
  })

  editor.onDidChangeModelContent(() => {
    const v = editor!.getValue()
    emit('update:modelValue', v)
    const count = editor!.getModel()?.getLineCount() || 0
    emit('lineCount', count)
    const max = props.maxLines || 500
    emit('lineCountExceeded', count > max)
  })

  // 初始行数
  const initCount = editor.getModel()?.getLineCount() || 0
  emit('lineCount', initCount)
})

watch(() => props.modelValue, (v) => {
  if (editor && v !== editor.getValue()) editor.setValue(v || '')
})

onBeforeUnmount(() => {
  editor?.dispose()
})
</script>

<template>
  <div class="code-editor-wrapper" :style="{ height: height || '420px' }">
    <div ref="editorRef" class="code-editor-mount" />
  </div>
</template>

<style scoped>
.code-editor-wrapper { width: 100%; border: 1px solid #e5e7eb; border-radius: 6px; overflow: hidden; }
.code-editor-mount { width: 100%; height: 100%; }
</style>
