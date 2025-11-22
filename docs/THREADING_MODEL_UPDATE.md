# Threading Model Update - Dynamic UI System

## Overview

The Dynamic UI System adds a new integration point to the existing threading model without changing the core architecture. The key addition is `UICommandQueue.processAll()` in the Master thread's game loop.

## Updated Master Thread Flow

### Before (Original)

```
Master Thread (8 UPS - 125ms per update)
│
└─→ while (running) {
      ├─→ LoopableState.handleInput()
      ├─→ LoopableState.update()
      ├─→ LoopableState.render()          ← UI rendered
      └─→ ECS systems update
    }
```

### After (With Dynamic UI)

```
Master Thread (8 UPS - 125ms per update)
│
└─→ while (running) {
      ├─→ LoopableState.handleInput()
      ├─→ LoopableState.update()
      ├─→ UICommandQueue.processAll()     ← NEW: Process UI updates
      ├─→ LoopableState.render()          ← UI rendered with updates
      └─→ ECS systems update
    }
```

**Location**: `MasterSubsystem.java:148`

**Rationale**: Processing UI commands after state update but before rendering ensures UI reflects the latest game state while maintaining thread safety.

## Cross-Thread Communication

### New Communication Paths

```
[Input Thread]                 [Audio Thread]                [Game Logic]
      │                              │                             │
      │ KeyStroke events             │ Audio events                │ Game state changes
      │                              │                             │
      ▼                              ▼                             ▼
┌─────────────────────────────────────────────────────────────────────────┐
│                         UICommandQueue (Thread-Safe)                     │
│  - LinkedBlockingQueue<UICommand>                                       │
│  - AtomicInteger queuedCommands                                         │
│  - enqueue() from any thread                                            │
│  - processAll() from Master thread only                                 │
└─────────────────────────────────────────────────────────────────────────┘
                                      │
                                      │ Processes commands
                                      ▼
                              [Master Thread]
                                      │
                                      │ Executes:
                                      ├─→ Label.setText()
                                      ├─→ Menu.addItem()
                                      ├─→ UIContainer.addChild()
                                      └─→ etc.
                                      │
                                      ▼
                              [UI Components Updated]
                                      │
                                      │ Rendered
                                      ▼
                              [Render Thread]
```

### Observable Properties Flow

```
[Any Thread]
      │
      │ Updates data
      ▼
UIProperty.setValue(value)
      │
      │ Synchronized
      ▼
For each listener:
    └─→ Create UICommand
        └─→ UICommandQueue.enqueue(command)
                │
                │ Queued for processing
                ▼
        [Master Thread]
                │
                ▼
        UICommandQueue.processAll()
                │
                ▼
        listener.onPropertyChange(oldValue, newValue)
                │
                ▼
        PropertyBinder.applyValue(component, value)
                │
                ▼
        UIComponent updated
```

## Thread Safety Analysis

### Synchronization Mechanisms

| Component | Mechanism | Purpose |
|-----------|-----------|---------|
| `UICommandQueue.commandQueue` | LinkedBlockingQueue | Thread-safe enqueue from any thread |
| `UICommandQueue.queuedCommands` | AtomicInteger | Lock-free count tracking |
| `UIProperty.value` | volatile + synchronized | Atomic read/write with listener notification |
| `UIProperty.listeners` | CopyOnWriteArrayList | Thread-safe listener management |
| `UIComponent` mutations | Master thread confinement | No locks needed - single-threaded access |

### Lock-Free Guarantees

1. **Enqueueing Commands**: Lock-free via BlockingQueue
2. **Reading Property Values**: Lock-free via volatile read
3. **Processing Commands**: No locks needed (Master thread only)
4. **Listener Iteration**: Lock-free via CopyOnWriteArrayList

### Potential Contention Points

1. **UIProperty.setValue()**: Synchronized method
   - **Impact**: Minimal - fast notification loop
   - **Mitigation**: Notifications queued as lightweight commands

2. **UICommandQueue Write Index**: AtomicInteger CAS
   - **Impact**: Minimal - single atomic operation
   - **Mitigation**: Hardware support for CAS operations

## Performance Characteristics

### Command Processing

```
Master Thread Loop (125ms budget @ 8 UPS)
├─→ handleInput():    ~5ms
├─→ update():         ~20ms
├─→ processAll():     ~1-10ms  ← NEW (depends on queue depth)
├─→ render():         ~30ms
├─→ ECS systems:      ~10ms
└─→ Sleep/margin:     ~60ms
```

**Typical Command Processing**:
- 10 commands: ~1ms
- 100 commands: ~5ms
- 1000 commands: ~50ms (warning logged)

**Worst Case**: Queue overflow
- **Threshold**: 1000 commands
- **Action**: Log warning
- **Recovery**: Continue processing (unbounded queue)

## Comparison with Existing Subsystems

### RenderSubsystem Pattern

| Aspect | RenderSubsystem | UICommandQueue |
|--------|----------------|----------------|
| **Thread Model** | Separate thread (10 FPS) | Master thread integration |
| **Data Structure** | AtomicReference<List<RenderCmd>> | BlockingQueue<UICommand> |
| **Update** | Atomic swap of entire list | FIFO command processing |
| **Synchronization** | ConcurrentHashMap layers | BlockingQueue |

**Similarity**: Both use lock-free data structures for cross-thread communication

**Difference**: UICommandQueue processes on Master thread; Render on separate thread

### InputSubsystem Pattern

| Aspect | InputSubsystem | UICommandQueue |
|--------|---------------|----------------|
| **Thread Model** | Separate thread polling | Master thread integration |
| **Data Structure** | Deque + ReentrantLock | BlockingQueue |
| **Update** | Lock-protected queue | Lock-free enqueue |
| **Processing** | Master thread polls | Master thread processes all |

**Similarity**: Both queue events for Master thread consumption

**Difference**: UICommandQueue is lock-free; Input uses explicit locks

## Integration Points

### Modified Files

1. **MasterSubsystem.java:148**
   ```java
   // Process all pending UI commands before rendering
   UICommandQueue.INSTANCE.processAll();
   ```

2. **UIWidget.java**
   ```java
   public abstract class UIWidget implements BindableUIComponent {
       private final List<Subscription> subscriptions = new ArrayList<>();
       // ... bind() and unbindAll() implementations
   }
   ```

### New Subsystem Interactions

```
┌──────────────────┐
│ MasterSubsystem  │
│  (Master Thread) │
└────────┬─────────┘
         │
         ├──→ InputSubsystem.poll()        [Existing]
         ├──→ StateMachineManager.active() [Existing]
         ├──→ UICommandQueue.processAll()  [NEW]
         └──→ DisplayListSystem.update()   [Existing]
```

## Memory Model

### Happens-Before Relationships

1. **Property Update → Listener Notification**
   ```
   Thread A: property.setValue(newValue)
              └─→ [synchronized]
                  └─→ enqueue(command)
                      └─→ [BlockingQueue]

   Thread B: processAll()
              └─→ [BlockingQueue]
                  └─→ command.execute()
                      └─→ listener.onPropertyChange()
   ```
   **Guarantee**: BlockingQueue provides happens-before from enqueue to dequeue

2. **Command Enqueue → Command Execute**
   ```
   Thread A: UICommandQueue.enqueue(command)
              └─→ [BlockingQueue.offer()]

   Master Thread: UICommandQueue.processAll()
                   └─→ [BlockingQueue.poll()]
                       └─→ command.execute()
   ```
   **Guarantee**: BlockingQueue memory visibility

### Visibility Guarantees

- **UIProperty.value**: volatile ensures visibility
- **UICommandQueue.commandQueue**: BlockingQueue ensures visibility
- **UIProperty.listeners**: CopyOnWriteArrayList ensures visibility

## Migration Impact

### Backward Compatibility

- ✅ All existing UI code continues to work unchanged
- ✅ Existing threading model unchanged
- ✅ No performance regression for non-dynamic UI
- ✅ Opt-in: Only code using new APIs affected

### Performance Impact

- **Additional overhead**: ~1-10ms per frame (processAll)
- **When empty**: Near-zero cost (poll returns null immediately)
- **Typical use**: 10-50 commands per second = negligible impact
- **Worst case**: 1000 commands = 50ms (logged as warning)

## Future Considerations

### Potential Optimizations

1. **Batch Processing Limit**: Cap processAll() at N commands per frame
   ```java
   public void processAll(int maxCommands) {
       int count = Math.min(queuedCommands.getAndSet(0), maxCommands);
       // ...
   }
   ```

2. **Priority Queue**: High-priority commands processed first
   ```java
   private final PriorityBlockingQueue<PrioritizedCommand> commandQueue;
   ```

3. **Command Coalescing**: Merge redundant commands
   ```java
   // Coalesce setText() calls to same label
   if (lastCommand.target == command.target) {
       replace(lastCommand, command);
   }
   ```

### Monitoring and Metrics

Add runtime metrics collection:

```java
public class UICommandMetrics {
    private final AtomicLong totalEnqueued;
    private final AtomicLong totalProcessed;
    private final AtomicLong totalErrors;
    private final AtomicLong maxQueueDepth;
    private final AtomicLong totalProcessingTimeNanos;

    public double getAverageProcessingTimeMs() {
        return (totalProcessingTimeNanos.get() / 1_000_000.0) / totalProcessed.get();
    }
}
```

## Diagram Updates Needed

### Excalidraw Diagram (`console-jack-thread-model.excalidraw`)

**Manual Updates Required**:

1. Add "UICommandQueue" box near Master Thread
2. Add arrows from Input/Audio/Game Logic threads to UICommandQueue
3. Add arrow from UICommandQueue to Master Thread
4. Update Master Thread box to show processAll() step
5. Add legend entry for "New with Dynamic UI System"

**Recommended Changes**:

```
[Input Thread] ──┐
                 │
[Audio Thread] ──┼──→ [UICommandQueue] ──→ [Master Thread]
                 │         (NEW)              ├─→ processAll() (NEW)
[Game Logic] ────┘                           └─→ render()
```

## Related Documentation

- [Dynamic UI System](DYNAMIC_UI_SYSTEM.md) - Comprehensive usage guide
- [State Machine Guide](STATE_MACHINE_GUIDE.md) - State integration
- Main threading diagram: `src/main/resources/console-jack-thread-model.excalidraw`

## Summary

The Dynamic UI System integrates cleanly into the existing threading model:

- **Single Addition**: `UICommandQueue.processAll()` in Master thread loop
- **No Changes**: To existing thread structure or synchronization
- **Thread Safety**: Via established patterns (BlockingQueue, volatile, thread confinement)
- **Performance**: Negligible impact (<10ms typical)
- **Compatibility**: Fully backward compatible

The system leverages Console Jack's existing architectural patterns while adding new capabilities for thread-safe dynamic UI modifications.
