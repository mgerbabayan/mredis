package com.mg.redism.tools.serializer;


import redis.clients.jedis.Jedis;

import java.io.*;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class TestSave {

    public static void main2(String[] args) throws IOException {
        Jedis jedis = new Jedis("localhost", 6397);
        jedis.auth("123123");
        HashMap<String, Object> mp = new HashMap<>();
        ApplicationRecord ap = new ApplicationRecord();
        ap.setId("sasasasasasasa");
        mp.put("ttt", ap);


        jedis.hset("test12:t3".getBytes(), toByteObject("field1"), toByteObject(mp));
    }

//    public static void main(String[] args) throws Exception {
//        APP app = new APP();
//        app.setA(123);
//        app.setDt(new Date());
//        app.setTest("qwerttakjsnxjkNxjkaskjndakd andjk asndjk ansjkd naskdn kjasn kjsnk aadsadsadasadaassss");
//        Kryo kryo = new Kryo();
//        kryo.setInstantiatorStrategy(new StdInstantiatorStrategy());
//        FileOutputStream fo = new FileOutputStream("filename.txt");
//        Output out = new Output(fo);
//        kryo.writeClassAndObject(out, app);
//        out.flush();
//        out.close();
//
//        FileInputStream in = new FileInputStream("filename.txt");
//        Object mp = new Kryo().readObject(new Input(in), APP.class);
//        System.out.println(mp);
//        in.close();
//    }

    private static byte[] toByteObject(Serializable s) throws IOException {
        ByteArrayOutputStream b = new ByteArrayOutputStream();
        ObjectOutputStream outputStream = new ObjectOutputStream(b);
        outputStream.writeObject(s);
        return b.toByteArray();
    }

    public static class APP implements Serializable {
        private String test;
        private int a;
        private Date dt;

        public String getTest() {
            return test;
        }

        public int getA() {
            return a;
        }

        public Date getDt() {
            return dt;
        }

        public void setTest(String test) {
            this.test = test;
        }

        public void setA(int a) {
            this.a = a;
        }

        public void setDt(Date dt) {
            this.dt = dt;
        }
    }

}
