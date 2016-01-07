
package com.kaibla.hamster.persistence.attribute;

import com.kaibla.hamster.persistence.model.Document;

/**
 *
 * @author kai
 */
public interface ComplexCopy {    
    public void createCopy(Document source, Document target, Document user, boolean temp);
    
}
