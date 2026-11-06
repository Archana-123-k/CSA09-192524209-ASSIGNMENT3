# Q7 — Concurrent fault-tolerant result processor

## Shared Work

### Stage 1 — Parallel tasks, shared data, failures, requirements

`generate_scores.ps1` creates the deterministic benchmark dataset: 10,000 candidate IDs, with IDs 0–99 missing, 100–199 corrupt, and the remaining 9,800 valid score files. The generated directory is ignored by Git and can be recreated with the included script. The hand-check set has 10 candidates: six valid, two corrupt, and two missing.

The processor uses `ExecutorService`, one `Callable<ScoreResult>` per candidate, and `Future.get()` to collect completion. Each task translates expected file/parse errors into its own result, so one bad input does not abort the run. `ConcurrentLinkedQueue` is thread-safe for collected results; results are aggregated after all futures have been retrieved. The measured requirements are a complete 10,000-candidate summary and three elapsed-time samples each for pool sizes 1, 2, 4, and 8.

### Stage 2 — Class diagram and pseudocode

```text
ScoreProcessor -> ExecutorService -> Callable<ScoreResult>
                                      └─ catches file/parse failures
Future<ScoreResult> -> ConcurrentLinkedQueue -> Summary

for each candidate:
    submit callable that returns OK, MISSING, or CORRUPT
for each future:
    get result; enqueue it (propagate unexpected task failure)
after all results:
    count success and reasons; report elapsed time
```

### Stage 3 — Measurements and unhandled failures

The captured `output.txt` contains the checked 10-file result, all pool-size averages as an ASCII graph, and the 10,000-candidate summary. `timings.csv` contains the 12 raw timing rows. Expected input errors are represented in results; any unexpected exception escaping a task is surfaced by `Future.get()` as `ExecutionException`, rather than silently counted as success. The 10-file hand check must report 6 successes, 2 corrupt, 2 missing.

### Stage 4 — Recommendation and limitation

This run measured average elapsed times of 1686.663 ms (1 worker), 710.653 ms (2), 416.756 ms (4), and 317.438 ms (8), as shown in `timings.csv` and `output.txt`. Eight workers were fastest for this local workload, so that is the measured recommendation here—not a universal value. File I/O is storage- and operating-system-dependent; beyond the device's useful concurrency, more workers can increase seek contention, queueing, and context switching and may not help. Completion/correctness is guaranteed by submitting all candidates and successfully calling `get()` on every future before aggregation; each candidate produces exactly one `ScoreResult`. A limitation is that this simple example holds one future and one result per candidate in memory, so a streaming/batched design is preferable for much larger inputs.

### Joint Conclusion

Isolated callable tasks and per-file result objects prevent expected bad inputs from terminating the batch. Future retrieval establishes completion before aggregation, and the concurrent collection safely stores results from the worker-driven pipeline.

### Alternative Concept and Complexity Analysis

`parallelStream()` can reduce boilerplate but offers less explicit executor/lifecycle and per-task failure control. One thread per file would create 10,000 threads, with memory roughly proportional to thread count times native stack reservation, and likely severe scheduling overhead. With a pool of p workers and n similarly sized files, ideal work is about O(n/p) batches (total O(n)), while the pool uses O(p) threads; result/future storage here is O(n).

## Student A

Verify the known 10-file counts and inspect the failure-reason results.

## Student B

Review the CSV and timing graph, then recommend a pool size based on measured results.

## Joint Conclusion

Use a bounded pool and explicit result values for expected per-file failures; size it empirically for the target storage and workload.
