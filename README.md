# Match 5

Luck-based tabletop for Paper **26.1.2**. Click blocks to reveal **minigame1** font icons. Find **5** of your icon within **10** chances.

Data: `plugins/GenesiCore/games/Match5/`

## Icons (ItemsAdder font `minigame1`)

| Char | Name |
|------|------|
| ꀆ | Dark hole (hidden / blank) |
| ꀈ | Yellow |
| ꀉ | Red / orange |
| ꀊ | Green |

Font file: `assets/minecraft/font/minigame1.json` + `genesicore:font/minigame1.png`

## Rules

- 2 players each get a random colored icon (yellow / red / green)
- Click a block → reveal flat TextDisplay glyph
- Your icon → +1 (keep turn); wrong/blank → turn ends
- 10 chances each; first to 5 wins

## Admin setup

```
/m5admin setorigin main      # near-LEFT corner while facing the table
/m5admin buildboard main     # places flat ꀆ dark holes
/m5admin setjoin main a
/m5admin setjoin main b
```
