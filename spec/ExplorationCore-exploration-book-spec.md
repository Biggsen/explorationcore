# ExplorationCore `/exploration` Book

## Goal

Add a `/exploration` command to ExplorationCore that opens a virtual
Minecraft written book showing the player's exploration progress.

This is intended to move the existing exploration statistics away from
the scoreboard.

Keep the first implementation small. It is a read-only progress summary,
not a full exploration browser.

## Data Sources

The book combines two sources, and both of them belong to
ExplorationCore.

### Player progress

Read directly from the existing ExplorationCore SQLite database:

`plugins/ExplorationCore/exploration.db`

Use the player's UUID to calculate discovery counts.

The `discoveries` table is the source of what the player has
discovered. When a ledger count disagrees with AdvancedAchievements,
the TAB scoreboard, or an EXPMETRIC state message, the book shows the
ledger. ExplorationCore does not read those plugins while building the
book.

Do not introduce additional counters or duplicate player progress into
configuration.

### World totals

How many entities exist is not something the ledger can know.
Undiscovered sites have no rows. Those totals live in ExplorationCore
configuration, as its own copy of the catalogue figures. Opening the
book does not ask Region Forge, TAB, or AdvancedAchievements for them.

Example concept:

``` yaml
exploration:
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

The actual Lowothra values should be verified rather than assumed from
this example.

Later, MCSM may generate this configuration from the Region Forge
catalogues. That is outside the current implementation.

## Command

Add:

`/exploration`

Player-only command.

When executed, ExplorationCore should:

1.  Identify the player's UUID.
2.  Query their discoveries.
3.  Aggregate the required counts.
4.  Combine those counts with the configured totals.
5.  Generate a written book.
6.  Open the book directly for the player.

The book should be virtual. Do not add an item to the player's
inventory.

## Book Content

The initial book should contain approximately the same information
currently presented through the exploration scoreboards.

### Page 1 - Overworld

-   Server/world heading
-   Player name
-   Regions discovered / total
-   Villages discovered / total
-   Hearts discovered / total
-   Nerves discovered / total
-   Optional overall progress

### Page 2+ - Structures

Show the existing structure categories:

-   Ancient Cities
-   Buried Treasures
-   Desert Wells
-   Jungle Temples
-   Ocean Ruins
-   Pillager Outposts
-   Shipwrecks
-   Trail Ruins
-   Structures total if useful

Split this across pages if required for Minecraft book readability.

### Final Page - Nether

-   Nether regions discovered / total
-   Nether hearts discovered / total
-   Optional overall Nether progress

## Structure Families

`entity_type = structure` does not name the family. Two ocean ruins
are two rows, and the family they share is the catalogue's
`structureType`, such as `ocean_ruin`.

Store that value on the discovery row as `structure_type` when the
discovery is recorded. The generator already knows it. The record
command must start accepting it. This is the schema change that lets
the ledger be the source of family progress. A config map from
`entity_id` to family, or a guess from the id prefix or the display
name, would leave the grouping outside the database.

The book counts rows whose `structure_type` equals the config key
under `structures`. The config key supplies the label and the total.
It does not decide which rows belong to the family.

Regions, villages, hearts, and nerves leave `structure_type` empty.
Their grouping is `entity_type`.

Rows already stored have no family. Add the column as nullable and
leave those rows unchanged. They remain in the table, they are omitted
from family lines, and each omission is logged. The book still opens.
A later backfill can write `structure_type` by joining `entity_id`
to the catalogue. The book does not perform that join, and it does not
borrow the AdvancedAchievements family counter to fill the gap. Until
the backfill, a player who found structures before the column existed
will see a lower family count than the scoreboard. That is the ledger
telling the truth about what it knows.

## Counting Rules

Progress is the number of matching discovery rows.

Examples:

-   Overworld regions: count rows where `world=overworld` and
    `entity_type=region`
-   Villages: `world=overworld`, `entity_type=village`
-   Hearts: corresponding world + `entity_type=heart`
-   Nerves: corresponding world + `entity_type=nerve`
-   Nether regions: `world=nether`, `entity_type=region`
-   Ocean Ruins: `entity_type=structure` and
    `structure_type=ocean_ruin`

Count every matching row for that player UUID. Include rows stored
under an earlier `server` value. One database file belongs to one
server.

A player with no discoveries still gets a book, with zero against each
configured total.

A discovered count may be higher than the configured total. Show both
numbers as stored. Do not cap the discovered count at the total.

## Error Handling

`/exploration` should fail gracefully.

A missing or malformed exploration configuration, or a database read
failure, must not affect discovery recording or other ExplorationCore
functionality.

A structure row with an empty `structure_type`, or a family key that
has no entry in the book configuration, is logged and left off the
family pages. That does not stop the book from opening.

Log useful diagnostics to console and give the player a short
appropriate message if the book cannot be opened.

## Out of Scope

Do not add yet:

-   clickable navigation
-   individual region pages
-   individual discovery lists
-   discovery timestamps
-   search
-   pagination controls beyond normal Minecraft book pages
-   physical journal items
-   Region Forge parsing
-   MCSM integration
-   automatic config generation
-   HTTP/API functionality
-   scoreboard replacement/removal
-   reading AdvancedAchievements, PlaceholderAPI, or TAB
-   backfilling `structure_type` on rows that predate the column

## Recording `structure_type`

This amends the v1 record command. Commands already installed, with
six arguments, keep working and store an empty family.

``` text
explorationcore record <player> <world> <entity-type> <entity-id> <display-name> <difficulty> [structure-type]
```

The seventh argument is one token, the catalogue `structureType`.
The plugin trims it and stores it in lowercase. `ceGenerator.ts`
passes it for structure discoveries and omits it for every other type.
A seventh token on any other entity type is rejected. A structure
recorded with no `structure_type` is stored, and the missing type is
logged. The plugin does not reject that row.

The column is not part of the unique key. A later record of the same
entity must not rewrite a family already stored. Filling an empty
family on an existing row is the backfill, which is outside this book.

## Design Principle

**The database says what the player has discovered, including which
structure family a structure was when it was recorded.**

**The static configuration says what exists.**

**When another plugin disagrees, the book follows the database.**

`/exploration` combines those two ExplorationCore sources into a
readable Minecraft book. It does not reconcile them with the
scoreboard.

The implementation should remain small enough that the book UI can be
replaced or expanded later without changing the underlying discovery
model.
