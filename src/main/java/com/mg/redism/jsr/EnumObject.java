package com.mg.redism.jsr;

/**
 * <p>
 * Represents an enum instance.  As noted in the serialization spec, this consists of
 * merely the class description (represented by a classdesc) and the string corresponding
 * to the enum's value.  No other fields are ever serialized.
 * </p>
 */
public class EnumObject extends ContentBase {
    /**
     * The enum's class description.
     */
    public ClassDescription classdesc;

    /**
     * The string that represents the enum's value.
     */
    public StringObject value;

    /**
     * Constructor.
     *
     * @param handle the enum's handle
     * @param cd the enum's class description
     * @param so the enum's value
     */
    public EnumObject(int handle, ClassDescription cd, StringObject so) {
        super(ContentType.ENUM);
        this.handle = handle;
        this.classdesc = cd;
        this.value = so;
    }
    public String toString() {
        return "[enum " + JDeserializer.hex(handle) + ": " + value.value + "]";
    }
}
