package com.cleardocs.exception;

public class DocumentNotFoundException extends RuntimeException {
    public DocumentNotFoundException(Long id) {
        super("Document not found with id: " + id);
    }
    public DocumentNotFoundException(String referenceNumber) {
        super("Document not found with reference: " + referenceNumber);
    }
}
