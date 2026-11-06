import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CyclicBarrier;

public class ConcurrencyDemos {
    public static void main(String[] args) throws InterruptedException {
        deadlockDemo();
        orderedTransferDemo();
        TokenCounter counter = new TokenCounter(true);
        long start = System.nanoTime();
        System.out.println("Sum of tokens collected: " + runTokenTest(counter, true) + " (expected 210)");
        System.out.println("Bounded-buffer run elapsed ns: " + (System.nanoTime() - start));
        for (int run = 1; run <= 3; run++) {
            runIfTrial(run);
        }
    }

    private static void deadlockDemo() throws InterruptedException {
        Account first = new Account(1, 100);
        Account second = new Account(2, 100);
        CyclicBarrier barrier = new CyclicBarrier(2);
        Thread a = new Thread(() -> first.transferUnordered(second, barrier), "transfer-1-to-2");
        Thread b = new Thread(() -> second.transferUnordered(first, barrier), "transfer-2-to-1");
        a.setDaemon(true);
        b.setDaemon(true);
        a.start();
        b.start();
        a.join(500);
        b.join(500);
        System.out.println("Opposite lock order deadlock detected: " + (a.isAlive() && b.isAlive()));
    }

    private static void orderedTransferDemo() throws InterruptedException {
        Account first = new Account(1, 100);
        Account second = new Account(2, 100);
        Thread a = new Thread(() -> first.transferOrdered(second, 10));
        Thread b = new Thread(() -> second.transferOrdered(first, 10));
        a.start();
        b.start();
        a.join();
        b.join();
        System.out.println("Ordered transfer completed; total balance: " + (first.balance + second.balance));
    }

    private static int runTokenTest(TokenCounter counter, boolean logSize) throws InterruptedException {
        int[] sum = {0};
        Thread producer = new Thread(() -> {
            for (int token = 1; token <= 20; token++) {
                try {
                    counter.put(token);
                    if (logSize) {
                        System.out.println("Buffer size after put: " + counter.size());
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }
        }, "producer");
        Thread consumer = new Thread(() -> {
            for (int i = 0; i < 20; i++) {
                try {
                    sum[0] += counter.take();
                    if (logSize) {
                        System.out.println("Buffer size after take: " + counter.size());
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }
        }, "consumer");
        producer.start();
        consumer.start();
        producer.join();
        consumer.join();
        return sum[0];
    }

    private static void runIfTrial(int run) throws InterruptedException {
        TokenCounter counter = new TokenCounter(false);
        List<Integer> received = Collections.synchronizedList(new ArrayList<>());
        Thread producer = new Thread(() -> {
            for (int token = 1; token <= 20; token++) {
                try {
                    counter.put(token);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }
        });
        Runnable consumeTen = () -> {
            for (int i = 0; i < 10; i++) {
                try {
                    received.add(counter.take());
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                } catch (RuntimeException e) {
                    System.out.println("if-trial worker exception: " + e.getClass().getSimpleName());
                    return;
                }
            }
        };
        Thread firstConsumer = new Thread(consumeTen);
        Thread secondConsumer = new Thread(consumeTen);
        producer.setDaemon(true);
        firstConsumer.setDaemon(true);
        secondConsumer.setDaemon(true);
        producer.start();
        firstConsumer.start();
        secondConsumer.start();
        producer.join(500);
        firstConsumer.join(500);
        secondConsumer.join(500);
        int sum = received.stream().mapToInt(Integer::intValue).sum();
        String status = producer.isAlive() || firstConsumer.isAlive() || secondConsumer.isAlive()
                ? "HANG" : (received.size() == 20 && sum == 210 ? "correct" : "incorrect");
        System.out.println("if-trial " + run + ": " + status + ", received=" + received.size()
                + ", sum=" + sum);
    }
}

class Account {
    final int id;
    int balance;

    Account(int id, int balance) {
        this.id = id;
        this.balance = balance;
    }

    void transferUnordered(Account other, CyclicBarrier barrier) {
        try {
            barrier.await();
            synchronized (this) {
                Thread.sleep(100);
                synchronized (other) {
                    balance--;
                    other.balance++;
                }
            }
        } catch (Exception e) {
            Thread.currentThread().interrupt();
        }
    }

    void transferOrdered(Account other, int amount) {
        Account lower = id < other.id ? this : other;
        Account higher = lower == this ? other : this;
        synchronized (lower) {
            synchronized (higher) {
                balance -= amount;
                other.balance += amount;
            }
        }
    }
}

class TokenCounter {
    private final int[] buffer = new int[5];
    private final boolean useWhile;
    private int in;
    private int out;
    private int count;

    TokenCounter(boolean useWhile) {
        this.useWhile = useWhile;
    }

    synchronized void put(int token) throws InterruptedException {
        if (useWhile) {
            while (count == buffer.length) {
                wait();
            }
        } else if (count == buffer.length) {
            wait();
        }
        buffer[in] = token;
        in = (in + 1) % buffer.length;
        count++;
        if (count < 0 || count > buffer.length) {
            throw new IllegalStateException("Buffer count out of range: " + count);
        }
        notifyAll();
    }

    synchronized int take() throws InterruptedException {
        if (useWhile) {
            while (count == 0) {
                wait();
            }
        } else if (count == 0) {
            wait();
        }
        int token = buffer[out];
        out = (out + 1) % buffer.length;
        count--;
        if (count < 0 || count > buffer.length) {
            throw new IllegalStateException("Buffer count out of range: " + count);
        }
        notifyAll();
        return token;
    }

    synchronized int size() {
        return count;
    }
}
