# Q5 — Cinema seat booking race condition

## Shared Work

There are ten seat flags and 100 users, ten competing users per seat. In the unsafe version, the check, yield/sleep, set, and increment are separate operations. Two users can both observe `false` before either sets it and each increment `bookingsMade`; the output records ten actual unsynchronized runs, followed by synchronized-method and synchronized-block results. The synchronized version must sell exactly ten seats. The benchmark performs 10,000 attempts for each synchronized form and reports measured nanoseconds in `output.txt`.

The critical section is the compound check-and-book invariant. `synchronized void bookMethod` locks the current `SeatBooking` object (`this`). `synchronized (this)` in `bookBlock` uses that same intrinsic monitor, so callers of these methods serialize with one another. Static synchronized methods instead lock the `Class` object and are appropriate only for state shared statically. Do not mix unrelated lock identities when protecting the same mutable state.

For 20 screens, use one lock per screen/seat inventory so bookings on unrelated screens can proceed concurrently; one global lock would unnecessarily serialize all screens.

### Joint Conclusion

Synchronization is required around the whole check-and-update operation, not just individual reads/writes. For a multi-screen cinema, partition the state and lock at screen granularity.

### Alternative Concept and Complexity Analysis

Use `AtomicBoolean[]` and `compareAndSet(false, true)` for a single-seat claim, `ReentrantLock` for grouped invariants/lock policies, or `ConcurrentHashMap.putIfAbsent` for sparse seat IDs. An uncontended claim is O(1); under contention the wait/retry cost depends on competing users and scheduling. Atomic flags add O(s) flag storage for s seats; a concurrent map adds per-entry/node overhead. A lock-based design can block; CAS may retry. The timed method/block comparison in the program is a small local measurement, not a stable benchmark.

## Student A

Explain the interleaving where two users both pass the availability check.

## Student B

Compare intrinsic monitor locking with a per-screen lock design and review the timing results.

## Joint Conclusion

Protect each seat inventory's check/update invariant with the narrowest shared lock that maintains correctness.
