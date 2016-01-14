
package com.kaibla.hamster.persistence;

import com.mongodb.client.gridfs.GridFSBucket;
import com.mongodb.gridfs.GridFS;
import java.io.Serializable;

/**
 *
 * @author Kai Orend
 */
public abstract class FileSystemProvider implements Serializable {
    private static final long serialVersionUID = 1L;

    public abstract GridFSBucket getFileSystem();
}
