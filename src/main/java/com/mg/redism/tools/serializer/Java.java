package com.mg.redism.tools.serializer;

import com.mg.redism.jsr.JDeserializer;

import java.io.FileInputStream;
import java.io.Serializable;
import java.util.Date;
import java.util.Map;

import static com.mg.redism.jsr.JsonUtil.buildJson;

/**
 * Created by mger on 05.09.2019.
 */
public class Java {

    private static final byte[] magic = {(byte) 0xAC, (byte) 0xED};

    final static byte TC_NULL = (byte) 0x70;
    final static byte TC_REFERENCE = (byte) 0x71;
    final static byte TC_CLASSDESC = (byte) 0x72;
    final static byte TC_OBJECT = (byte) 0x73;
    final static byte TC_STRING = (byte) 0x74;
    final static byte TC_ARRAY = (byte) 0x75;
    final static byte TC_CLASS = (byte) 0x76;
    final static byte TC_BLOCKDATA = (byte) 0x77;
    final static byte TC_ENDBLOCKDATA = (byte) 0x78;
    final static byte TC_RESET = (byte) 0x79;
    final static byte TC_BLOCKDATALONG = (byte) 0x7A;
    final static byte TC_EXCEPTION = (byte) 0x7B;
    final static byte TC_LONGSTRING = (byte) 0x7C;
    final static byte TC_PROXYCLASSDESC = (byte) 0x7D;
    final static byte TC_ENUM = (byte) 0x7E;
    final static int baseWireHandle = 0x7E0000;

    public Map<String, String> parse() {
        return null;
    }

    public enum TESTE{
        RECORD,PLAYER,RECORDER,ORDER
    }

    public static void main(String[] args) throws Exception {
//        ApplicationRecord record = new ApplicationRecord();
//        record.setIii(235);
//        record.setId("12345679a");
//        // record.setDate(new Date());
//        record.setWwwww(new TTest12());
//
//        Map<String, Object> ddd = new HashMap<>();
//        Map<String, Object> bbbbb = new HashMap<>();
//        ddd.put("1234dfgh", bbbbb);
//        bbbbb.put("9999", "sasasasa");
//        bbbbb.put("222", "eeeeeea");
//        bbbbb.put("dt", new Date());
//        bbbbb.put("dt2", null);
//        bbbbb.put("eee", TESTE.RECORDER);
//        bbbbb.put("lst", new ArrayList<>(Arrays.asList("test2", "test3", "test5")));
//        bbbbb.put("sset", new HashSet<>(Arrays.asList("test2", "test3", "test5")));
//        bbbbb.put("arrr", new String[]{"123456", "45567", "asddf"});
//        TreeSet<String> ddssd = new TreeSet<>();
//        ddssd.add("1qwe");
//        ddssd.add("2qwe");
//        ddssd.add("3qwe");
//        bbbbb.put("tree1", ddssd);
//
//        TreeMap tre = new TreeMap();
//        tre.put("11", "aaasd");
//        tre.put("12", "aabbb");
//        tre.put("13", "aabbcc");
//        tre.put("14", "aaffss");
//
//        TreeMap tre2 = new TreeMap();
//        tre2.put("112", "aaas2d");
//        tre2.put("122", "aab2bb");
//        tre2.put("132", "aabb2cc");
//        tre2.put("142", "aaff2ss");
//        bbbbb.put("ee23", Arrays.asList(tre, tre2));
//        record.setFff(ddd);
//
//        TTest12 rrr = new TTest12();
//        rrr.ln = 35;
//        rrr.test = "z3cv";
//        rrr.ui = 21;
//        record.setYyyyy(rrr);
//
//        FileOutputStream fos = new FileOutputStream("test9b.data");
//        ObjectOutputStream objectOut = new ObjectOutputStream(fos);
//        objectOut.writeObject(record);
//        fos.flush();
//        fos.close();
//
//        System.exit(9);

        System.out.println("aaaaaaaaaaasssssssssddddddd");
        FileInputStream fis = new FileInputStream("test9b.data");
//        //ObjectInputStream objectOut = new ObjectInputStream(fis);
//
//
//        JavaDeserializer javaDeserializer = new JavaDeserializer();
//        javaDeserializer.deserialize(fis);
//
//        Map<String, Object> map = new HashMap<>();
//        print("", javaDeserializer.getNonprimitiveClasses().get(0), map);
//
//        System.out.println(map);
//        System.exit(9);

//
//        Object obj = objectOut.readObject();
//
//        System.out.println(obj);

//         byte[] data = new byte[(int) new File("test.data").length()];
//        fis.read(data);
//        fis.close();


        JDeserializer jd = new JDeserializer();
        jd.run(fis, true);
        System.out.println(buildJson(jd.getContent()));
        // jd.dump(go);
        //readObject(data);

        //  ObjectReader objectReader = new ObjectReader(data);

        //   objectReader.parse();
    }

//    private static void readObject(byte data[], int cur) {
//        byte first = data[cur++];
//        if (first == TC_CLASS) {
//            readClass(data, cur);
//            return;
//        }
//
//        //reading length of field className
//        int len = bytesToInt(first, data[cur++]);
//        System.out.println(len);
//
//        //reading field className
//        String fieldName = bytesToString(data, cur, len);
//        System.out.println(fieldName);
//        cur += len;
//    }

    private static int bytesToInt(byte... bytes) {
        int i = 0;
        for (byte b : bytes) {
            i = i * 16 + b;
        }
        return i;
    }


    public static class ObjectReader {
        private int streamVersion;
        private int cur;
        private byte[] data;

        public ObjectReader(byte[] data) {
            this.data = data;
        }

        public void parse() throws Exception {
            //checking for magic
            if (data[cur++] != magic[0] || data[cur++] != magic[1]) {
                throw new Exception("Failed to parse object ! Wrong magic bytes!");
            }
            streamVersion = bytesToInt(data[cur++], data[cur++]);

            if (data[cur++] == TC_OBJECT) {
                //reading object
                readObject();
            }
        }

        private Object readToken() {
            switch (data[cur++]) {
                case TC_CLASS:

                case TC_OBJECT:

                case TC_STRING:
                    //string
                    int len = bytesToInt(data[cur++], data[cur++]);

                    // return new Pair<Class, Object>(String.class, bytesToString(len));
            }
            return null;
        }

        private String bytesToString(int length) {
            String s = new String(data, cur, length);
            cur += length;
            return s;
        }

        private long bytesToLong() {
            long ret = 0;
            for (int i = 0; i < 8; i++) {
                ret = ret * 256 + data[cur++];
            }

            return ret;
        }

        private void readClassDef() {
            //reading class className
            int len = bytesToInt(data[cur++], data[cur++]);
            String className = bytesToString(len);
            System.out.println(className);
            long serialId = bytesToLong();

            System.out.println(serialId);
            int flag = bytesToInt(data[cur++]);
            System.out.println(flag);
            int fields = bytesToInt(data[cur++], data[cur++]);
            System.out.println(fields);

            //reading fields
            for (int i = 0; i < fields; i++) {
                byte type = data[cur++]; // type
                //reading len
                int lField = bytesToInt(data[cur++], data[cur++]);
                String fieldName = bytesToString(lField);
                System.out.println("f=" + fieldName);

                if (type == 'L') {
                    if (data[cur++] == 0x74) {
                        int lFieldType = bytesToInt(data[cur++], data[cur++]);
                        String fieldType = bytesToString(lFieldType);
                        System.out.println("t=" + fieldType);
                    }
                }
            }
            readObject();
        }

        private void readObject() {
            if (cur >= data.length) {
                return;
            }
            //checking class def
            switch (data[cur++]) {
                case TC_CLASSDESC:
                    readClassDef();
                    break;
                case TC_OBJECT:
                case TC_ENDBLOCKDATA:
                    readObject();
                    break;
                case TC_BLOCKDATA:
                    //reading data
                    readData();
                    break;
                case TC_NULL:
                    readObject();
                    break;

            }
        }

        private void readData() {
            if (cur >= data.length) {
                return;
            }
            switch (data[cur++]) {
                case 0x08://Date
                    System.out.println(new Date(bytesToLong()));
                    break;
                case TC_STRING:
                    int len = bytesToInt(data[cur++], data[cur++]);
                    System.out.println(bytesToString(len));
                    break;
            }
            readData();
        }

        private void readClass() {
            int len = bytesToInt(data[cur++], data[cur++]);
            //rading class className
            System.out.println(bytesToString(len));
            cur += len;

            long versionID = bytesToLong();
            System.out.println(versionID);
            cur += 8;

            //ignoring flags
            int flag = data[cur++];

            int fields = bytesToInt(data[cur++], data[cur++]);

            System.out.println("fields=" + fields);


//  `B'       // byte
//  `C'       // char
//  `D'       // double
//  `F'       // float
//  `I'       // integer
//  `J'       // long
//  `S'       // short
//  `Z'       // boolean
            switch ((char) data[cur++]) {
                case 'L':
                    //object
                    // readObject(data);
                    break;
                case 'I':
                    //integer
                    break;
            }
        }

    }
}

//class TextA implements Serializable{
//    int abc=5;
//}

//class AppExt implements Serializable{
//    String det="asdfgh";
//}

class TTest12 implements Serializable {
    String test = "abcd";
    int ui = 7;
    long ln = 11;
}


class ApplicationRecord implements Serializable {
    private String id;
    private int iii;
    //private Date date;
    private int i = 5;
    private TTest12 wwwww;
    private TTest12 yyyyy;

    private Map<String, Object> fff;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public int getIii() {
        return iii;
    }

    public Map<String, Object> getFff() {
        return fff;
    }

    public void setFff(Map<String, Object> fff) {
        this.fff = fff;
    }

    public void setIii(int iii) {
        this.iii = iii;
    }

    //    public Date getDate() {
//        return date;
//    }
//
//    public void setDate(Date date) {
//        this.date = date;
//    }

    public TTest12 getWwwww() {
        return wwwww;
    }

    public void setWwwww(TTest12 wwwww) {
        this.wwwww = wwwww;
    }

    public TTest12 getYyyyy() {
        return yyyyy;
    }

    public void setYyyyy(TTest12 yyyyy) {
        this.yyyyy = yyyyy;
    }

}