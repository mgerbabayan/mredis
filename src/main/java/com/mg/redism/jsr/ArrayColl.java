package com.mg.redism.jsr;

import java.util.ArrayList;

/**
 * <p>Typed collection used for storing the values of a serialized array.  </p>
 *
 * <p>Primitive types are stored using their corresponding objects; for instance, an int is
 * stored as an Integer.  To determine whether or not this is an array of ints or of
 * Integer instances, check the className in the arrayobj's class description.</p>
 */
public class ArrayColl extends ArrayList<Object> {
    public static final long serialVersionUID = 2277356908919248L;

    private FieldType ftype;

    /**
     * Constructor.
     * @param ft field type of the array
     */
    public ArrayColl(FieldType ft) {
        super();
        this.ftype = ft;
    }

    /**
     * Gets the field type of the array.
     *
     * @return the field type of the array
     */
    public FieldType getFieldType() {
        return ftype;
    }
    public String toString() {
        StringBuffer sb = new StringBuffer();
        sb.append("[arraycoll sz ").append(this.size());
        boolean first = true;
        for(Object o: this) {
            if(first) {
                first = false;
                sb.append(' ');
            } else {
                sb.append(", ");
            }
            sb.append(o.toString());
        }
        return sb.toString();
    }
}
