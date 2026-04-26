# Image Box

Image Box 是一个面向创作场景的 Android AI 图像生成客户端，基于 Jetpack Compose 与 Material 3 构建，强调高频生图、提示词复用、模板管理与移动端易用性。

## 主要特性

### 图像生成
- 支持多模型 AI 生图
- 支持画面比例切换（如 16:9、9:16、1:1 等）
- 支持分辨率选择
- 支持批量提交任务
- 支持并发数与重试次数配置

### 提示词工作流
- 内置 AI 提示词优化
- 支持提示词整理与快速清空
- 支持独立的提示词模板中心
- 支持内置模板、最近使用模板、自定义模板
- 支持首页直接将当前提示词保存为模板
- 支持在模板中心手动新增模板
- 支持模板重命名、删除、复制内容
- 支持从结果页将当前提示词保存为模板

### 参考图与结果复用
- 支持本地上传多张参考图
- 支持网络图片 URL 作为参考图
- 支持单独移除某张参考图
- 支持将生成结果一键设为新的参考图
- 复用结果图为参考图时会自动替换旧参考图

### 历史记录与保存
- 本地保存生成历史记录
- 历史记录支持搜索与筛选
- 已保存到相册的历史记录优先使用本地缩略图
- 图床过期后，未保存的历史记录保留文字信息，不再无限加载缩略图
- 支持手动保存到系统相册
- 支持自动保存到系统相册

### 设置与工具能力
- 支持浅色 / 深色主题切换
- 支持默认接口与自定义接口配置
- 支持 LLM 提示词优化模型配置
- 支持余额查询配置
- 支持 GitHub 版本更新检测
- 支持 API 网站目录页，快速跳转到常见平台申请或查看 API

## 默认接口

默认使用 **[grsai](https://grsai.com/zh)** API 平台提供图像生成服务。

> 使用前需在设置中配置对应 API Key。应用同时支持切换为自定义接口，适合接入兼容 OpenAI 风格的生成服务。

## 已内置的 API 网站目录

应用内已提供常见平台网站入口，方便快速申请、查看或管理 API：

- grsai
- DeepSeek
- Gemini
- GPT / OpenAI
- Claude
- OpenRouter

## 技术栈

| 项目 | 说明 |
|---|---|
| 语言 | Kotlin |
| UI | Jetpack Compose + Material 3 |
| 最低 Android 版本 | Android 8.0 (API 26) |
| 目标版本 | Android 16 / API 36 |
| 构建系统 | Gradle 9.x |
| Android Gradle Plugin | AGP 9.x |

## 构建

```bash
./gradlew :app:assembleDebug
```

## 项目结构

```text
app/src/main/java/com/example/myimageapplication/
  MainActivity.kt        # 主要 UI、状态、网络与持久化逻辑
  ui/theme/
    Color.kt             # 色板定义
    Theme.kt             # Material 3 主题配置
    Type.kt              # 字体排版
```

## 说明

当前项目以单文件 Compose 结构为主，便于快速迭代和直接定制交互逻辑。后续如果功能继续扩展，可再逐步拆分页面、状态与数据层。

## License

MIT License
