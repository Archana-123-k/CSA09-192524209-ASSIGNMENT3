import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class ExamPortal {
    public static void main(String[] args) {
        Controller controller = new Controller(new ExamService(new Storage()));
        if (args.length > 0 && args[0].equals("--stack")) {
            try {
                new ExamService(new Storage()).submit("answer02.pdf", true, true, 200);
            } catch (ExamException e) {
                e.printStackTrace(System.out);
            }
            return;
        }
        controller.submit("answer01.pdf", true, true, 200);
        controller.submit("answer02.pdf", true, true, 200);
        SubmissionStorageException withoutCause =
                new SubmissionStorageException("Could not store answer03.pdf");
        System.out.println("Cause when omitted: " + withoutCause.getCause());
        benchmarkExceptionCost();
    }

    private static void benchmarkExceptionCost() {
        int sink = 0;
        long start = System.nanoTime();
        for (int i = 0; i < 10_000; i++) {
            sink += new SubmissionStorageException("failure").getMessage().length();
        }
        long exceptionNanos = System.nanoTime() - start;
        start = System.nanoTime();
        for (int i = 0; i < 10_000; i++) {
            sink += -1;
        }
        long statusNanos = System.nanoTime() - start;
        System.out.println("10,000 exception objects ns: " + exceptionNanos
                + "; status-code loop ns: " + statusNanos + "; sink=" + sink);
    }
}

class Controller {
    private final ExamService service;

    Controller(ExamService service) {
        this.service = service;
    }

    void submit(String fileName, boolean registered, boolean windowOpen, long size) {
        try {
            service.submit(fileName, registered, windowOpen, size);
        } catch (ExamException e) {
            System.out.println("Error: " + e.getMessage());
            if (e.getCause() != null) {
                System.out.println("Cause: " + e.getCause().getMessage());
            }
        }
    }
}

class ExamService {
    private final Storage storage;

    ExamService(Storage storage) {
        this.storage = storage;
    }

    void submit(String fileName, boolean registered, boolean windowOpen, long size)
            throws ExamException {
        if (!windowOpen) {
            throw new ExamWindowClosedException("Exam window is closed");
        }
        if (!registered) {
            throw new NotRegisteredException("Student is not registered");
        }
        if (size > 10_000) {
            throw new FileTooLargeException("File exceeds 10000 bytes");
        }
        storage.save(fileName);
    }
}

class Storage {
    private final List<String> saved = new ArrayList<>();

    void save(String fileName) throws SubmissionStorageException {
        try {
            if (fileName.equals("answer02.pdf")) {
                throw new IOException("Disk quota exceeded");
            }
            saved.add(fileName);
            System.out.println("Saved " + fileName);
        } catch (IOException e) {
            throw new SubmissionStorageException("Could not store " + fileName, e);
        }
    }
}

class ExamException extends Exception {
    ExamException(String message) {
        super(message);
    }

    ExamException(String message, Throwable cause) {
        super(message, cause);
    }
}

class ExamWindowClosedException extends ExamException {
    ExamWindowClosedException(String message) {
        super(message);
    }
}

class NotRegisteredException extends ExamException {
    NotRegisteredException(String message) {
        super(message);
    }
}

class FileTooLargeException extends ExamException {
    FileTooLargeException(String message) {
        super(message);
    }
}

class SubmissionStorageException extends ExamException {
    SubmissionStorageException(String message) {
        super(message);
    }

    SubmissionStorageException(String message, Throwable cause) {
        super(message, cause);
    }
}
