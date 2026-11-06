# Q1 — ATM withdrawal failures

## Shared Work

The program is in `ATM.java`; its default balance is 1000. `inputs/` contains reproducible console-input cases. `--reader-check` deliberately looks for an absent card-reader file so the checked `FileNotFoundException` path can be tested.

### Throwable hierarchy

```text
Throwable
├── Error
└── Exception
    ├── RuntimeException (unchecked)
    │   ├── IllegalArgumentException (negative amount)
    │   ├── NumberFormatException (bad account text)
    │   └── InputMismatchException (bad amount token)
    ├── IOException
    │   └── FileNotFoundException (missing reader file)
    └── InsufficientFundsException (checked)
```

### Failure points

| Failure point | Exception | Checked? | Where caught |
|---|---|---|---|
| Account text parsed as a long | NumberFormatException | No | numeric multi-catch |
| Amount token read by Scanner | InputMismatchException | No | numeric multi-catch |
| Negative withdrawal | IllegalArgumentException | No | amount catch |
| Amount exceeds balance | InsufficientFundsException | Yes | funds catch |
| Card-reader file cannot be opened | FileNotFoundException | Yes | reader-check catch |

The numeric multi-catch comes before `IllegalArgumentException`, because `NumberFormatException` is a subclass of it. A broad parent catch before a more specific catch makes the later catch unreachable and is a compile-time error. `finally` runs after normal completion and caught/uncaught exceptions once control leaves the try; it is not guaranteed after forced JVM termination, process kill, or power loss.

| Input / test | Catch/output path | Finally ran? |
|---|---|---|
| `inputs/approved.txt` | approved | Yes |
| `inputs/letters-account.txt` | numeric multi-catch: NumberFormatException | Yes |
| `inputs/letters-amount.txt` | numeric multi-catch: InputMismatchException | Yes |
| `inputs/negative.txt` | IllegalArgumentException catch | Yes |
| `inputs/over-balance.txt` | InsufficientFundsException catch | Yes |
| `--reader-check` | FileNotFoundException catch | Yes |

The real uncaught stack trace in `output.txt` reads line by line: `Exception in thread "main"` identifies the uncaught exception on the main thread; `IllegalStateException: Demonstration...` gives its type and message; `at ATM.main(ATM.java:11)` identifies the exact throw site. The compiler diagnostic in `broad-catch-compile-error.txt` says `IOException has already been caught`: the `catch (Exception)` clause catches all IOExceptions, so the subsequent `catch (IOException)` is unreachable.

### Joint Conclusion

An ATM should not use only `catch (Exception e)` around the transaction: it hides which expected failure occurred and risks treating programming defects as routine declines. A broad catch is acceptable at a process/UI boundary for logging and controlled shutdown, provided known exceptions are handled first and unexpected failures are not mislabeled. It loses type-specific recovery and useful user messaging.

### Alternative Concept and Complexity Analysis

Alternative: check `Scanner.hasNextInt()` before reading the amount and return `-1` for invalid input. Both approaches are O(1) time and O(1) additional space. The code-return approach requires explicit checks at every input and status propagation; exception-based code is clearer for exceptional control flow but creating/throwing stack-bearing exceptions is materially more expensive than returning a primitive status. `ATM` prints a `System.nanoTime()` duration for the successful withdrawal path in the run log; this tiny single-run figure is illustrative, not a benchmark.

## Student A

Complete the test-case review and verify the exception/catch mapping against the executed output.

## Student B

Review the throwable hierarchy and explain why checked insufficient funds differs from unchecked invalid input.

## Joint Conclusion

Use specific exception types for recoverable transaction failures and always clean up ordinary control-flow exits with `finally`.
