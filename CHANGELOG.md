# Release history

## 1.3.0-dev.2

The database now keeps a versioned schema. Existing discovery rows are left as they are, including databases from 1.0.0 that are missing `structure_type`. Two empty tables, `reward_intents` and `milestone_grants`, are created for later reward and milestone work. Recording, `/exploration`, and the counters behave as before.

Development builds use `1.3.0-dev.N`, then `1.3.0-rc.N`, then `1.3.0`. `1.2.0` remains the last stable release.

## 1.3.0-dev.1

`%explorationcore_total_discovered%` and the other discovery counters are answered from `exploration.db`. A player with no matching rows gets `0`. A database failure leaves the placeholder empty. Counts can trail a new discovery by a few seconds. Location placeholders and `/exploration` are unchanged.

## 1.2.0

`%exploration_current_region%`, `%exploration_current_village%`, and `%exploration_current_structure%` are answered by ExplorationCore when PlaceholderAPI and WorldGuard are present. The location index is `plugins/ExplorationCore/location/index.yml`.

## 1.1.0

`/exploration` opens a virtual book of the player's discovery progress against `plugins/ExplorationCore/totals.yml`. The file is read when the command runs. Recording is unchanged.

## 1.0.1

29 September 2026. First release in this repository.

Same ledger as 1.0.0, plus an optional `structure_type` on structure discoveries, so a family's progress can be counted from the database. `/explorationcore record` accepts a seventh argument for that. The older six-argument command still works and leaves the field empty. A database written by 1.0.0 gains the column in place.

## 1.0.0

28 September 2026. Built before this repository existed.

Discovery ledger in `plugins/ExplorationCore/exploration.db`, with `/explorationcore status` and `/explorationcore record`. Record takes six arguments. Rows have no `structure_type`.
