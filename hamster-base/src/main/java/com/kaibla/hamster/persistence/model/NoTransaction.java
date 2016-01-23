
package com.kaibla.hamster.persistence.model;

/**
 *
 * @author kai
 */
public class NoTransaction extends OptimisticLockException {
    
    public NoTransaction(Document stalledDocument) {
        super(stalledDocument,"Document is used by another transaction, but this write attempt is not in transaction itself");
    }
    
}
