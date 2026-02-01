# Changelog for Minecraft 1.20.1
All notable changes to this project will be documented in this file.

<a name="1.20.1-1.4.3"></a>
## [1.20.1-1.4.3](/compare/1.20.1-1.4.2...1.20.1-1.4.3) - 2026-02-01 14:22:50


### Fixed
* Fix crash due to incorrect Lists import, Closes #176

<a name="1.20.1-1.4.2"></a>
## [1.20.1-1.4.2](/compare/1.20.1-1.4.1...1.20.1-1.4.2) - 2026-01-17 14:14:28 +0100


### Fixed
* Fix formatting of some exception messages (#173)

<a name="1.20.1-1.4.1"></a>
## [1.20.1-1.4.1](/compare/1.20.1-1.4.0...1.20.1-1.4.1) - 2026-01-02 10:55:58 +0100


### Fixed
* Fix crafting storage not dropping when breaking crafting interfaces
* Fix crafting job completion when using importers, Closes #170

<a name="1.20.1-1.4.0"></a>
## [1.20.1-1.4.0](/compare/1.20.1-1.3.3...1.20.1-1.4.0) - 2025-12-31 15:03:05 +0100


### Changed
* Add dedicated storage per crafting job

When a crafting job is started, ingredients are immediately moved from
general storage to the new storage buffers per crafting job. This avoids
issues where ingredients can be consumed elsewhere (e.g. exporters or
other crafting jobs) before it is used by the crafting job.

This also improves overall performance, as it is not necessary to run synchronous observers anymore.

Closes #112

<a name="1.20.1-1.3.3"></a>
## [1.20.1-1.3.3](/compare/1.20.1-1.3.2...1.20.1-1.3.3) - 2025-11-21 19:51:37 +0100


### Changed
* Use classified ingredient maps to optimize recipe index, Closes #160

<a name="1.20.1-1.3.2"></a>
## [1.20.1-1.3.2](/compare/1.20.1-1.3.1...1.20.1-1.3.2) - 2025-11-11 15:34:25 +0100


### Fixed
* Fix crash when crafting job is null, Closes #161

<a name="1.20.1-1.3.1"></a>
## [1.20.1-1.3.1](/compare/1.20.1-1.3.0...1.20.1-1.3.1) - 2025-10-17 15:12:49 +0200


### Changed
* Avoid unnecessary recipe re-indexing for attuned crafting interfaces
  Related to CyclopsMC/IntegratedCrafting#156

### Fixed
* Fix grammar in infobook (#154)

<a name="1.20.1-1.3.0"></a>
## [1.20.1-1.3.0](/compare/1.20.1-1.2.3...1.20.1-1.3.0) - 2025-10-07 17:32:11 +0200


### Added
* Add Attuned Crafting Interface: Handles crafting for all recipes exposed by the target machine.

### Fixed
* Fix recursive recipes failing complete plan calculation

<a name="1.20.1-1.2.3"></a>
## [1.20.1-1.2.3](/compare/1.20.1-1.2.2...1.20.1-1.2.3) - 2025-07-29 17:13:30 +0200


### Fixed
* Fix autocraft crash when recipe does not fit in 2x2, Closes #149

<a name="1.20.1-1.2.2"></a>
## [1.20.1-1.2.2](/compare/1.20.1-1.2.1...1.20.1-1.2.2) - 2025-05-31 21:44:00 +0200


### Fixed
* Fix invalid imports

<a name="1.20.1-1.2.1"></a>
## [1.20.1-1.2.1](/compare/1.20.1-1.2.0...1.20.1-1.2.1) - 2025-05-31 21:12:25 +0200


### Fixed
* Fix aspect icons in Network Reader not loading

<a name="1.20.1-1.2.0"></a>
## [1.20.1-1.2.0](/compare/1.20.1-1.1.11...1.20.1-1.2.0) - 2025-05-10 08:55:00 +0200


### Added
* Add smithing table and stonecutter support, Closes #118

<a name="1.20.1-1.1.11"></a>
## [1.20.1-1.1.11](/compare/1.20.1-1.1.10...1.20.1-1.1.11) - 2025-02-10 16:30:07 +0100


### Fixed
* Fix over-estimation of storage contents when calculating jobs

This could occur when an ingredient could be partially extracted from
storage and partially had to be autocrafted via a sub-job.
The simulated extraction memory was being set to an amount that was too
low, which caused the algorithm to incorrectly think there was more to
extract.
The flaw in reasoning before this commit was that the simulation
extraction memory would only increment. But this is false, since it will
decrement when sub-jobs are calculated, and this decrement was not taken
into account.

Closes #125

<a name="1.20.1-1.1.10"></a>
## [1.20.1-1.1.10](/compare/1.20.1-1.1.9...1.20.1-1.1.10) - 2025-02-08 16:17:48 +0100


### Fixed
* Fix rare crash when finalizing crafting jobs, Closes #133

<a name="1.20.1-1.1.9"></a>
## [1.20.1-1.1.9](/compare/1.20.1-1.1.8...1.20.1-1.1.9) - 2024-10-26 15:51:11 +0200


### Fixed
* Allow multi-output recipes to be reused across jobs
  Even if not all outputs of such recipes are used in one sub-job,
  they can still be used by other sub-jobs.
  Previously, such cases would trigger multiple invocations of these
  recipes, while fewer would be sufficient.
  Closes CyclopsMC/IntegratedTerminals#131

<a name="1.20.1-1.1.8"></a>
## [1.20.1-1.1.8](/compare/1.20.1-1.1.7...1.20.1-1.1.8) - 2024-08-21 17:37:48 +0200


### Fixed
* Fix dynamic recipes in crafting interfaces broken after reload
  Related to CyclopsMC/IntegratedCrafting#110

<a name="1.20.1-1.1.7"></a>
## [1.20.1-1.1.7](/compare/1.20.1-1.1.6...1.20.1-1.1.7) - 2023-12-27 17:18:03 +0100


### Fixed
* Fix 2x2 Integrated Crafting recipes from Machine Reader failing, Closes CyclopsMC/IntegratedDynamics#1316

<a name="1.20.1-1.1.6"></a>
## [1.20.1-1.1.6](/compare/1.20.1-1.1.5...1.20.1-1.1.6) - 2023-10-10 17:01:14 +0200


### Fixed
* Fix restarting jobs when resetting network, Closes #99

<a name="1.20.1-1.1.5"></a>
## [1.20.1-1.1.5](/compare/1.20.1-1.1.4...1.20.1-1.1.5) - 2023-09-12 19:56:16 +0200


### Fixed
* Fix jobs not finishing if output is extracted outside network, Closes #98

<a name="1.20.1-1.1.4"></a>
## [1.20.1-1.1.4](/compare/1.20.1-1.1.3...1.20.1-1.1.4) - 2023-07-31 15:03:42 +0200


### Changed
* Clarify manual recipe creation for Auto-Planking advancement
  Closes CyclopsMC/IntegratedDynamics#1290

<a name="1.20.1-1.1.3"></a>
## [1.20.1-1.1.3] - 2023-07-02 08:11:36 +0200


Initial 1.20.1 release
