package com.mg.redism.jsr;

/**
 * <p>Represents an array instance, including the values the comprise the array.  </p>
 *
 * <p>Note that in arrays of primitives, the classdesc will be named "[x", where x is the
 * field type code representing the primitive type.  See jdeserialize.resolveJavaType()
 * for an example of analysis/generation of human-readable names from these class names.</p>
 */
public class ArrayObject extends ContentBase {
    /**
     * Type of the array instance.
     */
    public ClassDescription classdesc;

    /**
     * Values of the array, in the order they were read from the stream.
     */
    public ArrayColl data;

    public ArrayObject(int handle, ClassDescription cd, ArrayColl data) {
        super(ContentType.ARRAY);
        this.handle = handle;
        this.classdesc = cd;
        this.data = data;
    }
    public String toString() {
        return "[array " + JDeserializer.hex(handle) + " classdesc " + classdesc.toString() + ": "
            + data.toString() + "]";
    }
}

