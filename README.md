# Alex's Accurate Mob Spawn

> Control **how many** mobs spawn at once and **how often** they are picked for a natural spawn —
> for **any** mob, vanilla or modded, through plain TOML files.
> Built in defaults cover vanilla, Alex's Caves and Alex's Mobs; you can drop in your own files to
> control other mods' mobs.

**English** · [中文说明](#中文说明)

---

## English

| | |
|---|---|
| Mod ID | `alexsaccuratemobspawn` |
| Version | `0.0.4-1.20.1` |
| Minecraft | 1.20.1 |
| Loader | Forge 47 or newer |
| Dependencies | **none required** (Alex's Caves / Alex's Mobs are optional; the built in defaults become useful once they are installed) |
| Author | Huziyang520 |
| License | LGPL-3.0-only |

### 1. Two independent features

| Feature | Controls | Values |
|---|---|---|
| **Spawn multiplier** | **how many** spawn at once | `0.0` = every spawn is blocked; `1.0` = vanilla; `> 1` = more per spawn |
| **Spawn probability** | **how often** the mob is picked (= spawn frequency) | `0.0` = never spawns naturally; `1.0` = vanilla; `> 1` = more often (e.g. `3.0` ≈ triple) |

Example: probability `3.0` with multiplier `1.0` → roughly one mob in ~3× as many places; probability `3.0` with multiplier `5.0` → five mobs in each of those places.

#### Multiplier in detail

| `x` | Behaviour |
|---|---|
| `x = 1.0` | no change, pure vanilla |
| `0 < x < 1` | the spawn is kept with probability `x`, otherwise cancelled |
| `x = 0.0` | always cancelled, the mob never appears |
| `x > 1` | one mob spawns normally, then `⌊x⌋ - 1` copies are placed around it, plus one more with the probability of the fraction (e.g. `x = 2.5` → 2 or 3 in total); at most 50 extra copies per spawn |

The multiplier is applied when the entity joins the level, so it covers **every source**: natural spawning, mob spawners, structures, spawn eggs, `/summon`, event summons.

#### Probability in detail

Probability applies to **natural spawning only**: the vanilla spawn cycle, the world generation batch, and structure spawning. These sources are deliberately **not** affected: spawn eggs, `/summon` and other commands, mob spawner blocks, event / transformation spawns.

Two mechanisms internally, both perceived as "spawn frequency":

| Value | Mechanism | Effect |
|---|---|---|
| `0.0` | every natural spawn opportunity of that mob is blocked | the mob stops spawning naturally |
| `0 ~ 1` | each opportunity is kept with that probability, rolled independently | `0.5` ≈ half of the opportunities succeed |
| `1.0` | no change | vanilla frequency |
| `> 1` | the mob's weight in the biome spawn list is scaled up | `3.0` ≈ about triple frequency (approximate, not exact) |

- The `0 ~ 1` decision happens when the mob is about to join the level and **never modifies the vanilla spawn lists**, so it cannot shift the odds of any other mob and cannot interfere with vanilla's own spawn validation.
- The `> 1` boost applies to **biome spawn lists** (including the world generation batch and Alex's Caves' cave burst). Structure `spawn_overrides` and the nether fortress list are not scaled, but the `0 ~ 1` blocking still applies to them.
- Extra individuals created by a multiplier above `1.0` are not affected by probability (they are a multiplier effect).

### 2. Installing

1. Install **Minecraft 1.20.1 + Forge 47 or newer**.
2. Drop the mod jar into your `mods` folder.
3. Optional: install **Alex's Caves** and/or **Alex's Mobs** — the built in default entries for them become meaningful right away.
4. Start the game. The mod creates every config file under `config/alexsaccuratemobspawn/` on first launch.

Both the client and the server need the mod. The configuration is a **global config** (not stored per world); setting it on the server is enough.

On every login the mod shows a short yellow chat notice with the config folder path. Turn it off with `showChatNotice = false`.

### 3. Configuration files

Created on first launch:

```
config/alexsaccuratemobspawn/
├── common.toml                                  ← switches
├── alexscavessaccuratemobspawnmultiplier.toml   ← Alex's Caves spawn multiplier (43 entries)
├── alexsmobssaccuratemobspawnmultiplier.toml    ← Alex's Mobs spawn multiplier (116 entries)
├── alexscavessaccuratemobspawnprobability.toml  ← Alex's Caves spawn probability (43 entries)
├── alexsmobssaccuratemobspawnprobability.toml   ← Alex's Mobs spawn probability (116 entries)
└── <any .toml you add yourself>                 ← same format, later files override earlier ones
```

#### 3.1 `common.toml`

```toml
# Show the chat notice when a player joins a world
showChatNotice = true

# Master switch for the multiplier (false = never touch "how many spawn at once")
enableMultiplier = true

# Master switch for the probability (false = never touch "spawn frequency")
enableProbability = true
```

All three default to on and **take effect immediately** after saving.

#### 3.2 Rule file format

```toml
# Optional kind declaration: multiplier (default) or probability.
# Can be omitted when the file name already contains multiplier / probability.
type = "probability"

[mobs]
	# Tremorsaurus
	"alexscaves:tremorsaurus" = 0.2
	# vanilla zombie
	"minecraft:zombie" = 2.0
	# other mods work the same way
	"twilightforest:hydra" = 0.0
```

- Keys are the **full entity registry name** (`namespace:path`) and must be quoted.
- Accepted range: multiplier `0.0 ~ 10.0`, probability `0.0 ~ 100.0`. Out of range values are clamped and reported in the log.
- Mobs that are not listed are not touched (treated as `1.0`).
- Broken entries (invalid id, non numeric value) are skipped and listed in the log one by one — they can never crash the game.

#### 3.3 Telling multiplier and probability files apart

Multiplier and probability are two independent numbers, so the mod has to know which kind a file holds:

1. **Recommended: declare it at the top of the file**
   ```toml
   type = "multiplier"   # or type = "probability"
   ```
2. **Without `type` the file name decides**: a name containing `probability` / `prob` is a probability file, anything else is treated as a multiplier file (and the log tells you to add `type`).
3. The four built in file names are fixed: `...multiplier.toml` is a multiplier file, `...probability.toml` is a probability file. Renaming them makes the mod pick the wrong kind.

> In one sentence: **the same mob can have a line in both files** — the multiplier line says "how many at once", the probability line says "how often it spawns".

#### 3.4 Adding your own files for other mobs

Just create any `.toml` file in `config/alexsaccuratemobspawn/`, for example `my_rules.toml`:

```toml
type = "probability"

[mobs]
	# vanilla zombie: about twice as often
	"minecraft:zombie" = 2.0
	# vanilla creeper: no longer spawns naturally
	"minecraft:creeper" = 0.0
	# another mod's mob (use its full registry name)
	"twilightforest:hydra" = 0.5
```

```toml
type = "multiplier"

[mobs]
	# vanilla cow: five at a time
	"minecraft:cow" = 5.0
```

Rules:

- Keys are the **full entity registry name** `namespace:path` and must be quoted.
- **Later files override earlier ones**: load order is the four built in files (in the fixed order above), then your files in alphabetical order. To override a built in value, use a late name such as `zz_mine.toml`.
- **New files are picked up automatically**, no restart needed (the folder is scanned every 2 seconds; renaming and deleting work the same way).
- Unknown entity ids are ignored with a log warning and have no side effects.

#### 3.5 Checking that the rules really apply

The mod prints **bounded** evidence (once per kind, never one line per spawn). Search the log for `alexsaccuratemobspawn`:

| Log line | Meaning |
|---|---|
| `Config reloaded (startup): 159 spawn settings, 159 probability settings, switches[...]` | how many rules were read and the state of the three switches |
| `Applied spawn frequency boosts (probability > 1) to a spawn list of 8 entries` | a probability above `1` was applied (once per spawn list and reload) |
| `Spawn probability for <mob>: blocked natural spawn, probability 0.0` | probability `0`: the natural spawn was blocked |
| `Spawn probability for <mob>: dropped natural spawn, probability 0.5` | probability `< 1`: this opportunity was dropped |
| `Spawn multiplier for <mob>: spawning extra copies, multiplier 10.0` | the multiplier applied to that mob (once per mob and reload) |
| `Spawn multiplier for <mob>: blocked every spawn, multiplier 0.0` | multiplier `0`: the mob is blocked completely |
| `Config: <file>: ...` | one warning per broken config entry |

#### 3.6 Quick self test (3 minutes)

1. Create `verify.toml`:
   ```toml
   type = "probability"

   [mobs]
   	"minecraft:pig" = 0.0
   	"minecraft:cow" = 3.0
   ```
2. Start the game and search the log as described in 3.5: you should see `blocked natural spawn, probability 0.0` for the pig and `Applied spawn frequency boosts ...` for the cow, and you should **not** see thousands of repeated lines.
3. Walk into a biome you have never visited: pigs no longer appear on their own (spawn eggs still work), cows appear noticeably more often.
4. Reverse test: set `minecraft:pig = 1.0` and `minecraft:cow = 0.0`, wait a second for the hot reload, then check freshly generated chunks again.

#### 3.7 Hot reload

Every config file is watched by the mod itself: **edit and save while the game is running and it applies about a second later**, no restart. If a newly written file has a syntax error the mod keeps the last working configuration and prints a warning, so spawning never becomes uncontrollable.

### 4. Default supported mobs

#### Alex's Caves (43 entries)

| Config key (after `alexscaves:`) | Mob | Chinese name |
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

#### Alex's Mobs (116 entries)

All 116 Alex's Mobs entities (including parts, projectiles and vehicles) are preset with `1.0`.
**Every entry in the file carries a comment** explaining the mob and whether it can spawn naturally:

```
	# Grizzly Bear
	"alexsmobs:grizzly_bear" = 1.0
	# bone serpent part (multi part body, never spawns on its own; multiplier and probability do not apply)
	"alexsmobs:bone_serpent_part" = 1.0
	# Mimicube (structure only; probability does not apply, the multiplier still does)
	"alexsmobs:mimicube" = 1.0
```

Read the file itself — no lookup table needed.

#### Vanilla and other mods

Create a `.toml` file using the format from 3.2 and write the full registry name as the key.
**Ids that do not exist are ignored with a warning**, so a typo never has side effects.

### 5. Upgrading from the old version

The config file of the previous version (`Alex's Caves Accurate Mob Spawn`, mod id
`alexscavessaccuratemobspawn`), `config/alexscavessaccuratemobspawn-common.toml`, is **migrated
automatically** on first launch:

1. all 43 old multiplier values are written entry by entry into `config/alexsaccuratemobspawn/alexscavessaccuratemobspawnmultiplier.toml`;
2. the result is printed line by line in the log so you can verify it;
3. once the migration succeeded the old file is **deleted**.

If the old file cannot be parsed the mod leaves it untouched and only logs the problem, so nothing is lost.

### 6. Notes, boundaries and known limits

#### 6.1 Boundaries (important)

- **Probability only affects natural spawning**, and only three sources count as natural: the vanilla spawn cycle (`NATURAL`), the world generation batch (`CHUNK_GENERATION`) and structure spawning (`STRUCTURE`). Spawn eggs, `/summon` and other commands, mob spawner blocks and event / transformation spawns are **not** affected.
- **`0 ~ 1` never touches the vanilla spawn lists.** It blocks the mob while it is about to join the level, so it cannot change the odds of other mobs in the same list and cannot interfere with vanilla's own spawn-table validation. (This is the core of the 0.0.4 fix.)
- **`> 1` only scales biome spawn lists.** Structure `spawn_overrides` and the nether fortress list are not scaled, but the `0 ~ 1` blocking still applies to them.
- **`> 1` takes weight away from the others.** Raising one mob lowers the relative share of the other mobs in the same spawn list. That is inherent to "frequency", not a bug.
- **Extra individuals created by the multiplier are not filtered by probability** (they are a multiplier effect).
- **Mobs already in the world never disappear** because of a config change. Test in chunks you have never generated, or wait until the old mobs despawn.
- `0 ~ 1` is an **exact per-opportunity probability** (`0.5` ≈ half of the opportunities succeed); `> 1` internally scales weights with a 256× factor, so `3.0` means roughly triple. To disable a mob completely write `0.0` — `0.001` is close to zero but not zero.
- The configuration is **global**, not per world, and both client and server need the mod installed.

#### 6.2 Other limits

- Extra individuals from a multiplier above `1.0` are copies of the entity; they do not inherit equipment or spawn context, and using `> 1` on a boss spawns several bosses at once — use with care.
- When several mods change spawn weights (Alex's Caves' own `cave_creature_spawn_count_modifier`, Alex's Mobs' own `alexsmobs.toml` weights) this mod scales *on top of their result*, so the effects stack.
- Probability `> 1` uses a 256× internal scale; increments below `1/256` may be lost. Use `1.5` or more for a visible increase.
- This mod **does not modify world/save data** and can be removed at any time.

### 7. License and feedback

- License: **LGPL-3.0-only** (see `LICENSE.txt`).
- Author: **Huziyang520**
- Feedback and issues: `https://issue.mengcai.online/`

---

# 中文说明

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

概率作用于**自然生成**：原版刷怪循环、世界生成期的成群生成、以及结构生成。
以下来源**不受概率影响**（这是有意的）：刷怪蛋、`/summon` 等命令、刷怪笼方块、事件/转化召唤。

内部按数值分两种机制，观感上都是「生成频率」：

| 取值 | 实现方式 | 效果 |
|---|---|---|
| `0.0` | 每次自然生成机会都拦下该生物 | 该生物不再自然生成 |
| `0 ~ 1` | 每次自然生成机会以该概率保留，逐次独立判定 | `0.5` ≈ 一半的生成机会成功 |
| `1.0` | 不干预 | 原版频率 |
| `> 1` | 放大该生物在群系生成表里的权重 | `3.0` ≈ 约三倍频率（约数，不是精确三倍） |

- `0 ~ 1` 的拦截发生在「生物即将加入世界」这一层，**完全不改动原版生成表**，因此不影响其它生物的权重，也不可能干扰原版的生成校验。
- `> 1` 的加权只作用于**群系生成表**（含世界生成期成群与 Alex's Caves 的洞穴爆发）；结构 `spawn_overrides` 与下界要塞的生成表不会被放大，但 `0 ~ 1` 的拦截对它们**仍然生效**。
- 倍率 `> 1` 复制出来的额外个体不会再被概率拦截（它们属于倍率效果）。

---

## 2. 安装

1. 安装 **Minecraft 1.20.1 + Forge 47 及以上**。
2. 把本模组的 jar 放进 `mods` 文件夹。
3. （可选）装 **Alex's Caves** 和/或 **Alex's Mobs**：装了以后它们的内置默认配置条目会立刻有意义。
4. 启动游戏，模组会在 `config/alexsaccuratemobspawn/` 下自动生成全部配置文件。

单人游戏与服务器都需要安装本模组；配置是**全局配置**（不随存档），服务器端设置一次即可。
每次进入世界时，模组会在聊天框显示一条黄色提示（说明配置文件夹位置）。可用 `showChatNotice = false` 关闭。

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

### 3.3 怎么区分「倍率文件」和「概率文件」

倍率和概率是两套独立的数值，所以**必须让模组知道一个文件属于哪一类**。判定顺序如下：

1. **推荐：在文件顶部写 `type`**
   ```toml
   type = "multiplier"   # 或者 type = "probability"
   ```
2. **没写 `type` 时看文件名**：文件名里含 `probability` / `prob` → 概率文件；否则按**倍率文件**处理（同时会在日志里提醒你加 `type`）。
3. 四个内置文件名固定：`...multiplier.toml` 是倍率，`...probability.toml` 是概率，改文件名会让模组认错类型。

> 一句话：**同一只生物可以在两个文件里各写一行**——写进倍率文件的那行管「一次几只」，写进概率文件的那行管「多久刷一次」。

### 3.4 自己新建文件控制其它生物（含原版与其它模组）

在 `config/alexsaccuratemobspawn/` 下新建任意 `.toml` 文件即可，例如 `my_rules.toml`：

```toml
type = "probability"

[mobs]
	# 原版僵尸：约两倍刷新频率
	"minecraft:zombie" = 2.0
	# 原版苦力怕：不再自然生成
	"minecraft:creeper" = 0.0
	# 其它模组的生物同样可以（写它的完整注册名）
	"twilightforest:hydra" = 0.5
```

```toml
type = "multiplier"

[mobs]
	# 原版牛：一次生成 5 只
	"minecraft:cow" = 5.0
```

规则：

- 键是**完整的实体注册名** `命名空间:路径`，必须带引号。
- **后加载的文件覆盖先加载的同名条目**：加载顺序 = 4 个内置文件（固定顺序）→ 玩家文件（按文件名字典序）。想覆盖内置值，用 `zz_` 之类靠后的文件名即可。
- **新增文件也会被自动加载**，不用重启游戏（模组每 2 秒扫描一次配置目录，改名/删除同样生效）。
- 未知生物 ID 不会报错，只会被忽略并在日志提醒。

### 3.5 确认规则是否真的生效

模组会在日志里给出**有界**的证据（每类只打印一次，不会刷屏），可直接搜 `alexsaccuratemobspawn`：

| 日志行 | 含义 |
|---|---|
| `Config reloaded (startup): 159 spawn settings, 159 probability settings, switches[...]` | 读到了多少条规则、三个开关的状态 |
| `Applied spawn frequency boosts (probability > 1) to a spawn list of 8 entries` | 概率 `> 1` 的加权已生效（每次重载后每个生成表只打一次） |
| `Spawn probability for <生物>: blocked natural spawn, probability 0.0` | 概率 `0`，该生物的自然生成被拦下 |
| `Spawn probability for <生物>: dropped natural spawn, probability 0.5` | 概率 `< 1`，这一次自然生成机会按概率被丢弃 |
| `Spawn multiplier for <生物>: spawning extra copies, multiplier 10.0` | 倍率对该生物生效（每次重载后每类只打一次） |
| `Spawn multiplier for <生物>: blocked every spawn, multiplier 0.0` | 倍率为 `0`，该生物被彻底拦下 |
| `Config: <文件>: ...` | 配置写错时的逐条告警 |

> 概率是「生成频率」：`0 ~ 1` 是**逐次独立判定**（每次刷怪机会各掷一次骰子），`> 1` 是**提高被选中的权重**；两者都不改变原版刷怪上限与刷怪位置的产生方式。
> 已经生成的生物不会因为改配置而消失，改完请到**未生成过的区块**、或等旧生物被清除后再观察。

### 3.6 快速自测（3 分钟）

1. 新建 `verify.toml`：
   ```toml
   type = "probability"

   [mobs]
   	"minecraft:pig" = 0.0
   	"minecraft:cow" = 3.0
   ```
2. 进游戏后按 3.5 搜日志：应出现猪的 `blocked natural spawn, probability 0.0` 与牛的 `Applied spawn frequency boosts ...`；
   并确认**没有**出现成千上万条重复日志。
3. 去从未生成过的群系：猪不再自己出现（刷怪蛋仍能刷出），牛明显变多。
4. 反向测试：把猪改成 `1.0`、牛改成 `0.0`，等约 1 秒热重载后再去新区块观察。

### 3.7 热重载

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

## 6. 注意事项、边界与已知限制

### 6.1 边界（重要）

- **概率只作用于自然生成**，且只认三种来源：原版刷怪循环（`NATURAL`）、世界生成期成群（`CHUNK_GENERATION`）、结构生成（`STRUCTURE`）。刷怪蛋、`/summon` 等命令、**刷怪笼方块**、事件/转化召唤**不受概率影响**。
- **概率 `0 ~ 1` 完全不碰原版生成表**：它是在「生物即将加入世界」时按概率拦截，所以不会改变同一张表里其它生物的权重，也不可能干扰原版对生成表的校验（这正是 0.0.4 修复的核心）。
- **概率 `> 1` 只放大群系生成表**：结构 `spawn_overrides` 与下界要塞的生成表不会被放大；但 `0 ~ 1` 的拦截对它们**仍然生效**。
- **概率 `> 1` 是「抢占权重」**：把某只生物调高，同一张生成表里其它生物的**相对占比会下降**。这是「频率」语义的必然结果，不是 bug。
- **倍率复制出的额外个体不受概率拦截**（它们属于倍率效果）。
- **已经在世界里的生物不会因为改配置而消失**：验证请去**未生成过的区块**，或等旧生物被清除后再观察。
- `0 ~ 1` 是**精确的逐次概率**（`0.5` ≈ 一半机会成功）；`> 1` 内部按 256 倍放大权重，`3.0` 表示约三倍；想彻底禁用请直接写 `0.0`（写 `0.001` 只是接近 0，并不等于 0）。
- 配置是**全局的**（不随存档），单人/服务器都需要安装本模组。

### 6.2 其它限制

- 倍率 `> 1` 的“额外个体”是复制出来的新实体，不会继承原实体的装备、状态等生成上下文；对 Boss 设置大于 `1.0` 会同时出现多只 Boss，请谨慎。
- **概率对不自然生成的生物无效**：结构/方块实体/蛋/事件/实体转化产生的生物（如暝煌龙、撼地斯拉、水雷守卫者、甘草女巫、姜饼人、窥心者、遗弃者，以及 Alex's Mobs 的复刻怪、灵魂鹫、骷髅剑鱼、地底矿工等）概率配置对其无效；但**倍率对它们仍然生效**。
- 概率 `> 1` 内部做了 256 倍放大以获得精度，`1/256` 以下的增量可能被抹平；需要明显提高频率请用 `1.5` 以上。
- 多个模组同时改动生成权重时（例如 Alex's Caves 自带的 `cave_creature_spawn_count_modifier`、Alex's Mobs 自带的 `alexsmobs.toml` 生成权重），本模组是在它们的最终结果之上再做缩放，数值效果会叠加。
- **性能**：概率 `0 ~ 1` 只在生物加入世界时做一次判断，不产生任何列表改动；概率 `> 1` 每个生成表在每次配置版本下只重建一次。
- 本模组**不修改存档数据**，可以随时卸载；卸载后生成完全回到原版与各模组自己的行为。

---

## 7. 许可与反馈

- 许可证：**LGPL-3.0-only**（见 `LICENSE.txt`）。
- 作者：**Huziyang520**
- 反馈与问题：`https://issue.mengcai.online/`
