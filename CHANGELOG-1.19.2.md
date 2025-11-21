# Changelog for Minecraft 1.19.2
All notable changes to this project will be documented in this file.

<a name="1.19.2-1.1.14"></a>
## [1.19.2-1.1.14](/compare/1.19.2-1.1.13...1.19.2-1.1.14) - 2025-11-21 19:50:26


### Changed
* Use classified ingredient maps to optimize recipe index, Closes #160

<a name="1.19.2-1.1.13"></a>
## [1.19.2-1.1.13](/compare/1.19.2-1.1.12...1.19.2-1.1.13) - 2025-11-12 05:58:04 +0100


### Fixed
* Fix crash when crafting job is null, Closes #161

<a name="1.19.2-1.1.12"></a>
## [1.19.2-1.1.12](/compare/1.19.2-1.1.11...1.19.2-1.1.12) - 2025-07-29 17:12:13 +0200


### Fixed
* Fix autocraft crash when recipe does not fit in 2x2, Closes #149

<a name="1.19.2-1.1.11"></a>
## [1.19.2-1.1.11](/compare/1.19.2-1.1.10...1.19.2-1.1.11) - 2025-05-31 21:10:30 +0200


### Fixed
* Fix aspect icons in Network Reader not loading

<a name="1.19.2-1.1.10"></a>
## [1.19.2-1.1.10](/compare/1.19.2-1.1.9...1.19.2-1.1.10) - 2025-02-10 16:29:48 +0100


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

<a name="1.19.2-1.1.9"></a>
## [1.19.2-1.1.9](/compare/1.19.2-1.1.8...1.19.2-1.1.9) - 2025-02-08 16:17:07 +0100


### Fixed
* Fix rare crash when finalizing crafting jobs, Closes #133

<a name="1.19.2-1.1.8"></a>
## [1.19.2-1.1.8](/compare/1.19.2-1.1.7...1.19.2-1.1.8) - 2024-10-26 15:48:57 +0200


### Fixed
* Allow multi-output recipes to be reused across jobs
  Even if not all outputs of such recipes are used in one sub-job,
  they can still be used by other sub-jobs.
  Previously, such cases would trigger multiple invocations of these
  recipes, while fewer would be sufficient.
  Closes CyclopsMC/IntegratedTerminals#131

<a name="1.19.2-1.1.7"></a>
## [1.19.2-1.1.7](/compare/1.19.2-1.1.6...1.19.2-1.1.7) - 2024-08-21 17:34:05 +0200


### Fixed
* Fix dynamic recipes in crafting interfaces broken after reload
  Related to CyclopsMC/IntegratedCrafting#110

<a name="1.19.2-1.1.6"></a>
## [1.19.2-1.1.6](/compare/1.19.2-1.1.5...1.19.2-1.1.6) - 2023-12-27 17:13:37 +0100


### Fixed
* Fix 2x2 Integrated Crafting recipes from Machine Reader failing, Closes CyclopsMC/IntegratedDynamics#1316

<a name="1.19.2-1.1.5"></a>
## [1.19.2-1.1.5](/compare/1.19.2-1.1.4...1.19.2-1.1.5) - 2023-10-10 16:59:26 +0200


### Fixed
* Fix restarting jobs when resetting network, Closes #99

<a name="1.19.2-1.1.4"></a>
## [1.19.2-1.1.4](/compare/1.19.2-1.1.3...1.19.2-1.1.4) - 2023-09-12 19:51:50 +0200


### Fixed
* Fix jobs not finishing if output is extracted outside network, Closes #98

<a name="1.19.2-1.1.3"></a>
## [1.19.2-1.1.3](/compare/1.19.2-1.1.2...1.19.2-1.1.3) - 2023-07-31 15:02:36 +0200


### Changed
* Clarify manual recipe creation for Auto-Planking advancement
  Closes CyclopsMC/IntegratedDynamics#1290

<a name="1.19.2-1.1.2"></a>
## [1.19.2-1.1.2](/compare/1.19.2-1.1.1...1.19.2-1.1.2) - 2023-05-13 10:23:52 +0200


### Fixed
* Fix rare crash with reusable ingredients, Closes #94

<a name="1.19.2-1.1.1"></a>
## [1.19.2-1.1.1](/compare/1.19.2-1.1.0...1.19.2-1.1.1) - 2023-03-19 06:52:59 +0100


### Fixed
* Fix crash on job finish with non-completed dependencies
  Closes #92
  Closes CyclopsMC/IntegratedDynamics#1249

<a name="1.19.2-1.1.0"></a>
## [1.19.2-1.1.0](/compare/1.19.2-1.0.26...1.19.2-1.1.0) - 2023-02-11 13:56:23 +0100


### Added
* Allow reusable recipe ingredients, Closes #36
  If the player marks a recipe ingredient as reusable, this ingredient
  will not be crafted multiple times if the recipe is requested in bulk.
  This is useful for recipes that make use of items that only consume
  durability if they are used in a recipe.
* Add option to crafting interfaces to disable blocking mode
  When this option is enabled, the crafting interface will try to push as
  much parallel crafting jobs into the target as possible for bulk
  crafting jobs.
  This is mainly useful for machines that can process multiple inputs in
  parallel.
  Closes #90

### Changed
* Allow jobs to partially start if dependencies are not fully finished
  This fixes issues with multi-amount jobs with many dependencies,
  where the root job would not be able to start unless the dependencies would
  be fully finished.
  For example, recipes that involve chains of multiple machines
  will result in more efficient pipelining.
  Closes #6
* Only craft missing number of items in Crafting Writer
  This will now properly take into account the stacksize of items as desired amount.
  Closes #77

<a name="1.19.2-1.0.26"></a>
## [1.19.2-1.0.26](/compare/1.19.2-1.0.25...1.19.2-1.0.26) - 2022-09-17 12:22:09 +0200


### Fixed
* Fix craft planks advancement triggering too early
  Related to CyclopsMC/IntegratedTunnels#258

<a name="1.19.2-1.0.25"></a>
## [1.19.2-1.0.25](/compare/1.19.2-1.0.24...1.19.2-1.0.25) - 2022-09-17 10:28:05 +0200


### Added

### Changed

### Fixed
* Fix ignore storage contents overriding ignore crafting jobs, Closes #87

* Bump mod version

* Fix jobs after initial one getting stuck, Closes #83

This bug was introduced in febd15d7343b71c654d61b2574a126039ba2304b
due to synchronous item insertion into the network.
The fix involves registering observers earlier so that
crafting job completion can always be tracked.

<a name="1.19.2-1.0.24"></a>
## [1.19.2-1.0.24] - 2022-08-11 19:48:23 +0200


Update to MC 1.19.2
