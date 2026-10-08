# ULTRAS_TAB_V1

A standalone, lightweight Paper plugin: **tab list, scoreboard, nametags and animations**, with
per-player settings, Arabic/English, per-world designs and a small settings GUI.
It does not depend on (or copy anything from) the TAB plugin. PlaceholderAPI and Vault are optional.

## Features

- Tab header/footer per language, per-world **profiles**, group-based **sorting** and `[Rank] Name` formatting
- Sidebar **scoreboard** per world and language; hide individual lines; **compact levels**
- **Nametags** `[Rank] Name` above heads; hide ranks/names for yourself
- **Animations** from one shared clock - no task per player, no per-player animation state
- Everything personal is per player and saved by UUID: language, TAB, scoreboard (+fields, compact),
  animations, sounds, messages, rank privacy
- `/tab setting` GUI, quiet sounds, fully translatable messages (`messages/en.yml`, `messages/ar.yml`)
- Live `/tab reload` of every file

## Requirements

Paper 1.21+ (API 1.21), Java 21+ on the server. PlaceholderAPI / Vault optional (soft-depend, reflection).

## Commands (`/tab`, aliases `/ultrastab`, `/utab`)

| Command | Effect (only for you) |
|---|---|
| `/tab` | Help |
| `/tab setting` | Settings GUI |
| `/tab tab` | Tab header/footer on/off |
| `/tab scoreboard` | Scoreboard on/off |
| `/tab scoreboard <field>` | Show/hide one line: `name rank money uc kills deaths playtime online ping server` (ids come from `scoreboard.yml`) |
| `/tab scoreboard all` | Hide all fields / show all fields |
| `/tab scoreboard compact` | Full -> Compact I -> II -> III -> Full |
| `/tab animations` | Animations on/off (off = first frame, static) |
| `/tab sound`, `/tab messages` | Sounds / chat feedback on/off |
| `/tab language [en\|ar]` | Switch language (no argument = toggle) |
| `/tab tab-rank` | Show/hide **your** rank in the tab list |
| `/tab nametag-rank` | Show/hide **your** rank above your head (for everyone) |
| `/tab nametag-ranks` | See/hide **other players'** ranks above their heads |
| `/tab nametag-name` | See/hide nametags of other players |
| `/tab reload` | Admin: reload all files |
| `/tab reset <player>` | Admin: reset a player's settings |

## Permissions

| Permission | Default | |
|---|---|---|
| `ultrastab.use` | true | Personal commands + GUI (the node is configurable, see `permissions.use`) |
| `ultrastab.admin` | op | All admin commands (includes `.reload`) |
| `ultrastab.admin.reload` | op | `/tab reload` |
| `ultrastab.group.<name>` | - | Puts a player in that group (see below) |

## Configuration (one file per system)

| File | Purpose |
|---|---|
| `config.yml` | General: default language, `update-interval`, sounds, integrations, permissions, debug |
| `tab.yml` | Groups (sorting + labels + colors), name format, TAB **profiles** |
| `scoreboard.yml` | Scoreboard **profiles**, lines, ids, priorities |
| `nametag.yml` | Nametag rank format and server-wide visibility |
| `animations.yml` | Animations |
| `messages/*.yml` | Every message |
| `gui/*.yml` | Settings menus (layout, items, text) |

### TAB profiles

`tab.yml -> profiles`. `default` covers `world`, `world_nether`, `world_the_end`. For another world,
copy the `default` block, rename it, change `worlds`:

```yaml
profiles:
  event:
    worlds: [event_world]
    # header / footer / player-list ...
```

Worlds no profile lists use `unlisted-world-profile` from `config.yml` (empty = nothing shown).
Scoreboards work identically in `scoreboard.yml`.

### Groups and sorting

`tab.yml -> groups`: **order = sort order**. A player belongs to the first group (top to bottom) for which
they have `ultrastab.group.<name>` (e.g. `ultrastab.group.owner`); `default` is the fallback.
Note: operators have unset permissions treated as true by Bukkit, so an op lands in the top group unless you
negate it in your permissions plugin.

### Scoreboard lines

```yaml
- ""                                   # spacer
- { id: money, priority: 1, en: "...", ar: "..." }
```
`id` makes it hideable; `priority` (1 = essential .. 4) decides what each compact level keeps
(Full = all, I = <=3, II = <=2, III = only 1; spacers are dropped in compact modes).
Max 15 lines. Titles/lines support MiniMessage, `&` codes, `&#RRGGBB`, `#RRGGBB`, gradients.

### Animations

`animations.yml`: `interval` in ms, `frames` as a shared list or `en`/`ar` lists. Use `%animation:name%` in any text.
Shipped: `tab_meniu`, `tab_text1`, `tab_text2`, `tab_progres`, `UltraS`.

### Placeholders

Built in (no other plugin needed): `%player% %online% %max% %ping% %world% %rank% %group% %money% %uc% %kills% %deaths% %playtime%`.
`%money%` needs Vault + an economy; `%uc%` reads `%ultrasuc_balance%` via PlaceholderAPI. Any PlaceholderAPI
placeholder works when it is installed. Anything unresolved is replaced by `placeholders.unresolved` (default `-`); nothing throws.

## Performance design

- **One** repeating task (every 2 ticks) for the whole plugin; no task per player.
- Animation frames come from the clock; the task only learns *which animations changed*.
- A player is re-rendered only if: a used animation changed, their placeholders are due (`update-interval`),
  or something about them changed (world, language, setting). Static text is never re-rendered.
- Packets are sent only when the rendered text differs from what the player already has;
  scoreboard lines are updated individually; nametags use a handful of shared teams per viewer.
- Settings are cached; files are written asynchronously (and synchronously on quit/disable).

## Known limitations (by design, to stay stable without NMS)

- **No true scoreboard resize.** Paper has no safe API for it, so this plugin ships *Compact* levels
  (100/80/60/40 % of the information) instead of faking a size.
- **Nametag is one line**: `[Rank] Name`. A rank on its own line above the name needs extra entities; not done.
- **`/tab nametag-name` off hides the whole nametag** (name and rank), because Minecraft hides them together.
- **Tab-list names are shared by all viewers** (Paper has no per-viewer API for them), so they use the server
  default language, and an admin cannot see a rank a player chose to hide in TAB. Header, footer, scoreboard,
  nametags and GUI are all per-viewer.
- `/tab tab` off clears the header/footer for that player; the player list itself cannot be hidden.
- Each player gets a private scoreboard, so this plugin conflicts with other plugins that set scoreboards.
- Folia is not supported.

## Installation

1. Put `ULTRAS_TAB_V1-v1.jar` in `plugins/`, start the server once.
2. Edit the files in `plugins/ULTRAS_TAB_V1/`, then `/tab reload`.
3. Give staff `ultrastab.group.<name>` permissions.

## Build

Requires a Java 25 toolchain and **Gradle 9.1+** (Gradle 8.x cannot run on Java 25).

```bash
gradle clean build        # -> build/libs/ULTRAS_TAB_V1-v1.jar
```

`gradle.properties`:
- `paperApiVersion` - Paper API to compile against (bump for newer Paper).
- `javaRelease` - bytecode level of the jar (default 21 so it loads on Paper 1.21 servers running Java 21;
  set 25 only if all your servers run Java 25). The compiler itself always runs on Java 25.

GitHub Actions (`.github/workflows/build.yml`) builds with Java 25 + Gradle 9.1.0, checks the jar contains the
required resources and uploads `ULTRAS_TAB_V1-v1.jar`.

## Design (same style as Ultras Claims)
- Chat: `ULTRAS│ ✓ ᴍᴇssᴀɢᴇ` - prefix in normal capitals with a red gradient; symbols `✓` success, `×` error, `!` warning, `•` info; body in small caps (English).
- Menus: vanilla items only (no resource pack), small-caps names with a state symbol (`✓` green / `×` red) and a glint when on; no glass panes under buttons (`gui.filler: true` in `config.yml` fills empty slots with black glass).
- Updating an existing server: delete `plugins/ULTRAS_TAB_V1/gui/` and `messages/` once so the new files are copied.
