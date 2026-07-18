# Match 5

Luck-based tabletop minigame for Paper **26.1.2**. Find **5** of your assigned mob under the tiles — avoid your opponent's mob. First to 5 wins.

Uses **GenesiGamesApi** (`GenesiGamePlugin`) so configs live at:

```text
plugins/GenesiCore/games/Match5/
```

## Features

- 2 players, click seat blocks or `/match5 join`
- RNG board (default 6×6, 8 of each mob + blanks)
- ItemDisplay tiles (map item for hidden — no MapView renderer cost)
- Sidebar scoreboard shows your mob + scores
- Optional Vault fee + arcade points (`points.yml`)

## Build

```bash
JAVA_HOME=/opt/homebrew/opt/openjdk@25/libexec/openjdk.jdk/Contents/Home ./gradlew build
```

Jar: `build/libs/Match5-1.0.0.jar`

Requires **JDK 25+** and a sibling `../GenesiCore` checkout (composite build).

## Install (Club)

1. Ensure `GenesiGamesApi.jar` is in `plugins/`
2. Drop `Match5-1.0.0.jar` into `plugins/`
3. Restart Paper 26.1.2
4. Data auto-creates under `plugins/GenesiCore/games/Match5/`

## Admin setup

Build a flat tabletop of clickable blocks (e.g. 6×6 stone), plus two seat blocks.

```
/m5admin create main
/m5admin setlobby main
/m5admin setorigin main          # look at the near-left corner block
/m5admin setjoin main a          # look at seat A join block
/m5admin setjoin main b          # look at seat B join block
```

Optional: `/m5admin setsize main 6 6` · `/m5admin setfacing main NORTH`

## Player commands

```
/match5 join <arena>
/match5 leave
/match5 start
/match5 points
/match5 arenas
```

Aliases: `/m5`, `/match` — or click a seat join block.

## How to play

1. Two players join (command or seats).
2. Each gets a random mob shown on the sidebar.
3. On your turn, click a hidden tile.
4. Your mob → +1 score (and keep turn by default). Opponent's mob / blank → turn ends.
5. First to **5** wins.
