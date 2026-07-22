# Match 5

Luck-based tabletop for Paper **26.1.2**. Click blocks to reveal IconsAdder icons. Find **5** of your sidebar icon within **10** chances.

Data: `plugins/GenesiCore/games/Match5/`

## Rules

- 2 players, each assigned a random icon from the pool
- Board hides 5 of each icon + blanks
- Click a **block** to reveal the flat icon on top
- Your icon → +1 found (keep turn); wrong/blank → turn ends
- Each click costs 1 chance (default 10)
- First to find 5 wins; run out of chances and you’re done

## Admin setup

Stand facing the table. Origin = **near-left** corner from your view.

```
/m5admin create main
/m5admin setlobby main
/m5admin setorigin main      # look at near-LEFT corner block
/m5admin buildboard main     # flat ItemDisplay previews (not upright signs)
/m5admin setjoin main a
/m5admin setjoin main b
```

## Icons (ItemsAdder test pool)

Configured under `icons:` — currently 5 `genesicore:asset…` items. Swap IDs anytime.

## Build

```bash
JAVA_HOME=/opt/homebrew/opt/openjdk@25/libexec/openjdk.jdk/Contents/Home ./gradlew build
```
