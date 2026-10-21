# Release history

## 1.1.0

`/exploration` opens a virtual book of the player's discovery progress against `plugins/ExplorationCore/totals.yml`. The file is read when the command runs. Recording is unchanged.

## 1.0.1

29 September 2026. First release in this repository.

Same ledger as 1.0.0, plus an optional `structure_type` on structure discoveries, so a family's progress can be counted from the database. `/explorationcore record` accepts a seventh argument for that. The older six-argument command still works and leaves the field empty. A database written by 1.0.0 gains the column in place.

## 1.0.0

28 September 2026. Built before this repository existed.

Discovery ledger in `plugins/ExplorationCore/exploration.db`, with `/explorationcore status` and `/explorationcore record`. Record takes six arguments. Rows have no `structure_type`.
