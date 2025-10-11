# Changelog for Minecraft 1.21.1
All notable changes to this project will be documented in this file.

<a name="1.21.1-1.3.1"></a>
## [1.21.1-1.3.1](https://github.com/CyclopsMC/IntegratedCrafting/compare/1.21.1-1.3.0...1.21.1-1.3.1) - 2025-10-11 13:24:42


### Fixed
* Fix missing infobook index tag for attuned crafting interface

<a name="1.21.1-1.3.0"></a>
## [1.21.1-1.3.0](https://github.com/CyclopsMC/IntegratedCrafting/compare/1.21.1-1.2.3...1.21.1-1.3.0) - 2025-10-07 17:32:46 +0200


### Added
* Add Attuned Crafting Interface: Handles crafting for all recipes exposed by the target machine.

### Fixed
* Fix recursive recipes failing complete plan calculation

<a name="1.21.1-1.2.3"></a>
## [1.21.1-1.2.3](https://github.com/CyclopsMC/IntegratedCrafting/compare/1.21.1-1.2.2...1.21.1-1.2.3) - 2025-07-29 17:19:54 +0200


### Added
* Add translations through Crowdin (#148)
* Add PT_BR localization (#147)

### Fixed
* Fix autocraft crash when recipe does not fit in 2x2, Closes #149
* Fix some spelling and grammar typos in lang (#145)

<a name="1.21.1-1.2.2"></a>
## [1.21.1-1.2.2](https://github.com/CyclopsMC/IntegratedCrafting/compare/1.21.1-1.2.1...1.21.1-1.2.2) - 2025-05-31 21:36:01 +0200


### Fixed
* Fix aspect icons in Network Reader not loading

<a name="1.21.1-1.2.1"></a>
## [1.21.1-1.2.1](https://github.com/CyclopsMC/IntegratedCrafting/compare/1.21.1-1.2.0...1.21.1-1.2.1) - 2025-05-25 07:02:10 +0200


### Fixed
* Fix cursor centering on gui switching, Closes CyclopsMC/IntegratedDynamics#1514

<a name="1.21.1-1.2.0"></a>
## [1.21.1-1.2.0](https://github.com/CyclopsMC/IntegratedCrafting/compare/1.21.1-1.1.16...1.21.1-1.2.0) - 2025-05-10 09:01:12 +0200


### Added
* Add smithing table and stonecutter support, Closes #118

<a name="1.21.1-1.1.16"></a>
## [1.21.1-1.1.16](https://github.com/CyclopsMC/IntegratedCrafting/compare/1.21.1-1.1.15...1.21.1-1.1.16) - 2025-02-15 10:20:55 +0100


### Fixed
* Fix broken advancement icons

<a name="1.21.1-1.1.15"></a>
## [1.21.1-1.1.15](https://github.com/CyclopsMC/IntegratedCrafting/compare/1.21.1-1.1.14...1.21.1-1.1.15) - 2025-02-10 16:29:37 +0100


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

<a name="1.21.1-1.1.14"></a>
## [1.21.1-1.1.14](https://github.com/CyclopsMC/IntegratedCrafting/compare/1.21.1-1.1.13...1.21.1-1.1.14) - 2025-02-08 16:19:03 +0100


### Added
* Add tr_tr translations

### Fixed
* Fix rare crash when finalizing crafting jobs, Closes #133

<a name="1.21.1-1.1.13"></a>
## [1.21.1-1.1.13](https://github.com/CyclopsMC/IntegratedCrafting/compare/1.21.1-1.1.12...1.21.1-1.1.13) - 2024-11-22 07:13:55 +0100


### Fixed
* Fix unable to clear part IDs, Closes CyclopsMC/IntegratedTunnels#309

<a name="1.21.1-1.1.12"></a>
## [1.21.1-1.1.12](https://github.com/CyclopsMC/IntegratedCrafting/compare/1.21.1-1.1.11...1.21.1-1.1.12) - 2024-11-10 13:52:25 +0100


### Fixed
* Fix crash when recipe does not fit in 3x3 nor 2x2 grid, Closes #117

<a name="1.21.1-1.1.11"></a>
## [1.21.1-1.1.11](https://github.com/CyclopsMC/IntegratedCrafting/compare/1.21.1-1.1.10...1.21.1-1.1.11) - 2024-10-26 15:52:42 +0200


### Fixed
* Allow multi-output recipes to be reused across jobs
  Even if not all outputs of such recipes are used in one sub-job,
  they can still be used by other sub-jobs.
  Previously, such cases would trigger multiple invocations of these
  recipes, while fewer would be sufficient.
  Closes CyclopsMC/IntegratedTerminals#131

<a name="1.21.1-1.1.10"></a>
## [1.21.1-1.1.10](https://github.com/CyclopsMC/IntegratedCrafting/compare/1.21.1-1.1.9...1.21.1-1.1.10) - 2024-08-21 17:39:36 +0200


### Fixed
* Fix dynamic recipes in crafting interfaces broken after reload
  Related to CyclopsMC/IntegratedCrafting#110
* Refer to NeoForge's updateJSONURL instead of Forge's

<a name="1.21.1-1.1.9"></a>
## [1.21.1-1.1.9] - 2024-08-09 21:09:32 +0200


### Changed
* Update to updated CommonCapabilities API
  Required for CyclopsMC/IntegratedDynamics#1375
