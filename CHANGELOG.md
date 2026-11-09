# Release history

## 1.5.0

9 November 2026. Stable release.

Placing short grass near a Heart can turn that grass into a flower. Mining redstone ore near a Nerve can drop coal instead of redstone. Placing an unlit candle near a Soul can light that candle, up to 32 blocks above the Soul. The chance is highest at the Heart, Nerve, or Soul and falls off with distance. A candle that is already lit stays as it is. The sites come from `detection-sites.yml`. A missing or bad file leaves the grass, the ore, and the candle alone. No title, chat line, sound, or particle is added, and nothing is recorded.

An overworld soul is recorded as entity type `soul`. `%explorationcore_souls_discovered%` counts those rows, and they also count toward `%explorationcore_total_discovered%`. A nether soul does not. A newly recorded soul uses the Soul Discovered title, the beacon sound, and the end-rod burst. It grants the Soul Crate, unless the command ends with `nocrate`. Difficulty `0` is accepted.

`%explorationcore_regions_discovered%` counts overworld land regions and overworld water regions. Discovering a water region can cross a regions milestone. `%explorationcore_water_regions_discovered%` still counts water only. Nether water still counts toward neither.

`%explorationcore_total_available%` is the number of places in the world-explored percentage. That is overworld land regions, water regions, villages, hearts, nerves, and souls, plus nether regions and nether hearts. Structures stay out. The number comes from `totals.yml` and does not depend on the player. A missing or bad totals file leaves this placeholder empty.

`/exploration` lists Souls, then Hearts, then Nerves. Counted lines no longer use a colon. The count stays at the right of the line, and the label is still the page link. The book reads `overworld.souls.total` when that entry is present.

`/exploration book` gives the player an Exploration Journal. Console, remote console, and a command block use `/exploration book <player>` for an online player. The journal is a knowledge book. Right-clicking it in the air opens the same exploration book, built again from that player's discoveries. Right-clicking a block still uses that block. Giving the journal again gives another copy.

Recording, crates, and the other counters behave as they did in 1.4.0, apart from souls and the regions counter.

## 1.5.0-dev.5

`/exploration book` gives the player an Exploration Journal. Console, remote console, and a command block use `/exploration book <player>` for an online player. The journal is a knowledge book. Right-clicking it in the air opens the same exploration book as `/exploration`, built again from that player's discoveries. Right-clicking a block still uses that block. Giving the journal again gives another copy. `/exploration` and `/exploration preview` are unchanged.

## 1.5.0-dev.4

`%explorationcore_regions_discovered%` counts overworld land regions and overworld water regions. Discovering a water region can cross a regions milestone. `%explorationcore_water_regions_discovered%` still counts water only. Nether water still counts toward neither.

A candle can light up to 32 blocks above a Soul. The chance is still highest at the Soul and falls off with distance. Below the Soul, the zone reaches downward as it did in 1.5.0-dev.2. A candle higher than those 32 blocks stays unlit.

## 1.5.0-dev.3

The exploration book lists Souls, then Hearts, then Nerves. The journal pages follow that order. Counted lines no longer use a colon. The count stays at the right of the line, and the label is still the page link.

## 1.5.0-dev.2

An overworld soul is recorded as entity type `soul`. `%explorationcore_souls_discovered%` counts those rows, and they also count toward `%explorationcore_total_discovered%`. A nether soul does not. A newly recorded soul uses the Soul Discovered title, the beacon sound, and the end-rod burst. It grants the Soul Crate, unless the command ends with `nocrate`. Difficulty `0` is accepted.

`/exploration` lists Souls. The book reads `overworld.souls.total` when that entry is present. A totals file without it still loads, and the soul total is zero. Souls count toward the world-explored percentage.

Placing an unlit candle near a Soul can light that candle. Any candle can light, including the coloured ones and the plain candle. The chance is highest at the Soul and falls off with distance. The Soul's height is the top of the zone, and the zone reaches downward. A candle above the Soul stays unlit. A candle that is already lit stays as it is. No title, chat line, sound, or particle is added, and nothing is recorded. Heart grass and Nerve coal behave as they did in 1.5.0-dev.1.

`%explorationcore_total_available%` is the number of places in the world-explored percentage. That is overworld land regions, water regions, villages, hearts, nerves, and souls, plus nether regions and nether hearts. Structures stay out. The number comes from `totals.yml` and does not depend on the player. A missing or bad totals file leaves this placeholder empty. The other placeholders still answer.

## 1.5.0-dev.1

Placing short grass near a Heart can turn that grass into a flower. Mining redstone ore near a Nerve can drop coal instead of redstone. The chance is highest at the Heart or Nerve and falls off with distance, including height. A Heart reaches farther up and down than a Nerve.

The same place can succeed or fail on the next try. A Heart or Nerve that is already discovered still does this. No title, chat line, sound, or particle is added.

The sites come from `detection-sites.yml`. A missing or bad file leaves the grass and the ore alone. Recording is unchanged.

## 1.4.0

6 November 2026. Stable release.

An overworld water region is recorded as entity type `water`. `%explorationcore_water_regions_discovered%` counts those rows, and they also count toward `%explorationcore_total_discovered%`. `regions_discovered` stays land regions only. A repeat of the same discovery does nothing.

A newly recorded water region uses the Region Discovered title, the beacon sound, and the end-rod burst. It grants the Region Crate, unless the command ends with `nocrate`. Existing discovery rows, milestone grants, and ranks are left as they are.

`/exploration` lists Land Regions and Water Regions separately. The book needs `overworld.water_regions.total` in `totals.yml`.

Recording, crates, milestones, and the other counters behave as they did in 1.3.0.

## 1.4.0-dev.1

An overworld water region is recorded as entity type `water`. `%explorationcore_water_regions_discovered%` counts those rows, and they also count toward `%explorationcore_total_discovered%`. `regions_discovered` stays land regions only. A repeat of the same discovery does nothing.

A newly recorded water region uses the Region Discovered title, the beacon sound, and the end-rod burst. It grants the Region Crate, unless the command ends with `nocrate`. Existing discovery rows, milestone grants, and ranks are left as they are.

`/exploration` lists Land Regions and Water Regions separately. The book needs `overworld.water_regions.total` in `totals.yml`.

## 1.3.0

3 November 2026. Stable release.

`%explorationcore_total_discovered%` and the other discovery counters are answered from `exploration.db`. The database keeps a versioned schema. Existing discovery rows are left as they are.

A newly recorded structure, region, village, heart, or nerve shows a title, subtitle, chat line, the beacon power-select sound, and an end-rod burst. A heart says you discovered the Heart of that region, and a nerve says you discovered the Nerve of that region. Nether regions and Nether hearts use their own titles. A repeat of the same discovery does nothing. Player-facing exploration chat starts with the mark in `messages.prefix`.

With `rewards.enabled` on, a newly recorded structure grants the configured experience and claim blocks. A region, village, heart, or nerve can grant a CrazyCrates crate. A record command can end with `nocrate` to skip that one crate. An overworld region can grant a LuckPerms book permission for the stored name.

A new discovery can cross a milestone in `milestones.yml`. The claim is stored before payment, and only while the player is still online. Experience, items, enchanted books, potions, claim blocks, a promotion command, and a coloured broadcast are the rewards this release pays. A player who has already passed a threshold is not paid. A bad milestones file disables milestone rewards and leaves recording alone. Startup logs how many milestones loaded.

`/exploration` opens a written book of places already discovered. Structure finds are grouped by the families in `totals.yml`, and those family counters come from `structure-counters.yml`.

Location placeholders from 1.2.0 are unchanged.

## 1.3.0-dev.9.3

A newly recorded discovery plays the beacon power-select sound at volume 1.0 and pitch 2.0. Fifteen end-rod particles appear 1.5 blocks above the feet, spread 0.4 blocks sideways and 0.3 blocks vertically, and drift upward. A repeat of the same discovery does nothing.

## 1.3.0-dev.9.2

A newly recorded discovery plays the amethyst block chime at volume 1.0 and pitch 1.0. Fifteen end-rod particles appear 1.5 blocks above the feet, spread 0.4 blocks sideways and 0.3 blocks vertically, and drift upward. A repeat of the same discovery does nothing.

## 1.3.0-dev.9.1

A newly recorded discovery plays an amethyst block chime at volume 0.8 and pitch 0.8, and a short end-rod shimmer around the chest. A repeat of the same discovery does nothing.

Startup logs `Milestones loaded:` and the count of milestones that loaded.

## 1.3.0-dev.9

Structure family counters come from `plugins/ExplorationCore/structure-counters.yml`. The eight main counters stay in the plugin. A missing file is filled from the jar. A bad file is named in the log and loads no families, and recording continues. A milestones file that names an unknown counter still turns milestone rewards off.

`/exploration` uses `short` from `totals.yml` when a structure has one. That word is the summary line and the list title. Without it, the book uses `name`.

## 1.3.0-dev.8

`/exploration` opens a written book of places already discovered. The summary still shows the counts. A category with finds can be opened, and each list can return to the summary. Empty categories stay as a count. Structure finds are grouped by the families in `totals.yml`. Names are alphabetical. The book stops at 100 pages.

## 1.3.0-dev.7.2

Player-facing exploration chat notices start with a gold compass. The mark is `messages.prefix`. Leave that value blank to turn it off. Titles, subtitles, command replies, and milestone broadcasts stay as they were.

## 1.3.0-dev.7.1

A record command can end with `nocrate`. The discovery is still written. Only the crate grant for that discovery is skipped, including its chat line. Book permissions, the discovery title, structure experience, claim blocks, and milestones are unchanged. A repeat of the same discovery does nothing. `discovery-crates.enabled` still turns crate grants off for every discovery.

## 1.3.0-dev.7

A newly recorded region, village, heart, or nerve can grant a CrazyCrates crate. With `discovery-crates.enabled` on, the configured command runs while the player is still online. The shipped command is `cc give virtual {crate} 1 {player}`. The world and the discovery kind pick the crate. A failed dispatch is logged. The player is not told. A repeat of the same discovery does nothing.

A newly recorded overworld region can grant a LuckPerms book permission. With `region-books.enabled` on, the configured command runs for the name stored on the discovery, including after the player has disconnected. Nether regions, and every other discovery kind, are left alone. A failed dispatch is logged for manual recovery. The player is not told. A repeat of the same region does nothing.

## 1.3.0-dev.6.1

Milestone item rewards now use a readable name. `netherite_scrap` is announced as Netherite Scrap, one item is the name alone, and a larger stack is marked with × and the amount. A structured item entry can override that name in the chat line. The item given to the player is unchanged.

## 1.3.0-dev.6

A new discovery can now cross a milestone in `milestones.yml`. The milestone title waits until the discovery title has finished, then shows the configured message and only the rewards that were actually delivered. Experience, items, claim blocks, a promotion command, and a coloured broadcast are the rewards this build pays. The claim is stored in `milestone_grants` before payment, and only while the player is still online. A player who has already passed a threshold is not paid. A bad milestones file disables milestone rewards and leaves recording alone. `/exploration` is unchanged.

## 1.3.0-dev.5.1

A heart says you discovered the Heart of that region, and a nerve says you discovered the Nerve of that region. The chat line matches. Other discovery wording is unchanged from 1.3.0-dev.5.

## 1.3.0-dev.5

A newly recorded region, village, heart, or nerve shows a title, subtitle, chat line, challenge sound, and particle burst. Nether regions and Nether hearts use their own titles. A repeat of the same discovery does nothing. Structure wording, experience, and claimblocks stay as they were in 1.3.0-dev.4. Turning `rewards.enabled` off does not hide the new presentation.

## 1.3.0-dev.4

A newly recorded structure still shows the discovery presentation. With `rewards.enabled` on, it grants the configured experience points and dispatches `claimblocks-command` for the configured claimblock amount. The shipped command is `acb {player} {amount}`. A console acceptance is announced as claimblocks received. A repeat of the same structure does nothing. `/exploration`, the counters, and older discovery rows are unchanged.

## 1.3.0-dev.3

A newly recorded structure shows a title, subtitle, chat line, challenge sound, and a short particle burst. With `rewards.enabled` on, that discovery also grants the configured experience points. Rewards stay off unless the config says otherwise. Claim blocks are listed and not granted. A repeat of the same structure does nothing. `/exploration`, the counters, and older discovery rows are unchanged.

## 1.3.0-dev.2

The database now keeps a versioned schema. Existing discovery rows are left as they are, including databases from 1.0.0 that are missing `structure_type`. Two empty tables, `reward_intents` and `milestone_grants`, are created for later reward and milestone work. Recording, `/exploration`, and the counters behave as before.

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
