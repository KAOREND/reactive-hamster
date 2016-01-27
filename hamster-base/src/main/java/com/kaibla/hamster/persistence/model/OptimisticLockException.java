package com.kaibla.hamster.persistence.model;

import com.kaibla.hamster.base.Context;

/**
 * This Exception is being thrown when the Document revision was changed in the Database by a different
 * process and the version known to this process was outdated.
 * 
 * @author korend
 */
public class OptimisticLockException extends RuntimeException {
    
    Document staleDocument;

    public OptimisticLockException(Document staleDocument) {
        super("Document could not be written as it was changed by another process in between. Stale document:  "+staleDocument.getDataObject().toJson());
        this.staleDocument = staleDocument;
        if(Context.getTransaction() != null) {
           Context.getTransaction().setRollbackCause(this);
        }
    }
    
    public OptimisticLockException(Document stalledDocument,String msg) {
        super(msg);
        this.staleDocument = stalledDocument;
        if(Context.getTransaction() != null) {
            Context.getTransaction().setRollbackCause(this);
        }
    }
}
