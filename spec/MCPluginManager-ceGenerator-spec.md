# MC Plugin Manager --- ExplorationCore record commands

This specification is for `electron/ceGenerator.ts` in the MC Plugin
Manager.

ExplorationCore v1 is already installed and proven on Lowothra. The
command contract lives in `ExplorationCore-v1-spec.md`. This document
says where that command is generated, from which fields, and which
existing ConditionalEvents behaviour stays untouched.

ConditionalEvents detects. ExplorationCore remembers. The generator
connects them.

------------------------------------------------------------------------

## 1. What changes

`generateDiscoverOnceEvent`, `generateStructureDiscoverOnceEvent`, and
`generateFirstJoinEvent` each gain one `console_command` action:

``` text
console_command: explorationcore record %player% <world> <entity-type> <entity-id> <display-name> <difficulty> [structure-type]
```

`%player%` stays literal in the YAML. ConditionalEvents expands it when
the event runs. The plugin resolves that online player to a UUID.

`<display-name>` is UTF-8 base64url with the `=` padding removed, the
same encoding ExplorationCore already accepts. `Sakonotur` is
`U2Frb25vdHVy`. `Inner Core` is `SW5uZXIgQ29yZQ`.

Every other argument is one token. The generated string contains no
quoted display name.

The server name is not an argument. ExplorationCore reads it from its
own config.

------------------------------------------------------------------------

## 2. Where the action sits

The record command is the first entry of `actions.default`.

Discovery-once events already have their condition
(`%region% == <id>`) before any action runs. `first_join` has no
conditions; the join event itself is the discovery of the start region.

The command goes in front of every `wait`, achievement, crate, book
permission, message, title, teleport, and EXPMETRIC line. Existing
actions keep their current order after it.

Hearts and nerves currently start at `aach give`. Villages, regions,
and structures currently start at `wait: 3`. `first_join` currently
starts at the teleport. In all of those events the record command is
still index 0.

------------------------------------------------------------------------

## 3. Which events get a command

One record command per individual discovery:

| Generator function | Event key | Entity type |
| --- | --- | --- |
| `generateDiscoverOnceEvent` | `<id>_discover_once` | `region`, `village`, `heart`, or `nerve` |
| `generateStructureDiscoverOnceEvent` | `<id>_discover_once` | `structure` |
| `generateFirstJoinEvent` | `first_join` | `region` |

`first_join` records the start region only. That region is omitted from
the on-enter list today, so this is the only event that can persist it.

------------------------------------------------------------------------

## 4. Which events stay without one

Do not add a record command to:

-   `join_log`
-   `leave_log`
-   `region_heart_discover_once`
-   `region_nerve_discover_once`
-   enchantment call events
-   potion call events
-   `world_change`
-   `store_reminder_on_join`

`region_heart_discover_once` and `region_nerve_discover_once` are shared
lodestone tips. They are not one entity. The per-id
`<id>_discover_once` event is the discovery.

Do not emit a record command for:

-   `kind: water`
-   `kind: system`
-   `discover.method` other than `on_enter`, except the start region
    inside `first_join`
-   structures that are already skipped because they have no family
    counter
-   `world: end`

`world: end` exists on `RegionRecord`. ExplorationCore v1 accepts only
`overworld` and `nether`. Emitting `end` would make the plugin reject
the write. Do not rewrite `end` as `overworld`.

`kind: system` must not fall through the heart branch into a record
command. The current `else` branch treats anything that is not a
village, region, or nerve as a heart. The record command uses `kind`
directly and only emits the five supported types.

------------------------------------------------------------------------

## 5. Field mapping

Build the command from the region record and the same helpers the
discovery actions already use. Do not parse an EXPMETRIC string to
recover these fields.

### Entity id

`region.id`.

This is not the AdvancedAchievements command id, not
`discover.commandIdOverride`, and not the display name.

### World

`region.world`, when it is `overworld` or `nether`.

For `first_join`, use the start region's `world`. The generator already
prefers the overworld record when the same id exists in more than one
world.

### Entity type

`region.kind`, when it is `region`, `village`, `heart`, `nerve`, or
`structure`.

### Display name

Use the display string that event already computes, then encode it.

| Kind | Display string, before encoding |
| --- | --- |
| `region`, `village`, `structure`, and the `first_join` start region | `formatRegionLabel(region)` |
| `heart` | `formatRegionTitle` of the id with the `heart_of_` prefix removed |
| `nerve` | `formatRegionTitle` of the id with the `nerve_of_` prefix removed |

Hearts and nerves keep that parent-title rule. A display-name override
on the parent region does not change the heart or nerve label. That
matches the EXPMETRIC `region=` value those events already emit.

`formatRegionLabel` still honours `discover.displayNameOverride` for
regions, villages, structures, and the start region.

### Structure type

Structure discoveries append one more token: `region.structureType`,
such as `ocean_ruin`. That is the catalogue family, the same key the
TAB build groups on. It is not the AdvancedAchievements counter name,
and it is not the label.

Every other recorded kind omits the argument.

A structure that is recorded must have a non-empty `structureType`.
Fail that ConditionalEvents build when it does not. Structures that
are already skipped because they have no family counter still get no
record command.

### Difficulty

| Kind | Value |
| --- | --- |
| `region`, including the `first_join` start region | `difficultyToDiff(regionBands?.[region.id])` |
| `village`, `heart`, `nerve`, `structure` | `0` |

`difficultyToDiff` already maps `easy` through `deadly` to `1` through
`5`, and anything missing to `0`.

------------------------------------------------------------------------

## 6. What stays the same

Do not change:

-   event type, `one_time`, or conditions
-   achievement grants and counter increments
-   waits and their order
-   crate rewards, book permissions, titles, teleports, and player
    messages
-   EXPMETRIC `type=discovery`, `type=state`, `type=join`, and
    `type=leave` lines, including their text
-   fragment file names and which event keys land in which file
-   the parent region stored on a structure definition

Parent region is catalogue data. It is not an argument of the record
command.

A structure's family counter remains an achievement counter for the
scoreboard. The record command also stores `region.structureType` as
`structure_type`, so the ledger can group those rows without reading
the counter. The command still stores `region.id`, so two ocean ruins
stay two rows.

------------------------------------------------------------------------

## 7. Bad identity

`region.id` is documented as lowercase snake case.

Fail the ConditionalEvents build, and write no ConditionalEvents files
for that build, when a discovery that should be recorded has:

-   an empty entity id, or an id containing whitespace
-   a display name that is blank after the section 5 rules
-   a `first_join` whose `startRegionId` matches no region record

The error names the id. A broken record command must not be emitted,
and the rest of that event must not ship without one. Gameplay actions
with no ledger row are a split history.

`world: end`, `kind: system`, `kind: water`, and the events in section
4 are not failures. They are emitted exactly as they are today, with
no record command.

------------------------------------------------------------------------

## 8. Generated examples

Region, difficulty from bands, display name `Sakonotur`:

``` text
console_command: explorationcore record %player% overworld region sakonotur U2Frb25vdHVy 2
```

Heart. The stored display name is still `Sakonotur`. The id is not:

``` text
console_command: explorationcore record %player% overworld heart heart_of_sakonotur U2Frb25vdHVy 0
```

Structure whose label is `Inner Core` and whose `structureType` is
`ocean_ruin`:

``` text
console_command: explorationcore record %player% overworld structure inner_core SW5uZXIgQ29yZQ 0 ocean_ruin
```

Nether region. World is `nether` on the command. Do not copy the
overworld achievement placeholder from the EXPMETRIC state line:

``` text
console_command: explorationcore record %player% nether region <id> <encoded-label> <diff>
```

------------------------------------------------------------------------

## 9. Check the YAML before installing it

Generate Lowothra and read one event in each of:

-   `overworld-regions`
-   `overworld-villages`
-   `overworld-hearts`
-   `overworld-nerves`
-   `overworld-structures`
-   `nether-regions`
-   `nether-hearts`
-   `server-core` `first_join`

For each discovery event, the record command is the first action. The
following actions are the ones that file used to start with.

Confirm:

-   `%player%` is still the placeholder
-   world is the region's world
-   entity type is the region's kind
-   entity id is `region.id`
-   the decoded display name matches section 5
-   difficulty matches section 5
-   a structure command ends with `region.structureType`, and every
    other recorded kind omits that argument
-   `join_log`, `leave_log`, and the two lodestone tip events have no
    record command
-   an EXPMETRIC line in the same event is unchanged

Lowothra already has three console test rows for `verzion`: overworld
region `sakonotur`, heart `heart_of_sakonotur`, and structure
`inner_core`. Walking into those in-game must leave the row count
unchanged. Discover a different entity to prove the generated command
writes a new row.

------------------------------------------------------------------------

## 10. Design rule

The generator already has the discovery in its hands.

> Emit the record command from that discovery. Do not rebuild it from
> the log line.
