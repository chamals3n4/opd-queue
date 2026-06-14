package lk.opdqueue.exception;

public class PatientAlreadyInQueueException extends RuntimeException {
    public PatientAlreadyInQueueException(String message) {
        super(message);
    }
}
