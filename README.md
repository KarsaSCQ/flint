# Flint 燧石

个人自用的极简搜索直达启动器。唤起 → 输入 → 去向，一条链路。

## 特性

- **拼音直达**：简拼（`zgyh` → 中国银行）/ 全拼 / 混合匹配，内置 2.3 万条多音字消歧词表（银行→yinhang、重庆→chongqing 开箱即对）
- **玻璃质感**：FLAG_BLUR_BEHIND 真实壁纸模糊 + Monet 动态取色 + 发丝描边，无 M3 模板味
- **翻译**：ML Kit 端侧 + MyMemory 在线双引擎，Sticky 原文卡 + 结果内滚
- **AI 问答**：OpenAI 兼容端点（DeepSeek/自建均可），SSE 流式输出，用完即焚
- **零驻留**：退出界面进程立刻销毁，无后台、无通知、无「最近使用」
- **入口**：2x1 微件 / 快捷磁贴 / 文本选择菜单（PROCESS_TEXT）
- **同步**：WebDAV 静默上传，24h 冷启动拉取 + 一键导入

## 构建

```
JDK 21 + Android SDK (platform 37)
./gradlew :app:assembleDebug
```

自用 app，`QUERY_ALL_PACKAGES` 需在系统设置中授予。

## 输入法提示

Flint 无法强制输入法语言。拼音直达需要英文键盘——把默认输入法语言设为英文，或在输入法里用中英切换键。

## 结构

```
app/src/main/java/io/github/yizhu/flint/
├── core/        拼音索引、DP 匹配、应用扫描、配置
├── ui/          玻璃组件、搜索界面、翻译/AI 卡片、设置
├── translate/   ML Kit / MyMemory / Google 引擎
├── ai/          OpenAI 兼容 SSE 客户端
├── sync/        WebDAV 静默同步
├── widget/      2x1 Glance 微件
└── tile/        快捷设置磁贴
```
