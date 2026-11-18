# Advanced NUC Runs

Client-side Fabric mod for Minecraft **26.1.2**. It marks Nucleus run waypoints as red boxes that show through walls, times your runs, and shows your run time, PB and average on screen.

Needs Fabric Loader, Fabric API and Java 25.

## Commands

Upper and lower case do not matter. Tab completion works.

| Command | What it does |
| --- | --- |
| `/anc` or `/anc Menu` | Opens the menu: turn the mod, the tracker and the GUI on or off, choose where they show, open the tracker and the GUI mover, and see the session history |
| `/anc Jungle Temple` or `/anc JT` | Sets 3 waypoints, offset from the block you stand in |
| `/anc Waypoints` or `/anc WP` | Sets 1 waypoint on the block you stand in |
| `/anc Clear all` | Removes every waypoint |
| `/anc Clear waypoint <number>` or `/anc Clear <number>` | Removes one waypoint by its number |
| `/anc Timer start` | Starts the clock by hand, or restarts the current run |
| `/anc Timer reset` | Stops and clears the clock. The next finish starts it again |
| `/anc GUI` | Opens a screen where you move, resize and hide the on-screen boxes. `/anc Timer move` opens the same screen |
| `/anc Tracker` | Opens the profit tracker screen: every drop and the run count (editable) plus the tracker settings |
| `/anc Session reset` | Saves the session tracker to the history and starts it again from zero (same as its Reset button) |
| `/anc Rank <rank> <runs>` | Your place on the Nucleus runs leaderboard and your runs, as read from the leaderboard just now, for example `/anc Rank 91 3046` |
| `/anc Rank next <runs>` | The runs of the player one place above you, for example `/anc Rank next 3047` |
| `/anc Rank goal <place> <runs>` | The place you want to reach and the runs of the player who holds it, for example `/anc Rank goal 10 8800` |
| `/anc Rank clear` | Forgets the rank, the runs and the goal. `/anc Rank` alone shows them in chat |
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
- The Run Time / PB popup is centered on its spot, so the text stays lined up in the middle when it changes between `Run Time` and `PB` or the time gets longer or shorter.
- Positions, sizes, hidden boxes and the PB are saved in `config/advanced_nuc_runs.properties`.
- To try it without doing a run, use `/anc Run 2 min`. Test runs count as real runs for the PB and average, so use `/anc Stats reset` to clear them afterwards. A test run does not touch the Timer box.

### Crystal Nucleus Profit Tracker

A panel (box `Tracker` in `/anc GUI`) that counts every drop from the loot bundle in chat, including drops the mod has never seen before.

- Shows the best drops by value as `1X Name value` (3 by default, 3 to 20 in the tracker screen), then `Cost`, `Profit`, `Profit Per Run`, `Runs` and `Timer`.
- Item names are blue, Jaderald is purple and Jade Dye is green.
- Profit = value of all drops - runs x (Precursor Apparatus + Goblin Egg + Jungle Key).
- Prices come from the game: the Bazaar (sell offer by default, or instant sell) and the lowest BIN for everything else. They refresh every 5 minutes, and with `Refresh prices` in the tracker screen.
- The last row has two buttons you can click, also while the chat is open: `Total` / `Session` switches between everything ever tracked and this game session, and `Pause` / `Resume` stops the timer. The timer also pauses by itself after 5 minutes without a run.
- `/anc Tracker` opens the tracker screen. Scroll the list of drops. Click a number to type a new one, or use `+` and `-` (Shift = 10, Ctrl = 100). Settings: Pity, price mode, rows on screen, view, timer pause, refresh prices, reset session, reset everything.
- Saved in `config/advanced_nuc_runs_tracker.properties`.

- Prices that were found once are kept: if a list is down, or one Auction House scan misses an item (Divan's Alloy and Quick Claw are only priced from the Auction House), the last price stays on screen instead of turning into `?`. They are saved, so they are also there right after the game starts.

### Session tracker

A second panel (box `Session` in `/anc GUI`) that looks like the Crystal Nucleus Profit Tracker, but its last row says `Session` and has a `Reset` button next to `Pause`. It counts the same drops and runs, but only since you last reset it, and it keeps its numbers between game starts. Use it for "how much did I make today" or "in these 100 runs".

- Click `Reset` (also works while the chat is open) or use `/anc Session reset`. The numbers are saved to the history first, then the panel starts again from zero. An empty session is not saved.
- The history is in the menu, tab `History`: every reset with its date and runs. Click one to see `In these 225 runs you got:` with every drop, the cost, profit, profit per run, runs and timer, valued with the prices at the moment of the reset. `Delete this entry` removes one.
- It uses the same price mode and rows setting as the normal tracker.
- Saved in `config/advanced_nuc_runs_session.properties` and `config/advanced_nuc_runs_history.properties`.

### Last Alloy, Quick Claw and Jade Dye

When Divan's Alloy, a Quick Claw or a Jade Dye drops, the chat says how many runs ago the one before it dropped, for example `Last Alloy: 552 runs ago`. The first one the mod sees says `Alloy: first one tracked`. The mod has its own run counter for this, so editing the run count in the tracker screen does not change it.

### Leaderboard rank

A box (`Rank` in `/anc GUI`, or the `Rank` tab of the menu) that shows where you are on the Nucleus runs leaderboard, for example:

```
#91 Ninjachicken784
Runs: 3046
Next #90: 1 run left
Top 10: 5754 runs left
```

The mod cannot read a Discord bot such as SkyHelper, so you type in what the leaderboard says (see the commands above). After that the mod adds every run you finish by itself, so the box counts down on its own: `Top 10: 5754 runs left` becomes `5753` after the next run. Read the leaderboard again now and then and type it in again, to correct the numbers and your place. Runs are only counted while the tracker is on. The numbers are saved in `config/advanced_nuc_runs_tracker.properties`.

### Menu

`/anc` opens the menu. Tabs on the left, the settings of the open tab on the right.

| Tab | Settings |
| --- | --- |
| General | `Mod` on/off (off turns off the boxes, the tracker, waypoints and the timer; only `/anc` still works). `Show tracker and GUI`: `Always` or `Crystal Hollows only` (read from the `Area:` line of the tab list) |
| Tracker | `Tracker` on/off (off: nothing is counted and the panels and the RNG meter are hidden). Show or hide the Total panel and the Session panel. `Open Tracker` |
| GUI | `GUI` on/off (the Timer, Run Time, PB and Average boxes). `Change GUI` opens the screen where you move, resize and hide the boxes |
| Rank | Shows or hides the rank box, and lists the `/anc Rank` commands |
| History | The reset sessions, see above. `Reset session now` |

The switches are saved in `config/advanced_nuc_runs.properties`.

### RNG meter

A box (`Meter` in `/anc GUI`) showing the selected drop's icon and its progress, for example `320.3k/1m`.

- Open the in-game `Crystal Nucleus RNG Meter` menu once: the mod copies your stored XP and the selected drop from it. The icon is copied from the menu too, so open it once after each game start.
- After that every run adds 1,000 XP (1,100 with Pity on in the tracker screen), and the XP goes back to 0 when the selected drop drops.
- Nothing is shown until the menu has been opened once.

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
