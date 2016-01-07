
package com.kaibla.hamster.persistence.model;

import com.kaibla.hamster.persistence.model.FilteredModel;
import com.kaibla.hamster.base.ChangedListener;
import com.kaibla.hamster.base.AbstractListenerOwner;
import com.kaibla.hamster.base.HamsterEngine;
import com.kaibla.hamster.persistence.attribute.Attribute;
import java.util.Arrays;

/**
 *
 * @author kai
 */
public class AttributeFilteredModel extends FilteredModel<Attribute>{
    private static final long serialVersionUID = 1L;

    public AttributeFilteredModel(HamsterEngine engine) {
        super(engine);
    }
    
    public void addChangedListener(ChangedListener listener,Attribute... attrs) {
      super.addChangedListener(listener, attrs);
        if (listener instanceof AbstractListenerOwner) {           
            ((AbstractListenerOwner) listener).addEventFilter(new AttributeFilter(this, Arrays.asList(attrs)));
        }
    }
}
