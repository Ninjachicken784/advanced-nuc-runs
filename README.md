# Advanced NUC Runs

Client-side Fabric mod for Minecraft **26.1.2**. It marks Nucleus run waypoints as red boxes that show through walls, and times your runs.

Needs Fabric Loader, Fabric API and Java 25.

## Commands

Upper and lower case do not matter. Tab completion works.

| Command | What it does |
| --- | --- |
| `/anc Jungle Temple` or `/anc JT` | Sets 3 waypoints, offset from the block you stand in, and starts the run timer |
| `/anc Waypoints` or `/anc WP` | Sets 1 waypoint on the block you stand in |
| `/anc Clear all` | Removes every waypoint |
| `/anc Clear waypoint <number>` or `/anc Clear <number>` | Removes one waypoint by its number |
| `/anc Timer start` | Starts (or restarts) the run timer by hand |
| `/anc Timer reset` | Clears the run timer |
| `/anc Timer move` | Opens a screen where you drag the timer to a new spot |

### Jungle Temple offsets

From the block you are standing in (X, Y, Z):

| Waypoint | X | Y | Z |
| --- | --- | --- | --- |
| 1 | +4 | +10 | +65 |
| 2 | +29 | -32 | +65 |
| 3 | +29 | -32 | +48 |

Standing at `100 0 200` gives `104 10 265`, `129 -32 265` and `129 -32 248`.

### Run timer

- `/anc JT` starts a fresh timer.
- The timer stops when the **CRYSTAL NUCLEUS LOOT BUNDLE** message shows up in chat. The mod then prints `Run finished in 3:42.18` in chat.
- The timer is shown on screen: grey `Run --:--` before a run, yellow while running, green with the final time after a run. It stays on screen until the next run starts.
- If the loot bundle arrives while no run was being timed, the mod says so instead of showing a time.
- Move it with `/anc Timer move`: drag the box, then press Esc. The position is saved in `config/advanced_nuc_runs.properties`.

### Notes

- Coordinates are rounded down to the block you are in (100.7 counts as 100).
- Each waypoint shows its number and distance. Numbers are the lowest free one, and clearing one waypoint does not renumber the rest.
- Running `/anc JT` again replaces the earlier Jungle Temple waypoints and restarts the timer. Waypoints made with `/anc WP` stay.
- Waypoints and run times are not saved. They are gone when you close the game. Only the timer position is saved.

## Building

```
./gradlew build
```

The mod jar is in `build/libs/`. GitHub Actions builds it on every push (see the Actions tab).
