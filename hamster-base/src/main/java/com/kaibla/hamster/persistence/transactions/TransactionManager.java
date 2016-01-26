package com.kaibla.hamster.persistence.transactions;

import com.kaibla.hamster.base.Context;
import com.kaibla.hamster.base.HamsterEngine;
import com.kaibla.hamster.persistence.attribute.Attribute;
import com.kaibla.hamster.persistence.model.Document;
import com.kaibla.hamster.persistence.model.Document.DocumentData;
import com.mongodb.client.MongoDatabase;
import java.util.Iterator;
import java.util.Map.Entry;
import org.bson.types.ObjectId;

/**
 *
 * @author korend
 */
public class TransactionManager {

    HamsterEngine engine;
    MongoDatabase db;
    Transactions transactions;

    public TransactionManager(HamsterEngine engine, MongoDatabase db) {
        this.engine = engine;
        this.db = db;
        transactions = new Transactions(engine, db);
    }

    public Transaction startTransaction() {
        Context.setTransaction(null);
        Transaction transaction = new Transaction(transactions.createNew());
        Context.setTransaction(transaction);
        return transaction;
    }

    public void commit(Transaction transaction) {
        // change state to committing
        transaction.setState(Transactions.State.COMMTTING);
        Transaction t = Context.getTransaction();
        Context.setTransaction(null);
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
                bson.remove(Document.DIRTY);
                bson.remove(Document.NEW);
                doc.writeToDatabase(false);
            }
        }

        // change transaction state to committed
        // run after commit task        
        for (Runnable task : transaction.getAfterCommitTasks()) {
            task.run();
        }
        transaction.setState(Transactions.State.COMMITTED);
        transaction.setFinished(true);

        Context.setTransaction(t);
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
        transaction.setState(Transactions.State.ROLLING_BACK);

        Transaction oldT = Context.getTransaction();
        Context.setTransaction(null);
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
                bson.remove(Document.DIRTY);
                doc.writeToDatabase(false);
            }
        }

        // change transaction state to rolled back
        // run after commit task        
        for (Runnable task : transaction.getAfterRollbackTasks()) {
            task.run();
        }
        transaction.setState(Transactions.State.ROLLED_BACK);
        transaction.setFinished(true);

        Context.setTransaction(oldT);
    }

    public void rollback() {
        rollback(Context.getTransaction());
        Context.setTransaction(null);
    }

    public static void addAfterRollbackTask(Transaction transaction, Runnable task) {
        transaction.getAfterRollbackTasks().add(task);
    }

    public static void addAfterRollbackTask(Runnable task) {
        addAfterRollbackTask(Context.getTransaction(), task);
    }

    /**
     * Determines the current state of a transaction. When this method is called inside a transaction it will return
     * always the same state for a transaction in order to keep the view of the current transaction consistent.
     *
     * @param transactionId
     * @return The state of the transaction or State.NOT_EXISTENT if the transaction cannot be found anymore
     */
    public Transactions.State getTransactionState(ObjectId transactionId) {
        if (Context.getTransaction() != null) {
            //check if we already fetched the state of this transaction during our current transaction
            //and return the last seen state instead, in order to keep the view of this transaction consistent
            Transactions.State state = Context.getTransaction().getStateCache().get(transactionId);
            if (state != null) {
                return state;
            }
        }
        Document d = transactions.getById(transactionId.toHexString());
        if (d == null) {
            return Transactions.State.NOT_EXISTENT;
        } else {
            Transactions.State state = Transactions.STATE.get(d);
            if (Context.getTransaction() != null) {
                Context.getTransaction().getStateCache().put(transactionId, state);
            }
            return state;
        }
    }
}
