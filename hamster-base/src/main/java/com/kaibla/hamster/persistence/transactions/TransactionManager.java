package com.kaibla.hamster.persistence.transactions;

import com.kaibla.hamster.base.Context;
import com.kaibla.hamster.base.HamsterEngine;
import com.kaibla.hamster.persistence.attribute.Attribute;
import com.kaibla.hamster.persistence.model.Document;
import com.kaibla.hamster.persistence.model.Document.DocumentData;
import com.kaibla.hamster.persistence.model.OptimisticLockException;
import com.mongodb.client.MongoDatabase;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.logging.Logger;
import static java.util.logging.Logger.getLogger;
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

    public void runInTransaction(Runnable runable) {
        runInTransaction(runable, -1);
    }

    public void runInTransaction(Runnable runnable, int retries) {

        Transaction old = Context.getTransaction();
        try {
            Transaction t = null;
            try {
                t = startTransaction();
                t.setRetriesLeft(retries);
                if (t.isDestroyed()) {
                    throw new RuntimeException("Current transaction is already finished, it cannot be used for further processing");
                }
                runnable.run();
            } catch (OptimisticLockException ex) {
                rollback();
                if (t.getRetriesLeft() > 0) {
                    //retry
                    Context.setTransaction(null);
                    LOG.info("retrying transaction  retries left: " + retries);
                    runInTransaction(runnable, retries - 1);
                    return;
                } else {
                    throw new RollbackException(ex);
                }
            } catch (Throwable throwable) {
                rollback();
                throw new RollbackException(throwable);
            }

            try {
                commit(t);
            } catch (Throwable th) {
                rollback(t);
                throw new RollbackException(th);
            }
            if (t.getRollbackCause() != null) {
                rollback(t);
                throw new RollbackException(t.getRollbackCause());
            }
        } finally {
            Context.setTransaction(old);
        }
    }

    public void commit(final Transaction transaction) {
        if (transaction.isDestroyed()) {
            //nothing to do anymore
            //was very likely already committed
            return;
        }
        // change state to committing
        transaction.setState(Transactions.State.COMMTTING);
        transaction.setCommitOrRollback(true);
        // clean up dirty documents        
        for (Entry<Document, DocumentData> entry : transaction.getPrivateDataObjects().entrySet()) {
            Document doc = entry.getKey();
            DocumentData data = entry.getValue();
            org.bson.Document bson = data.getDataObject();
            if (!data.getChangedAttributes().isEmpty() && bson.containsKey(Document.TRANSACTION)) {
                if (bson.getObjectId(Document.TRANSACTION).equals(transaction.getTransactionId())) {
                    for (Iterator<Attribute> it = data.getChangedAttributes().iterator(); it.hasNext();) {
                        Attribute attr = it.next();
                        attr.deleteShadowCopy(bson);
                    }
                    bson.remove(Document.TRANSACTION);
                    bson.remove(Document.DIRTY);
                    if (bson.containsKey(Document.NEW)) {
                        bson.remove(Document.NEW);
                    }
                    try {
                        doc.writeToDatabase(false);
                    } catch (OptimisticLockException ex) {
                        throw ex;
                    }
                }
            }
        }

        // change transaction state to committed       
        transaction.setState(Transactions.State.COMMITTED);
        transaction.setFinished(true);
        // run after commit task 
        for (Runnable task : new ArrayList<Runnable>(transaction.getAfterCommitTasks())) {
            task.run();
        }
    }


    public void commit() {
        if(Context.getTransaction() != null) {
            commit(Context.getTransaction());
        }
        // Context.setTransaction(null);
    }

    public static void addAfterCommitTask(Transaction transaction, Runnable task) {
        if (transaction.isDestroyed()) {
            throw new RuntimeException("transaction is already finished and cannot accept after commit tasks anymore");
        }
        transaction.getAfterCommitTasks().add(task);
    }

    public static void addAfterCommitTask(Runnable task) {
        addAfterCommitTask(Context.getTransaction(), task);
    }

    public void rollback(final Transaction transaction) {
        // change state to rolling back
        transaction.setState(Transactions.State.ROLLING_BACK);
        transaction.setCommitOrRollback(true);
        // revert changes documents        
        for (Entry<Document, DocumentData> entry : new ArrayList<Entry<Document, DocumentData>>(transaction.getPrivateDataObjects().entrySet())) {
            Document doc = entry.getKey();
            DocumentData data = entry.getValue();
            org.bson.Document bson = data.getDataObject();
            if (bson.containsKey(Document.TRANSACTION)) {
                if (bson.getObjectId(Document.TRANSACTION).equals(transaction.getTransactionId())) {
                    if (bson.containsKey(Document.NEW)) {
                        //the creation of this document needs to be rolled back
                        doc.delete();
                    } else if (!data.getChangedAttributes().isEmpty()) {
                        for (Iterator<Attribute> it = data.getChangedAttributes().iterator(); it.hasNext();) {
                            Attribute attr = it.next();
                            attr.revertChanges(bson);
                        }
                        bson.remove(Document.TRANSACTION);
                        bson.remove(Document.DIRTY);
                        doc.writeToDatabase(false);
                    }
                }
            }
        }
        // change transaction state to rolled back      
        transaction.setState(Transactions.State.ROLLED_BACK);
        transaction.setFinished(true);

        // run after rollback tasks        
        for (Runnable task : new ArrayList<Runnable>(transaction.getAfterRollbackTasks())) {
            task.run();
        }

    }

    public void rollback() {
        rollback(Context.getTransaction());
        Context.setTransaction(null);
    }

    public static void addAfterRollbackTask(Transaction transaction, Runnable task) {
        if (transaction.isDestroyed()) {
            throw new RuntimeException("transaction is already finished and cannot accept after rollback tasks");
        }
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

    private static final Logger LOG = getLogger(TransactionManager.class.getName());
}
