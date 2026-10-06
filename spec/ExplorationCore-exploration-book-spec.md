# ExplorationCore `/exploration` Book

## Goal

Add a `/exploration` command to ExplorationCore that opens a virtual
Minecraft written book showing the player's exploration progress.

This is intended to move the existing exploration statistics away from
the scoreboard.

Keep the first implementation small. It is a read-only progress summary,
not a full exploration browser.

The work is three phases. Phase 1 produces the totals file. Phase 2
puts that file on the server. Phase 3 adds the command. Phase 1 can be
built on its own.

## Sources

The book combines two sources, and both of them belong to
ExplorationCore.

### What the player has discovered

Read directly from the existing ExplorationCore SQLite database:

`plugins/ExplorationCore/exploration.db`

Use the player's UUID to calculate discovery counts.

The `discoveries` table is the source of what the player has
discovered. When a ledger count disagrees with an achievement counter
or a scoreboard line, the book shows the ledger. ExplorationCore does
not read those plugins while building the book.

Structure rows already store `structure_type`. That column and the
record command that writes it are specified in
`ExplorationCore-v1-spec.md` section 30. This book does not change
recording.

Do not introduce additional counters or duplicate player progress into
configuration.

### What exists

How many entities exist is not something the ledger can know.
Undiscovered sites have no rows. Those totals live in a file
ExplorationCore owns:

`plugins/ExplorationCore/totals.yml`

The file is separate from `config.yml`. ExplorationCore does not ship
a server's real numbers inside the plugin jar. The export that writes
`totals.yml` is phase 1. Opening the book does not read another
plugin's data folder, a world region file, or an achievement counter.

## Phases

### Phase 1 — Export `totals.yml`

Produce `totals.yml` from the imported world meta the export already
holds. One file covers both worlds.

The export counts entries whose `discover.method` is `on_enter`.
Entries with any other discover method are absent from the totals.
Entries whose `kind` is `system` or `water` are absent. The plugin
never interprets `discover.method`. That rule belongs to the export.

Overworld totals:

- `regions.total` is the count of `kind: region`
- `villages.total` is the count of `kind: village`
- `hearts.total` is the count of `kind: heart`
- `nerves.total` is the count of `kind: nerve`

Overworld structures are grouped by `structureType`. Each group that
has at least one counted structure becomes one map entry. The map key
is the `structureType` string stored on discovery rows, such as
`ocean_ruin`, `shipwreck`, or `trail_ruins`. That stored form is one
token with no whitespace, trimmed and lowercased. The export writes
keys in that form. The book compares a key to a row with exact string
equality. `name` is that group's label, such as `Ocean Ruins`, and may
contain spaces and capitals. `total` is the count of counted
structures with that `structureType`.

Do not write the achievement counter name. It is not a field in this
file.

Nether totals:

- `regions.total` is the count of `kind: region`
- `hearts.total` is the count of `kind: heart`

The nether section has no villages, nerves, or structures.

Example shape. The numbers here illustrate the file. They are not the
figures to write. The export counts those figures.

``` yaml
overworld:
  regions:
    total: 544
  villages:
    total: 56
  hearts:
    total: 30
  nerves:
    total: 30
  structures:
    ancient_city:
      name: "Ancient Cities"
      total: 12
    buried_treasure:
      name: "Buried Treasures"
      total: 53
    desert_well:
      name: "Desert Wells"
      total: 1
    jungle_temple:
      name: "Jungle Temples"
      total: 11
    ocean_ruin:
      name: "Ocean Ruins"
      total: 169
    pillager_outpost:
      name: "Pillager Outposts"
      total: 8
    shipwreck:
      name: "Shipwrecks"
      total: 126
    trail_ruins:
      name: "Trail Ruins"
      total: 19

nether:
  regions:
    total: 18
  hearts:
    total: 8
```

A structure key is included only when its counted total is at least
one. The key set is whatever the imported meta contains, not a fixed
list in this spec. Every `total` is an integer of zero or more. Every
`name` is a non-blank string. A file that breaks those rules is
malformed.

### Phase 2 — Put the file on the server

Copy the exported `totals.yml` into the ExplorationCore data folder:

`plugins/ExplorationCore/totals.yml`

This phase is delivery. It does not change the plugin. `/exploration`
cannot open a book until this file is present and well formed.

### Phase 3 — `/exploration`

Add the command. This phase is last. It depends on phase 2.

`/exploration`

Player-only command, separate from `/explorationcore`.

Permission `explorationcore.exploration`, default true. A player can
run it without being an operator.

The console, and any non-player sender, receives a short refusal and
the book does not open.

When a player runs it, ExplorationCore should:

1.  Identify the player's UUID.
2.  Load `totals.yml` from the plugin data folder.
3.  Query that player's discoveries.
4.  Aggregate the required counts.
5.  Combine those counts with the totals.
6.  Generate a written book.
7.  Open the book directly for the player.

Load and validate `totals.yml` when the command runs. Do not validate
it during plugin enable. A missing or malformed file must not affect
discovery recording or prevent the plugin from enabling.

The book is virtual. Do not add an item to the player's inventory. Its
title is `Exploration`. Its author is the configured `server-name`.

Page 1 uses the online player's current name and the configured
`server-name`. It does not use a `player_name` stored on an older
discovery row.

#### Page 1 — Overworld

-   Server name and overworld heading
-   Player name
-   Regions discovered / total
-   Villages discovered / total
-   Hearts discovered / total
-   Nerves discovered / total

There is no overall progress line.

#### Following pages — Structures

One line per entry under `overworld.structures`, in the order the
file lists them.

-   The entry's `name`
-   Discovered / total

There is no structures total.

Split these lines across pages only when a single written-book page
cannot hold them.

#### Final page — Nether

-   Nether regions discovered / total
-   Nether hearts discovered / total

There is no overall Nether progress line.

#### Counting

Progress is the number of matching discovery rows for that player
UUID. Include rows stored under an earlier `server` value. One
database file belongs to one server.

-   Overworld regions: `world=overworld` and `entity_type=region`
-   Villages: `world=overworld` and `entity_type=village`
-   Hearts: `world=overworld` and `entity_type=heart`
-   Nerves: `world=overworld` and `entity_type=nerve`
-   A structure line: `world=overworld`, `entity_type=structure`, and
    `structure_type` equal to that entry's key
-   Nether regions: `world=nether` and `entity_type=region`
-   Nether hearts: `world=nether` and `entity_type=heart`

Every configured line is shown, including when the discovered count
is zero.

A discovered count may be higher than the configured total. Show both
numbers. Do not cap the discovered count at the total.

A structure row with an empty `structure_type` is logged and left out
of the structure lines. A `structure_type` that has no entry in
`totals.yml` is logged and left out. Neither case stops the book from
opening.

A player with no discoveries still gets a book, with zero against
each configured total.

#### When the book cannot open

`/exploration` fails on its own.

A missing totals file, a malformed totals file, or a database read
failure is logged. The player gets a short message. Discovery
recording and the rest of ExplorationCore keep working.

## Out of scope

Do not add yet:

-   clickable navigation
-   individual region pages
-   individual discovery lists
-   discovery timestamps
-   search
-   pagination controls beyond normal Minecraft book pages
-   physical journal items
-   reading another plugin's files
-   reading world region files
-   reading achievement counters or scoreboard placeholders
-   changes to discovery recording
-   scoreboard replacement or removal
-   HTTP or API functionality

## Design principle

**The database says what the player has discovered, including the
`structure_type` stored on a structure row.**

**`totals.yml` says what exists.**

**When another plugin disagrees, the book follows the database.**

`/exploration` combines those two ExplorationCore sources into a
readable Minecraft book. It does not reconcile them with the
scoreboard.

The implementation should remain small enough that the book UI can be
replaced or expanded later without changing the underlying discovery
model.
