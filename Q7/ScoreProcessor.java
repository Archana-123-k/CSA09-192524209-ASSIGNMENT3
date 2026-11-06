import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class ScoreProcessor {
    private static final int[] POOL_SIZES = {1, 2, 4, 8};

    public static void main(String[] args) throws Exception {
        boolean benchmark = args.length > 0 && args[0].equals("--benchmark");
        Path directory = Path.of(args.length > (benchmark ? 1 : 0)
                ? args[benchmark ? 1 : 0] : "inputs/hand-check");
        if (benchmark) {
            benchmark(directory);
        } else {
            Summary summary = process(directory, 10, 4);
            printSummary("Hand-checked 10-file sample", summary);
        }
    }

    private static void benchmark(Path directory) throws Exception {
        StringBuilder csv = new StringBuilder("pool_size,run,successes,failures,elapsed_ns\n");
        long[] totals = new long[POOL_SIZES.length];
        Summary firstSummary = null;
        for (int poolIndex = 0; poolIndex < POOL_SIZES.length; poolIndex++) {
            int poolSize = POOL_SIZES[poolIndex];
            for (int run = 1; run <= 3; run++) {
                Summary summary = process(directory, 10_000, poolSize);
                if (firstSummary == null) {
                    firstSummary = summary;
                }
                totals[poolIndex] += summary.elapsedNanos;
                csv.append(poolSize).append(',').append(run).append(',')
                        .append(summary.successes).append(',').append(summary.failures).append(',')
                        .append(summary.elapsedNanos).append('\n');
            }
        }
        Files.writeString(Path.of("timings.csv"), csv.toString());
        printSummary("10,000-candidate result (first benchmark run)", firstSummary);
        System.out.println("Pool | Average ms | Relative bar (lower is better)");
        long smallest = Long.MAX_VALUE;
        long[] averages = new long[POOL_SIZES.length];
        for (int i = 0; i < totals.length; i++) {
            averages[i] = totals[i] / 3;
            smallest = Math.min(smallest, averages[i]);
        }
        for (int i = 0; i < POOL_SIZES.length; i++) {
            int barLength = (int) Math.max(1, Math.round(30.0 * smallest / averages[i]));
            System.out.printf("%4d | %10.3f | %s%n", POOL_SIZES[i],
                    averages[i] / 1_000_000.0, "#".repeat(barLength));
        }
        System.out.println("Raw measurements: timings.csv (3 runs per pool size)");
    }

    private static Summary process(Path directory, int candidateCount, int poolSize)
            throws InterruptedException, ExecutionException {
        ExecutorService executor = Executors.newFixedThreadPool(poolSize);
        ConcurrentLinkedQueue<ScoreResult> results = new ConcurrentLinkedQueue<>();
        List<Future<ScoreResult>> futures = new ArrayList<>(candidateCount);
        long start = System.nanoTime();
        try {
            for (int i = 0; i < candidateCount; i++) {
                final int id = i;
                Callable<ScoreResult> task = () -> readScore(directory.resolve("score-" + id + ".txt"), id);
                futures.add(executor.submit(task));
            }
            for (Future<ScoreResult> future : futures) {
                ScoreResult result = future.get();
                results.add(result);
            }
        } finally {
            executor.shutdown();
        }
        long elapsed = System.nanoTime() - start;
        int successes = 0;
        int missing = 0;
        int corrupt = 0;
        for (ScoreResult result : results) {
            if (result.success) {
                successes++;
            } else if (result.reason.equals("MISSING")) {
                missing++;
            } else {
                corrupt++;
            }
        }
        return new Summary(candidateCount, successes, missing, corrupt, elapsed);
    }

    private static ScoreResult readScore(Path file, int id) {
        try {
            String text = Files.readString(file).trim();
            int score = Integer.parseInt(text);
            if (score < 0 || score > 100) {
                return new ScoreResult(id, false, "CORRUPT");
            }
            return new ScoreResult(id, true, "OK");
        } catch (java.nio.file.NoSuchFileException e) {
            return new ScoreResult(id, false, "MISSING");
        } catch (IOException | NumberFormatException e) {
            return new ScoreResult(id, false, "CORRUPT");
        }
    }

    private static void printSummary(String title, Summary summary) {
        System.out.println(title + ": candidates=" + summary.candidates + ", successes="
                + summary.successes + ", missing=" + summary.missing + ", corrupt=" + summary.corrupt
                + ", failures=" + summary.failures + ", elapsed_ms="
                + String.format("%.3f", summary.elapsedNanos / 1_000_000.0));
    }

    private static final class ScoreResult {
        private final int id;
        private final boolean success;
        private final String reason;

        private ScoreResult(int id, boolean success, String reason) {
            this.id = id;
            this.success = success;
            this.reason = reason;
        }
    }

    private static final class Summary {
        private final int candidates;
        private final int successes;
        private final int missing;
        private final int corrupt;
        private final long elapsedNanos;
        private final int failures;

        private Summary(int candidates, int successes, int missing, int corrupt, long elapsedNanos) {
            this.candidates = candidates;
            this.successes = successes;
            this.missing = missing;
            this.corrupt = corrupt;
            this.elapsedNanos = elapsedNanos;
            this.failures = missing + corrupt;
        }
    }
}
