# Console-Jack Project Conventions for AI Agents

This document outlines the key architectural patterns and coding conventions established in the Console-Jack project. Adhering to these rules ensures consistency, maintainability, and architectural integrity.

## 1. Architecture: Subsystems and Managers

The project uses a distinct pattern to separate core logic from its public-facing API.

- **Subsystems**: These are core, often stateful, components that manage a specific domain (e.g., rendering, input, state).
    - **Implementation**: Subsystems **MUST** be implemented as **enum singletons** (e.g., `public enum RenderSubsystem { INSTANCE; }`). This provides thread-safe, global access and aligns with *Effective Java, Item 3*.
    - **Location**: `net.luxsolari.engine.systems.internal`

- **Managers**: These are the public-facing APIs for the subsystems.
    - **Implementation**: Managers **MUST** be **stateless facades** containing only `public static` methods.
    - **Function**: They delegate all calls to the corresponding subsystem's `INSTANCE`. They should not contain any state or logic themselves.
    - **Example**: `public final class StateMachineManager { private StateMachineManager() {} public static void push(LoopableState state) { StateMachineSubsystem.INSTANCE.push(state); } }`
    - **Location**: `net.luxsolari.engine.manager`

## 2. State Management

Game flow is controlled by a stack-based state machine.

- **State Interface**: All game states (e.g., `MainMenuState`, `GameplayState`) **MUST** implement the `LoopableState` interface.
- **State Transitions**: All state changes (e.g., `push`, `pop`, `replace`, `clear`) **MUST** be performed through the `StateMachineManager` facade. Game states or other classes should **NEVER** access `MasterSubsystem` or `StateMachineSubsystem` directly for state transitions.

## 3. Coding Conventions

- **Logging**: Use `java.util.logging.Logger` for all logging. Each class should have its own static logger instance.
  - `private static final Logger LOGGER = Logger.getLogger(MyClass.class.getSimpleName());`
- **Immutability & Encapsulation**: Follow *Effective Java* principles. Minimize mutability and use private fields with accessors where appropriate.
- **Concurrency**: Subsystems that require their own thread are managed by `MasterSubsystem`. Any shared mutable data within a subsystem must be handled with thread-safe collections or appropriate synchronization.

## 4. Git Commits

- Follow the **Conventional Commits** specification for all commit messages (e.g., `feat:`, `fix:`, `refactor:`, `docs:`). Refer to `.windsurf/rules/conventional-commits.md` for details.
