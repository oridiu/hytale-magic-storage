# Magic Storage (Hytale-Mod)

Eine Hytale-Mod für Chapter 1 (Update 7) mit zwei neuen Gegenständen:

- **Magische Truhe:** eine Truhe, deren Inhalt von überall erreichbar ist.
- **Magischer Beutel:** wird mit einer Magischen Truhe verknüpft und öffnet sie von überall, z. B. im Forgotten Temple.

## Entwickeln

Voraussetzungen: Java 25 (Temurin), IntelliJ IDEA, installiertes Hytale.

1. Projekt in IntelliJ öffnen und Gradle laden lassen. Das Projekt kompiliert gegen die `HytaleServer.jar` deiner Hytale-Installation.
2. Run-Konfiguration **HytaleServer** starten (oder `gradlew.bat runServer`).
3. Beim ersten Start in der Server-Konsole `auth login device` eingeben und bestätigen, danach `auth persistence Encrypted`.
4. Hytale starten und mit `localhost` verbinden.

## Bauen und auf den Server bringen

`gradlew.bat build` erzeugt `build/libs/MagicStorage-<version>.jar`. Diese Datei in den Ordner `mods` des Servers kopieren und den Server neu starten. Server und Mod müssen dieselbe Hytale-Version haben.
