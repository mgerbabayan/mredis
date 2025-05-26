package com.mg.redism.jsr;

import java.io.IOException;

/**
 * This class represents a field within a class description/declaration (classdesc).  It
 * contains information about the type and className of the field.  Fields themselves don't
 * have a handle; inside the stream, they exist only as part of a class description.
 */
public class Field {
    /**
     * The type of the field.
     */
    public FieldType type;

    /**
     * The className of the field.
     */
    public String name; 

    /**
     * The string object representing the class className.
     */
    public StringObject classname;

    private boolean isInnerClassReference = false;

    /**
     * Tells whether or not this class is an inner class reference.  This value is set by
     * connectMemberClasses() -- if this hasn't been called, or if the field hasn't been
     * otherwise set by setIsInnerClassReference(), it will be false;
     *
     * @return true if the class is an inner class reference
     */
    public boolean isInnerClassReference() {
        return isInnerClassReference;
    }

    /**
     * Sets the flag that denotes whether this class is an inner class reference.
     *
     * @param nis the value to set; true iff the class is an inner class reference.
     */
    public void setIsInnerClassReference(boolean nis) {
        this.isInnerClassReference = nis;
    }

    /**
     * Constructor.  
     *
     * @param type the field type
     * @param name the field className
     * @param classname the class className
     */
    public Field(FieldType type, String name, StringObject classname) throws ValidityException {
        this.type = type;
        this.name = name;
        this.classname = classname;
        if(classname != null) {
            validate(classname.value);
        }
    }

    /**
     * Constructor for simple fields.
     * 
     * @param type the field type
     * @param name the field className
     */
    public Field(FieldType type, String name) throws ValidityException {
        this(type, name, null);
    }

    /**
     * Get a string representing the type for this field in Java (the language)
     * format.
     * @return a string representing the fully-qualified type of the field
     * @throws IOException if a validity or I/O error occurs
     */
    public String getJavaType() throws IOException {
        return JDeserializer.resolveJavaType(this.type, this.classname == null ? null : this.classname.value, true, false);
    }
    
    /**
     * Changes the className of an object reference to the className specified.  This is used by
     * the inner-class-connection code to fix up field references.
     * @param newname the fully-qualified class 
     * @throws ValidityException if the field isn't a reference type, or another
     * validity error occurs
     */
    public void setReferenceTypeName(String newname) throws ValidityException {
        if(this.type != FieldType.OBJECT) {
            throw new ValidityException("can't fix up a non-reference field!");
        }
        String nname = "L" + newname.replace('.', '/') + ";";
        this.classname.value = nname;
    }
    public void validate(String jt) throws ValidityException {
        if(this.type == FieldType.OBJECT) {
            if(jt == null) {
                throw new ValidityException("classname can't be null");
            }
            if(jt.charAt(0) != 'L') {
                throw new ValidityException("invalid object field type descriptor: " + classname.value);
            }
            int end = jt.indexOf(';');
            if(end == -1 || end != (jt.length()-1)) {
                throw new ValidityException("invalid object field type descriptor (must end with semicolon): " + classname.value);
            }
        }
    }
}
