package com.kaibla.hamster.persistence.attribute;

import com.mongodb.BasicDBObject;
import com.kaibla.hamster.persistence.model.Document;
import com.kaibla.hamster.persistence.model.DocumentCollection;
import java.util.logging.Logger;
import static java.util.logging.Logger.getLogger;

/**
 *
 * @author Kai Orend
 */
public class DocumentReferenceAttribute<T extends DocumentCollection> extends Attribute {

    private DocumentCollection table;
    private String referenceCollectionClass=null;

    public DocumentReferenceAttribute(Class sourcetable, String name, DocumentCollection table) {
        super(sourcetable, name);
        this.table = table;
    }
    
    public DocumentReferenceAttribute(Class sourcetable, String name, Class referenceCollection) {
        super(sourcetable, name);
        referenceCollectionClass=referenceCollection.getName();
    }

    /**
     * @return the table
     */
    public DocumentCollection getTable() {
        if(table == null) {
            table = DocumentCollection.getByClassName(referenceCollectionClass);
        }
        return table;
    }

    /**
     * @param table the table to set
     */
    public void setTable(DocumentCollection table) {
        this.table = table;
    }

    @Override
    public int compare(Object o1, Object o2) {
        return 0;
    }

    @Override
    public Object get(BasicDBObject dataObject) {
       return table.getById(dataObject.getString(getName()));
    }
    
    

    @Override
    public boolean equals(Object o1, Object o2) {      
        return o1 == o2;
    }
    private static final Logger LOG = getLogger(DocumentReferenceAttribute.class.getName());
}
