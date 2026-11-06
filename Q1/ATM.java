import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.InputMismatchException;
import java.util.Scanner;

public class ATM {
    private static final int BALANCE = 1000;

    public static void main(String[] args) {
        if (args.length > 0 && args[0].equals("--uncaught")) {
            throw new IllegalStateException("Demonstration of an uncaught failure");
        }
        if (args.length > 0 && args[0].equals("--reader-check")) {
            checkReader();
            return;
        }

        Scanner input = new Scanner(System.in);
        try {
            System.out.print("Account number: ");
            String account = input.nextLine();
            Long.parseLong(account);
            System.out.print("Withdrawal amount: ");
            int amount = input.nextInt();
            long start = System.nanoTime();
            withdraw(amount);
            long elapsed = System.nanoTime() - start;
            System.out.println("Withdrawal approved. Remaining balance: " + (BALANCE - amount));
            System.out.println("Withdrawal validation ns: " + elapsed);
            compareExceptionalAndStatusPaths();
        } catch (InputMismatchException | NumberFormatException e) {
            System.out.println("Invalid numeric input: " + e.getClass().getSimpleName());
        } catch (IllegalArgumentException e) {
            System.out.println("Invalid amount: " + e.getMessage());
        } catch (InsufficientFundsException e) {
            System.out.println("Declined: " + e.getMessage());
        } finally {
            System.out.println("Finally: transaction cleanup completed");
            input.close();
        }
    }

    private static void withdraw(int amount) throws InsufficientFundsException {
        if (amount < 0) {
            throw new IllegalArgumentException("amount must not be negative");
        }
        if (amount > BALANCE) {
            throw new InsufficientFundsException("requested " + amount + ", available " + BALANCE);
        }
    }

    private static void checkReader() {
        try (FileReader ignored = new FileReader("missing-card-reader.cfg")) {
            System.out.println("Card reader ready");
        } catch (FileNotFoundException e) {
            System.out.println("Card-reader failure: " + e.getClass().getSimpleName());
        } catch (Exception e) {
            System.out.println("Card-reader failure: " + e.getClass().getSimpleName());
        } finally {
            System.out.println("Finally: reader check completed");
        }
    }

    private static void compareExceptionalAndStatusPaths() {
        int sink = 0;
        long start = System.nanoTime();
        for (int i = 0; i < 1_000; i++) {
            try {
                throw new IllegalArgumentException("invalid");
            } catch (IllegalArgumentException e) {
                sink += e.getMessage().length();
            }
        }
        long exceptionNanos = System.nanoTime() - start;
        start = System.nanoTime();
        for (int i = 0; i < 1_000; i++) {
            int status = -1;
            if (status < 0) {
                sink++;
            }
        }
        long statusNanos = System.nanoTime() - start;
        System.out.println("1,000 invalid-input paths ns: exceptions=" + exceptionNanos
                + ", status checks=" + statusNanos + ", sink=" + sink);
    }
}

class InsufficientFundsException extends Exception {
    InsufficientFundsException(String message) {
        super(message);
    }
}
