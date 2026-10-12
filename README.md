# Advanced NUC Runs

Client-side Fabric mod for Minecraft **26.1.2**. It marks Nucleus run waypoints as red boxes that show through walls, times your runs, and shows your run time, PB and average on screen.

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
| `/anc GUI` | Opens a screen where you move, resize and hide the on-screen boxes. `/anc Timer move` opens the same screen |
| `/anc Run <time>` | Test command: acts as if you just finished a run of that length. Example: `/anc Run 2 min` or `/anc Run 1 min 30 sec` |
| `/anc Stats reset` | Clears your saved PB and this session's average |

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

### On-screen boxes

Times on screen are shown as minutes and seconds, like `1min 22sec`.

| Box | What it shows |
| --- | --- |
| Timer | The current run counting up. `Timer --` while the clock is not running |
| Run Time | Pops up for 5 seconds after each run with how long it took. If the run is a new PB it says `PB` instead of `Run Time` |
| PB | Your fastest run. `PB --` until you have one |
| Average | The average of all runs finished this session. `Average --` until you finish one |

- A run is a new PB if it is your first run, or strictly faster than your PB. Equal does not count.
- The PB is saved when you close the game. The average starts again every session.
- `/anc GUI` opens the move screen: drag a box with the left mouse button, scroll the mouse wheel over a box to make it bigger (scroll up) or smaller (scroll down), right-click a box to hide or show it, and press Esc to save. Each box has its own size. Hidden boxes are grey with a red frame on that screen. The Run Time box shows a sample on that screen so you can place it.
- The text is bold with a black outline and a soft shadow.
- Positions, sizes, hidden boxes and the PB are saved in `config/advanced_nuc_runs.properties`.
- To try it without doing a run, use `/anc Run 2 min`. Test runs count as real runs for the PB and average, so use `/anc Stats reset` to clear them afterwards. A test run does not touch the Timer box.

### Notes

- Coordinates are rounded down to the block you are in (100.7 counts as 100).
- Each waypoint shows its number and distance. Numbers are the lowest free one, and clearing one waypoint does not renumber the rest.
- Running `/anc JT` again replaces the earlier Jungle Temple waypoints. It does not touch the timer. Waypoints made with `/anc WP` stay.
- Waypoints are not saved. They are gone when you close the game.

## Building

```
./gradlew build
```

The mod jar is in `build/libs/`. GitHub Actions builds it on every push (see the Actions tab).
