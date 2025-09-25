package edu.ccrm.exception;

// A checked exception for a specific business rule violation.
public class DuplicateEnrollmentException extends Exception {
    public DuplicateEnrollmentException(String message) {
        super(message);
    }
}