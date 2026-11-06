# Q6 — Deadlock and inter-thread communication

## Shared Work

### Part A: account transfers

The first transfer demo deliberately acquires two account monitors in opposite orders, with a barrier and a delay between locks; daemon workers let the test report a timed-out deadlock without preventing JVM exit. The second version locks the lower account id first, then the higher, and completes both transfers.

The four Coffman conditions are mutual exclusion (intrinsic locks are exclusive), hold-and-wait (a thread holds one account while requesting another), no preemption (a monitor is not forcibly taken away), and circular wait (each transfer waits for the account held by the other). Consistent account-id ordering prevents circular wait.

### Part B: bounded buffer

`TokenCounter` has a five-element circular buffer. `wait()` is called only while holding its intrinsic monitor; it releases that monitor while waiting and reacquires it before returning. The `while` predicate handles spurious wakeups and competition: another consumer/producer may change the state before a notified thread reacquires the lock. `notifyAll()` wakes all waiters so the ones whose condition is now true can proceed; `notify()` can wake a waiter for the wrong condition and leave eligible waiters asleep. `sleep()` pauses without releasing a monitor; `wait()` releases the monitor.

The output logs each buffer size after each operation, and the correct run totals 210. A small `System.nanoTime()` duration is recorded for the bounded-buffer run. Three `if`-instead-of-`while` experiments record whether results were wrong, workers failed, or timed out; their result is runtime/scheduler-dependent.

### Joint Conclusion

Both problems require threads to coordinate over shared state. Consistent lock ordering breaks deadlock cycles; condition loops plus notification ensure bounded-buffer operations wait only while their required state is unavailable and proceed when it changes.

### Alternative Concept and Complexity Analysis

Use `ArrayBlockingQueue<Integer>(5)` for the bounded buffer; put/take encapsulate the condition loops and locking, with O(1) operation time and O(5) storage. For transfers, `ReentrantLock.tryLock(timeout)` can avoid waiting forever and enable rollback/retry, at the cost of more control-flow/error paths; two account locks still need safe ordering or timeout handling. The blocking queue is generally less error-prone for producer-consumer code.

## Student A

Explain the timed deadlock observation and why the ordered version has no cycle.

## Student B

Inspect the buffer-size trace and compare `while` and `if` experiment results.

## Joint Conclusion

Use the highest-level concurrency primitive that directly expresses the required coordination, while preserving explicit progress and failure semantics.
