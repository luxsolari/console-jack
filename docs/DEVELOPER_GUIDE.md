# Console Jack - Developer Guide

## Getting Started

### Prerequisites
- **Java 21+** with preview features support
- **Maven 3.x**
- **Terminal emulator** (supports ANSI colors)

### Initial Setup
```bash
git clone <repository-url>
cd console-jack
mvn compile
mvn exec:java  # Verify everything works
```

## Development Workflow

### 1. Daily Development
```bash
# Start development session
mvn compile
mvn exec:java

# Make code changes...

# Quick verification
mvn compile && mvn exec:java
```

### 2. Code Quality Checks
```bash
# Style validation (Google Java Style)
mvn checkstyle:check

# Fix style issues and re-check
mvn checkstyle:check
```

### 3. Build Verification
```bash
# Full clean build
mvn clean compile

# Create distributable
mvn clean package

# Test the packaged version
java -jar target/java-packageable-base-1.0-SNAPSHOT-jar-with-dependencies.jar
```

## Build Commands Reference

### Compilation
```bash
mvn compile                    # Compile source code only
mvn clean compile             # Clean and compile
mvn test-compile              # Compile test sources (when tests exist)
```

### Execution
```bash
mvn exec:java                 # Run with exec plugin (development)
mvn exec:java -Dexec.args="--debug"  # Pass arguments
```

### Packaging
```bash
mvn package                   # Create JAR with dependencies
mvn clean package             # Clean build
java -jar target/java-packageable-base-1.0-SNAPSHOT-jar-with-dependencies.jar
```

### Platform-Specific Builds
```bash
# Windows executable
mvn clean package -Pwindows
# Output: build-win/

# macOS package
mvn clean package -Pmac
# Output: build-mac/

# Linux executable
mvn clean package -Plinux
# Output: build-linux/
```

### Code Quality
```bash
mvn checkstyle:check          # Style validation
mvn checkstyle:checkstyle     # Generate style report
```

## Code Style Guidelines

### Java Style
- **Standard**: Google Java Style Guide
- **Enforcement**: Checkstyle with `google_checks.xml`
- **IDE Setup**: Import Google style settings

### Naming Conventions
```java
// Classes: PascalCase
public class GameplayState { }

// Methods: camelCase
public void startNewGame() { }

// Fields: camelCase
private boolean gameRunning;

// Constants: UPPER_SNAKE_CASE
private static final int TARGET_UPS = 8;

// Packages: lowercase.separated
net.luxsolari.engine.systems
```

### Documentation Standards
```java
/**
 * Brief description of the class purpose.
 *
 * <p>Longer description with implementation details,
 * design decisions, or usage examples.
 */
public class ExampleClass {

    /**
     * Brief method description.
     *
     * @param parameter description of parameter
     * @return description of return value
     * @throws ExceptionType when this exception occurs
     */
    public String exampleMethod(String parameter) {
        // implementation
    }
}
```

## Architecture Guidelines

### Adding New Subsystems
1. **Create enum singleton**:
```java
public enum NewSubsystem implements Subsystem {
    INSTANCE;

    @Override
    public void init() throws ResourceInitializationException {
        // initialization
    }

    @Override
    public void start() { /* start logic */ }

    @Override
    public void stop() { /* stop logic */ }

    @Override
    public void cleanUp() throws ResourceCleanupException {
        // cleanup
    }
}
```

2. **Register in MasterSubsystem**
3. **Create corresponding Manager if needed**

### Adding New Game States
1. **Implement LoopableState**:
```java
public class NewGameState implements LoopableState {
    @Override
    public void onEnter() {
        // State initialization
    }

    @Override
    public void update() {
        // Per-frame logic
    }

    @Override
    public void onExit() {
        // State cleanup
    }
}
```

2. **Add state transitions** in existing states
3. **Register with StateMachineManager**

### Adding ECS Components
1. **Create data record**:
```java
public record NewComponent(
    String data,
    int value
) implements Component {}
```

2. **Create or modify systems** to process the component
3. **Add to entities** in EntityPool when needed

### Adding UI Components
1. **Extend base classes**:
```java
public class NewWidget extends UIWidget implements Focusable {
    @Override
    public void render(Screen screen) {
        // Rendering logic
    }

    @Override
    public boolean handleInput(KeyStroke keyStroke) {
        // Input handling
        return false; // true if consumed
    }
}
```

## Testing Strategy

### Manual Testing
```bash
# Run and verify core functionality
mvn exec:java

# Test areas:
# - Main menu navigation
# - Game state transitions
# - Input responsiveness
# - Audio playback (if enabled)
# - Clean exit
```

### Performance Testing
```bash
# Monitor during gameplay:
# - UPS consistency (should be ~8)
# - Memory usage
# - Thread behavior
# - Audio synchronization
```

### Build Testing
```bash
# Test all platforms
mvn clean package -Pwindows
mvn clean package -Pmac
mvn clean package -Plinux

# Verify each package runs correctly
```

## Common Development Tasks

### Adding a New Menu Item
1. Create `MenuAction` for the action
2. Add `MenuItem` to the menu
3. Implement action logic
4. Test navigation

### Adding Sound Effects
1. Place WAV file in `src/main/resources/audio/sfx/`
2. Load in `AudioManager`
3. Trigger via `AudioSubsystem.INSTANCE`

### Adding New Card Graphics
1. Create `CardArt` component
2. Update `CardSprite` rendering
3. Modify `DisplayListSystem` if needed

### Debugging Tips
```java
// Add temporary logging
private static final Logger LOGGER =
    Logger.getLogger(YourClass.class.getName());

LOGGER.info("Debug message: " + value);
```

### Performance Profiling
- Use JVisualVM or JProfiler
- Focus on game loop timing
- Monitor ECS system performance
- Check for memory leaks

## Troubleshooting

### Common Issues

**Build Failures:**
```bash
# Clear Maven cache
mvn clean
rm -rf ~/.m2/repository/net/luxsolari

# Rebuild
mvn compile
```

**Runtime Issues:**
- Check Java version (must be 21+)
- Verify terminal supports ANSI colors
- Ensure audio dependencies are available

**Performance Issues:**
- Profile with JVisualVM
- Check for excessive object creation
- Verify thread synchronization

## Release Process

### Pre-Release Checklist
- [ ] All code passes Checkstyle
- [ ] Application runs without errors
- [ ] All platforms build successfully
- [ ] Performance meets targets (8 UPS)
- [ ] Audio works correctly
- [ ] Documentation is current

### Creating Release
```bash
# Version update (if needed)
# Update version in pom.xml

# Build all platforms
mvn clean package -Pwindows
mvn clean package -Pmac
mvn clean package -Plinux

# Create release packages
# Package build-win/, build-mac/, build-linux/
```

## Resources

- **Lanterna Documentation**: [GitHub](https://github.com/mabe02/lanterna)
- **AudioCue Documentation**: [GitHub](https://github.com/philfrei/AudioCue)
- **Google Java Style**: [Style Guide](https://google.github.io/styleguide/javaguide.html)
- **Effective Java**: Best practices reference

## Getting Help

1. Check this guide first
2. Review `ARCHITECTURE.md` for design questions
3. Check `IMPROVEMENT_PLAN.md` for planned changes
4. Review code comments and JavaDoc
5. Test with `mvn exec:java` to verify behavior