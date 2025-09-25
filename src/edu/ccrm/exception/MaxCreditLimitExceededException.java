package edu.ccrm.exception;

// Another checked exception for a business rule.
public class MaxCreditLimitExceededException extends Exception {
    public MaxCreditLimitExceededException(String message) {
        super(message);
    }
}