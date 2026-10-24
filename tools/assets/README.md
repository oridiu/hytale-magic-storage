# Asset-Werkzeuge

Die Modelle und Texturen der Mod basieren auf Vanilla-Assets (Royal-Magic-Truhe, Futterbeutel). Diese Skripte erzeugen daraus die eigenen Dateien unter `src/main/resources/Common/`. Sie brauchen Python 3 mit Pillow und NumPy.

`VANILLA` ist ein entpackter `Common/`-Ordner aus `Assets.zip` (mindestens die Truhe, der Futterbeutel und `Fertilizer_Crystal.png`), `RES` ist `src/main/resources`.

```
python tools/assets/chest_texture.py VANILLA RES   # Truhentextur (violett, Void-Inneres, Kristallflächen)
python tools/assets/chest_model.py   VANILLA RES   # Truhenmodell mit schwebendem Void-Kristall
python tools/assets/chest_large.py   VANILLA RES   # große Truhe (Modell und Textur), aus der großen Royal-Magic-Truhe
python tools/assets/bag.py           VANILLA RES   # Beutelmodell und -textur
```

Nach Änderungen in Blockbench müssen die Skripte nicht erneut laufen. Nur die Inventar-Icons neu rendern:

```
python tools/assets/render_icon.py RES/Common/Blocks/MagicStorage/Magic_Chest.blockymodel RES/Common/Blocks/MagicStorage/Magic_Chest_Texture.png RES/Common/Icons/ItemsGenerated/Magic_Chest.png 30,45,0
python tools/assets/render_icon.py RES/Common/Blocks/MagicStorage/Magic_Chest_Large.blockymodel RES/Common/Blocks/MagicStorage/Magic_Chest_Large_Texture.png RES/Common/Icons/ItemsGenerated/Magic_Chest_Large.png 30,45,0
python tools/assets/render_icon.py RES/Common/Items/MagicStorage/Magic_Bag.blockymodel RES/Common/Items/MagicStorage/Magic_Bag_Texture.png RES/Common/Icons/ItemsGenerated/Magic_Bag.png 30,45,0
```

`render_icon.py` legt zusätzlich eine `_256.png`-Vorschau neben das Icon; die gehört nicht ins Repo.
