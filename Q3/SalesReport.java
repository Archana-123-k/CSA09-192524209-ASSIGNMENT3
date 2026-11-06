import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class SalesReport {
    public static void main(String[] args) {
        List<Path> goodFiles = Arrays.asList(
                Path.of("inputs", "store-a.csv"), Path.of("inputs", "store-b.csv"));
        long start = System.nanoTime();
        String manual = report(goodFiles, false);
        System.out.println("Manual report elapsed ns: " + (System.nanoTime() - start));
        start = System.nanoTime();
        String automatic = report(goodFiles, true);
        System.out.println("Try-with-resources report elapsed ns: " + (System.nanoTime() - start));
        System.out.println("Manual report: " + manual);
        System.out.println("Try-with-resources report: " + automatic);
        System.out.println("Reports equal: " + manual.equals(automatic));

        List<Path> badFile = List.of(Path.of("inputs", "store-bad.csv"));
        report(badFile, false);
        System.out.println("Open readers after manual failure: " + CountingReader.openCount);
        CountingReader.closeLeaked();
        report(badFile, true);
        System.out.println("Open readers after try-with-resources failure: " + CountingReader.openCount);
        CountingReader.closeLeaked();
        closeOrderDemo();
        suppressedDemo();
    }

    private static String report(List<Path> files, boolean automatic) {
        long total = 0;
        int rows = 0;
        for (Path path : files) {
            if (automatic) {
                try (CountingReader reader = new CountingReader(path)) {
                    long[] values = read(reader);
                    total += values[0];
                    rows += (int) values[1];
                } catch (IOException | NumberFormatException e) {
                    System.out.println("Failed file: " + path + " (" + e.getClass().getSimpleName() + ")");
                }
            } else {
                CountingReader reader = null;
                try {
                    reader = new CountingReader(path);
                    long[] values = read(reader);
                    total += values[0];
                    rows += (int) values[1];
                    reader.close();
                } catch (IOException | NumberFormatException e) {
                    System.out.println("Failed file: " + path + " (" + e.getClass().getSimpleName() + ")");
                } finally {
                    System.out.println("Manual finally reached for " + path);
                }
            }
        }
        return "rows=" + rows + ", total=" + total;
    }

    private static long[] read(BufferedReader reader) throws IOException {
        long total = 0;
        int rows = 0;
        String line;
        while ((line = reader.readLine()) != null) {
            String[] fields = line.split(",");
            if (fields.length != 2) {
                throw new NumberFormatException("expected item,amount");
            }
            total += Long.parseLong(fields[1]);
            rows++;
        }
        return new long[] {total, rows};
    }

    private static void closeOrderDemo() {
        try (NamedResource first = new NamedResource("first");
             NamedResource second = new NamedResource("second")) {
            System.out.println("Opened first then second");
        }
    }

    private static void suppressedDemo() {
        try (AutoCloseable resource = () -> {
            throw new IOException("close failed");
        }) {
            throw new IOException("read failed");
        } catch (Exception e) {
            System.out.println("Primary exception: " + e.getMessage());
            System.out.println("Suppressed exception: " + e.getSuppressed()[0].getMessage());
        }
    }
}

class CountingReader extends BufferedReader {
    static int openCount;
    private static final List<CountingReader> leaked = new ArrayList<>();
    private boolean closed;

    CountingReader(Path path) throws IOException {
        super(new FileReader(path.toFile()));
        openCount++;
        leaked.add(this);
    }

    @Override
    public void close() throws IOException {
        if (!closed) {
            super.close();
            closed = true;
            openCount--;
            leaked.remove(this);
        }
    }

    static void closeLeaked() {
        for (CountingReader reader : new ArrayList<>(leaked)) {
            try {
                reader.close();
            } catch (IOException e) {
                throw new IllegalStateException("Cleanup of demonstration leak failed", e);
            }
        }
    }
}

class NamedResource implements AutoCloseable {
    private final String name;

    NamedResource(String name) {
        this.name = name;
    }

    @Override
    public void close() {
        System.out.println("Closed " + name);
    }
}
