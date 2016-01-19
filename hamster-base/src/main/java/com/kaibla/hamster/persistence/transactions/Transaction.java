package com.kaibla.hamster.persistence.transactions;

import com.kaibla.hamster.persistence.model.Document;
import com.kaibla.hamster.persistence.model.Document.DocumentData;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

/**
 *
 * @author korend
 */
public class Transaction {
    
    private List<Runnable> afterCommitTasks;
    
    private List<Runnable> afterRollbackTasks;
    
    private HashMap<Document,DocumentData> privateDataObjects;
            
    protected Transaction() {
        afterCommitTasks = new LinkedList<>();
        afterRollbackTasks = new LinkedList<>();
        privateDataObjects = new HashMap<>();
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
}
