# Magic Storage (Hytale-Mod)

Eine Hytale-Mod für Chapter 1 (Update 7) mit zwei neuen Gegenständen:

- **Magische Truhe:** eine Truhe, deren Inhalt von überall erreichbar ist.
- **Magischer Beutel:** wird mit einer Magischen Truhe verknüpft und öffnet sie von überall, z. B. im Forgotten Temple.

## Im Spiel

- **Magische Truhe** und **Magischer Beutel** werden an der Goblin-Werkbank (Goblin Tinkerer's Workbench) im Forgotten Temple unter „Gadgets“ gecraftet. Die Werkbank muss mit Strom versorgt sein.
  - Truhe: 40 Scraps, 8 Void Crystal Shards, 2 Dubious Assembly, 1 Small Crude Chest
  - Beutel: 20 Scraps, 4 Void Crystal Shards, 2 Spider Silk, 1 Voidheart
- **Verknüpfen:** Beutel in die Hand nehmen und die Magische Truhe benutzen (Taste zum Öffnen von Truhen). Jeder Beutel darf mit jeder Magischen Truhe verknüpft werden, auch mit fremden. Beliebig viele Beutel können auf dieselbe Truhe zeigen.
- **Öffnen von überall:** Rechtsklick mit dem Beutel in der Hand öffnet die verknüpfte Truhe, auch in anderen Welten oder Dungeon-Instanzen.
- **Ein Beutel pro Spieler:** Ein zweiter Beutel wird wieder fallen gelassen, und solange man einen trägt, lässt sich kein weiterer craften.
- **Tod:** Der Beutel bleibt im Inventar.
- **Truhe abbauen:** Der Inhalt fällt heraus, verknüpfte Beutel melden danach „Truhe existiert nicht mehr“.

## Einstellungen

Beim ersten Start legt der Server `mods/oridiu_MagicStorage/config.json` an:

| Einstellung | Standard | Bedeutung |
|---|---|---|
| `AllowDepositViaBag` | `true` | `false`: Über den Beutel kann man nur entnehmen, nicht ablegen |
| `ChestCapacity` | `36` | Plätze einer neuen Magischen Truhe (9 bis 180) |
| `AutoSaveSeconds` | `30` | Wie oft geänderte Truhen gespeichert werden |

Der Truheninhalt liegt in `mods/oridiu_MagicStorage/chests/`, eine Datei pro Truhe.

## Entwickeln

Voraussetzungen: Java 25 (Temurin), IntelliJ IDEA, installiertes Hytale.

1. Projekt in IntelliJ öffnen und Gradle laden lassen. Das Projekt kompiliert gegen die `HytaleServer.jar` deiner Hytale-Installation.
2. Run-Konfiguration **HytaleServer** starten (oder `gradlew.bat runServer`).
3. Beim ersten Start in der Server-Konsole `auth login device` eingeben und bestätigen, danach `auth persistence Encrypted`.
4. Hytale starten und mit `localhost` verbinden.

## Bauen und auf den Server bringen

`gradlew.bat build` erzeugt `build/libs/MagicStorage-<version>.jar`. Diese Datei in den Ordner `mods` des Servers kopieren und den Server neu starten. Server und Mod müssen dieselbe Hytale-Version haben.

### GitHub Actions

Jeder Push auf `main` und jeder Pull Request baut die Jar automatisch. Sie liegt danach im Lauf unter **Actions → Build → Artifacts** als `MagicStorage`. CI kompiliert gegen `com.hypixel.hytale:Server` in der Version `hytale_build` aus `gradle.properties`.

Für ein Release `version` in `gradle.properties` erhöhen, committen und ein passendes Tag pushen, z. B. `git tag v0.1.0` und `git push origin v0.1.0`. Die Action legt dann ein GitHub-Release mit der Jar an.
