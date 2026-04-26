# Image Box

AI 图像生成 Android 客户端，基于 Jetpack Compose 与 Material3 构建。

## 功能

- **多模型 AI 生图** — 支持多种图像生成模型，可选画面比例和尺寸
- **参考图上传** — 支持本地上传多张参考图，可单独移除或一键清空
- **网络图片链接** — 支持粘贴 HTTP/HTTPS 图片 URL 作为参考图
- **批量生成** — 支持一次提交多张，自定义并发数和重试次数
- **提示词优化** — 内置 LLM 提示词优化，一键润色生图提示词
- **结果复用** — 生成结果可一键复用为参考图或复制提示词
- **历史记录** — 本地保存生成历史，支持浏览和重新查看
- **保存到相册** — 图片可手动或自动保存到系统相册（Pictures/Image box）
- **深色模式** — 支持浅色/深色主题切换

## 默认接口

默认使用 **[grsai](https://grsai.com/zh) API 平台** 提供图像生成服务。

> 需在设置中配置 API Key 后方可使用。支持切换为自定义接口，兼容 OpenAI 兼容协议的图像生成 API。

## 构建

| 项 | 值 |
|---|---|
| 最低 SDK | 26 (Android 8.0) |
| 目标 SDK | 36 |
| 语言 | Kotlin |
| UI 框架 | Jetpack Compose + Material3 |
| 构建工具 | Gradle 9.x + AGP 9.x |

```bash
./gradlew :app:assembleDebug
```

## 结构

```
app/src/main/java/com/example/myimageapplication/
  MainActivity.kt        # 应用主入口，全部 UI 与业务逻辑
  ui/theme/
    Color.kt              # 主题色板
    Theme.kt              # Material3 主题配置
    Type.kt               # 字体排版
```

## 许可

MIT License
