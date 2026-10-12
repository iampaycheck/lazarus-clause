# License scope and original asset notice

The root `LICENSE` contains the MIT License for original Lazarus Clause code,
including Ghost Core, and documentation associated with that code.

Associated code resources include the interface and translation data in
`mods/ghostcore/src/main/resources/assets/ghostcore/lang/en_us.json`; that entire
file is MIT-licensed, including its player-facing text. Functional hitbox shapes
and gameplay code in
`mods/ghostcore/src/main/java/com/iampaycheck/ghostcore/block/TransmatBeaconBlock.java`
are MIT-licensed, including `SHAPE` and `getShape()`, even where their dimensions
match a reserved visual model.

Original models, textures, audio and other artistic assets are excluded from
that MIT grant. All rights to those original artistic assets are reserved for
now; no general reuse license is granted for them. This applies regardless of
file format, including original model geometry embedded in source code. The
rendering and animation implementation code remains MIT-licensed.

In Ghost Core, this includes original art in
`mods/ghostcore/src/main/resources/assets/ghostcore/models/` and `textures/`,
and the original structure/layout asset
`mods/ghostcore/src/main/resources/data/ghostcore/structure/platform.nbt`.
In
`mods/ghostcore/src/main/java/com/iampaycheck/ghostcore/client/render/GhostModel.java`,
the reservation is limited to the artistic geometry data defined by
`createLayer()`: the core, lens, antenna and shard cube dimensions, positions,
UV mapping and model-part layout, including the texture-size specification.
The method's implementation that builds the layer, and all other code in that
file, including the constructor, constants, `animate()`, `setupAnim()` and
`renderToBuffer()`, remain MIT-licensed. The reservation does not extend to the
API calls or control flow used to construct the model.

These boundaries record the owner's code-versus-art policy; they do not
establish ownership or assert that basic shapes are copyrightable.
In the jar, resource paths start at `assets/ghostcore/` or `data/ghostcore/`.
This is not a blanket
reservation of every file under `assets/` or every third-party model reference.

Third-party code, mods, dependencies, models, textures, audio and other materials
retain their own licenses, permissions and notices. Neither the MIT grant nor
the original asset reservation changes those terms or revokes rights they
grant. This includes Minecraft, NeoForge, TaCZ and its ports, and any third-party
assets. Preserve their notices and follow their terms when distributing them.

Ghost Core jars include both `LICENSE` and `LICENSE-SCOPE.md` at the archive root.
The mod metadata summarizes this split; it does not license third-party content.
