package com.kaibla.hamster.persistence.attribute;

import com.mongodb.gridfs.GridFS;
import com.mongodb.gridfs.GridFSDBFile;
import com.kaibla.hamster.persistence.model.Document;
import com.kaibla.hamster.persistence.FileSystemProvider;
import com.mongodb.Block;
import com.mongodb.client.gridfs.model.GridFSFile;
import com.mongodb.client.model.Filters;
import java.util.logging.Logger;
import static java.util.logging.Logger.getLogger;
import static java.util.logging.Logger.getLogger;

/**
 *
 * @author kai
 */
public class FileAttribute extends Attribute {

    protected final FileSystemProvider fileSystemProvider;

    public FileAttribute(Class table, String name, FileSystemProvider fileSystemProvider) {
        super(table, name);
        this.fileSystemProvider = fileSystemProvider;
    }

    @Override
    public int compare(Object o1, Object o2) {
        String s1 = o1.toString();
        String s2 = o2.toString();
        return s1.compareTo(s2);
    }

    @Override
    public boolean equals(Object o1, Object o2) {
        String s1 = o1.toString();
        String s2 = o2.toString();
        return s1.equals(s2);
    }

    public void prePostProcessing(Document user, Document mo, String fileName) {
//        if (mo.get(this) != null) {
//            deleteFile(mo);
//        }
    }

    public void runPostProcessing(Document user, Document mo, String fileName) {
//        try {
//            mo.getDataObject().put(getName(), fileName);
//            mo.addChangedAttribute(this);
//            //mo.writeToDatabase();
//        } catch (Exception ex) {
//            deleteFile(mo);
//            LOG.log(java.util.logging.Level.SEVERE, ex.getMessage(), ex);
//        }
    }

    public void deleteFile(Document mo) {
        String filename = mo.get(this);
        if (filename != null) {
            fileSystemProvider.getFileSystem().find(Filters.eq("filename", filename)).forEach(new Block<GridFSFile>() {
                @Override
                public void apply(GridFSFile file) {
                    fileSystemProvider.getFileSystem().delete(file.getObjectId());
                }
            });
            mo.getDataObject().remove(this.getName());
//            mo.writeToDatabase(false);
        }
    }
    private static final Logger LOG = getLogger(FileAttribute.class.getName());
}
