# Changelog for Minecraft 26.1.2
All notable changes to this project will be documented in this file.

<a name="26.1.2-1.6.0"></a>
## [26.1.2-1.6.0](https://github.com/CyclopsMC/IntegratedCrafting/compare/26.1.2-1.5.0...26.1.2-1.6.0) - 2026-09-05 18:05:31


### Added
* Let the Attuned Crafting Interface enable and disable individual recipes (#221), Closes #162
* Additions for external mods:
  * Keep the initiator of crafting jobs that are distributed (#224), Required for CyclopsMC/IntegratedCrafting#175
  * Estimate a duration for recipes that are crafted instantly (#223), Required for CyclopsMC/IntegratedTerminals#145
  * Emit an event when a crafting job is completed (#222), Required for #175
  * Expose crafting job progress and measured recipe durations (#220), Required for CyclopsMC/IntegratedTerminals#145
* Performance improvements
  * Skip the crafting network lookup of an idle crafting interface
  * Resolve crafting job dependency edges without boxing
  * Classify the crafting job index by ingredient category
  * Stop aggregating network capacity on every recipe input evaluation
  * Look crafting table recipes up through the recipe cache

<a name="26.1.2-1.5.0"></a>
## [26.1.2-1.5.0](https://github.com/CyclopsMC/IntegratedCrafting/compare/26.1.2-1.4.8...26.1.2-1.5.0) - 2026-08-24 21:16:22 +0200


### Added
* Add part offset support to crafting interfaces (#215), Closes #138, Closes #152

### Fixed
* Fix memory leak in per-level FakePlayer cache

<a name="26.1.2-1.4.8"></a>
## [26.1.2-1.4.8](https://github.com/CyclopsMC/IntegratedCrafting/compare/26.1.2-1.4.7...26.1.2-1.4.8) - 2026-08-06 08:57:02 +0200


### Added
* Add translations through Crowdin (#209)

### Fixed
* Fix item dupe from replacing crafting interfaces with crafting buffers, Closes #212

<a name="26.1.2-1.4.7"></a>
## [26.1.2-1.4.7](https://github.com/CyclopsMC/IntegratedCrafting/compare/26.1.2-1.4.6...26.1.2-1.4.7) - 2026-04-29 19:25:44 +0200


### Fixed
* Fix MissingIngredients incorrectly serializing ingredients

<a name="26.1.2-1.4.6"></a>
## [26.1.2-1.4.6](https://github.com/CyclopsMC/IntegratedCrafting/compare/26.1.2-1.4.5...26.1.2-1.4.6) - 2026-04-26 13:56:53 +0200


### Added
* Add translations through Crowdin (#196)

### Fixed
* Fix channel mixup in crafting interface settings
* Fix settings text colors being transparant

<a name="26.1.2-1.4.5"></a>
## [26.1.2-1.4.5] - 2026-04-23 20:21:57 +0200


Initial 26.1.2 release
