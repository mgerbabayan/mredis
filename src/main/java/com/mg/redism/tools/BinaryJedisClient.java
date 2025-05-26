package com.mg.redism.tools;

import javafx.util.Pair;
import redis.clients.jedis.resps.Tuple;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;

public interface BinaryJedisClient {

    Map<String, Pair<byte[], Set<byte[]>>> keys(final byte[] pattern, Map<String, byte[]> cursors) throws Exception;

    String type(final byte[] key);

    Long ttl(final byte[] key);

    Map<byte[], byte[]> hgetAll(final byte[] key);

    List<byte[]> lrange(final byte[] key, final long start, final long end);

    byte[] get(final byte[] key);

    List<Tuple> zrangeWithScores(final byte[] key, final long start, final long end);

    String ping();

    Map<String, List<byte[]>> configGet(final byte[] pattern);

    Map<String, List<String>> clientList();

    Map<String, List<String>> info();

    void close() throws IOException;

    long dbSize();

    String auth(final String password);

    void connect();

    boolean isCluster();

    int amountOfShards();
}
