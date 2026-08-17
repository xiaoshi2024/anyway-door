# 🚪 哆啦A梦的任意门 (Doraemon's Anywhere Door)

> 一个为哆啦A梦粉丝打造的 Minecraft 模组，完美还原漫画中的神奇道具！

---

## 📖 项目简介

这是一个基于 **Fabric** 开发的 Minecraft 模组，核心功能是**任意门**——右键打开即可看到目的地景象，走进去瞬间传送！灵感源自《哆啦A梦》中最经典的道具之一。

---

## ✨ 已实现功能

### 🚪 任意门 (Anywhere Door)

- ✅ 右键开门/关门（切换操作）
- ✅ 开门时显示目的地"门中景"效果
- ✅ 走进门框自动传送
- ✅ 跨维度传送（支持其他模组维度）
- ✅ 双向传送门（目的地也会生成任意门）
- ✅ 传送冷却（防止无限循环）
- ✅ 中空碰撞箱（可走进门内）
- ✅ 铁门开/关音效 + 传送音效
- ✅ GeckoLib 3D 动画模型
- ✅ 动作栏提示（不占用聊天栏）

### ⌨️ 指令系统

| 指令 | 功能 |
|------|------|
| `/rym set <维度> <x> <y> <z>` | 设置目标到指定维度坐标 |
| `/rym set <x> <y> <z>` | 设置目标到当前维度坐标 |
| `/rym info` | 查看当前任意门信息 |
| `/rym close` | 手动关闭任意门 |

**维度示例：**
```bash
/rym set overworld 100 64 200      # 主世界
/rym set the_nether 0 80 0         # 下界
/rym set the_end 0 60 0            # 末地
/rym set twilightforest:twilight_forest 0 64 0  # 暮色森林
```

---

## 🎨 模型 & 动画

| 文件 | 说明 |
|------|------|
| `dimensional_door.geo.json` | 3D门框模型（含门板骨骼） |
| `dimensional_door.animation.json` | 开门/关门动画（GeckoLib） |
| `dimensional_door.png` | 纹理贴图 |

**门板骨骼**：`door2` — 围绕 pivot (8, 19, -7) 旋转 -155° 开关

---

## 🎵 音效系统

| 操作 | 音效 | 来源 |
|------|------|------|
| 开门 | `IRON_DOOR_OPEN` | Minecraft 原版 |
| 关门 | `IRON_DOOR_CLOSE` | Minecraft 原版 |
| 传送 | `ENDERMAN_TELEPORT` | Minecraft 原版 |

---

## 📁 项目结构

```
src/main/
├── java/xiaoshi2022/anywaydoor/
│   ├── AnywayDoor.java                 # 主类（指令注册、Tick事件）
│   ├── block/
│   │   └── DimensionalDoorBlock.java   # 任意门方块
│   ├── block/entity/
│   │   └── DimensionalDoorBlockEntity.java  # 方块实体（动画、传送）
│   ├── client/
│   │   ├── AnywayDoorClient.java       # 客户端初始化
│   │   └── renderer/
│   │       ├── DimensionalDoorModel.java
│   │       └── DimensionalDoorRenderer.java
│   ├── teleport/
│   │   └── DimensionalDoorTeleporter.java  # 传送逻辑
│   └── regsiter/
│       ├── ModBlocks.java
│       └── ModBlockEntities.java
├── resources/
│   ├── assets/anyway-door/
│   │   ├── geo/
│   │   │   └── dimensional_door.geo.json
│   │   ├── animations/
│   │   │   └── dimensional_door.animation.json
│   │   ├── textures/block/
│   │   │   └── dimensional_door.png
│   │   └── lang/
│   │       ├── zh_cn.json
│   │       └── en_us.json
│   └── data/
│       ├── anyway-door/
│       │   └── loot_table/blocks/
│       │       └── dimensional_door.json
│       └── minecraft/tags/block/
│           ├── mineable/pickaxe.json
│           └── needs_iron_tool.json
```

---

## 🔧 技术栈

| 技术 | 用途 |
|------|------|
| **Fabric** | 模组平台 |
| **GeckoLib 4.x** | 3D 模型动画 |
| **沉浸传送门 (Immersive Portals)** | 门中景渲染 |
| **Minecraft 1.21.1** | 游戏版本 |

---

## 🎯 未来计划

### 🔲 铜锣烧 (Dorayaki)
- 恢复 8 饥饿值
- 获得"力量 I"效果 30 秒
- 合成配方：小麦 + 红豆/糖

### 🔲 画圈圈动作
- 使用任意门时播放手臂画圈动画
- 粒子效果环绕

### 🔲 哆啦A梦变形
- 集成变形模组
- 哆啦A梦、哆啦美、大雄、静香、胖虎、小夫
- 小夫的尖嘴巴 🦊

### 🔲 更多道具
- 竹蜻蜓（飞行）
- 缩小灯（缩小玩家）
- 记忆面包（复制书本）

---

## 📦 依赖

```gradle
dependencies {
    modImplementation "net.fabricmc.fabric-api:fabric-api:${project.fabric_version}"
    modImplementation "software.bernie.geckolib:geckolib-fabric-${minecraft_version}:${geckolib_version}"
    modImplementation "qouteall.immersive_portals:immersive-portals-fabric:${immersive_portals_version}"
}
```

---

## 📝 开发者笔记

### 传送门位置计算
```java
// door2 门板中心相对于方块原点: (0, 19, -7)
// 世界坐标 = 方块坐标 + 本地坐标 / 16
Vec3 origin = new Vec3(
    pos.getX() + 0.5,              // X: 方块中心
    pos.getY() + 19.0 / 16.0,      // Y: 1.1875
    pos.getZ() + 0.5 + (-7.0 / 16.0) // Z: 0.0625
);
```

### 传送冷却
- 冷却时间：**1秒** (20 tick)
- 防止玩家在双向门间无限循环

### 门板旋转
- 关闭状态：`[0, 0, 0]`
- 打开状态：`[0, -155, 0]`

---

## 📄 许可证

MIT License

---

## 🙏 致谢

- **藤子·F·不二雄** — 创作了《哆啦A梦》这个美好的世界
- **Fabric 社区** — 提供了优秀的模组开发工具
- **沉浸传送门模组** — 实现了"门中景"效果
- **GeckoLib** — 让 3D 动画变得简单

---

> 🎐 你正在看的，是来自 22 世纪的模组哦！

---
*最后更新：2026年*