import java.util.concurrent.atomic.AtomicInteger;

public class SeatBooking {
    private final boolean[] booked = new boolean[10];
    private int bookingsMade;

    void bookUnsafe(int seat) {
        if (!booked[seat]) {
            Thread.yield();
            try {
                Thread.sleep(1);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
            booked[seat] = true;
            bookingsMade++;
        }
    }

    synchronized void bookMethod(int seat) {
        if (!booked[seat]) {
            booked[seat] = true;
            bookingsMade++;
        }
    }

    void bookBlock(int seat) {
        synchronized (this) {
            if (!booked[seat]) {
                booked[seat] = true;
                bookingsMade++;
            }
        }
    }

    int getBookingsMade() {
        return bookingsMade;
    }

    static int runRace(boolean safe) throws InterruptedException {
        SeatBooking booking = new SeatBooking();
        Thread[] users = new Thread[100];
        for (int user = 0; user < users.length; user++) {
            int seat = user / 10;
            users[user] = new Thread(() -> {
                if (safe) {
                    booking.bookMethod(seat);
                } else {
                    booking.bookUnsafe(seat);
                }
            });
        }
        for (Thread user : users) {
            user.start();
        }
        for (Thread user : users) {
            user.join();
        }
        return booking.getBookingsMade();
    }

    static long benchmark(boolean block) {
        SeatBooking booking = new SeatBooking();
        AtomicInteger completed = new AtomicInteger();
        long start = System.nanoTime();
        Thread[] workers = new Thread[10];
        for (int worker = 0; worker < workers.length; worker++) {
            final int offset = worker;
            workers[worker] = new Thread(() -> {
                for (int i = 0; i < 1_000; i++) {
                    int seat = (i + offset) % 10;
                    if (block) {
                        booking.bookBlock(seat);
                    } else {
                        booking.bookMethod(seat);
                    }
                    completed.incrementAndGet();
                }
            });
        }
        for (Thread worker : workers) {
            worker.start();
        }
        for (Thread worker : workers) {
            try {
                worker.join();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("Benchmark interrupted", e);
            }
        }
        long elapsed = System.nanoTime() - start;
        if (completed.get() != 10_000) {
            throw new IllegalStateException("Expected 10000 attempts");
        }
        return elapsed;
    }

    public static void main(String[] args) throws InterruptedException {
        for (int run = 1; run <= 10; run++) {
            System.out.println("Unsynchronized run " + run + ": Seats sold: " + runRace(false));
        }
        System.out.println("Synchronized method: Seats sold: " + runRace(true));
        SeatBooking blockBooking = new SeatBooking();
        for (int seat = 0; seat < 10; seat++) {
            blockBooking.bookBlock(seat);
        }
        System.out.println("Synchronized block: Seats sold: " + blockBooking.getBookingsMade());
        System.out.println("10,000 synchronized-method attempts ns: " + benchmark(false));
        System.out.println("10,000 synchronized-block attempts ns: " + benchmark(true));
    }
}
