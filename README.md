# mc-symmetrywand
A symmetrywand mod for Minecraft

## Symmetry Wand

A NeoForge mod (Minecraft 1.21.1) that adds a Symmetry Wand item for mirroring block
placement and removal across a configurable plane.

- Craft: Iron Ingot (top), Stick (middle), Ender Pearl (bottom) in a vertical line.
- Right-click a block with the wand to set/move the symmetry plane, oriented along the
  clicked face's axis.
- Shift-right-click to cycle the plane's orientation (X -> Z -> Y -> X).
- Right-click air to reset/clear the plane.
- The wand only needs to be carried in your inventory (not held) for mirroring of normal
  block placement/removal to apply; it must be held to set, reorient or reset the plane.
- Settings (max range, facing mirroring, resource requirement in Survival, chat feedback)
  are configurable via the generated `config/symmetrywand-common.toml` file.

### Building

```powershell
.\gradlew build
```

### Running in-dev

```powershell
.\gradlew runClient
```

