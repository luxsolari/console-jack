---
trigger: glob
description: 
globs: *.java, *.class
---

# The Complete "Effective Java" Ruleset for AI-Assisted Development

A comprehensive guide based on all 90 items in Joshua Bloch's "Effective Java, 3rd Edition."
This part covers the last chapters of the book. For the first 8 chapters, refer to the "effective-java-part-1.md" file.

## Chapter 9: General Programming

* **Item 57: Minimize the scope of local variables.** Declare variables at the point of first use to improve readability
  and reduce errors.
* **Item 58: Prefer for-each loops to traditional for loops.** The for-each loop is more readable, less error-prone, and
  works on any `Iterable`.
* **Item 59: Know and use the libraries.** Don't reinvent the wheel. Familiarize yourself with and use the rich features
  of the Java standard library.
* **Item 60: Avoid `float` and `double` if exact answers are required.** For calculations requiring exact results, like
  monetary calculations, use `BigDecimal`, `int`, or `long`.
* **Item 61: Prefer primitive types to boxed primitives.** Boxed primitives can lead to subtle bugs (`==` comparison)
  and performance issues due to object creation and unboxing `NullPointerException`s.
* **Item 62: Avoid strings where other types are more appropriate.** Don't use strings to represent numeric data, enums,
  or complex aggregate types.
* **Item 63: Beware the performance of string concatenation.** Using `+` in a loop to concatenate strings has quadratic
  performance. Use `StringBuilder` instead.
* **Item 64: Refer to objects by their interfaces.** Using an interface as the type for variables, parameters, and
  return values makes your code more flexible.
* **Item 65: Prefer interfaces to reflection.** Reflection is powerful but loses compile-time type checking, is verbose,
  and is slow. Use it only when absolutely necessary.
* **Item 66: Use native methods judiciously.** Native methods are rarely needed for performance and introduce
  platform-dependency and safety risks.
* **Item 67: Optimize judiciously.** Don't optimize prematurely. Write a good, clean program first, then use a profiler
  to find and fix performance bottlenecks.
* **Item 68: Adhere to generally accepted naming conventions.** Follow the standard Java naming conventions for
  packages, classes, methods, and fields to improve readability.

## Chapter 10: Exceptions

* **Item 69: Use exceptions only for exceptional conditions.** Do not use exceptions for ordinary control flow.
* **Item 70: Use checked exceptions for recoverable conditions and runtime exceptions for programming errors.** Checked
  exceptions force the caller to handle the error, while runtime exceptions indicate a bug.
* **Item 71: Avoid unnecessary use of checked exceptions.** Overuse of checked exceptions can make an API painful to
  use. Consider returning an `Optional` or an empty collection instead.
* **Item 72: Favor the use of standard exceptions.** Reuse standard exceptions like `IllegalArgumentException` and
  `IllegalStateException` to make your API easier to learn and use.
* **Item 73: Throw exceptions appropriate to the abstraction.** Catch lower-level exceptions and re-throw them as
  exceptions that are meaningful in the context of the higher-level abstraction (exception translation).
* **Item 74: Document all exceptions thrown by each method.** Use the `@throws` Javadoc tag to document every checked
  and unchecked exception a method can throw.
* **Item 75: Include failure-capture information in detail messages.** An exception's detail message should include the
  values of all parameters and fields that contributed to the failure.
* **Item 76: Strive for failure atomicity.** A failed method invocation should leave the object in the state it was in
  before the invocation.
* **Item 77: Don’t ignore exceptions.** An empty `catch` block is a recipe for disaster. At a minimum, log the
  exception.

## Chapter 11: Concurrency

* **Item 78: Synchronize access to shared mutable data.** Synchronization is necessary to ensure that one thread doesn't
  see an object in an inconsistent state while another is modifying it, and to ensure visibility of changes across
  threads.
* **Item 79: Avoid excessive synchronization.** Hold locks for the shortest time possible. Do not call alien methods (
  methods designed for extension or provided by clients) from within a synchronized block.
* **Item 80: Prefer executors, tasks, and streams to threads.** The `java.util.concurrent` framework provides high-level
  utilities for managing concurrency that are safer and more powerful than working directly with `Thread`s.
* **Item 81: Prefer concurrency utilities to `wait` and `notify`.** Use higher-level concurrency utilities like
  `CountDownLatch`, `Semaphore`, `CyclicBarrier`, and `BlockingQueue` instead of the error-prone `wait` and `notify`
  methods.
* **Item 82: Document thread safety.** Clearly document the thread safety guarantees of your classes using annotations
  like `@Immutable`, `@ThreadSafe`, or `@NotThreadSafe`.
* **Item 83: Use lazy initialization judiciously.** If you need lazy initialization for performance on a static field,
  use the lazy initialization holder class idiom. For instance fields, use the double-check idiom.
* **Item 84: Don’t depend on the thread scheduler.** Any program that relies on the thread scheduler for correctness or
  performance is likely to be non-portable. Do not rely on `Thread.yield` or thread priorities.

## Chapter 12: Serialization

* **Item 85: Prefer alternatives to Java serialization.** Serialization is fragile and dangerous. Prefer structured data
  representations like JSON or Protocol Buffers.
* **Item 86: Implement `Serializable` with great caution.** Implementing `Serializable` is a huge commitment. It makes
  the private implementation details of your class part of its public API.
* **Item 87: Consider using a custom serialized form.** Don't accept the default serialized form unless it's a
  reasonable description of the object's logical state.
* **Item 88: Write `readObject` methods defensively.** When deserializing an object, assume the byte stream is malicious
  and make defensive copies of all mutable components.
* **Item 90: Consider serialization proxies instead of serialized instances.** The serialization proxy pattern is a
  robust way to handle the challenges of serialization for complex objects.
* **Item 89: For instance control, prefer enum types to `readResolve`.** The `readResolve` feature can be used to
  substitute another instance for the one created during deserialization, but enum types provide this guarantee
  effortlessly.
* **Item 90: Consider serialization proxies instead of serialized instances.** The serialization proxy pattern is a
  robust way to handle the challenges of serialization for complex objects. 