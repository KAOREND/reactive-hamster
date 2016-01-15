package com.kaibla.hamster.persistence.model;

/**
 * This Exception is being thrown when the Document revision was changed in the Database by a different
 * version and version known to this process was outdated.
 * 
 * @author korend
 */
public class OptimisticLockException extends RuntimeException {
    
    Document stalledDocument;

    public OptimisticLockException(Document stalledDocument) {
        super("Document could not be written as it was changed by another process in between. Stalled document:  "+stalledDocument.getDataObject().toJson());
        this.stalledDocument = stalledDocument;
    }
}
