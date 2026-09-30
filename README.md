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

Probability is the **occurrence multiplier** of that mob's natural spawning, and one single rule covers every value:

> **expected natural spawns = vanilla occurrences × probability**

| Value | Effect |
|---|---|
| `0.0` | every natural spawn opportunity of this mob is voided — it stops spawning naturally |
| `0 ~ 1` | each opportunity this mob already won survives with that probability, the rest are voided |
| `1.0` | no change, vanilla frequency |
| `> 1` | the vanilla occurrence is kept and the missing occurrences are created by the mod's own spawn library |

- **No other mob is affected, ever.** A voided opportunity is simply discarded — it is never handed to another mob — and the added occurrences are produced by the mod itself instead of rewriting the shared spawn list. No other mob's weight, entry or behaviour is touched at all.
- **Added occurrences are spread out, not copies.** Each one is an independent vanilla style attempt: a random direction and a random distance of 8–48 blocks around the original spawn, validated exactly like a natural spawn (the biome must really list this mob, the placement rules must pass, the spot must be free). Zombies at `10.0` therefore appear in roughly 10× as many places, scattered just like vanilla spawns.
- **Hard limits:** at most 100 extra individuals for a single natural spawn (the configured value itself caps at 100, so at most 99 are ever used) and at most 100 extra individuals alive around the spawn point. The added individuals do **not** count towards the vanilla mob cap, so the vanilla spawn cycle keeps giving every other mob its full, unchanged share.
- Only natural spawns count: `NATURAL`, `CHUNK_GENERATION` and `STRUCTURE`. Spawn eggs, `/summon` and other commands, mob spawner blocks and event / transformation spawns are never affected.
- Individuals created this way are not processed by probability a second time (no doubling), but they **are** handled by the multiplier, so the two features stack independently.

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

#### 3.4.1 Copy and paste examples

```toml
# 1. Zombies about three times as often. Other mobs are not touched.
#    File: zz_my_zombie.toml
type = "probability"

[mobs]
	"minecraft:zombie" = 3.0
```

```toml
# 2. Creepers no longer spawn naturally (spawn eggs and commands still work).
type = "probability"

[mobs]
	"minecraft:creeper" = 0.0
```

```toml
# 3. Endermen about half as often.
type = "probability"

[mobs]
	"minecraft:enderman" = 0.5
```

```toml
# 4. Stacking: twice as many occurrences and four at a time.
#    probability file:
type = "probability"

[mobs]
	"minecraft:cow" = 2.0
```
```toml
#    multiplier file (same folder, different file):
type = "multiplier"

[mobs]
	"minecraft:cow" = 4.0
```

```toml
# 5. Override a built in value: built in files load first, your files load last,
#    so a late file name wins. Here tremorsaurus goes back to vanilla.
type = "probability"

[mobs]
	"alexscaves:tremorsaurus" = 1.0
```

```toml
# 6. Any mod works, just use the full registry name.
type = "multiplier"

[mobs]
	"twilightforest:hydra" = 1.0
	"iceandfire:dragon" = 0.0
```

#### 3.5 Checking that the rules really apply

The mod prints **bounded** evidence (once per kind, never one line per spawn). Search the log for `alexsaccuratemobspawn`:

| Log line | Meaning |
|---|---|
| `Config reloaded (startup): 159 spawn settings, 159 probability settings, switches[...]` | how many rules were read and the state of the three switches |
| `Spawn probability for <mob>: queued N extra spread out attempts, probability 10.0` | probability above `1`: the missing occurrences were queued (once per mob and reload) |
| `Spawn probability for <mob>: voided this opportunity, probability 0.5` | probability below `1`: this opportunity was voided |
| `Spawn probability for <mob>: voided every natural opportunity, probability 0.0` | probability `0`: the mob no longer spawns naturally |
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
2. Start the game and search the log as described in 3.5: for the pig you should see `Spawn probability for minecraft:pig: voided every natural opportunity, probability 0.0` and for the cow `Spawn probability for minecraft:cow: queued 2 extra spread out attempts, probability 3.0`.
3. Walk into a biome you have never visited: pigs no longer appear on their own (spawn eggs still work), cows appear in roughly three times as many places and spread out, while every other mob keeps its vanilla rate.
4. Reverse test: set `minecraft:pig = 1.0` and `minecraft:cow = 0.0`, wait a second for the hot reload, then check freshly generated chunks again.

#### 3.7 Hot reload

Every config file is watched by the mod itself: **edit and save while the game is running and it applies about a second later**, no restart. If a newly written file has a syntax error the mod keeps the last working configuration and prints a warning, so spawning never becomes uncontrollable.

#### 3.8 Why it can look like nothing happened

**Probability is applied to natural spawning only.** That is by design, and it is the reason it cannot affect any other mob:

| How the mob appears | Does probability apply? |
|---|---|
| Spawn egg | **No** (`SPAWN_EGG`) |
| `/summon`, command blocks | **No** (`COMMAND`) |
| Mob spawner block | **No** (`SPAWNER`) |
| Event / summoning / transformation (Alex's Caves' gobthumper and licowitch, the giant squid made by lightning) | **No** (`MOB_SUMMONED`, `CONVERSION`) |
| The vanilla spawn cycle | **Yes** (`NATURAL`) |
| World generation batch | **Yes** (`CHUNK_GENERATION`) |
| **Alex's Caves' cave creature burst** (it runs its own loop while a chunk is generated instead of the vanilla cycle) | **Yes** (`CHUNK_GENERATION`) |
| **Alex's Caves' roost feature and its world generated dinosaur eggs** (they create the mob without ever calling `finalizeSpawn`) | **Yes** - world generated eggs (`needs_player = true`) and roost spawns count as natural; eggs a player places or gets from breeding stay unaffected |
| Structure spawns (and Alex's Caves' amber monolith) | **Yes** (`STRUCTURE` / `CHUNK_GENERATION`) |
| Alex's Mobs' beached whale spawner | **No** (it passes `SPAWNER`) |
| Any other mod's own spawn logic | follows the `MobSpawnType` it passes to `finalizeSpawn` |

**The multiplier applies to every source above** — so "the multiplier reacts immediately while probability appears dead" almost always means the test used a spawn egg or a command.

Other common misreadings:

1. **Probability above `1` needs a natural spawn of that mob as its anchor.** It only adds anything where the biome already spawns that mob and where it really got spawned naturally. Editing the value and then standing in the same old chunks shows nothing, because those spawns already happened.
2. **The added individuals are placed by vanilla placement rules** (light, ground, collision, biome), so some attempts fail ⇒ the real increase can be smaller than the factor; hard limits are 100 added per spawn and 100 extra alive around the spawn point.
3. **The built in probability files default to `1.0` everywhere** — save your edits, the mod reloads about a second later and logs `Config reloaded`.
4. **`0 ~ 1` and a multiplier below `1` both make a mob rarer**: probability only affects natural spawning (spawn eggs and commands still work), the multiplier affects every source. Use the multiplier to remove a mob completely, probability to lower its natural spawn frequency.

**How to test it properly (3 steps)**

1. Use a **common vanilla mob** (zombie, cow or pig is easiest) and set it to `3.0`. **Do not use a spawn egg.**
2. Walk into **chunks you have never generated** (travel far or `/tp`) and let the natural spawn cycle run for one to three minutes.
3. Search the log for `Spawn probability for`:
   - `queued 2 extra spread out attempts, probability 3.0` ⇒ two additional attempts per natural spawn (about 3× in total) ✅
   - `voided every natural opportunity, probability 0.0` ⇒ that mob's natural opportunities are voided ✅
   You should see the difference in game while **every other mob keeps its vanilla amount**.

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

- **No other mob is ever affected.** Probability only voids this mob's own opportunities and, above `1`, creates the missing occurrences with the mod's own spawn library. The shared spawn list is never rewritten, no other mob's weight or entry is touched, and because nothing is replaced vanilla's own spawn validation cannot be disturbed either.
- **Probability only affects natural spawning**: the vanilla spawn cycle (`NATURAL`), the world generation batch and Alex's Caves' cave burst (`CHUNK_GENERATION`), and structure spawning including Alex's Caves' amber monolith (`STRUCTURE` / `CHUNK_GENERATION`). Spawn eggs, `/summon` and other commands, mob spawner blocks and event / summoning / transformation spawns (`MOB_SUMMONED`, `CONVERSION`, `SPAWNER`) are **not** affected. The origin is read from `Mob#finalizeSpawn`, so any other mod's own spawn logic is judged by the type it passes there.
- **`> 1` creates real individuals**, so the population of that category does rise. Those individuals are excluded from the vanilla mob cap (as requested), which is exactly why every other mob keeps its full vanilla spawn rate — at the price that the world can hold more mobs than vanilla normally allows.
- **Hard limits:** at most 100 extra individuals for one natural spawn and at most 100 extra individuals alive around the spawn point.
- **`0 ~ 1` versus a multiplier below `1`:** both make the mob rarer. Probability voids natural opportunities only (spawn eggs and commands still work), the multiplier acts on *every* source. Keep both: probability for natural spawn frequency, the multiplier for "how many at once" or to hide a mob completely.
- **Extra individuals created by the multiplier are not filtered by probability**, and individuals created by probability go through the multiplier normally — that is what makes the two features independent and stackable.
- **Mobs already in the world never disappear** because of a config change. Test in chunks you have never generated, or wait until the old mobs despawn.
- `1.0` means untouched; a very small non-zero value (`0.001`) means "almost never" rather than exactly never — write `0.0` to remove a mob from natural spawning completely.
- The configuration is **global**, not per world, and both client and server need the mod installed.

#### 6.2 Other limits

- Extra individuals from a multiplier above `1.0` are copies of the entity; they do not inherit equipment or spawn context, and using `> 1` on a boss spawns several bosses at once — use with care.
- This mod changes no spawn weight at all, so it does not interfere with other mods' spawn settings: Alex's Caves' `cave_creature_spawn_count_modifier` and Alex's Mobs' own weights keep working exactly as they do without this mod.
- Probability `> 1` adds occurrences instead of changing weights, so the factor is exact: `10.0` really is about ten times as many occurrences (bounded by the hard limits and by how many valid spots the area offers).
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

概率 = 这只生物自然生成的**次数倍率**，一条规则覆盖所有取值：

> **期望自然生成次数 = 原版次数 × 概率值**

| 取值 | 效果 |
|---|---|
| `0.0` | 它的自然生成机会全部作废 —— 不再自然生成 |
| `0 ~ 1` | 它**已经拿到的**每次机会按该概率保留，其余作废 |
| `1.0` | 不干预，原版频率 |
| `> 1` | 原版那一次照常生成，缺的次数由模组**自建生成通道**补出来 |

- **完全不影响任何其它生物**：作废的机会不会被转给别的生物；补出来的次数由模组自己生成、**不改原版生成表**，其它生物的权重、条目、行为一个都不动。
- **补出来的是"分散出现"，不是"贴身复制"**：每次都是独立的、原版风格的刷怪尝试 —— 在原生成点周围 **8~48 格**随机方向、随机距离选点，并按原版规则校验（该群系必须真的列出这只生物、放置规则必须通过、地面必须空闲）。所以僵尸 `10.0` 的效果是"**约 10 倍多的刷怪位置**"，分布和原版自己刷怪一样分散。
- **硬上限**：单次生成最多补 **100** 个（概率值本身上限 100，实际最多补 99 次）；生成点周围额外存活的个体最多 **100** 个。补出来的个体**不计入原版刷怪上限**，因此原版给其它所有生物的刷怪机会一点不变。
- 概率只作用于**自然生成**（`NATURAL` / `CHUNK_GENERATION` / `STRUCTURE`）；刷怪蛋、`/summon` 等命令、刷怪笼方块、事件/转化召唤不受影响。
- 补出来的个体不会被概率二次处理（不会套娃），但**会正常被倍率处理**，因此两功能独立叠加。

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

#### 3.4.1 直接抄用的示例

```toml
# 例 1：原版僵尸出现次数约 3 倍（其它生物不受影响）
# 文件名：zz_my_zombie.toml
type = "probability"

[mobs]
	"minecraft:zombie" = 3.0
```

```toml
# 例 2：原版苦力怕不再自然生成（刷怪蛋、命令仍可用）
type = "probability"

[mobs]
	"minecraft:creeper" = 0.0
```

```toml
# 例 3：末影人出现次数减半
type = "probability"

[mobs]
	"minecraft:enderman" = 0.5
```

```toml
# 例 4：两功能叠加 —— 出现次数 2 倍、且一次生成 4 只
# 概率文件：
type = "probability"

[mobs]
	"minecraft:cow" = 2.0
```
```toml
# 倍率文件（同目录、另一个文件）：
type = "multiplier"

[mobs]
	"minecraft:cow" = 4.0
```

```toml
# 例 5：覆盖内置文件里的值 —— 内置先加载、玩家文件后加载，靠后的文件名生效
# 这里把撼地龙改回原版
type = "probability"

[mobs]
	"alexscaves:tremorsaurus" = 1.0
```

```toml
# 例 6：任意模组的生物，写完整注册名即可
type = "multiplier"

[mobs]
	"twilightforest:hydra" = 1.0
	"iceandfire:dragon" = 0.0
```

### 3.5 确认规则是否真的生效

模组会在日志里给出**有界**的证据（每类只打印一次，不会刷屏），可直接搜 `alexsaccuratemobspawn`：

| 日志行 | 含义 |
|---|---|
| `Config reloaded (startup): 159 spawn settings, 159 probability settings, switches[...]` | 读到了多少条规则、三个开关的状态 |
| `Spawn probability for <生物>: queued N extra spread out attempts, probability 10.0` | 概率 `> 1`：缺的次数已排队由自建通道补足（每次重载后每类只打一次） |
| `Spawn probability for <生物>: voided this opportunity, probability 0.5` | 概率 `< 1`：这一次机会被作废 |
| `Spawn probability for <生物>: voided every natural opportunity, probability 0.0` | 概率 `0`：该生物不再自然生成 |
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
2. 进游戏后按 3.5 搜日志：猪应出现 `Spawn probability for minecraft:pig: voided every natural opportunity, probability 0.0`，
   牛应出现 `Spawn probability for minecraft:cow: queued 2 extra spread out attempts, probability 3.0`。
3. 去从未生成过的群系：猪不再自己出现（刷怪蛋仍能刷出）；牛在约三倍多的位置**分散**出现，其它生物保持原版刷新量。
4. 反向测试：把猪改成 `1.0`、牛改成 `0.0`，等约 1 秒热重载后再去新区块观察。

### 3.7 热重载

所有配置文件都由模组自己监听：**在游戏运行时直接编辑保存，约 1 秒后自动生效**，不需要重启游戏。
如果新写入的文件语法有误，模组会保留上一份可用的配置并打印告警，不会让生成变得不可控。

### 3.8 为什么有时「看起来没生效」—— 常见误区与正确验证

**概率只作用于自然生成**，这是设计如此，也是它"完全不影响别的生物"的前提：

| 生物是怎么来的 | 概率会生效吗 |
|---|---|
| 刷怪蛋放下 | **不会**（`SPAWN_EGG`） |
| `/summon`、命令方块 | **不会**（`COMMAND`） |
| 刷怪笼方块 | **不会**（`SPAWNER`） |
| 事件 / 召唤 / 实体转化（Alex's Caves 的尖啸锤、甘草女巫召唤物，闪电生成的巨型乌贼等） | **不会**（`MOB_SUMMONED`、`CONVERSION`） |
| 原版刷怪循环刷出的 | ✅ 会（`NATURAL`） |
| 世界生成期成群 | ✅ 会（`CHUNK_GENERATION`） |
| **Alex's Caves 洞穴爆发**（区块生成时它自己跑一套循环，不走原版刷怪循环） | ✅ 会（`CHUNK_GENERATION`） |
| **Alex's Caves 巢穴地物与"世界生成的恐龙蛋"**（它们创建生物时**不调 `finalizeSpawn`**） | ✅ 会（世界生成的蛋 `needs_player = true` 与巢穴生成按自然生成处理；玩家自己放的蛋/繁殖出的蛋不受影响） |
| 结构生成（以及 Alex's Caves 琥珀独石） | ✅ 会（`STRUCTURE` / `CHUNK_GENERATION`） |
| Alex's Mobs 搁浅抹香鲸刷怪器 | **不会**（它传的是 `SPAWNER`） |
| 其它模组自己的刷怪逻辑 | 按它调用 `finalizeSpawn` 时传入的 `MobSpawnType` 判定 |

**倍率对上表所有来源都生效** —— 所以"改了倍率立刻看到变化、改概率却没反应"，绝大多数情况是因为测试用的是刷怪蛋或命令。

其它容易误判的点：

1. **概率 `> 1` 需要"原版先自然生成它一次"作为锚点**：它只在该群系本来就刷这种生物、并且真的自然刷出来时才补足。改完数值后站在原地看同一片老区块是看不到变化的（那里早就刷完了）。
2. **补出来的个体按原版规则另找位置**（光照 / 地面 / 碰撞 / 生物群系都要通过），被挡掉就少一个 ⇒ 实际增量可能少于 `p` 倍；硬上限是单次最多补 **100**、生成点周围额外存活最多 **100**。
3. **内置概率文件默认全部是 `1.0`**，改完记得保存；模组约 1 秒热重载，日志会打印 `Config reloaded`。
4. **概率 `0~1` 与倍率 `<1` 都会"变少"**：概率只管自然生成（刷怪蛋、命令仍可用），倍率管所有来源。想彻底禁掉用倍率 `0`；只想降低自然刷怪频率用概率。

**正确验证方法（3 步）**

1. 用**原版常见生物**做实验（僵尸、牛、猪最容易看到），写成 `3.0`；**不要用刷怪蛋**。
2. 去**从未生成过的区块**（跑远一点或用 `/tp`），在那里等 1~3 分钟让它自然刷怪。
3. 搜日志 `Spawn probability for`：
   - `queued 2 extra spread out attempts, probability 3.0` ⇒ 每次自然生成额外补 2 次（合计约 3 倍）✅
   - `voided every natural opportunity, probability 0.0` ⇒ 该生物的自然生成机会被作废 ✅
   同时在游戏里应能看到数量变化，而**其它生物保持原版刷新量**。

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

- **完全不影响任何其它生物**：概率只作废它**自己**的机会；`> 1` 时缺的次数由模组**自建生成通道**补出来。原版生成表从头到尾没有被改写，别的生物的权重/条目一个都不动；因为什么都没被替换，也不可能干扰原版自己的生成校验。
- **概率只作用于自然生成**：`NATURAL`、世界生成期成群（`CHUNK_GENERATION`）、结构生成（`STRUCTURE`）。刷怪蛋、`/summon` 等命令、**刷怪笼方块**、事件/转化召唤**不受概率影响**。
- **`> 1` 补出来的是真实个体**，因此该类别的生物总量确实会上升。这些个体**不计入原版刷怪上限**（按你选的路线乙），这正是"其它生物保持原版刷怪量"的原因 —— 代价是世界里能容纳的生物总量会超过原版允许值。
- **硬上限**：单次自然生成最多补 **100** 个；生成点周围额外存活的个体最多 **100** 个。
- **`0 ~ 1` 与倍率 `<1` 的关系**：两者都会让生物更少见。概率只作废自然生成机会（刷怪蛋、命令照常可用），倍率作用于**所有来源**。建议都保留：概率用于"自然刷怪频率"，倍率用于"一次几只"或"彻底禁掉"。
- **倍率复制出的额外个体不受概率过滤**；概率补出来的个体**会正常被倍率处理** —— 这正是两功能独立且可叠加的原因。
- **已经在世界里的生物不会因为改配置而消失**：验证请去**未生成过的区块**，或等旧生物被清除后再观察。
- `1.0` = 不干预；很小的非零值（`0.001`）表示"几乎不出现"而不是"完全不出现"；要彻底禁用请写 `0.0`。
- 配置是**全局的**（不随存档），单人/服务器都需要安装本模组。

### 6.2 其它限制

- 倍率 `> 1` 的“额外个体”是复制出来的新实体，不会继承原实体的装备、状态等生成上下文；对 Boss 设置大于 `1.0` 会同时出现多只 Boss，请谨慎。
- **概率只作用于"自然来源"**：原版刷怪循环（`NATURAL`）、世界生成期成群与 **Alex's Caves 洞穴爆发**（`CHUNK_GENERATION`）、结构生成与琥珀独石（`STRUCTURE` / `CHUNK_GENERATION`）。刷怪蛋、`/summon`、刷怪笼、以及用 `MOB_SUMMONED` / `EVENT` / `CONVERSION` 生成的（如甘草女巫召唤物、尖啸锤召唤物、姜饼人、闪电生成的巨型乌贼、Alex's Mobs 的搁浅抹香鲸刷怪器）**不受概率影响**；但**倍率对它们仍然生效**。
- **判据是"生成时传进来的类型"**：模组自己刷怪时传给 `finalizeSpawn` 的 `MobSpawnType` 决定它算不算自然生成 —— 这也是本模组能覆盖 Alex's Caves 洞穴爆发、Alex's Mobs 自带刷怪器等"绕过原版刷怪循环"的路径的原因。
- 本模组**不改动任何生成权重**，因此与其它模组的刷怪设置互不干扰：Alex's Caves 的 `cave_creature_spawn_count_modifier`、Alex's Mobs 自带的权重都照原样生效。
- 概率 `> 1` 是**补次数**而不是改权重，所以倍数是精确的：`10.0` 就是约十倍的出现次数（受硬上限与该区域可用刷怪点数量限制）。
- **性能**：概率 `0 ~ 1` 只在生物加入世界时做一次判断；`> 1` 的补足任务入队后每 tick 最多执行 8 个、单次最多 100 个，不会瞬间堆积。
- 本模组**不修改存档数据**，可以随时卸载；卸载后生成完全回到原版与各模组自己的行为。

---

## 7. 许可与反馈

- 许可证：**LGPL-3.0-only**（见 `LICENSE.txt`）。
- 作者：**Huziyang520**
- 反馈与问题：`https://issue.mengcai.online/`
