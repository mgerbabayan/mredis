package com.mg.redism.jsr;

import java.io.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The main user-facing class for the jdeserialize library.  Also the implementation of
 * the command-line tool.<br/>
 * <br/>
 * Library:<br/>
 * <br/>
 * The jdeserialize class parses the stream (method run()).  From there, call the
 * getContent() method to get an itemized list of all items written to the stream, 
 * or getHandleMaps() to get a list of all handle->content maps generated during parsing.
 * The objects are generally instances that implement the interface "content"; see the
 * documentation of various implementors to get more information about the inner
 * representations.<br/>
 * <br/>
 * To enable debugging on stdout, use the enableDebug() or disableDebug() options.   <br/> 
 * <br/>
 * <br/>
 * Command-line tool:   <br/>
 * <br/>
 * The tool reads in a set of files and generates configurable output on stdout.  The
 * primary output consists of three separate stages.  The first stage is  a textual 
 * description of every piece of content in the stream, in the order it was written.
 * There is generally a one-to-one mapping between ObjectOutputStream.writeXXX() calls and
 * items printed in the first stage.  The first stage may be suppressed with the
 * -nocontent command-line option. <br/>
 * <br/>
 * The second stage is a list of every class declaration serialized in the file.  These
 * are formatted as normal Java language class declarations.  Several options are
 * available to govern this stage, including -filter, -showarrays, -noclasses, and
 * -fixnames. <br/>
 * <br/>
 * The third stage is a dump of every instance embedded inside the stream, including
 * textual descriptions of field values.  This is useful for casual viewing of class data. 
 * To suppress this stage, use -noinstances. <br/>
 * <br/>
 * You can also get debugging information generated during the parse phase by supplying
 * -debug.
 * <br/>
 * The data from block data objects can be extracted with the -blockdata <file> option.
 * Additionally, a manifest describing the size of each individual block can be generated
 * with the -blockdatamanifest <file> option.
 * <br/>
 * References: <br/>
 *     - Java Object Serialization Specification ch. 6 (Object Serialization Stream
 *       Protocol): <br/>
 *       http://download.oracle.com/javase/6/docs/platform/serialization/spec/protocol.html <br/>
 *     - "Modified UTF-8 Strings" within the JNI specification: 
 *       http://download.oracle.com/javase/1.5.0/docs/guide/jni/spec/types.html#wp16542 <br/>
 *     - "Inner Classes Specification" within the JDK 1.1.8 docs:
 *       http://java.sun.com/products/archive/jdk/1.1/ <br/>
 *     - "Java Language Specification", third edition, particularly section 3:
 *       http://java.sun.com/docs/books/jls/third_edition/html/j3TOC.html <br/>
 *
 * @see Content
 */
public class JDeserializer {
    public static final long serialVersionUID = 78790714646095L;
    public static final String INDENT = "    ";
    public static final int CODEWIDTH = 90;
    public static final String linesep = System.getProperty("line.separator");
    public static final String[] keywords = new String[] {
        "abstract", "continue", "for", "new", "switch", "assert", "default", "if",
        "package", "synchronized", "boolean", "do", "goto", "private", "this",
        "break", "double", "implements", "protected", "throw", "byte", "else",
        "import", "public", "throws", "case", "enum", "instanceof", "return",
        "transient", "catch", "extends", "int", "short", "try", "char", "final",
        "interface", "static", "void", "class", "finally", "long", "strictfp",
        "volatile", "const", "float", "native", "super", "while" };
    public static HashSet<String> keywordSet;

   // private String filename;
    private HashMap<Integer, Content> handles = new HashMap<Integer, Content>();
    private ArrayList<Map<Integer, Content>> handlemaps = new ArrayList<Map<Integer, Content>>();
    private ArrayList<Content> content;
    private int curhandle;
    private boolean debugEnabled;

    static {
        keywordSet = new HashSet<String>();
        for(String kw: keywords) {
            keywordSet.add(kw);
        }
    }

    /**
     * <p>
     * Retrieves the list of content objects that were written to the stream.  Each item
     * generally corresponds to an invocation of an ObjectOutputStream writeXXX() method.
     * A notable exception is the class exceptionstate, which represents an embedded
     * exception that was caught during serialization.
     * </p>
     *
     * <p>
     * See the various implementors of content to get information about what data is
     * available.
     * </p>
     *
     * <p>
     * Entries in the list may be null, because it's perfectly legitimate to write a null
     * reference to the stream.
     * </p>
     *
     * @return a list of content objects
     * @see Content
     * @see ExceptionState
     */
    public List<Content> getContent() {
        return content;
    }

    /**
     * <p>
     * Return a list of Maps containing every object with a handle.  The keys are integers
     * -- the handles themselves -- and the values are instances of type content.
     * </p>
     *
     * <p>
     * Although there is only one map active at a given point, a stream may have multiple
     * logical maps: when a reset happens (indicated by TC_RESET), the current map is
     * cleared.
     * </p>
     *
     * <p>
     * See the spec for details on handles.
     * </p>
     * @return a list of <Integer,content> maps
     */
    public List<Map<Integer, Content>> getHandleMaps() {
        return handlemaps;
    }

    /**
     * Suitably escapes non-printable-ASCII characters (and doublequotes) for use
     * in a Java string literal.
     *
     * @param str string to escape
     * @return an escaped version of the string
     */
    public static String unicodeEscape(String str) {
        StringBuffer sb = new StringBuffer();
        int cplen = str.codePointCount(0, str.length());
        for(int i = 0; i < cplen; i++) {
            int cp = str.codePointAt(i);
            if(cp == '"') {
                sb.append("\\\"");
            }
            if(cp < 0x20 || cp > 0x7f) {
                sb.append("\\u" + hexnoprefix(4));
            } else {
                sb.appendCodePoint(cp);
            }
        }
        return sb.toString();
    }

    public static String indent(int level) {
        StringBuffer sb = new StringBuffer("");
        for(int i = 0; i < level; i++) {
            sb.append(INDENT);
        }
        return sb.toString();
    }

    public void read_Classdata(DataInputStream dis, Instance inst) throws IOException {
        ArrayList<ClassDescription> classes = new ArrayList<ClassDescription>();
        inst.classdesc.getHierarchy(classes);
        Map<ClassDescription, Map<Field, Object>> alldata = new HashMap<ClassDescription, Map<Field, Object>>();
        Map<ClassDescription, List<Content>> ann = new HashMap<ClassDescription, List<Content>>();
        for(ClassDescription cd: classes) {
            Map<Field, Object> values = new HashMap<Field, Object>();
            if((cd.descflags & ObjectStreamConstants.SC_SERIALIZABLE) != 0) {
                if((cd.descflags & ObjectStreamConstants.SC_EXTERNALIZABLE) != 0) {
                    throw new IOException("SC_EXTERNALIZABLE & SC_SERIALIZABLE encountered");
                }
                for(Field f: cd.fields) {
                    Object o = read_FieldValue(f.type, dis);
                    values.put(f, o);
                }
                alldata.put(cd, values);
                if((cd.descflags & ObjectStreamConstants.SC_WRITE_METHOD) != 0) {
                    if((cd.descflags & ObjectStreamConstants.SC_ENUM) != 0) {
                        throw new IOException("SC_ENUM & SC_WRITE_METHOD encountered!");
                    }
                    ann.put(cd, read_classAnnotation(dis));
                }
            } else if((cd.descflags & ObjectStreamConstants.SC_EXTERNALIZABLE) != 0) {
                if((cd.descflags & ObjectStreamConstants.SC_SERIALIZABLE) != 0) {
                    throw new IOException("SC_SERIALIZABLE & SC_EXTERNALIZABLE encountered");
                }
                if((cd.descflags & ObjectStreamConstants.SC_BLOCK_DATA) != 0) {
                    throw new EOFException("hit externalizable with nonzero SC_BLOCK_DATA; can't interpret data");
                } else {
                    ann.put(cd, read_classAnnotation(dis));
                }
            }
        }
        inst.annotations = ann;
        inst.fielddata = alldata;
    }

    public Object read_FieldValue(FieldType f, DataInputStream dis) throws IOException {
        switch(f) {
            case BYTE:
                return Byte.valueOf(dis.readByte());
            case CHAR:
                return Character.valueOf(dis.readChar());
            case DOUBLE:
                return Double.valueOf(dis.readDouble());
            case FLOAT:
                return Float.valueOf(dis.readFloat());
            case INTEGER:
                return Integer.valueOf(dis.readInt());
            case LONG:
                return Long.valueOf(dis.readLong());
            case SHORT:
                return Short.valueOf(dis.readShort());
            case BOOLEAN:
                return Boolean.valueOf(dis.readBoolean());
            case OBJECT:
            case ARRAY:
                byte stc = dis.readByte();
                if(f == FieldType.ARRAY && stc != ObjectStreamConstants.TC_ARRAY) {
                    throw new IOException("array type listed, but typecode is not TC_ARRAY: " + hex(stc));
                }
                Content c = read_Content(stc, dis, false);
                if(c != null && c.isExceptionObject()) {
                    throw new ExceptionReadException(c);
                }
                return c;
            default:
                throw new IOException("can't process type: " + f.toString());
        }
    }
    public JDeserializer() {
       // this.filename = filename;
    }
    private int newHandle() {
        return curhandle++;
    }

    public static String resolveJavaType(FieldType type, String classname, boolean convertSlashes, boolean fixname)  throws IOException {
        if(type == FieldType.ARRAY) {
            StringBuffer asb = new StringBuffer("");
            for(int i = 0; i < classname.length(); i++) {
                char ch = classname.charAt(i);
                switch(ch) {
                    case '[':
                        asb.append("[]");
                        continue;
                    case 'L':
                        String cn = decodeClassName(classname.substring(i), convertSlashes);
                        if(fixname) {
                            cn = fixClassName(cn);
                        }
                        return cn + asb.toString();
                    default:
                        if(ch < 1 || ch > 127) {
                            throw new ValidityException("invalid array field type descriptor character: " + classname);
                        }
                        FieldType ft = FieldType.get((byte)ch);
                        if(i != (classname.length()-1)) {
                            throw new ValidityException("array field type descriptor is too long: " + classname);
                        }
                        String ftn = ft.getJavaType();
                        if(fixname) {
                            ftn = fixClassName(ftn);
                        }
                        return ftn + asb.toString();
                }
            }
            throw new ValidityException("array field type descriptor is too short: " + classname);
        } else if(type == FieldType.OBJECT) {
            return decodeClassName(classname, convertSlashes);
        } else {
            return type.getJavaType();
        }
    }

    public List<Content> read_classAnnotation(DataInputStream dis) throws IOException {
        List<Content> list = new ArrayList<Content>();
        while(true) {
            byte tc = dis.readByte();
            if(tc == ObjectStreamConstants.TC_ENDBLOCKDATA) {
                return list;
            }
            if(tc == ObjectStreamConstants.TC_RESET) {
                reset();
                continue;
            }
            Content c = read_Content(tc, dis, true);
            if(c != null && c.isExceptionObject()) {
                throw new ExceptionReadException(c);
            }
            list.add(c);
        }
    }
    public static void dump_Instance(int indentlevel, Instance inst, PrintStream ps) {
        StringBuffer sb = new StringBuffer();
        sb.append("[instance " + hex(inst.handle) + ": " + hex(inst.classdesc.handle) + "/" + inst.classdesc.name);
        if(inst.annotations != null && inst.annotations.size() > 0) {
            sb.append(linesep).append("  object annotations:").append(linesep);
            for(ClassDescription cd: inst.annotations.keySet()) {
                sb.append("    ").append(cd.name).append(linesep);
                for(Content c: inst.annotations.get(cd)) {
                    sb.append("        ").append(c.toString()).append(linesep);
                }
            }
        }
        if(inst.fielddata != null && inst.fielddata.size() > 0) {
            sb.append(linesep).append("  field data:").append(linesep);
            for(ClassDescription cd: inst.fielddata.keySet()) {
                sb.append("    ").append(hex(cd.handle)).append("/").append(cd.name).append(":").append(linesep);
                for(Field f: inst.fielddata.get(cd).keySet()) {
                    Object o = inst.fielddata.get(cd).get(f);
                    sb.append("        ").append(f.name).append(": ");
                    if(o instanceof Content) {
                        Content c = (Content)o;
                        int h = c.getHandle();
                        if(h == inst.handle) {
                            sb.append("this");
                        } else {
                            sb.append("r" + hex(h));
                        }
                        sb.append(": ").append(c.toString());
                        sb.append(linesep);
                    } else {
                        sb.append("" + o).append(linesep);
                    }
                }
            }
        }
        sb.append("]");
        ps.println(sb);
    }

    /**
     * "Fix" the given className by transforming illegal characters, such that the end result
     * is a legal Java identifier that is not a keyword.
     * If the string is modified at all, the result will be prepended with "$__".
     *
     * @param name the className to be transformed
     * @return the unmodified string if it is legal, otherwise a legal-identifier version
     */
    public static String fixClassName(String name) {
        if(name == null) {
            return "$__null";
        }
        if(keywordSet.contains(name)) {
            return "$__" + name;
        }
        StringBuffer sb = new StringBuffer();
        int cplen = name.codePointCount(0, name.length());
        if(cplen < 1) {
            return "$__zerolen";
        }
        boolean modified = false;
        int scp = name.codePointAt(0);
        if(!Character.isJavaIdentifierStart(scp)) {
            modified = true;
            if(!Character.isJavaIdentifierPart(scp) || Character.isIdentifierIgnorable(scp)) {
                sb.append("x");
            } else {
                sb.appendCodePoint(scp);
            }
        } else {
            sb.appendCodePoint(scp);
        }

        for(int i = 1; i < cplen; i++) {
            int cp = name.codePointAt(i);
            if(!Character.isJavaIdentifierPart(cp) || Character.isIdentifierIgnorable(cp)) {
                modified = true;
                sb.append("x");
            } else {
                sb.appendCodePoint(cp);
            }
        }
        if(modified) {
            return "$__" + sb.toString();
        } else {
            return name;
        }
    }

    public static void dump_ClassDesc(int indentlevel, ClassDescription cd, PrintStream ps, boolean fixname) throws IOException {
        String classname = cd.name;
        if(fixname) {
            classname = fixClassName(classname);
        }
        if(cd.annotations != null && cd.annotations.size() > 0) {
            ps.println(indent(indentlevel) + "// annotations: ");
            for(Content c: cd.annotations) {
                ps.print(indent(indentlevel) + "// " + indent(1));
                ps.println(c.toString());
            }
        }
        if(cd.classtype == ClassDescriptionType.NORMALCLASS) {
            if((cd.descflags & ObjectStreamConstants.SC_ENUM) != 0) {
                ps.print(indent(indentlevel) + "enum " + classname + " {");
                boolean shouldindent = true;
                int len = indent(indentlevel+1).length();
                for(String econst: cd.enumconstants) {
                    if(shouldindent) {
                        ps.println("");
                        ps.print(indent(indentlevel+1));
                        shouldindent = false;
                    }
                    len += econst.length();
                    ps.print(econst + ", ");
                    if(len >= CODEWIDTH) {
                        len = indent(indentlevel+1).length();
                        shouldindent = true;
                    }
                }
                ps.println("");
                ps.println(indent(indentlevel) + "}");
                return;
            }
            ps.print(indent(indentlevel));
            if(cd.isStaticMemberClass()) {
                ps.print("static ");
            }
            ps.print("class " + (classname.charAt(0) == '[' ? resolveJavaType(FieldType.ARRAY, cd.name, false, fixname) : classname));
            if(cd.superclass != null) {
                ps.print(" extends " + cd.superclass.name);
            }
            ps.print(" implements ");
            if((cd.descflags & ObjectStreamConstants.SC_EXTERNALIZABLE) != 0) {
                ps.print("java.io.Externalizable");
            } else {
                ps.print("java.io.Serializable");
            }
            if(cd.interfaces != null) {
                for(String intf: cd.interfaces) {
                    ps.print(", " + intf);
                }
            }
            ps.println(" {");
            for(Field f: cd.fields) {
                if(f.isInnerClassReference()) {
                    continue;
                }
                ps.print(indent(indentlevel+1) + f.getJavaType());
                ps.println(" " + f.name + ";");
            }
            for(ClassDescription icd: cd.innerclasses) {
                dump_ClassDesc(indentlevel+1, icd, ps, fixname);
            }
            ps.println(indent(indentlevel)+"}");
        } else if(cd.classtype == ClassDescriptionType.PROXYCLASS) {
            ps.print(indent(indentlevel) + "// proxy class " + hex(cd.handle));
            if(cd.superclass != null) {
                ps.print(" extends " + cd.superclass.name);
            }
            ps.println(" implements ");
            for(String intf: cd.interfaces) {
                ps.println(indent(indentlevel) + "//    " + intf + ", ");
            }
            if((cd.descflags & ObjectStreamConstants.SC_EXTERNALIZABLE) != 0) {
                ps.println(indent(indentlevel) + "//    java.io.Externalizable");
            } else {
                ps.println(indent(indentlevel) + "//    java.io.Serializable");
            }
        } else {
            throw new ValidityException("encountered invalid classdesc type!");
        }
    }

    public void setHandle(int handle, Content c) throws IOException {
        if(handles.containsKey(handle)) {
            throw new IOException("trying to reset handle " + hex(handle));
        }
        handles.put(handle, c);
    }
    public void reset() {
        debug("reset ordered!");
        if(handles != null && handles.size() > 0) {
            HashMap<Integer, Content> hm = new HashMap<Integer, Content>();
            hm.putAll(handles);
            handlemaps.add(hm);
        }
        handles.clear();
        curhandle = ObjectStreamConstants.baseWireHandle;  // 0x7e0000
    }
    /**
     * Read the content of a thrown exception object.  According to the spec, this must be
     * an object of type Throwable.  Although the Sun JDK always appears to provide enough
     * information about the hierarchy to reach all the way back to java.lang.Throwable,
     * it's unclear whether this is actually a requirement.  From my reading, it's
     * possible that some other ObjectOutputStream implementations may leave some gaps in
     * the hierarchy, forcing this app to hit the classloader.  To avoid this, we merely
     * ensure that the written object is indeed an instance; ensuring that the object is
     * indeed a Throwable is an exercise left to the user.
     */
    public Content read_Exception(DataInputStream dis) throws IOException {
        reset();
        byte tc = dis.readByte();
        if(tc == ObjectStreamConstants.TC_RESET) {
            throw new ValidityException("TC_RESET for object while reading exception: what should we do?");
        }
        Content c = read_Content(tc, dis, false);
        if(c == null) {
            throw new ValidityException("stream signaled for an exception, but exception object was null!");
        }
        if(!(c instanceof Instance)) {
            throw new ValidityException("stream signaled for an exception, but content is not an object!");
        }
        if(c.isExceptionObject()) {
            throw new ExceptionReadException(c);
        }
        c.setIsExceptionObject(true);
        reset();
        return c;
    }

    public ClassDescription read_classDesc(DataInputStream dis) throws IOException {
        byte tc = dis.readByte();
        ClassDescription cd = handle_classDesc(tc, dis, false);
        return cd;
    }
    public ClassDescription read_newClassDesc(DataInputStream dis) throws IOException {
        byte tc = dis.readByte();
        ClassDescription cd = handle_newClassDesc(tc, dis);
        return cd;
    }
    public Content read_prevObject(DataInputStream dis) throws IOException {
            int handle = dis.readInt();
            if(!handles.containsKey(Integer.valueOf(handle))) {
                throw new ValidityException("can't find an entry for handle " + hex(handle));
            }
            Content c = handles.get(handle);
            debug("prevObject: handle " + hex(c.getHandle()) + " classdesc " + c.toString());
            return c;
    }

    public ClassDescription handle_newClassDesc(byte tc, DataInputStream dis) throws IOException {
        return handle_classDesc(tc, dis, true);
    }
    public ClassDescription handle_classDesc(byte tc, DataInputStream dis, boolean mustBeNew) throws IOException {
        if(tc == ObjectStreamConstants.TC_CLASSDESC) {
            String name = dis.readUTF();
            long serialVersionUID = dis.readLong();
            int handle = newHandle();
            byte descflags = dis.readByte();
            short nfields = dis.readShort();
            if(nfields < 0) {
                throw new IOException("invalid field count: " + nfields);
            }
            Field[] fields = new Field[nfields];
            for(short s = 0; s < nfields; s++) {
                byte ftype = dis.readByte();
                if(ftype == 'B' || ftype == 'C' || ftype == 'D'
                        || ftype == 'F' || ftype == 'I' || ftype == 'J'
                        || ftype == 'S' || ftype == 'Z') {
                    String fieldname = dis.readUTF();
                    fields[s] = new Field(FieldType.get(ftype), fieldname);
                } else if(ftype == '[' || ftype == 'L') {
                    String fieldname = dis.readUTF();
                    byte stc = dis.readByte();
                    StringObject classname = read_newString(stc, dis);
                    //String classname = dis.readUTF();
                    fields[s] = new Field(FieldType.get(ftype), fieldname, classname);
                } else {
                    throw new IOException("invalid field type char: " + hex(ftype));
                }
            }
            ClassDescription cd = new ClassDescription(ClassDescriptionType.NORMALCLASS);
            cd.name = name;
            cd.serialVersionUID = serialVersionUID;
            cd.handle = handle;
            cd.descflags = descflags;
            cd.fields = fields;
            cd.annotations = read_classAnnotation(dis);
            cd.superclass = read_classDesc(dis);
            setHandle(handle, cd);
            debug("read new classdesc: handle " + hex(handle) + " className " + name);
            return cd;
        } else if(tc == ObjectStreamConstants.TC_NULL) {
            if(mustBeNew) {
                throw new ValidityException("expected new class description -- got null!");
            }
            debug("read null classdesc");
            return null;
        } else if(tc == ObjectStreamConstants.TC_REFERENCE) {
            if(mustBeNew) {
                throw new ValidityException("expected new class description -- got a reference!");
            }
            Content c = read_prevObject(dis);
            if(!(c instanceof ClassDescription)) {
                throw new IOException("referenced object not a class description!");
            }
            ClassDescription cd = (ClassDescription)c;
            return cd;
        } else if(tc == ObjectStreamConstants.TC_PROXYCLASSDESC) {
            int handle = newHandle();
            int icount = dis.readInt();
            if(icount < 0) {
                throw new IOException("invalid proxy interface count: " + hex(icount));
            }
            String interfaces[] = new String[icount];
            for(int i = 0; i < icount; i++) {
                interfaces[i] = dis.readUTF();
            }
            ClassDescription cd = new ClassDescription(ClassDescriptionType.PROXYCLASS);
            cd.handle = handle;
            cd.interfaces = interfaces;
            cd.annotations = read_classAnnotation(dis);
            cd.superclass = read_classDesc(dis);
            setHandle(handle, cd);
            cd.name = "(proxy class; no className)";
            debug("read new proxy classdesc: handle " + hex(handle) + " names [" + Arrays.toString(interfaces) + "]");
            return cd;
        } else {
            throw new ValidityException("expected a valid class description starter got " + hex(tc));
        }
    }
    public ArrayObject read_newArray(DataInputStream dis) throws IOException {
        ClassDescription cd = read_classDesc(dis);
        int handle = newHandle();
        debug("reading new array: handle " + hex(handle) + " classdesc " + cd.toString());
        if(cd.name.length() < 2) {
            throw new IOException("invalid className in array classdesc: " + cd.name);
        }
        ArrayColl ac = read_arrayValues(cd.name.substring(1), dis);
        return new ArrayObject(handle, cd, ac);
    }
    public ArrayColl read_arrayValues(String str, DataInputStream dis) throws IOException {
        byte b = str.getBytes("UTF-8")[0];
        FieldType ft = FieldType.get(b);
        int size = dis.readInt();
        if(size < 0) {
            throw new IOException("invalid array size: " + size);
        }

        ArrayColl ac = new ArrayColl(ft);
        for(int i = 0; i < size; i++) {
            ac.add(read_FieldValue(ft, dis));
            continue;
        }
        return ac;
    }
    public ClassObject read_newClass(DataInputStream dis) throws IOException {
        ClassDescription cd = read_classDesc(dis);
        int handle = newHandle();
        debug("reading new class: handle " + hex(handle) + " classdesc " + cd.toString());
        ClassObject c = new ClassObject(handle, cd);
        setHandle(handle, c);
        return c;
    }
    public EnumObject read_newEnum(DataInputStream dis) throws IOException {
        ClassDescription cd = read_classDesc(dis);
        if(cd == null) {
            throw new IOException("enum classdesc can't be null!");
        }
        int handle = newHandle();
        debug("reading new enum: handle " + hex(handle) + " classdesc " + cd.toString());
        byte tc = dis.readByte();
        StringObject so = read_newString(tc, dis);
        cd.addEnum(so.value);
        setHandle(handle, so);
        return new EnumObject(handle, cd, so);
    }
    public StringObject read_newString(byte tc, DataInputStream dis) throws IOException {
        byte[] data;
        if(tc == ObjectStreamConstants.TC_REFERENCE) {
                Content c = read_prevObject(dis);
                if(!(c instanceof StringObject)) {
                    throw new IOException("got reference for a string, but referenced value was something else!");
                }
                return (StringObject)c;
        }
        int handle = newHandle();
        if(tc == ObjectStreamConstants.TC_STRING) {
            int len = dis.readUnsignedShort();
            data = new byte[len];
        } else if(tc == ObjectStreamConstants.TC_LONGSTRING) {
            long len = dis.readLong();
            if(len < 0) {
                throw new IOException("invalid long string length: " + len);
            }
            if(len > 2147483647) {
                throw new IOException("long string is too long: " + len);
            }
            if(len < 65536) {
                debugerr("warning: small string length encoded as TC_LONGSTRING: " + len);
            }
            data = new byte[(int)len];
        } else if(tc == ObjectStreamConstants.TC_NULL) {
            throw new ValidityException("stream signaled TC_NULL when string type expected!");
        } else {
            throw new IOException("invalid tc byte in string: " + hex(tc));
        }
        dis.readFully(data);
        debug("reading new string: handle " + hex(handle) + " bufsz " + data.length);
        StringObject sobj = new StringObject(handle, data);
        setHandle(handle, sobj);
        return sobj;
    }
    public BlockData read_blockdata(byte tc, DataInputStream dis) throws IOException {
        int size;
        if(tc == ObjectStreamConstants.TC_BLOCKDATA) {
            size = dis.readUnsignedByte();
        } else if(tc == ObjectStreamConstants.TC_BLOCKDATALONG) {
            size = dis.readInt();
        } else {
            throw new IOException("invalid tc value for blockdata: " + hex(tc));
        }
        if(size < 0) {
            throw new IOException("invalid value for blockdata size: " + size);
        }
        byte[] b = new byte[size];
        dis.readFully(b);
        debug("read blockdata of size " + size);
        return new BlockData(b);
    }
    public Instance read_newObject(DataInputStream dis) throws IOException {
        ClassDescription cd = read_classDesc(dis);
        int handle = newHandle();
        debug("reading new object: handle " + hex(handle) + " classdesc " + cd.toString());
        Instance i = new Instance();
        i.classdesc = cd;
        i.handle = handle;
        setHandle(handle, i);
        read_Classdata(dis, i);
        debug("done reading object for handle " + hex(handle));
        return i;
    }

    /**
     * <p>
     * Read the next object corresponding to the spec grammar rule "content", and return
     * an object of type content.
     * </p>
     *
     * <p>
     * Usually, there is a 1:1 mapping of content items and returned instances.  The
     * one case where this isn't true is when an exception is embedded inside another
     * object.  When this is encountered, only the serialized exception object is
     * returned; it's up to the caller to backtrack in order to gather any data from the
     * object that was being serialized when the exception was thrown.
     * </p>
     *
     * @param tc the last byte read from the stream; it must be one of the TC_* values
     * within ObjectStreamConstants.*
     * @param dis the DataInputStream to read from
     * @param blockdata whether or not to read TC_BLOCKDATA (this is the difference
     * between spec rules "object" and "content").
     * @return an object representing the last read item from the stream
     * @throws IOException when a validity or I/O error occurs while reading
     */
    public Content read_Content(byte tc, DataInputStream dis, boolean blockdata) throws IOException {
        try {
            switch(tc) {
                case ObjectStreamConstants.TC_OBJECT:
                    return read_newObject(dis);
                case ObjectStreamConstants.TC_CLASS:
                    return read_newClass(dis);
                case ObjectStreamConstants.TC_ARRAY:
                    return read_newArray(dis);
                case ObjectStreamConstants.TC_STRING:
                case ObjectStreamConstants.TC_LONGSTRING:
                    return read_newString(tc, dis);
                case ObjectStreamConstants.TC_ENUM:
                    return read_newEnum(dis);
                case ObjectStreamConstants.TC_CLASSDESC:
                case ObjectStreamConstants.TC_PROXYCLASSDESC:
                    return handle_newClassDesc(tc, dis);
                case ObjectStreamConstants.TC_REFERENCE:
                    return read_prevObject(dis);
                case ObjectStreamConstants.TC_NULL:
                    return null;
                case ObjectStreamConstants.TC_EXCEPTION:
                    return read_Exception(dis);
                case ObjectStreamConstants.TC_BLOCKDATA:
                case ObjectStreamConstants.TC_BLOCKDATALONG:
                    if(blockdata == false) {
                        throw new IOException("got a blockdata TC_*, but not allowed here: " + hex(tc));
                    }
                    return read_blockdata(tc, dis);
                default:
                    throw new IOException("unknown content tc byte in stream: " + hex(tc));
            }
        } catch (ExceptionReadException ere) {
            return ere.getExceptionObject();
        }
    }

    /**
     * <p>
     * Reads in an entire ObjectOutputStream output on the given stream, filing
     * this object's content and handle maps with data about the objects in the stream.
     * </p>
     *
     * <p>
     * If shouldConnect is true, then jdeserialize will attempt to identify member classes
     * by their names according to the details laid out in the Inner Classes
     * Specification.  If it finds one, it will set the classdesc's flag indicating that
     * it is an member class, and it will create a reference in its enclosing class.
     * </p>
     *
     * @param is an open InputStream on a serialized stream of data
     * @param shouldConnect true if jdeserialize should attempt to identify and connect
     * member classes with their enclosing classes
     *
     * Also see the <pre>connectMemberClasses</pre> method for more information on the
     * member-class-detection algorithm.
     */
    public void run(InputStream is, boolean shouldConnect) throws IOException {
        LoggerInputStream lis = null;
        DataInputStream dis = null;
        try {
            lis = new LoggerInputStream(is);
            dis = new DataInputStream(lis);

            short magic = dis.readShort();
            if(magic != ObjectStreamConstants.STREAM_MAGIC) {
                throw new ValidityException("file magic mismatch!  expected " + ObjectStreamConstants.STREAM_MAGIC + ", got " + magic);
            }
            short streamversion = dis.readShort();
            if(streamversion != ObjectStreamConstants.STREAM_VERSION) {
                throw new ValidityException("file version mismatch!  expected " + ObjectStreamConstants.STREAM_VERSION + ", got " + streamversion);
            }
            reset();
            content = new ArrayList<Content>();
            while(true) {
                byte tc;
                try {
                    lis.record();
                    tc = dis.readByte();
                    if(tc == ObjectStreamConstants.TC_RESET) {
                        reset();
                        continue;
                    }
                } catch (EOFException eoe) {
                    break;
                }
                Content c = read_Content(tc, dis, true);
                System.out.println("read: " + c.toString());
                if(c != null && c.isExceptionObject()) {
                    c = new ExceptionState(c, lis.getRecordedData());
                }
                content.add(c);
            }
        } finally {
            if(dis != null) {
                try {
                    dis.close();
                } catch (Exception ignore) { }
            }
            if(lis != null) {
                try {
                    lis.close();
                } catch (Exception ignore) {}
            }
        }
        for(Content c: handles.values()) {
            c.validate();
        }
        if(shouldConnect) {
            connectMemberClasses();
            for(Content c: handles.values()) {
                c.validate();
            }
        }
        if(handles != null && handles.size() > 0) {
            HashMap<Integer, Content> hm = new HashMap<Integer, Content>();
            hm.putAll(handles);
            handlemaps.add(hm);
        }
    }
    public void dump(Getopt go) throws IOException {
        if(go.hasOption("-blockdata") || go.hasOption("-blockdatamanifest")) {
            List<String> bout = go.getArguments("-blockdata");
            List<String> mout = go.getArguments("-blockdatamanifest");
            FileOutputStream bos = null, mos = null;
            PrintWriter pw = null;
            try {
                if(bout != null && bout.size() > 0) {
                    bos = new FileOutputStream(bout.get(0));
                }
                if(mout != null && bout.size() > 0) {
                    mos = new FileOutputStream(mout.get(0));
                    pw = new PrintWriter(mos);
                    pw.println("# Each line in this file that doesn't begin with a '#' contains the size of");
                    pw.println("# an individual blockdata block written to the stream.");
                }
                for(Content c: content) {
                    System.out.println(c.toString());
                    if(c instanceof BlockData) {
                        BlockData bd = (BlockData)c;
                        if(mos != null) {
                            pw.println(bd.buf.length);
                        }
                        if(bos != null) {
                            bos.write(bd.buf);
                        }
                    }
                }
            } finally {
                if(bos != null) {
                    try {
                        bos.close();
                    } catch (IOException ignore) { }
                }
                if(mos != null) {
                    try {
                        pw.close();
                        mos.close();
                    } catch (IOException ignore) { }
                }
            }
        }
        if(!go.hasOption("-nocontent")) {
            System.out.println("//// BEGIN stream content output");
            for(Content c: content) {
                System.out.println(c.toString());
            }
            System.out.println("//// END stream content output");
            System.out.println("");
        }

        if(!go.hasOption("-noclasses")) {
            boolean showarray = go.hasOption("-showarrays");
            List<String> fpat = go.getArguments("-filter");
            System.out.println("//// BEGIN class declarations"
                    + (showarray? "" : " (excluding array classes)")
                    + ((fpat != null && fpat.size() > 0)
                        ? " (exclusion filter " + fpat.get(0) + ")"
                        : ""));
            for(Content c: handles.values()) {
                if(c instanceof ClassDescription) {
                    ClassDescription cl = (ClassDescription)c;
                    if(showarray == false && cl.isArrayClass()) {
                        continue;
                    }
                    // Member classes will be displayed as part of their enclosing
                    // classes.
                    if(cl.isStaticMemberClass() || cl.isInnerClass()) {
                        continue;
                    }
                    if(fpat != null && fpat.size() > 0 && cl.name.matches(fpat.get(0))) {
                        continue;
                    }
                    dump_ClassDesc(0, cl, System.out, go.hasOption("-fixnames"));
                    System.out.println("");
                }
            }
            System.out.println("//// END class declarations");
            System.out.println("");
        }
        if(!go.hasOption("-noinstances")) {
            System.out.println("//// BEGIN instance dump");
            for(Content c: handles.values()) {
                if(c instanceof Instance) {
                    Instance i = (Instance)c;
                    dump_Instance(0, i, System.out);
                }
            }
            System.out.println("//// END instance dump");
            System.out.println("");
        }
    }


    /**
     * <p>
     * Connects member classes according to the rules specified by the JDK 1.1 Inner
     * Classes Specification.
     * </p>
     *
     * <pre>
     * Inner classes:
     * for each class C containing an object reference member R named this$N, do:
     *     if the className of C matches the pattern O$I
     *     AND the className O matches the className of an existing type T
     *     AND T is the exact type referred to by R, then:
     *         don't display the declaration of R in normal dumping,
     *         consider C to be an inner class of O named I
     *
     * Static member classes (after):
     * for each class C matching the pattern O$I,
     * where O is the className of a class in the same package
     * AND C is not an inner class according to the above algorithm:
     *     consider C to be an inner class of O named I
     * </pre>
     *
     * <p>
     * This functions fills in the isInnerClass value in classdesc, the
     * isInnerClassReference value in field, the isLocalInnerClass value in
     * classdesc, and the isStaticMemberClass value in classdesc where necessary.
     * </p>
     *
     * <p>
     * A word on static classes: serializing a static member class S doesn't inherently
     * require serialization of its superClass class P.  Unlike inner classes, S doesn't
     * retain an instance of P, and therefore P's class description doesn't need to be
     * written.  In these cases, if superClass classes can be found, their static member
     * classes will be connected; but if they can't be found, the names will not be
     * changed and no ValidityException will be thrown.
     * </p>
     *
     * @throws ValidityException if the found values don't correspond to spec
     */
    public void connectMemberClasses() throws IOException {
        HashMap<ClassDescription, String> newnames = new HashMap<ClassDescription, String>();
        HashMap<String, ClassDescription> classes = new HashMap<String, ClassDescription>();
        HashSet<String> classnames = new HashSet<String>();
        for(Content c: handles.values()) {
            if(!(c instanceof ClassDescription)) {
                continue;
            }
            ClassDescription cd = (ClassDescription)c;
            classes.put(cd.name, cd);
            classnames.add(cd.name);
        }
        Pattern fpat = Pattern.compile("^this\\$(\\d+)$");
        Pattern clpat = Pattern.compile("^((?:[^\\$]+\\$)*[^\\$]+)\\$([^\\$]+)$");
        for(ClassDescription cd: classes.values()) {
            if(cd.classtype == ClassDescriptionType.PROXYCLASS) {
                continue;
            }
            for(Field f: cd.fields) {
                if(f.type != FieldType.OBJECT) {
                    continue;
                }
                Matcher m = fpat.matcher(f.name);
                if(!m.matches()) {
                    continue;
                }
                boolean islocal = false;
                Matcher clmat = clpat.matcher(cd.name);
                if(!clmat.matches()) {
                    throw new ValidityException("inner class enclosing-class reference field exists, but class className doesn't match expected pattern: class " + cd.name + " field " + f.name);
                }
                String outer = clmat.group(1), inner = clmat.group(2);
                ClassDescription outercd = classes.get(outer);
                if(outercd == null) {
                    throw new ValidityException("couldn't connect inner classes: outer class not found for field className " + f.name);
                }
                if(!outercd.name.equals(f.getJavaType())) {
                    throw new ValidityException("outer class field type doesn't match field type className: " + f.classname.value + " outer class className " + outercd.name);
                }
                outercd.addInnerClass(cd);
                cd.setIsLocalInnerClass(islocal);
                cd.setIsInnerClass(true);
                f.setIsInnerClassReference(true);
                newnames.put(cd, inner);
            }
        }
        for(ClassDescription cd: classes.values()) {
            if(cd.classtype == ClassDescriptionType.PROXYCLASS) {
                continue;
            }
            if(cd.isInnerClass()) {
                continue;
            }
            Matcher clmat = clpat.matcher(cd.name);
            if(!clmat.matches()) {
                continue;
            }
            String outer = clmat.group(1), inner = clmat.group(2);
            ClassDescription outercd = classes.get(outer);
            if(outercd != null) {
                outercd.addInnerClass(cd);
                cd.setIsStaticMemberClass(true);
                newnames.put(cd, inner);
            }
        }
        for(ClassDescription ncd: newnames.keySet()) {
            String newname = newnames.get(ncd);
            if(classnames.contains(newname)) {
                throw new ValidityException("can't rename class from " + ncd.name + " to " + newname + " -- class already exists!");
            }
            for(ClassDescription cd: classes.values()) {
                if(cd.classtype == ClassDescriptionType.PROXYCLASS) {
                    continue;
                }
                for(Field f: cd.fields) {
                    if(f.getJavaType().equals(ncd.name)) {
                        f.setReferenceTypeName(newname);
                    }
                }
            }
            if(classnames.remove(ncd.name) == false) {
                throw new ValidityException("tried to remove " + ncd.name + " from classnames cache, but couldn't find it!");
            }
            ncd.name = newname;
            if(classnames.add(newname) == false) {
                throw new ValidityException("can't rename class to " + newname + " -- class already exists!");
            }
        }
    }

    /**
     * Decodes a class className according to the field-descriptor format in the jvm spec,
     * section 4.3.2.
     * @param fdesc className in field-descriptor format (Lfoo/bar/baz;)
     * @param convertSlashes true iff slashes should be replaced with periods (true for
     * "real" field-descriptor format; false for names in classdesc)
     * @return a fully-qualified class className
     * @throws ValidityException if the className isn't valid
     */
    public static String decodeClassName(String fdesc, boolean convertSlashes) throws ValidityException {
        if(fdesc.charAt(0) != 'L' || fdesc.charAt(fdesc.length()-1) != ';' || fdesc.length() < 3) {
            throw new ValidityException("invalid className (not in field-descriptor format): " + fdesc);
        }
        String subs = fdesc.substring(1, fdesc.length()-1);
        if(convertSlashes) {
            return subs.replace('/', '.');
        }
        return subs;
    }

    public static String hexnoprefix(long value) {
        return hexnoprefix(value, 2);
    }
    public static String hexnoprefix(long value, int len) {
        if(value < 0) {
            value = 256 + value;
        }
        String s = Long.toString(value, 16);
        while(s.length() < len) {
            s = "0" + s;
        }
        return s;
    }
    public static String hex(long value) {
        return "0x" + hexnoprefix(value);
    }
    public static void debugerr(String message) {
        System.err.println(message);
    }
    public void debug(String message) {
        if(debugEnabled) {
            System.out.println(message);
        }
    }
//
//    public static void main(String[] args) {
//        HashMap<String, Integer> options = new HashMap<String, Integer>();
//        Getopt go = new Getopt();
//        go.addOption("-help", 0, "Show this list.");
//        go.addOption("-debug", 0, "Write debug info generated during parsing to stdout.");
//        go.addOption("-filter", 1, "Exclude classes that match the given String.matches() regex from class output.");
//        go.addOption("-nocontent", 0, "Don't output descriptions of the content in the stream.");
//        go.addOption("-noinstances", 0, "Don't output descriptions of every instance.");
//        go.addOption("-showarrays", 0, "Show array class declarations (e.g. int[]).");
//        go.addOption("-noconnect", 0, "Don't attempt to connect member classes to their enclosing classes.");
//        go.addOption("-fixnames", 0, "In class names, replace illegal Java identifier characters with legal ones.");
//        go.addOption("-noclasses", 0, "Don't output class declarations.");
//        go.addOption("-blockdata", 1, "Write raw blockdata out to the specified file.");
//        go.addOption("-blockdatamanifest", 1, "Write blockdata manifest out to the specified file.");
//        try {
//            go.parse(args);
//        } catch (Getopt.OptionParseException ope) {
//            System.err.println("argument error: " + ope.getMessage());
//            System.out.println(go.getDescriptionString());
//            System.exit(1);
//        }
//        if(go.hasOption("-help")) {
//            System.out.println(go.getDescriptionString());
//            System.exit(1);
//        }
//        List<String> fargs = go.getOtherArguments();
//        if(fargs.size() < 1) {
//            debugerr("args: [options] file1 [file2 .. fileN]");
//            System.err.println("");
//            System.err.println(go.getDescriptionString());
//            System.exit(1);
//        }
//        for(String filename: fargs) {
//            FileInputStream fis = null;
//            try {
//                fis = new FileInputStream(filename);
//                JDeserializer jd = new JDeserializer();
//                if(go.hasOption("-debug")) {
//                    jd.debugEnabled = true;
//                } else {
//                    jd.debugEnabled = false;
//                }
//                jd.run(fis, !go.hasOption("-noconnect"));
//                jd.dump(go);
//            } catch(EOFException eoe) {
//                debugerr("EOF error while attempting to decode file " + filename + ": " + eoe.getMessage());
//                eoe.printStackTrace();
//            } catch(IOException ioe) {
//                debugerr("error while attempting to decode file " + filename + ": " + ioe.getMessage());
//                ioe.printStackTrace();
//            } finally {
//                if(fis != null) {
//                    try {
//                        fis.close();
//                    } catch (Exception ignore) { }
//                }
//            }
//        }
//    }
}
