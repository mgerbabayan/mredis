package com.mg.redism.tools;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.KryoSerializable;
import com.mg.redism.Constants;
import com.mg.redism.DetailsControler;
import com.mg.redism.jsr.JDeserializer;
import com.mg.redism.jsr.JsonUtil;
import com.mg.redism.to.RConnection;
import com.mg.redism.to.RField;
import com.mg.redism.to.RNode;
import com.mg.redism.to.RType;
import javafx.concurrent.Task;
import javafx.util.Pair;

import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.*;
import redis.clients.jedis.exceptions.JedisConnectionException;
import redis.clients.jedis.resps.Tuple;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Created by mger on 01.09.2019.
 */
public class KeysUtil {

    private static final Logger logger = LoggerFactory.getLogger(KeysUtil.class);

//    Set<HostAndPort> addresses = new HashSet<>();
//    JedisCluster cluster = new JedisCluster(addresses);


    private BinaryJedisClient jedis;
    private RConnection connection;

    public KeysUtil(BinaryJedisClient jedis, RConnection connection) {
        this.jedis = jedis;
        this.connection = connection;
    }

    public Long getDbSize() {
        checkAndReconnect();
        return jedis.dbSize();
    }

    public LinkedHashMap<String, RNode> getKeys(String search, DataLoader task, LinkedHashMap<String, RNode> ret) throws Exception {
        long shards = jedis.amountOfShards();
        checkAndReconnect();
        long max = getDbSize();
        long progress = 0;
        Map<String, Pair<byte[], Set<byte[]>>> keys = null;
        LinkedHashMap<String, RNode> dataMap = ret;
        boolean active = true;

        while (active && !task.isCancelled()) {
            long gSize = 0;
            keys = jedis.keys(search.getBytes(), keys == null ? null : keys.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().getKey())));
            logger.info("--- loding data from nodes {} ", keys.keySet());
            synchronized (ret) {
                //building tree
                active = false;
                for (Map.Entry<String, Pair<byte[], Set<byte[]>>> entry : keys.entrySet()) {
                    active = true;
                    keys:
                    for (byte[] kb : entry.getValue().getValue()) {
                        String server = entry.getKey();
                        gSize++;
                        String k = new String(kb);

                        String tokens[] = k.split(":");
                        StringBuilder path = new StringBuilder(tokens[0]);
                        RNode current = dataMap.get(tokens[0]);
                        if (current == null) {
                            current = new RNode(tokens[0], path.toString());
                            current.setServerNode(server);
                            dataMap.put(tokens[0], current);
                        }
                        for (int i = 1; i < tokens.length; i++) {
                            String sp = tokens[i];
                            path.append(":");
                            path.append(sp);
                            LinkedHashMap<String, RNode> currentChildren = current.getChildren();
                            if (currentChildren == null) {
                                currentChildren = new LinkedHashMap<>();
                                current.setChildren(currentChildren);
                            }
                            RNode child = currentChildren.get(sp);
                            if (child == null) {
                                //does not exist, check amount of children
                                if (currentChildren.size() < Constants.MAX_CHILDREN) {
                                    child = new RNode(sp, path.toString());
                                    child.setParent(current);
                                    child.setServerNode(server);
                                    currentChildren.put(sp, child);
                                } else {
                                    RNode rNode = currentChildren.get("more");
                                    if (rNode == null) {
                                        //adding new type
                                        rNode = new RNode<>("more...", null);
                                        rNode.setType(RType.more);
                                        currentChildren.put("more", rNode);
                                        rNode.setParent(current);
                                    } else {
                                        rNode.setName("(" + (current.getSize() - Constants.MAX_CHILDREN + 1) + " more)");
                                    }
                                    current.setSize(current.getSize() + 1);
                                    continue keys;
                                }
                            }

                            current.setSize(current.getSize() + 1);
                            current = child;

                            if (tokens.length == i + 1) {
                                //last leaf
                            }
                        }
                    }
                }
                if (!task.isCancelled()) {
                    progress += (gSize > 0 ? gSize : Constants.MAX_PACK * shards);
                    if (progress >= max) {
                        progress = max - Constants.MAX_PACK * shards-10;
                    }
                    logger.info("increasing progress on {} , max={}, shards={}, gSize={}", (gSize > 0 ? gSize : Constants.MAX_PACK * shards), max, shards, gSize);
                    task.setProgress(progress, max);
                }
            }

        }

        return ret;
    }

    public void getRNodeFields(RNode rNode) {

        checkAndReconnect();
        String type = jedis.type(rNode.getPath().getBytes());
        rNode.setTtl(jedis.ttl(rNode.getPath().getBytes()));

        List<RField> fields = new ArrayList<>();
        rNode.setData(fields);

        RType rType = RType.valueOf(type);
        rNode.setType(rType);

        switch (rType) {
            case hash:
                Map<byte[], byte[]> mpFields = jedis.hgetAll(rNode.getPath().getBytes());
                if (mpFields == null) {
                    return;
                }
                for (Map.Entry<byte[], byte[]> entry : mpFields.entrySet()) {
                    fields.add(new RField(transform(entry.getKey(), DetailsControler.ViewMode.Auto), entry.getValue()));
                }
                break;
            case list:
                List<byte[]> list = jedis.lrange(rNode.getPath().getBytes(), 0, -1);

                if (list == null) {
                    return;
                }
                for (int i = 0; i < list.size(); i++) {
                    fields.add(new RField(rNode.getPath() + "[" + i + "]", list.get(i)));
                }
                break;
            case string:
                String val = new String(jedis.get(rNode.getPath().getBytes()));
                fields.add(new RField(rNode.getPath(), val));
                break;
            case zset:
                List<Tuple> zIndexes = jedis.zrangeWithScores(rNode.getPath().getBytes(), 0, -1);
                for (Tuple tuple : zIndexes) {
                    fields.add(new RField(new String(tuple.getElement()), "Score=" + tuple.getScore()));
                }
                break;
            default:
                // System.out.println("" + type);
                logger.info("Got type {}", type);
        }
    }

    public static String transform(byte[] data, DetailsControler.ViewMode mode) {
        switch (mode) {
            case Auto:
                try {
                    JDeserializer jDeserializer = new JDeserializer();
                    jDeserializer.run(new ByteArrayInputStream(data), true);
                    return JsonUtil.buildJson(jDeserializer.getContent());
                } catch (IOException e) {
                    // ignoring
                }
                try {
                    Map mp = new Kryo().readObject(null, HashMap.class);
                } catch (Exception e) {
                    // ignoring
                }
                return new String(data);
            case Java:
                try {
                    JDeserializer jDeserializer = new JDeserializer();
                    jDeserializer.run(new ByteArrayInputStream(data), true);
                    return JsonUtil.buildJson(jDeserializer.getContent());
                } catch (IOException e) {
                    e.printStackTrace();
                }
                return "<Failed to transform to Java object>";
            case Text:
            default:
                return new String(data);
        }
    }

    public BinaryJedisClient checkAndReconnect() {

        try {
            logger.info("Pinging redis -- {}{}", connection.isSsl() ? "SSL:" : "", connection.getServer());
            jedis.ping();
        } catch (JedisConnectionException ex) {

            this.jedis = connect(connection.getServer(), connection.getPass(), 15000, connection.isSsl());
            jedis.auth(connection.getPass());
            logger.info("Connection was closed by server - will try to reconnect -- {}{}", connection.isSsl() ? "SSL:" : "", connection.getServer());
        }

        return jedis;
    }

    public static BinaryJedisClient connect(String address, String pass, long timeout, boolean ssl) {

        List<HostAndPort> servers = Arrays.stream(address.split(",")).map(host -> {
            String h[] = host.split(":");
            int port = h.length == 1 ? 6379 : Integer.valueOf(h[1]);
            return new HostAndPort(h[0], port);
        }).collect(Collectors.toList());

        boolean isCluster = false;
        for (HostAndPort server : servers) {
            isCluster = checkRedisHostAndPort(server.getHost(), server.getPort(), pass, ssl);
        }

        if (servers.size() == 1) {
            //cluster cannot be with single node - we should use single instance
            isCluster = false;
        }

        BinaryJedisClient binaryJedisClient = null;
        //trying to check if this is cluster
        if (isCluster) {
            //is cluster
            binaryJedisClient = new ClusterBinaryJedisClient(new JedisCluster(new HashSet<>(servers), 15000, 15000, 3, pass, null, new GenericObjectPoolConfig(), ssl));
        } else {
            // this is not cluster
            binaryJedisClient = new SingleBinaryJedisClient(new Jedis(servers.get(0).getHost(), servers.get(0).getPort(), 15000, ssl), servers.get(0).getHost() + ":" + servers.get(0).getPort());
        }

        return binaryJedisClient;
    }

    private static boolean checkRedisHostAndPort(String server, int port, String pass, boolean ssl) {
        try {
            Jedis binaryJedis = null;
            try {
                logger.info("connecting to {}:{} ssl={}", server, port, ssl);
                binaryJedis = new Jedis(server, port, 15000, ssl);
                binaryJedis.auth(pass);
                binaryJedis.connect();
                return binaryJedis.info().toLowerCase().contains("redis_mode:cluster");
            } finally {
                if (binaryJedis != null) {
                    binaryJedis.close();
                }
            }
        } catch (Throwable ex) {
            //do nothing
            throw ex;
        }
    }

    public Map<String, List<String[]>> getConfig() {
        // Map<String, String> ret = new HashMap<>();
        Map<String, List<String[]>> retMap = new LinkedHashMap<>();

        checkAndReconnect();
        Map<String, List<byte[]>> resultM = jedis.configGet("*".getBytes());
        for (Map.Entry<String, List<byte[]>> entry : resultM.entrySet()) {
            List<String[]> ret = new ArrayList<>();
            retMap.put(entry.getKey(), ret);
            StringBuilder sb = new StringBuilder();
            List<byte[]> result = entry.getValue();
            for (int i = 0; i < result.size(); i += 2) {
                ret.add(new String[]{result.get(i) == null ? "null" : new String(result.get(i)), result.get(i + 1) == null ? "null" : new String(result.get(i + 1))});
            }
        }
        return retMap;
    }


    public Map<String, List<String[]>> getClients() {
        // Map<String, String> ret = new HashMap<>();
        Map<String, List<String[]>> retMap = new LinkedHashMap<>();

        //id=2 addr=127.0.0.1:60173 fd=9 name= age=162157 idle=159539 flags=N db=0 sub=0 psub=0 multi=-1 qbuf=0 qbuf-free=0 obl=0 oll=0 omem=18446744073709546386 events=r cmd=hset
        checkAndReconnect();
        Map<String, List<String>> linesMap = jedis.clientList();
        for (Map.Entry<String, List<String>> entry : linesMap.entrySet()) {
            List<String[]> ret = new ArrayList<>();
            retMap.put(entry.getKey(), ret);
            for (String line : entry.getValue()) {
                ret.add(splitKeyValue(line));
            }
        }
        return retMap;
    }

    private String[] splitKeyValue(String line) {
        int i = line.indexOf("addr=");
        if (i >= 0) {
            i += 5;
            int j = line.indexOf(" ", i);
            return new String[]{line.substring(i, j), line};
        }
        return new String[]{"unknown", line};
    }

    private static final SimpleDateFormat sdf = new SimpleDateFormat("yyyy.MM.dd HH:mm:ss");

    public Map<String, List<String[]>> getInfo() {
        // Map<String, String> ret = new HashMap<>();
        Map<String, List<String[]>> retMap = new LinkedHashMap<>();
        checkAndReconnect();
        Map<String, List<String>> linesMap = jedis.info();

        for (Map.Entry<String, List<String>> entry : linesMap.entrySet()) {
            List<String[]> ret = new ArrayList<>();
            retMap.put(entry.getKey(), ret);
            for (String line : entry.getValue()) {
                int i = line.indexOf(":");
                if (i < 0) {
                    ret.add(new String[]{line, ""});
                    continue;
                }

                String key = line.substring(0, i);
                String val = line.substring(i + 1).trim();

                if ("last_save_time".equalsIgnoreCase(key)) {
                    try {
                        ret.add(new String[]{key, sdf.format(new Date(1000 * Long.valueOf(val))) + "  (" + val + ")"});
                        continue;
                    } catch (Exception ex) {
                        //nothing to do
                    }
                }
                ret.add(new String[]{key, val});
            }
        }
        return retMap;
    }

}
