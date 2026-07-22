# Match 5

Luck-based tabletop for Paper **26.1.2**. **Floor signs** show ItemsAdder `minigame1` glyphs. Find **5** of your icon in **10** chances.

## Icons

| Char | Name |
|------|------|
| ꀆ | Dark hole (hidden) |
| ꀈ | Yellow |
| ꀉ | Red |
| ꀊ | Green |

## Setup

```
/m5admin setorigin main       # near-LEFT corner of the colored grid
/m5admin buildboard main      # places oak signs ON TOP of each block (removes old paper)
/match5 join main             # BOTH players must join
```

Optional: `/m5admin setlobby` · `/m5admin setjoin a|b`

## Start

Once 2 players have joined, the lobby countdown starts automatically, or:
```
/match5 start
/m5admin forcestart main
```
