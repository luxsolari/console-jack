# Console Jack
Console-based, text-graphics stlye implementation of the classic casino game.

Aims to be a "Card RPG" of sorts where you start a little career as a frequent player on a local casino, and "move up the ranks" up to the top-tier casinos of the world.
All of that, in ASCII text-graphics, terminal-emulator-driven glory.

More details will be added during development.

### Architecture Notes

Core subsystems (Render, Input, Audio, Master) are enforced as JVM-wide singletons via the *enum singleton* pattern (see Effective Java, Item 3). Access them through `RenderSubsystem.INSTANCE`, etc.
