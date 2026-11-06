# Q2 — Online examination portal

## Shared Work

`ExamPortal.java` models the three layers Controller → Service → Storage. `ExamException` and its domain subclasses are checked, so each layer must handle the error or declare it with `throws`. Storage catches low-level `IOException` and wraps it as `SubmissionStorageException("Could not store " + fileName, e)`. The cause is preserved with exception chaining.

```text
Controller
  └─ ExamService.submit(...) throws ExamException
       ├─ ExamWindowClosedException
       ├─ NotRegisteredException
       ├─ FileTooLargeException
       └─ Storage.save(...) throws SubmissionStorageException
            └─ cause: IOException("Disk quota exceeded")
```

### Propagation and tests

The storage exception travels Storage → Service (`throws`) → Controller (`catch`). The student sees the stable domain message, `Could not store answer02.pdf`, and may see the underlying cause as a separate diagnostic line. `output.txt` contains the exact expected success/error/cause sequence plus `Cause when omitted: null`. `stack-trace.txt` contains the full captured `printStackTrace`, including `Caused by: java.io.IOException: Disk quota exceeded`.

| Scenario | Exception | Expected handling |
|---|---|---|
| Current time is outside exam window | ExamWindowClosedException | Controller displays exam-window message |
| Student is not registered | NotRegisteredException | Controller displays registration message |
| Upload exceeds 10,000 bytes | FileTooLargeException | Controller displays size message |
| Storage quota is exceeded | SubmissionStorageException | Controller displays domain error and cause |
| Successful upload | None | Storage prints saved file |

These are checked because submission preconditions/storage failure are expected operation outcomes that callers should acknowledge. They are not `RuntimeException` programming bugs. If the IOException is omitted from the wrapper constructor, `getCause()` returns `null`; calling `getCause().getMessage()` then throws `NullPointerException`. Re-throwing a new exception without its cause loses the lower-level type, message, original stack, and causal relationship, making diagnosis harder.

### Joint Conclusion

`SubmissionStorageException` keeps the UI independent from disk details and gives it a stable, user-appropriate domain error. The cause remains available to logs/support without exposing raw storage implementation as the UI contract.

### Alternative Concept and Complexity Analysis

If subclasses extend `RuntimeException`, remove `throws ExamException` from service/storage methods and callers need not catch or declare them. Validation and storage operations remain O(1) time and O(1) extra space per request. Runtime exceptions still capture a stack trace by default; that construction/capture has allocation and CPU cost, and chaining keeps the cause object graph reachable (including its stack) until the wrapper becomes unreachable. `System.nanoTime()` timing of repeated exception construction versus a status return is recorded in `output.txt`; it is a local micro-measurement, not a universal performance claim.

## Student A

Trace a quota failure through all three layers and check that the original cause is retained.

## Student B

Review the validation exceptions and identify which conditions are checked before storage is touched.

## Joint Conclusion

Use domain-specific checked exceptions at a layer boundary, and preserve the original cause for diagnostics.
