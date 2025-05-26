package com.mg.redism.jsr;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Represents an instance of a non-enum, non-Class, non-ObjectStreamClass, 
 * non-array class, including the non-transient field values, for all classes in its
 * hierarchy and inner classes.
 */
public class Instance extends ContentBase {
    /**
     * Collection of field data, organized by class description.  
     */
    public Map<ClassDescription, Map<Field, Object>> fielddata;

    /**
     * Class description for this instance.
     */
    public ClassDescription classdesc;

    /**
     * Constructor.
     */
    public Instance() {
        super(ContentType.INSTANCE);
        this.fielddata = new HashMap<ClassDescription, Map<Field, Object>>();
    }
    public String toString() {
        StringBuffer sb = new StringBuffer();
        sb.append(classdesc.name).append(' ').append("_h").append(JDeserializer.hex(handle))
            .append(" = r_").append(JDeserializer.hex(classdesc.handle)).append(";  ");
        //sb.append("// [instance " + jdeserialize.hex(handle) + ": " + jdeserialize.hex(classdesc.handle) + "/" + classdesc.className).append("]");
        return sb.toString();
    }
    /**
     * Object annotation data.
     */
    public Map<ClassDescription, List<Content>> annotations;
}
