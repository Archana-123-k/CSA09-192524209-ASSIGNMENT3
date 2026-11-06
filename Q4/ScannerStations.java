import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ScannerStations {
    public static void main(String[] args) throws InterruptedException {
        directRunDemo();
        priorityDemo();
        for (int run = 1; run <= 10; run++) {
            runStations(run);
        }
        daemonDemo();
    }

    private static void runStations(int run) throws InterruptedException {
        List<String> finishingOrder = Collections.synchronizedList(new ArrayList<>());
        ThreadStation first = new ThreadStation("station-1", finishingOrder);
        ThreadStation second = new ThreadStation("station-2", finishingOrder);
        RunnableStation third = new RunnableStation("station-3", finishingOrder);
        RunnableStation fourth = new RunnableStation("station-4", finishingOrder);
        Thread[] stations = {
                first, second, new Thread(third, "station-3"), new Thread(fourth, "station-4")
        };
        long start = System.nanoTime();
        for (Thread station : stations) {
            station.start();
        }
        System.out.println("Run " + run + " alive immediately after start: " + stations[0].isAlive());
        for (Thread station : stations) {
            station.join();
        }
        int total = first.getCount() + second.getCount() + third.getCount() + fourth.getCount();
        System.out.println("Run " + run + " station counts: " + first.getCount() + ", "
                + second.getCount() + ", " + third.getCount() + ", " + fourth.getCount());
        System.out.println("Run " + run + " finish order: " + String.join(", ", finishingOrder));
        System.out.println("Run " + run + " total scans: " + total);
        System.out.println("Run " + run + " elapsed ns: " + (System.nanoTime() - start));
    }

    private static void directRunDemo() {
        List<String> order = new ArrayList<>();
        ThreadStation station = new ThreadStation("direct", order);
        station.run();
        System.out.println("Direct run executed on: " + Thread.currentThread().getName()
                + "; worker name remains: " + station.getName());
    }

    private static void priorityDemo() throws InterruptedException {
        RunnableStation low = new RunnableStation("priority-low", new ArrayList<>());
        RunnableStation high = new RunnableStation("priority-high", new ArrayList<>());
        Thread a = new Thread(low, "priority-low");
        Thread b = new Thread(high, "priority-high");
        a.setPriority(Thread.MIN_PRIORITY);
        b.setPriority(Thread.MAX_PRIORITY);
        a.start();
        b.start();
        a.join();
        b.join();
        System.out.println("Priority experiment completed (low=" + a.getPriority()
                + ", high=" + b.getPriority() + "); priority is a scheduling hint");
    }

    private static void daemonDemo() throws InterruptedException {
        Thread daemon = new Thread(() -> {
            try {
                Thread.sleep(2_000);
                System.out.println("Daemon finished (normally not printed before JVM exit)");
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }, "daemon-station");
        daemon.setDaemon(true);
        daemon.start();
        System.out.println("Daemon flag: " + daemon.isDaemon() + "; main ends without joining it");
    }
}

interface CountingStation {
    int getCount();
}

class ThreadStation extends Thread implements CountingStation {
    private final List<String> finishOrder;
    private int count;

    ThreadStation(String name, List<String> finishOrder) {
        super(name);
        this.finishOrder = finishOrder;
    }

    @Override
    public void run() {
        count = scan();
        finishOrder.add(getName());
    }

    private int scan() {
        int scans = 0;
        for (int i = 0; i < 100; i++) {
            scans++;
            if (i % 7 == 0) {
                Thread.yield();
            }
        }
        return scans;
    }

    @Override
    public int getCount() {
        return count;
    }
}

class RunnableStation implements Runnable, CountingStation {
    private final String name;
    private final List<String> finishOrder;
    private int count;

    RunnableStation(String name, List<String> finishOrder) {
        this.name = name;
        this.finishOrder = finishOrder;
    }

    @Override
    public void run() {
        for (int i = 0; i < 100; i++) {
            count++;
            if (i % 7 == 0) {
                Thread.yield();
            }
        }
        finishOrder.add(name);
    }

    @Override
    public int getCount() {
        return count;
    }
}
