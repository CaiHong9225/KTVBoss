# K歌打Boss 技术方案

## 1. 技术目标

在不依赖完整业务系统的前提下，快速搭出一个可运行的大屏 demo。整体设计遵循两个原则：

- 先保证演示体验和反馈节奏
- 再保证代码结构清晰，便于后续继续扩展

## 2. 推荐技术栈

如果从空目录启动，推荐使用以下方案：

- 平台：Android TV / Android 机顶盒
- 语言：Kotlin
- UI：Jetpack Compose for TV 或普通 Compose
- 架构：MVVM
- 动画：Compose Animation + Lottie
- 音频采集：`AudioRecord`
- 音量分析：PCM 峰值或 RMS
- 基础音高检测：自实现简化版自相关算法，或接入轻量第三方算法
- AI 接口：HTTP 调用 LLM 服务
- 数据持有：`StateFlow`

如果当前环境必须基于传统 View，也可以把页面结构映射成：

- `Activity` + `Fragment`
- 自定义 View 负责战斗特效层
- `ViewModel` 管理状态

## 3. 核心模块

建议拆成以下包结构：

```text
com.example.ktvboss
├─ app
│  ├─ MainActivity
│  ├─ navigation
│  └─ theme
├─ feature_home
│  ├─ HomeScreen
│  └─ HomeViewModel
├─ feature_battle
│  ├─ BattleScreen
│  ├─ BattleViewModel
│  ├─ BattleState
│  ├─ BattleEvent
│  ├─ engine
│  │  ├─ BossBattleEngine
│  │  ├─ AttackResolver
│  │  └─ SkillTriggerResolver
│  └─ widget
│     ├─ BossPanel
│     ├─ PlayerPanel
│     ├─ EnergyBar
│     ├─ ComboPanel
│     └─ LyricTrackView
├─ feature_result
│  ├─ ResultScreen
│  ├─ ResultViewModel
│  └─ poster
├─ audio
│  ├─ MicCaptureManager
│  ├─ VolumeAnalyzer
│  ├─ PitchAnalyzer
│  └─ AudioFeatureFrame
├─ ai
│  ├─ AiCommentService
│  ├─ BossPromptBuilder
│  └─ ResultPromptBuilder
├─ model
│  ├─ SongSegment
│  ├─ BossProfile
│  ├─ BattleMetrics
│  └─ ResultSummary
└─ common
   ├─ time
   ├─ ext
   └─ logger
```

## 4. 状态流设计

### 战斗页输入

- 麦克风实时音量
- 当前时间轴位置
- 目标演唱段配置
- 当前 Boss 血量
- 当前玩家能量值

### 战斗页输出

- 攻击类型
- 是否命中
- 连击数
- 伤害值
- 是否触发必杀
- 受击动画状态
- 战斗结算数据

## 5. 战斗计算逻辑

MVP 阶段不追求专业评分，只追求稳定、直观、可解释。

### 输入特征

- `volumeLevel`：当前音量等级
- `pitchStability`：短时间窗口内的音高稳定度
- `activeSegmentType`：当前歌曲片段类型，普通段 / 副歌段 / 爆发段

### 映射规则

- 音量高于阈值：产生攻击
- 音高稳定：提高命中率和伤害系数
- 连续多个窗口达标：增加 combo
- 到达高潮段且能量足够：触发必杀技
- 连续不达标：Boss 反击

### 伪代码

```kotlin
if (volumeLevel >= attackThreshold) {
    val baseDamage = if (pitchStability >= stableThreshold) 12 else 7
    val comboBonus = comboCount * 2
    val segmentBonus = if (activeSegmentType == CHORUS) 8 else 0
    val damage = baseDamage + comboBonus + segmentBonus
    bossHp -= damage
    comboCount += 1
    energy += 10
} else {
    comboCount = 0
    playerHp -= 5
}

if (energy >= 100 && activeSegmentType == HIGHLIGHT) {
    triggerUltimate()
}
```

## 6. 歌曲数据结构

MVP 只需要一首歌的时间轴配置，不需要完整歌词引擎。

可使用本地 JSON 配置：

```json
{
  "songId": "demo_song_01",
  "songName": "孤勇者-演示版",
  "durationMs": 90000,
  "segments": [
    { "startMs": 0, "endMs": 12000, "type": "intro" },
    { "startMs": 12000, "endMs": 28000, "type": "verse" },
    { "startMs": 28000, "endMs": 42000, "type": "chorus" },
    { "startMs": 42000, "endMs": 52000, "type": "highlight" }
  ]
}
```

## 7. AI 文案生成接口

建议输入以下结构给 LLM：

- Boss 名称
- Boss 人设
- 歌曲名
- 最大连击数
- 命中率区间
- 高光时刻描述
- 翻车时刻描述

输出内容：

- 开场嘲讽文案
- 战斗中触发台词
- 结算点评
- 玩家称号

## 8. Android TV 交互要求

- 首屏必须能通过遥控器焦点直接开始
- 重要按钮数量少，避免复杂焦点跳转
- 战斗过程中尽量减少可交互元素
- 结算页仅保留两个焦点：再来一次、返回首页
- 所有关键反馈字号要大，远距离也能看清

## 9. 开发优先级

### P0

- 首页
- 战斗页基础布局
- 音量驱动攻击
- Boss 血量变化
- 结算页

### P1

- 音高稳定度
- 连击系统
- 必杀技动画
- AI 文案生成

### P2

- 海报导出
- 更完整的歌词轨道
- 多个 Boss 皮肤

## 10. 代码落地建议

如果下一步开始建工程，建议按以下顺序创建代码：

1. 创建 `MainActivity` 和导航
2. 先画静态 `HomeScreen`、`BattleScreen`、`ResultScreen`
3. 接入假数据跑通页面跳转
4. 接入 `AudioRecord`
5. 用音量先驱动攻击动画
6. 加入音高稳定度和连击
7. 最后接入 AI 文案接口

这样可以保证即使 AI 接口还没接好，作品依然能演示核心玩法。
