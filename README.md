# Advanced NUC Runs

Client-side Fabric mod for Minecraft **26.1.2**. It marks Nucleus run waypoints as red boxes that show through walls.

Needs Fabric Loader, Fabric API and Java 25.

## Commands

Upper and lower case do not matter. Tab completion works.

| Command | What it does |
| --- | --- |
| `/anc Jungle Temple` or `/anc JT` | Sets 3 waypoints, offset from the block you stand in |
| `/anc Waypoints` or `/anc WP` | Sets 1 waypoint on the block you stand in |
| `/anc Clear all` | Removes every waypoint |
| `/anc Clear waypoint <number>` or `/anc Clear <number>` | Removes one waypoint by its number |

### Jungle Temple offsets

From the block you are standing in (X, Y, Z):

| Waypoint | X | Y | Z |
| --- | --- | --- | --- |
| 1 | +4 | +10 | +65 |
| 2 | +29 | -32 | +65 |
| 3 | +29 | -32 | +48 |

Standing at `100 0 200` gives `104 10 265`, `129 -32 265` and `129 -32 248`.

### Notes

- Coordinates are rounded down to the block you are in (100.7 counts as 100).
- Each waypoint shows its number and distance. Numbers are the lowest free one, and clearing one waypoint does not renumber the rest.
- Running `/anc JT` again replaces the earlier Jungle Temple waypoints. Waypoints made with `/anc WP` stay.
- Waypoints are not saved. They are gone when you close the game.

## Building

```
./gradlew build
```

The mod jar is in `build/libs/`. GitHub Actions builds it on every push (see the Actions tab).
