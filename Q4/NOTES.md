# Q4 — Thread creation and life cycle

## Shared Work

`ScannerStations.java` uses `extends Thread` for two stations and `implements Runnable` for two stations. Every station is started with `start()`, the main thread observes `isAlive()`, and joins every worker before printing the final count. The captured output contains ten runs and their actual finish orders, a direct `run()` demonstration, priority values, a daemon demonstration, and small `System.nanoTime()` durations for each run.

```text
NEW --start()--> RUNNABLE <--> RUNNING (scheduler execution)
                     |             |
                     +--sleep()/wait()/blocked I/O--> TIMED_WAITING/WAITING/BLOCKED
                     |                                  |
                     +<------------ wake/lock acquired-+
RUNNING --run() returns or throws--> TERMINATED
```

Calling `run()` directly executes on the calling thread; it does not create/schedule a new one. `join()` blocks the caller until the target terminates (or until a requested timeout); the join-before-total ordering ensures the printed total includes completed stations. A daemon thread does not keep the JVM alive, so the main thread can finish while daemon work is still pending. Priorities are hints to the scheduler, not a correctness or ordering guarantee.

### Joint Conclusion

Finish order varies because the OS/JVM scheduler and workload interleavings vary. Correct programs use synchronization/joining for required relationships and never depend on a particular scheduling order.

### Alternative Concept and Complexity Analysis

Use a fixed-size `ExecutorService` and submit `Callable<Integer>` tasks; collect each `Future` after submission. For `n` similarly sized scans and `p` workers, ideal elapsed work is approximately O(n/p) batches, with total work O(n). A fixed pool uses O(p) Java thread objects and roughly p native stacks rather than one stack per station when n is large. `shutdown()` and `Future.get()` provide explicit lifecycle/error handling.

## Student A

Compare the ten recorded completion sequences and confirm the total is always 400.

## Student B

Explain the direct-run and daemon results using the lifecycle diagram.

## Joint Conclusion

Choose `Runnable`/tasks for work reuse and executor management; use `Thread` directly only when custom thread behavior is actually needed.
