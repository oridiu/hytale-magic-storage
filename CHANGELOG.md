# Changelog

## [1.1.0](https://github.com/oridiu/hytale-magic-storage/compare/v1.0.2...v1.1.0) (2026-10-26)


### Features

* add the system that hands the linked chest to crafting benches ([ba5e048](https://github.com/oridiu/hytale-magic-storage/commit/ba5e0483b6fc0b0b8dbdba560da4c3d39f683a6e))
* let crafting benches use items from the linked Magic Chest ([1fcfe57](https://github.com/oridiu/hytale-magic-storage/commit/1fcfe57ace743c53505d74a74ae8d84d5f7eafd7))


### Bug Fixes

* size a large chest by used slots when joining a full chest ([6dc4083](https://github.com/oridiu/hytale-magic-storage/commit/6dc4083d39e61c9e8d7d7c2cbfd172c8582ef3b8))

## [1.0.2](https://github.com/oridiu/hytale-magic-storage/compare/v1.0.1...v1.0.2) (2026-10-25)


### Bug Fixes

* show item names in the game's language for German clients ([eae45ae](https://github.com/oridiu/hytale-magic-storage/commit/eae45ae9a681b13b4389c9ea8d840dad46fdf259))

## [1.0.1](https://github.com/oridiu/hytale-magic-storage/compare/v1.0.0...v1.0.1) (2026-10-25)


### Bug Fixes

* create chest storage on place and link so empty chests open via bag ([bb27640](https://github.com/oridiu/hytale-magic-storage/commit/bb27640e2b72b9cb5da81291fa695a8e1c18205a))

## 1.0.0 (2026-10-25)


### ⚠ BREAKING CHANGES

* **storage:** AutoSaveSeconds is no longer read from config.json and the "autosave" setting is gone from /magicstorage set and the settings window.

### Features

* add Magic Chest and Magic Bag to reach a linked chest from anywhere ([f87c61a](https://github.com/oridiu/hytale-magic-storage/commit/f87c61a1c27e3b1eeb2affd7368f503732008540))
* give Magic Chest and Magic Bag their own models, textures and icons ([f87c61a](https://github.com/oridiu/hytale-magic-storage/commit/f87c61a1c27e3b1eeb2affd7368f503732008540))
* **glow:** mark the linked chest with rising sparks and a glowing gem ([73fac30](https://github.com/oridiu/hytale-magic-storage/commit/73fac305148924da9ccc728ba2ff94004da49a45))


### Bug Fixes

* clear a chest's viewers when it is broken ([1b92307](https://github.com/oridiu/hytale-magic-storage/commit/1b9230789709e6792cd28fa1716778925d7677d5))
* do not replace an unreadable chest file with an empty chest ([568e8dd](https://github.com/oridiu/hytale-magic-storage/commit/568e8dd3ed3848401477affc14786ac6dddc9f17))
* do not write a chest save after the chest is deleted ([0459b8d](https://github.com/oridiu/hytale-magic-storage/commit/0459b8d15781e01ff01eea79ddc5b6dec4e4e5ba))
* give Arena 1 and Arena_1 different chest files ([54eddf6](https://github.com/oridiu/hytale-magic-storage/commit/54eddf646970b7b5fd7066936ab1099d93f487a3))
* keep saving after a chest fails to encode ([545a3d5](https://github.com/oridiu/hytale-magic-storage/commit/545a3d51128e73835dea37635934c91fdf66444a))
* put a death-stashed Magic Bag back with EVERYTHING ([ca07bea](https://github.com/oridiu/hytale-magic-storage/commit/ca07bea273bb6ae9ab28d7d9c1884d0ca2c960de))
* treat a bag link with no world as not linked ([a522fc2](https://github.com/oridiu/hytale-magic-storage/commit/a522fc2b18ef8453f5aca2c774a637d164846f15))


### Code Refactoring

* **storage:** save chests on window close and world save ([07aabf9](https://github.com/oridiu/hytale-magic-storage/commit/07aabf9a1de080aeca3369c9679a59544aa904b5))
