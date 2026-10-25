<p align="center">
  <img src="docs/images/banner.png" alt="Magic Storage: Linked Chest &amp; Bag" width="100%">
</p>

# Magic Storage: Linked Chest & Bag

Tired of running back to base from the Forgotten Temple? Magic Storage is a Hytale server plugin for Chapter 1 (Update 7) that adds two items:

- **Magic Chest:** a chest whose contents can be reached from anywhere.
- **Magic Bag:** link it to a Magic Chest and open that chest from anywhere, even in other worlds and dungeon instances.

## Features

- **Crafting:** both items are crafted at the Goblin Tinkerer's Workbench in the Forgotten Temple, under **Gadgets**. The workbench must be powered.

  | Item | Ingredients |
  |---|---|
  | Magic Chest | 40 Scraps, 8 Void Crystal Shards, 2 Dubious Assembly, 1 Small Crude Chest |
  | Magic Bag | 20 Scraps, 4 Void Crystal Shards, 2 Spider Silk, 1 Voidheart |

- **Linking:** hold the bag and use a Magic Chest (the key you open chests with). Any bag can be linked to any Magic Chest, including chests placed by other players, and any number of bags can share one chest.
- **Open from anywhere:** right-click with the bag in your hand to open the linked chest, even from another world or a dungeon instance.
- **One bag per player:** a second bag is dropped again, and you cannot craft another one while you carry a bag.
- **Death:** the bag stays in your inventory.
- **Large chest:** place two Magic Chests side by side, facing the same way, and they join into a Large Magic Chest with twice the slots, like vanilla chests. Their contents are combined, and bags linked to either half open the whole chest.
- **Find your chest:** while you carry a linked bag, blue sparks rise from the linked chest and the gem on its front glows. Only you see your own chest's glow.
- **Breaking the chest:** its contents drop, and linked bags report that the chest no longer exists. Breaking either half of a large chest drops both Magic Chests.
- **Languages:** English and German.

## Installation

Magic Storage is a single server plugin with its assets bundled in the `.jar`. Players do not need to install anything.

1. Download `MagicStorage-<version>.jar`.
2. Put it into the `mods` folder of your server.
3. Restart the server.

Requires Hytale server 0.7.4 or a later 0.7.x build. Server and plugin must target the same Hytale version.

## Configuration

On first start the server creates `mods/oridiu_MagicStorage/config.json`:

| Setting | Default | Description |
|---|---|---|
| `AllowDepositViaBag` | `true` | Set to `false` to only allow taking items out through the bag, not putting them in |
| `ChestCapacity` | `18` | Slots of a Magic Chest, same as a vanilla chest (9 to 90). A large chest has twice as many. Raising it grows existing chests the next time they are opened; chests never shrink |
| `ShowLinkedChest` | `true` | Show sparks and a glowing gem on the chest a player's bag is linked to |

Chest contents are stored in `mods/oridiu_MagicStorage/chests/`, one file per chest. A chest is saved when its window closes, whenever the world saves, and when the server stops. `links.json` next to it records which positions belong to a large chest.

### In-game commands

Operators (or anyone with the plugin's command permission) can change the settings in game. Changes take effect immediately and are written to `config.json`.

| Command | Description |
|---|---|
| `/magicstorage settings` | Opens a settings window |
| `/magicstorage show` | Lists the current settings |
| `/magicstorage set <setting> <value>` | Changes one setting, e.g. `/magicstorage set deposit false` or `/magicstorage set capacity 27`. Short names: `deposit`, `capacity`, `glow` |
| `/magicstorage reload` | Reads `config.json` again after editing it by hand |

`/ms` works as a short form of `/magicstorage`.

## Development

Requirements: Java 25 (Temurin), IntelliJ IDEA, and an installed copy of Hytale.

1. Open the project in IntelliJ and let Gradle sync. The project compiles against the `HytaleServer.jar` of your Hytale installation. If Hytale is not installed in the default location, set `hytale_home` in `gradle.properties` or pass `-Phytale_home=<path>`.
2. Start the **HytaleServer** run configuration, or run `gradlew.bat runServer`.
3. On first start, enter `auth login device` in the server console and confirm, then `auth persistence Encrypted`.
4. Start Hytale and connect to `localhost`.

### Models and textures

The chest and bag have their own models, textures and icons under `src/main/resources/Common/`:

| File | Content |
|---|---|
| `Blocks/MagicStorage/Magic_Chest.blockymodel` + `_Texture.png` | Violet chest with a void crystal on the lid and a void swirl inside |
| `Blocks/MagicStorage/Magic_Chest_Large.blockymodel` + `_Texture.png` | The same as a two-block large chest |
| `Items/MagicStorage/Magic_Bag.blockymodel` + `_Texture.png` | Violet bag with a gold rim, an eye rune and a void crystal |
| `Icons/ItemsGenerated/Magic_Chest.png`, `Magic_Chest_Large.png`, `Magic_Bag.png` | Inventory icons (64×64) |

The `.blockymodel` files open in Blockbench with the Hytale plugin. How they were made and how to re-render the icons is described in `tools/assets/README.md`.

### Building

`gradlew.bat build` creates `build/libs/MagicStorage-<version>.jar`.

Every push to `main` and every pull request builds the jar on GitHub Actions. It is attached to the run under **Actions → Build → Artifacts** as `MagicStorage`. CI compiles against `com.hypixel.hytale:Server` in the version set by `hytale_build` in `gradle.properties`.

### Releasing

Releases and `CHANGELOG.md` are made by [release-please](https://github.com/googleapis/release-please). It reads the commit messages on `main`, so they have to follow [Conventional Commits](https://www.conventionalcommits.org/):

| Prefix | Example | Effect |
|---|---|---|
| `feat:` | `feat: add a sort button to the chest` | new minor version, listed under Features |
| `fix:` | `fix: bag keeps its link after relog` | new patch version, listed under Bug Fixes |
| `feat!:` or a `BREAKING CHANGE:` footer | `feat!: new storage format` | breaking change (minor bump before 1.0) |
| `docs:`, `chore:`, `ci:`, `refactor:`, `test:` | `ci: cache Gradle` | no release, not in the changelog |

Pull requests are squash merged, so the pull request title becomes the commit message and has to use one of these prefixes; a check on every pull request enforces it.

After each push to `main`, release-please opens or updates a release pull request that bumps `version` in `gradle.properties` and `manifest.json` and adds the new section to `CHANGELOG.md`. Merging it creates the tag (for example `v0.1.0`) and the GitHub release with those notes; the workflow then attaches the jar. Do not bump the version or push tags by hand. To force a version, add a `Release-As: 1.0.0` footer to a commit.

The release also uploads the jar to CurseForge, with the release notes as changelog, using [itsmeow/curseforge-upload](https://github.com/itsmeow/curseforge-upload). It needs the repository secrets `CURSEFORGE_TOKEN` (an upload token from your CurseForge account settings) and `CURSEFORGE_PROJECT_ID`; without them the step is skipped. Optional repository variables:

| Variable | Default | Description |
|---|---|---|
| `CURSEFORGE_RELEASE_TYPE` | `beta` | `alpha`, `beta` or `release` |
| `CURSEFORGE_GAME_VERSION` | `1.0` | Hytale version name(s) on CurseForge, comma-separated |

To upload an existing release again (for example after a failed upload), run the **Build** workflow by hand under **Actions** and enter its tag, such as `v1.0.0`, in **Upload the jar of an existing release to CurseForge**.

## License

[MIT](LICENSE) © oridiu

Magic Storage is a fan-made mod and is not affiliated with or endorsed by Hypixel Studios.
