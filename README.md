# Advanced NUC Runs

Client-side Fabric mod for Minecraft **26.1.2**. It marks Nucleus run waypoints as red boxes that show through walls, and times your runs.

Needs Fabric Loader, Fabric API and Java 25.

## Commands

Upper and lower case do not matter. Tab completion works.

| Command | What it does |
| --- | --- |
| `/anc Jungle Temple` or `/anc JT` | Sets 3 waypoints, offset from the block you stand in |
| `/anc Waypoints` or `/anc WP` | Sets 1 waypoint on the block you stand in |
| `/anc Clear all` | Removes every waypoint |
| `/anc Clear waypoint <number>` or `/anc Clear <number>` | Removes one waypoint by its number |
| `/anc Timer start` | Starts the clock by hand, or restarts the current run |
| `/anc Timer reset` | Stops and clears the clock. The next finish starts it again |
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

- Runs are timed back to back: each run is measured from the last finish to the next finish.
- A finish is the **CRYSTAL NUCLEUS LOOT BUNDLE** message in chat. The mod prints `Run finished in 3:42.18` and starts timing the next run at that same moment.
- The very first finish has nothing before it to measure from, so it only starts the clock (`Timer started...`). You can also start the clock by hand with `/anc Timer start`.
- The timer on screen shows the current run counting up: grey `Run --:--` while the clock is not running, yellow while it is. The finished time of each run is printed in chat.
- Move it with `/anc Timer move`: drag the box, then press Esc. The position is saved in `config/advanced_nuc_runs.properties`.

### Notes

- Coordinates are rounded down to the block you are in (100.7 counts as 100).
- Each waypoint shows its number and distance. Numbers are the lowest free one, and clearing one waypoint does not renumber the rest.
- Running `/anc JT` again replaces the earlier Jungle Temple waypoints. It does not touch the timer. Waypoints made with `/anc WP` stay.
- Waypoints and run times are not saved. They are gone when you close the game. Only the timer position is saved.

## Building

```
./gradlew build
```

The mod jar is in `build/libs/`. GitHub Actions builds it on every push (see the Actions tab).
