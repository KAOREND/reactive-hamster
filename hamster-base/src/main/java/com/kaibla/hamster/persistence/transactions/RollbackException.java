package com.kaibla.hamster.persistence.transactions;

/**
 *
 * @author kai
 */
public class RollbackException extends RuntimeException {

    public RollbackException(Throwable cause) {
        super("Transaction was rolled back caused by: "+cause.getMessage(), cause);
    }
    
    
}
