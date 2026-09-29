# Alex's Accurate Mob Spawn

> 用普通 TOML 配置文件控制**任意生物**的生成数量与生成频率的 Forge 模组。
> 内置覆盖原版、Alex's Caves 与 Alex's Mobs 的默认条目，也支持你自己新建文件控制其它模组的生物。

| 项目 | 值 |
|---|---|
| 模组 ID | `alexsaccuratemobspawn` |
| 当前版本 | `0.0.4-1.20.1` |
| Minecraft | 1.20.1 |
| 加载器 | Forge 47 及以上 |
| 前置 | **无强制前置**（Alex's Caves / Alex's Mobs 为可选，装了会用上内置默认条目） |
| 作者 | Huziyang520 |
| 许可证 | LGPL-3.0-only |

---

## 1. 两个功能，互不影响

| 功能 | 控制什么 | 数值含义 |
|---|---|---|
| **生成倍率**（multiplier） | 一次生成**几只** | `0.0` = 每次生成都被拦截；`1.0` = 原版；`> 1` = 一次生成更多 |
| **生成概率**（probability） | 生成**频率**，即像原版那样多常在某个位置刷出这种生物 | `0.0` = 不再自然生成；`1.0` = 原版频率；`> 1` = 提高频率（例如 `3.0` ≈ 三倍频率） |

举例：概率 `3.0`、倍率 `1.0` → 大约在 3 倍多的位置各生成 1 只；概率 `3.0`、倍率 `5.0` → 大约在 3 倍多的位置各生成 5 只。

### 倍率的具体行为

| 倍率 `x` | 行为 |
|---|---|
| `x = 1.0` | 不干预，完全走原版逻辑 |
| `0 < x < 1` | 以 `x` 的概率保留这次生成，其余拦截 |
| `x = 0.0` | 100% 拦截，该生物不再出现 |
| `x > 1` | 先正常生成 1 只，再在周围额外复制 `⌊x⌋ - 1` 只，小数部分按概率再加 1 只（例如 `x = 2.5` → 共生成 2 或 3 只）；单次额外复制上限 50 只 |

倍率挂在“实体加入世界”这一层，因此对**任何来源**都生效：自然生成、刷怪笼、结构生成、刷怪蛋、`/summon`、事件召唤。

### 概率的具体行为

概率作用于**自然生成**：原版刷怪循环、世界生成期的成群生成、以及结构的 `spawn_overrides`。
以下来源**不受概率影响**（这是有意的）：刷怪蛋、`/summon` 等命令、事件/转化召唤。

---

## 2. 安装

1. 安装 **Minecraft 1.20.1 + Forge 47 及以上**。
2. 把本模组的 jar 放进 `mods` 文件夹。
3. （可选）装 **Alex's Caves** 和/或 **Alex's Mobs**：装了以后它们的内置默认配置条目会立刻有意义。
4. 启动游戏，模组会在 `config/alexsaccuratemobspawn/` 下自动生成全部配置文件。

单人游戏与服务器都需要安装本模组；配置是**全局配置**（不随存档），服务器端设置一次即可。

---

## 3. 配置文件

首次启动后自动生成：

```
config/alexsaccuratemobspawn/
├── common.toml                                  ← 开关
├── alexscavessaccuratemobspawnmultiplier.toml   ← Alex's Caves 生成倍率（43 项）
├── alexsmobssaccuratemobspawnmultiplier.toml    ← Alex's Mobs 生成倍率（116 项）
├── alexscavessaccuratemobspawnprobability.toml  ← Alex's Caves 生成概率（43 项）
├── alexsmobssaccuratemobspawnprobability.toml   ← Alex's Mobs 生成概率（116 项）
└── <你可以自己新建任意 .toml>                    ← 同格式，后加载的覆盖先加载的
```

### 3.1 `common.toml`

```toml
# 是否在每次进入世界时显示模组提示信息
showChatNotice = true

# 倍率功能总开关（false = 完全不干预“一次生成几只”）
enableMultiplier = true

# 概率功能总开关（false = 完全不干预“生成频率”）
enableProbability = true
```

三个开关默认全部打开，且**改了立刻生效**。

### 3.2 规则文件格式

```toml
# 可选的类型声明：multiplier（默认）或 probability。
# 文件名里带 multiplier / probability 时可以省略这一行。
type = "probability"

[mobs]
	# 撼地龙
	"alexscaves:tremorsaurus" = 0.2
	# 原版僵尸
	"minecraft:zombie" = 2.0
	# 其它模组的生物同样可以
	"twilightforest:hydra" = 0.0
```

- 键是**完整的实体注册名**（`命名空间:路径`），带引号书写。
- 取值范围：倍率 `0.0 ~ 10.0`，概率 `0.0 ~ 100.0`；超出会被钳制到边界并在日志里告警。
- 没写进文件的生物 = 不干预（按 `1.0` 处理）。
- 写错的条目（非法 ID / 非数字）会被跳过，并在日志里逐条列出，**不会导致游戏崩溃**。

### 3.3 热重载

所有配置文件都由模组自己监听：**在游戏运行时直接编辑保存，约 1 秒后自动生效**，不需要重启游戏。
如果新写入的文件语法有误，模组会保留上一份可用的配置并打印告警，不会让生成变得不可控。

---

## 4. 默认支持的生物

### Alex's Caves（43 项）

| 配置键（`alexscaves:` 之后的部分） | 生物 | 中文名 |
|---|---|---|
| `magnetron` | Magnetron | 磁控机兵 |
| `teletor` | Teletor | 磁流灵 |
| `boundroid` | Boundroid | 弹跳机兵 |
| `ferrouslime` | Ferrouslime | 富铁史莱姆 |
| `notor` | Notor | 扫描机兵 |
| `gammaroach` | Gammaroach | 伽马蟑螂 |
| `brainiac` | Brainiac | 舐脑魔 |
| `nucleeper` | Nucleeper | 核能苦力怕 |
| `raycat` | Raycat | 射线猫 |
| `radgill` | Radgill | 辐鳃鱼 |
| `subterranodon` | Subterranodon | 地底翼龙 |
| `vallumraptor` | Vallumraptor | 阔鼻迅猛龙 |
| `grottoceratops` | Grottoceratops | 洞穴角龙 |
| `tremorsaurus` | Tremorsaurus | 撼地龙 |
| `relicheirus` | Relicheirus | 遗迹恐手龙 |
| `atlatitan` | Atlatitan | 擎天龙 |
| `trilocaris` | Trilocaris | 三叶虾 |
| `deep_one` | Deep One | 深潜者 |
| `deep_one_knight` | Deep One Knight | 深潜者骑士 |
| `deep_one_mage` | Deep One Mage | 深潜者法师 |
| `lanternfish` | Lanternfish | 灯笼鱼 |
| `hullbreaker` | Hullbreaker | 碎船兽 |
| `tripodfish` | Tripodfish | 三脚鱼 |
| `sea_pig` | Sea Pig | 海猪 |
| `gossamer_worm` | Gossamer Worm | 浮蚕 |
| `mine_guardian` | Mine Guardian | 水雷守卫者 |
| `gloomoth` | Gloomoth | 幽暗蛾 |
| `underzealot` | Underzealot | 地底异教徒 |
| `corrodent` | Corrodent | 蚀牙兽 |
| `vesper` | Vesper | 夜行蝠 |
| `watcher` | Watcher | 窥心者 |
| `forsaken` | Forsaken | 遗弃者 |
| `candicorn` | Candicorn | 糖果独角兽 |
| `gummy_bear` | Gummy Bear | 软糖熊 |
| `caniac` | Caniac | 嗜糖魔 |
| `gumbeeper` | Gumbeeper | 糖球苦力怕 |
| `gum_worm` | Gum Worm | 口香糖蠕虫 |
| `caramel_cube` | Caramel Cube | 焦糖怪 |
| `licowitch` | Licowitch | 甘草女巫 |
| `gingerbread_man` | Gingerbread Man | 姜饼人 |
| `sweetish_fish` | Sweetish Fish | 软糖鱼 |
| `luxtructosaurus` | Luxtructosaurus | 暝煌龙 |
| `tremorzilla` | Tremorzilla | 撼地斯拉 |

### Alex's Mobs（116 项）

Alex's Mobs 的全部 116 个实体（含部件、投射物、载具）都已预置，默认值一律 `1.0`。
**文件里每一条都带中文名与分类注释**，例如：

```
	# 灰熊
	"alexsmobs:grizzly_bear" = 1.0
	# 骨蟒骨节（多体节部件，不独立生成；倍率与概率均不适用）
	"alexsmobs:bone_serpent_part" = 1.0
	# 复刻怪（仅结构生成；概率对其无效，倍率仍生效）
	"alexsmobs:mimicube" = 1.0
```

直接对照文件即可，不必查表。

### 其它生物 / 原版 / 其它模组

新建一个 `.toml` 文件，套用 3.2 的格式即可，键写该生物的完整注册名。
**不存在的生物 ID 会被忽略并告警**，写错不会有副作用。

---

## 5. 从旧版本升级

旧版本（`Alex's Caves Accurate Mob Spawn`，模组 ID `alexscavessaccuratemobspawn`）的配置文件
`config/alexscavessaccuratemobspawn-common.toml` 会在首次启动时**自动迁移**：

1. 43 项旧的倍率值逐条写入 `config/alexsaccuratemobspawn/alexscavessaccuratemobspawnmultiplier.toml`；
2. 迁移结果会逐条打印在日志里，便于核对；
3. 迁移成功后**删除旧文件**。

如果旧文件本身无法解析，模组会保留原文件不动、只打日志，不会造成数据丢失。

---

## 6. 注意事项与已知限制

- **倍率 `> 1` 的“额外个体”是复制出来的新实体**，不会继承原实体的装备、状态等生成上下文；对 Boss 设置大于 `1.0` 会同时出现多只 Boss，请谨慎。
- **概率只作用于自然生成**：结构/方块实体/蛋/事件/实体转化产生的生物（如暝煌龙、撼地斯拉、水雷守卫者、甘草女巫、姜饼人、窥心者、遗弃者，以及 Alex's Mobs 的复刻怪、灵魂鹫、骷髅剑鱼、地底矿工等）概率配置对其无效；但**倍率对它们仍然生效**。
- 概率对**提高**频率的实现方式是比较“被选中”的权重，所以 `3.0` 表示约三倍，不是精确的三倍；`0.0` 则是彻底不再自然生成。
- 概率的精度：内部做了 256 倍放大，低于约 `1/256` 的概率会被抬到最小值，想彻底禁用请直接写 `0.0`。
- 多个模组同时改动生成权重时（例如 Alex's Caves 自带的 `cave_creature_spawn_count_modifier`、Alex's Mobs 自带的 `alexsmobs.toml` 生成权重），本模组是在它们的最终结果之上再做缩放，数值效果会叠加。
- 本模组**不修改存档数据**，可以随时卸载。

---

## 7. English Summary

**Alex's Accurate Mob Spawn** (Forge 1.20.1) gives you two independent, hot reloadable settings per mob:

- **Multiplier** – how many individuals spawn at once (`0` = never, `1` = vanilla, `2.5` = 2 or 3).
- **Probability** – how often the mob is picked for a spawn (= spawn frequency, `0` = disabled, `3` = roughly triple).

Everything lives in plain TOML files under `config/alexsaccuratemobspawn/`. Built in defaults cover all 43
Alex's Caves mobs and all 116 Alex's Mobs entities, and you can add your own files to control **any** mob from
vanilla or other mods. Alex's Caves and Alex's Mobs are optional dependencies.

---

## 8. 许可与反馈

- 许可证：**LGPL-3.0-only**（见 `LICENSE.txt`）。
- 作者：**Huziyang520**
- 反馈与问题：`https://issue.mengcai.online/`
