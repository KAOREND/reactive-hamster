package com.kaibla.hamster.persistence.model;

import com.kaibla.hamster.persistence.attribute.Attribute;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import static java.util.logging.Logger.getLogger;

/**
 *
 * @author kai
 */
public class Schema implements Serializable {

    List<Schema> childSchemas;

    List<Attribute> attributes;

    String name;

    public Schema(String name) {
        this.childSchemas = new ArrayList<Schema>();
        this.attributes = new ArrayList<Attribute>();

        this.name = name;
    }

    public String getName() {
        return name;
    }

    public Schema addAttribute(Attribute attr) {
        attributes.add(attr);
        return this;
    }

    public Schema createChildSchema(String name) {
        Schema childSchema = new Schema(name);
        childSchemas.add(childSchema);
        return childSchema;
    }

    public List<Attribute> getAttributes() {
        return attributes;
    }

    public List<Schema> getChildSchemas() {
        return childSchemas;
    }
    private static final Logger LOG = getLogger(Schema.class.getName());

}
