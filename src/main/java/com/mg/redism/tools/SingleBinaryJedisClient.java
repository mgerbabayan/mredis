package com.mg.redism.tools;

import com.mg.redism.Constants;
import javafx.util.Pair;
import redis.clients.jedis.*;
import redis.clients.jedis.params.ScanParams;
import redis.clients.jedis.resps.ScanResult;
import redis.clients.jedis.resps.Tuple;

import java.io.IOException;
import java.util.*;

public class SingleBinaryJedisClient implements BinaryJedisClient {

    private Jedis jedis;
    private String name;

    public SingleBinaryJedisClient(Jedis jedis, String name) {
        this.jedis = jedis;
        this.name = name;
    }

    @Override
    public Map<String, Pair<byte[], Set<byte[]>>> keys(byte[] pattern, Map<String, byte[]> cursors) {
        Map<String, Pair<byte[], Set<byte[]>>> retMap = new HashMap<>();
        Pair<byte[], Set<byte[]>> p = readBulk(cursors == null ? null : cursors.get(name), jedis, pattern);
        if (p != null) {
            retMap.put(name, p);
        }
        return retMap;
    }

    private Pair<byte[], Set<byte[]>> readBulk(byte[] current, Jedis jedis, byte[] pattern) {
        if (current != null && '0' == current[0] && current.length == 1) {
            return null;
        }

        ScanParams scanParams = new ScanParams();
        scanParams.count(Constants.MAX_PACK);
        scanParams.match(pattern);

        Set<byte[]> result = new HashSet<>();
        current = current == null ? "0".getBytes() : current;

        while (result.size() < Constants.MAX_PACK) {
            ScanResult<byte[]> results = jedis.scan(current, scanParams);
            current = results.getCursorAsBytes();
            result.addAll(results.getResult());
            if ('0' == current[0] && current.length == 1) {
                break;
            }
        }

        return new Pair<>(current, result);
    }

    @Override
    public String type(byte[] key) {
        return jedis.type(key);
    }

    @Override
    public Long ttl(byte[] key) {
        return jedis.ttl(key);
    }

    @Override
    public Map<byte[], byte[]> hgetAll(byte[] key) {
        return jedis.hgetAll(key);
    }

    @Override
    public List<byte[]> lrange(byte[] key, long start, long end) {
        return jedis.lrange(key, start, end);
    }

    @Override
    public byte[] get(byte[] key) {
        return jedis.get(key);
    }

    @Override
    public List<Tuple> zrangeWithScores(byte[] key, long start, long end) {
        return jedis.zrangeWithScores(key, start, end);
    }

    @Override
    public String ping() {
        return jedis.ping();
    }

    @Override
    public Map<String, List<byte[]>> configGet(byte[] pattern) {
        Map<String, List<byte[]>> retMap = new HashMap<>();

        //retMap.put(name, jedis.configGet(pattern));@TODO revert!!!
        return retMap;
    }

    @Override
    public Map<String, List<String>> clientList() {
        Map<String, List<String>> retMap = new HashMap<>();
        retMap.put(name, Arrays.asList(new String(jedis.clientListBinary()).split("\n")));
        return retMap;
    }

    @Override
    public Map<String, List<String>> info() {
        Map<String, List<String>> info = new HashMap<>();
        //jedis.info();
        info.put(name, Arrays.asList(jedis.info().split("\n")));
        return info;
    }

    @Override
    public void close() throws IOException {
        jedis.close();
    }

    @Override
    public long dbSize() {
        return jedis.dbSize();
    }

    @Override
    public String auth(String password) {
        return jedis.auth(password);
    }

    @Override
    public void connect() {
        jedis.connect();
    }

    @Override
    public boolean isCluster() {
        return false;
    }

    @Override
    public int amountOfShards() {
        return 1;
    }
}
