package com.kaibla.hamster.persistence.transactions;

import com.kaibla.hamster.base.AbstractListenerContainer;
import com.kaibla.hamster.base.AbstractListenerOwner;
import com.kaibla.hamster.base.Context;
import com.kaibla.hamster.base.DataEvent;
import com.kaibla.hamster.base.HamsterEngine;
import com.kaibla.hamster.persistence.model.Document;
import com.kaibla.hamster.persistence.model.Document.DocumentData;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import org.bson.types.ObjectId;

/**
 *
 * @author korend
 */
public class Transaction extends AbstractListenerOwner {
    
    private List<Runnable> afterCommitTasks;
    
    private List<Runnable> afterRollbackTasks;
    
    private HashMap<Document,DocumentData> privateDataObjects;
    
    private Document transactionDocument;
    
    private boolean rollback=false;
            
    private boolean finished=false;
    
    private HashMap<ObjectId, Transactions.State> stateCache;
    
    protected Transaction(Document transactionDocument) {
        afterCommitTasks = new LinkedList<>();
        afterRollbackTasks = new LinkedList<>();
        privateDataObjects = new HashMap<>();
        this.transactionDocument = transactionDocument;
        stateCache = new HashMap<>();
        this.setListenerContainer(new AbstractListenerContainer(transactionDocument.getEngine(), this) {
            @Override
            public void dataChanged(DataEvent e) {
                
            }
            
            @Override
            public boolean isDestroyed() {
                return finished;
            }
        });
        transactionDocument.addHolder(this);
    }

    public List<Runnable> getAfterCommitTasks() {
        return afterCommitTasks;
    }

    public List<Runnable> getAfterRollbackTasks() {
        return afterRollbackTasks;
    }

    public HashMap<Document, DocumentData> getPrivateDataObjects() {
        return privateDataObjects;
    }

    @Override
    public void dataChanged(DataEvent e) {
       
    }

    protected HashMap<ObjectId, Transactions.State> getStateCache() {
        return stateCache;
    }
    
    public ObjectId getTransactionId() {
        return transactionDocument.getObjectId();
    }

    public void setRollback(boolean rollback) {
        this.rollback = rollback;
    }

    public boolean isRollback() {
        return rollback;
    }
    
    public void setState(Transactions.State state) {
        Transaction t=Context.getTransaction();
        Context.setTransaction(null);
        transactionDocument.set(Transactions.STATE, state);
        if(state == Transactions.State.COMMITTED || state == Transactions.State.ROLLED_BACK) {
            transactionDocument.set(Transactions.END, new Date());
        }
        transactionDocument.writeToDatabase();
        Context.setTransaction(t);
    }

    public void setFinished(boolean finished) {
        this.finished = finished;
    }
    
    @Override
    public boolean isDestroyed() {
       return !finished;
    }
}
