package com.kaibla.hamster.persistence.transactions;

import com.kaibla.hamster.base.Context;
import com.kaibla.hamster.persistence.attribute.Attribute;
import com.kaibla.hamster.persistence.model.Document;
import com.kaibla.hamster.persistence.model.Document.DocumentData;
import java.util.Iterator;
import java.util.Map.Entry;

/**
 *
 * @author korend
 */
public class TransactionManager {

    public Transaction startTransaction() {
        Transaction transaction = new Transaction();
        return transaction;
    }

    public void commit(Transaction transaction) {
        // change state to committing

        // clean up dirty documents        
        for (Entry<Document, DocumentData> entry : transaction.getPrivateDataObjects().entrySet()) {
            Document doc = entry.getKey();
            DocumentData data = entry.getValue();
            org.bson.Document bson = data.getDataObject();
            if (!data.getChangedAttributes().isEmpty() || bson.containsKey(Document.TRANSACTION)) {
                for (Iterator<Attribute> it = data.getChangedAttributes().iterator(); it.hasNext();) {
                    Attribute attr = it.next();
                    attr.deleteShadowCopy(bson);
                }
                bson.remove(Document.TRANSACTION);
                doc.writeToDatabase(false);
            }
        }

        // change transaction state to committed
        
        // run after commit task        
        for(Runnable task : transaction.getAfterCommitTasks()) {
            task.run();
        }
    }

    public void commit() {
        commit(Context.getTransaction());
    }

    public static void addAfterCommitTask(Transaction transaction, Runnable task) {
        transaction.getAfterCommitTasks().add(task);
    }

    public static void addAfterCommitTask(Runnable task) {
        addAfterCommitTask(Context.getTransaction(), task);
    }

    public void rollback(Transaction transaction) {
        // change state to rolling back

        // revert changes documents        
        for (Entry<Document, DocumentData> entry : transaction.getPrivateDataObjects().entrySet()) {
            Document doc = entry.getKey();
            DocumentData data = entry.getValue();
            org.bson.Document bson = data.getDataObject();
            if (!data.getChangedAttributes().isEmpty() || bson.containsKey(Document.TRANSACTION)) {
                for (Iterator<Attribute> it = data.getChangedAttributes().iterator(); it.hasNext();) {
                    Attribute attr = it.next();
                    attr.revertChanges(bson);
                }
                bson.remove(Document.TRANSACTION);
                doc.writeToDatabase(false);
            }
        }

        // change transaction state to rolled back
        
        // run after commit task        
        for(Runnable task : transaction.getAfterRollbackTasks()) {
            task.run();
        }
    }

    public void rollback() {
        rollback(Context.getTransaction());
    }

    public static void addAfterRollbackTask(Transaction transaction, Runnable task) {
        transaction.getAfterRollbackTasks().add(task);
    }

    public static void addAfterRollbackTask(Runnable task) {
        addAfterRollbackTask(Context.getTransaction(), task);
    }
}
