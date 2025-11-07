<!--
  ============================================================================
  SYNC IMPACT REPORT
  ============================================================================
  Version Change: 1.0.0 → 1.0.0 (Validation Pass)

  Validation Date: 2025-10-15

  Modified Principles:
    - None (all principles validated and current)

  Added Sections:
    - None (constitution is complete)

  Removed Sections:
    - None

  Templates Status:
    ✅ .specify/templates/plan-template.md - References Constitution Check (validated)
    ✅ .specify/templates/spec-template.md - No constitution references needed (validated)
    ✅ .specify/templates/tasks-template.md - Implicitly follows constitution (validated)
    ✅ .specify/templates/checklist-template.md - Generic template (validated)

  Codebase Validation:
    ✅ Enum singleton pattern confirmed for subsystems (RenderSubsystem, MasterSubsystem, etc.)
    ✅ Package structure matches constitution (engine/* and game/* separation verified)
    ✅ Threading model architecture present (Master, Render, Input, Audio threads)
    ✅ State machine implementation follows LoopableState pattern
    ✅ ECS components are data-only (Position, Visual, Card, etc.)
    ✅ Java 21 and Maven configuration confirmed in pom.xml

  Follow-up TODOs:
    - None - constitution is current and accurate
  ============================================================================
-->

# Console Jack Project Constitution

## Core Principles

### I. Enum Singleton Pattern for Core Subsystems

All core subsystems MUST use the enum singleton pattern (Effective Java, Item 3). Access to subsystems MUST be via `.INSTANCE` notation.

**Rationale**: Ensures thread-safe, lazy initialization with guaranteed single instance per subsystem. This pattern is critical for the multi-threaded architecture where Master, Render, Input, and Audio threads coordinate through singleton subsystems.

**Required Subsystems**:
- `RenderSubsystem.INSTANCE` - Terminal rendering
- `InputSubsystem.INSTANCE` - Keyboard/input handling
- `AudioSubsystem.INSTANCE` - Sound effects and music
- `MasterSubsystem.INSTANCE` - Main game loop coordination
- `StateMachineSubsystem.INSTANCE` - Game state management

### II. Threading Model Discipline

The application MUST maintain exactly four threads with defined responsibilities:

1. **Master Thread**: Game loop at 8 UPS (125ms per update) - NO rendering, NO blocking I/O
2. **Render Thread**: Terminal UI rendering using Lanterna - ONLY render operations
3. **Input Thread**: Keyboard/input event processing - ONLY input handling
4. **Audio Thread**: Audio subsystem management - ONLY audio operations

**Non-Negotiable Rules**:
- Thread responsibilities MUST NOT overlap
- Master thread MUST NOT perform rendering operations
- Render thread MUST NOT perform game logic
- Input thread MUST NOT modify game state directly (use event queue)
- Audio thread MUST NOT block other threads

**Rationale**: Clear thread separation prevents deadlocks, race conditions, and maintains consistent 8 UPS game loop timing. Violating thread boundaries causes observable frame drops and input latency.

### III. State Machine Architecture

Game states MUST implement `LoopableState` interface. State transitions MUST use `StateMachineManager` facade via push/pop/replace operations only.

**Required State Structure**:
- All states implement `LoopableState` interface
- Initial state is `MainMenuState`
- State transitions ONLY via `StateMachineManager` static facade
- States reside in `net.luxsolari.game.states.*` package

**Forbidden**:
- Direct state instantiation outside StateMachineManager
- State transitions by field assignment
- Circular state dependencies

**Rationale**: Centralized state management enables predictable state transitions, proper cleanup on state exit, and easier debugging of state flow. Direct state manipulation bypasses lifecycle hooks and breaks save/restore functionality.

### IV. Entity Component System (ECS) Integrity

Game entities MUST be managed through `EntityPool`. Components MUST be data-only (no behavior). Systems MUST process components (no direct entity manipulation).

**Required ECS Structure**:
- **EntityPool**: Manages all game entities
- **Components**: Pure data (`Position`, `Visual`, `Card`, `CardArt`, `CardSprite`)
- **Systems**: Process components (`DisplayListSystem` for rendering)
- ECS updates MUST run in main game loop (Master Thread)

**Component Rules**:
- Components contain ONLY data fields
- Components MUST NOT contain methods beyond getters/setters
- Components MUST NOT reference systems or subsystems

**System Rules**:
- Systems process components in batch
- Systems MUST NOT store entity references between frames
- Systems operate on component views, not entity objects

**Rationale**: Data-oriented design enables cache-friendly iteration, easy serialization for save games, and clean separation between data and behavior. Component behavior breaks the ECS pattern and couples data to logic.

### V. Java 21 and Maven Build Standards

Project MUST use Java 21 with preview features enabled. Build MUST use Maven with Google Checkstyle enforcement.

**Required Configuration**:
- Java 21 with `--enable-preview` flag
- Maven 3.x with Assembly plugin for JAR with dependencies
- Checkstyle with `google_checks.xml` configuration
- Main class: `net.luxsolari.game.Main`
- Three platform-specific packaging profiles (Windows, macOS, Linux)

**Build Discipline**:
- `mvn compile` MUST succeed before commits
- `mvn checkstyle:check` warnings MUST be addressed or explicitly justified
- Preview features MUST be documented in code comments
- Platform packages MUST build successfully for all three targets

**Rationale**: Java 21 preview features (pattern matching, record patterns) enable cleaner code for state machines and ECS. Google Checkstyle maintains consistent code style across the codebase. Multi-platform packaging is a core requirement for distribution.

## Development Standards

### Package Structure Discipline

Code MUST follow the established two-tier package structure:

**Engine Tier** (`net.luxsolari.engine.*`): Reusable game engine components
- `ecs/` - Entity-Component-System implementation
- `manager/` - Static utility managers (Input, Audio, Render, StateMachine)
- `systems/` - Subsystem interfaces and implementations
- `states/` - Base state interfaces
- `ui/` - Custom UI framework components

**Game Tier** (`net.luxsolari.game.*`): Game-specific implementation
- `states/` - Concrete game states (MainMenu, Gameplay, Pause)
- `ecs/` - Game-specific components (Card, etc.)

**Rules**:
- Engine code MUST NOT depend on game code
- Game code MAY depend on engine code
- No circular dependencies between packages
- New packages require explicit justification

**Rationale**: Clean separation between reusable engine and game-specific code enables future game projects to reuse the engine tier. Engine-to-game dependencies break reusability.

### UI Component Framework

Custom UI components MUST follow the established framework:

**Base Interfaces**:
- `UIComponent` - Base for all UI elements
- `UIContainer` - Components that contain other components
- `UIWidget` - Leaf components (buttons, labels, etc.)
- `Focusable` - Components that can receive keyboard focus

**Navigation**:
- `Menu` and `MenuItem` for hierarchical navigation
- Focus management via `Focusable` interface
- Z-layer system for rendering depth

**Rules**:
- UI components MUST NOT directly access subsystems (use dependency injection)
- UI state MUST be serializable for save/restore
- Custom widgets MUST extend base framework classes

**Rationale**: Lanterna provides low-level terminal rendering, but the custom UI framework provides game-appropriate abstractions (menus, focus management, z-layers). Direct Lanterna usage outside RenderSubsystem couples code to terminal implementation.

### Terminal UI Constraints

All visual output MUST use Lanterna 3.1.2 terminal UI framework. ONLY ASCII text graphics are permitted.

**Requirements**:
- All rendering via `RenderSubsystem.INSTANCE`
- ASCII art ONLY (no Unicode box-drawing in game assets)
- Terminal dimensions MUST be dynamic (no hardcoded sizes)
- Color support MUST degrade gracefully (monochrome fallback)

**Forbidden**:
- Direct `System.out.println()` for game rendering
- Hardcoded terminal dimensions
- Dependency on Unicode support
- Graphical libraries (Swing, JavaFX, etc.)

**Rationale**: Lanterna ensures cross-platform terminal compatibility. Unicode dependencies break on older terminals. Hardcoded dimensions fail on smaller terminal windows. ASCII-only ensures widest compatibility.

## Quality Gates

### Pre-Commit Gates

Before ANY commit:

1. **Build Verification**: `mvn compile` MUST succeed with zero errors
2. **Code Style**: `mvn checkstyle:check` MUST pass or violations MUST be explicitly justified in commit message
3. **Thread Safety**: Changes to subsystems MUST be reviewed for thread-safety implications
4. **ECS Integrity**: Component changes MUST NOT introduce behavior (methods beyond getters/setters)

### Pre-PR Gates

Before creating pull requests:

1. **Integration Test**: `mvn exec:java` MUST launch successfully and reach MainMenuState
2. **Platform Builds**: All three platform packages (Windows, macOS, Linux) MUST build successfully
3. **State Transitions**: Manual verification of state transitions (MainMenu → Gameplay → Pause → MainMenu)
4. **Performance**: Game loop MUST maintain 8 UPS (125ms per update) under normal load

### Architecture Compliance

All PRs MUST verify:

- [ ] Enum singleton pattern maintained for core subsystems
- [ ] Thread responsibilities not violated
- [ ] State transitions only via StateMachineManager
- [ ] ECS components contain only data
- [ ] Package structure discipline maintained (engine vs game tier)
- [ ] No direct Lanterna usage outside RenderSubsystem
- [ ] Java 21 preview features documented in code comments

## Governance

This constitution supersedes all other development practices and guidelines. All code changes MUST comply with the Core Principles defined above.

### Amendment Procedure

Constitution amendments require:

1. **Proposal**: Written proposal with clear rationale for change
2. **Impact Analysis**: Analysis of which existing code would be affected
3. **Migration Plan**: Concrete plan for bringing existing code into compliance
4. **Approval**: Documented approval with stakeholder sign-off
5. **Version Bump**: Semantic versioning applied (see versioning rules below)

### Versioning Policy

Constitution version MUST follow semantic versioning:

- **MAJOR** (X.0.0): Backward-incompatible governance changes, principle removals, or redefinitions that require code rewrites
- **MINOR** (x.Y.0): New principles added, material expansions to existing guidance, new quality gates
- **PATCH** (x.y.Z): Clarifications, wording improvements, typo fixes, non-semantic refinements

### Compliance Review

- **Frequency**: Constitution compliance MUST be verified in every code review
- **Tooling**: Checkstyle configuration MUST enforce structure rules where possible
- **Documentation**: Deviations MUST be documented with explicit justification and tracked as technical debt
- **Escalation**: Repeated violations require architectural review

### Runtime Development Guidance

For detailed development guidance, tooling recommendations, and workflow instructions, see:
- `CLAUDE.md` - Claude Code integration guide
- `ARCHITECT_MODE.md` - Architectural planning mode for Claude Code
- `RIPER_MODE.md` - Structured implementation protocol
- `README.md` - Quick start and project overview

---

**Version**: 1.0.0 | **Ratified**: 2025-10-15 | **Last Amended**: 2025-10-15
