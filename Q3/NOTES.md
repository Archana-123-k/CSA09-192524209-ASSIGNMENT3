# Q3 — Daily sales report and resource safety

## Shared Work

Each store CSV reader is a file resource. It must close after processing, on both success and failure. The manual version closes on its success path only: a malformed row throws before `close()`, and its `finally` merely logs, so one reader remains open. This intentional defect is isolated to the comparison; the demo explicitly closes the tracked leak after reporting it. Try-with-resources closes even when parsing fails.

The measured successful reports are identical (`rows=4, total=250`). The failure input causes the manual open-reader counter to remain at 1 and the try-with-resources counter to be 0. `output.txt` records the failure filename, output from both variants, and local `System.nanoTime()` timings for report generation. A two-resource example shows close order is reverse declaration order. The suppressed-exception example shows the read exception remains primary and the close exception is available through `getSuppressed()`.

| Resource | Acquired at | Must close |
|---|---|---|
| Store `CountingReader` | Per input file | After each file, on every path |
| FileReader wrapped by BufferedReader | CountingReader construction | By closing the outer reader |

Use the debugger to set breakpoints in `read`, manual `close()`, and `finally`; step through the malformed row, inspect `openCount`, then repeat with the try-with-resources block. Add your own debugger screenshots; they are not generated here.

### Joint Conclusion

Try-with-resources is not enough when resources are not `AutoCloseable`, when a resource is acquired outside the resource declaration, or when cleanup must include application-specific actions beyond `close()`. Those cases still require disciplined ownership and explicit cleanup.

### Alternative Concept and Complexity Analysis

Alternative 1: close in `finally` using a null check (or a nested `try` to preserve the primary exception). Alternative 2: do not close at all. For `f` files, processing/closing time is O(f); with sequential per-file scope, normal maximum open handles are O(1). On a failure, the intentionally defective manual version retains one handle per failed file, so over f failures the leak can reach O(f). Correct try-with-resources/manual-finally stays O(1).

## Student A

Step through the malformed store file and capture the leaked-reader count in the debugger.

## Student B

Verify report equality and inspect the suppressed exception in the debugger.

## Joint Conclusion

Prefer try-with-resources for lexical ownership; it reduces cleanup omissions and preserves suppressed close failures.
