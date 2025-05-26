package com.mg.redism.tools;

import com.mg.redism.Constants;
import javafx.util.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.*;
import redis.clients.jedis.params.ScanParams;
import redis.clients.jedis.resps.ScanResult;
import redis.clients.jedis.resps.Tuple;


import java.io.IOException;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class ClusterBinaryJedisClient implements BinaryJedisClient {
    private static Logger logger = LoggerFactory.getLogger(ClusterBinaryJedisClient.class);

    private JedisCluster jedisCluster;

    private ExecutorService executorService;

    public ClusterBinaryJedisClient(JedisCluster jedisCluster) {
        this.jedisCluster = jedisCluster;
        this.executorService = Executors.newFixedThreadPool(6);
    }

    @Override
    public Map<String, Pair<byte[], Set<byte[]>>> keys(byte[] pattern, Map<String, byte[]> cursors) throws Exception {
        //need to go across all nodes
        Map<String, Pair<byte[], Set<byte[]>>> resultMap = new ConcurrentHashMap<>();

        cursors = cursors == null ? new HashMap<>() : cursors;
        final Map<String, byte[]> cursorsA = cursors;
        boolean isEmpty = true;
        List<CompletableFuture<Boolean>> futures = new ArrayList<>();
        for (Map.Entry<String, ConnectionPool> jedisPoolEntry : jedisCluster.getClusterNodes().entrySet()) {
            CompletableFuture<Boolean> future = CompletableFuture.supplyAsync(
                    new MSup(jedisPoolEntry, resultMap, cursors, pattern), executorService);
            futures.add(future);

        }
        for (CompletableFuture<Boolean> future : futures) {
            if (future.get()) {
                continue;
            }
            isEmpty = false;
        }
        logger.info("isEmpty global is {} result size is {} ", isEmpty, resultMap.size());
        return isEmpty ? new HashMap<>() : resultMap;
    }


    static class MSup implements Supplier<Boolean> {
        private Map.Entry<String, ConnectionPool> jedisPoolEntry;
        private Map<String, Pair<byte[], Set<byte[]>>> resultMap;
        private Map<String, byte[]> cursors;

        private byte[] pattern;

        public MSup(Map.Entry<String, ConnectionPool> jedisPoolEntry,
                    Map<String, Pair<byte[], Set<byte[]>>> resultMap,
                    Map<String, byte[]> cursors,
                    byte[] pattern) {
            this.jedisPoolEntry = jedisPoolEntry;
            this.resultMap = resultMap;
            this.cursors = cursors;
            this.pattern = pattern;
        }

        @Override
        public Boolean get() {
            boolean isEmptyA = true;

            Connection jedis = jedisPoolEntry.getValue().getResource();
            try {
                // resultMap.put(jedisPoolEntry.getKey(), jedis.keys(pattern));
                Pair<byte[], Set<byte[]>> p = readBulk(cursors.get(jedisPoolEntry.getKey()), new Jedis(jedis), pattern);
                if (p == null) {
                    logger.info("got p null");
                    p = new Pair<>(cursors.get(jedisPoolEntry.getKey()), new HashSet<>());
                } else {
                    logger.info("set isEmpty false");
                    isEmptyA = false;
                }
                logger.info("setting resultMap {} ", jedisPoolEntry.getKey());
                resultMap.put(jedisPoolEntry.getKey(), p);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            } finally {
                jedis.close();
            }
            return isEmptyA;
        }
    }

    private static Pair<byte[], Set<byte[]>> readBulk(byte[] current, Jedis jedis, byte[] pattern) throws
            InterruptedException {
        if (current != null && '0' == current[0] && current.length == 1) {
            return null;
        }

        ScanParams scanParams = new ScanParams();
        scanParams.count(Constants.MAX_PACK);
        scanParams.match(pattern);

        Set<byte[]> result = new HashSet<>();
        current = current == null ? "0".getBytes() : current;
        int maxTry = 10;
        while (maxTry > 0) {
            try {
                logger.info("loadding from {} ... {}", jedis.getClient().toString(), pattern + " max: " + Constants.MAX_PACK);
                ScanResult<byte[]> results = jedis.scan(current, scanParams);
                logger.info("done cursor {} found {}", results.getCursor(), results.getResult().size());

                current = results.getCursorAsBytes();
                result.addAll(results.getResult());
                break;
//                if ('0' == current[0] && current.length == 1) {
//                    break;
//                }
            } catch (Exception ex) {
                logger.info(ex.getMessage(), ex);
                maxTry--;
                Thread.sleep(100);
                if (maxTry < 0) {
                    throw ex;
                }
            }
        }

        return new Pair<>(current, result);
    }

    @Override
    public String type(byte[] key) {
        return jedisCluster.type(key);
    }

    @Override
    public Long ttl(byte[] key) {
        return jedisCluster.ttl(key);
    }

    @Override
    public Map<byte[], byte[]> hgetAll(byte[] key) {
        return jedisCluster.hgetAll(key);
    }

    @Override
    public List<byte[]> lrange(byte[] key, long start, long end) {
        return jedisCluster.lrange(key, start, end);
    }

    @Override
    public byte[] get(byte[] key) {
        return jedisCluster.get(key);
    }

    @Override
    public List<Tuple> zrangeWithScores(byte[] key, long start, long end) {
        return jedisCluster.zrangeWithScores(key, start, end);
    }

    @Override
    public String ping() {
        // return jedisCluster.ping();
        return "not implemented";
    }

    @Override
    public Map<String, List<byte[]>> configGet(byte[] pattern) {

        Map<String, List<byte[]>> configMap = new LinkedHashMap<>();
        for (Map.Entry<String, ConnectionPool> jedisPoolEntry : jedisCluster.getClusterNodes().entrySet()) {
            Jedis jedis = new Jedis(jedisPoolEntry.getValue().getResource());
            try {
                List<byte[]> config = new ArrayList<>();
                configMap.put(jedisPoolEntry.getKey(), config);
                config.add("keyNodeName".getBytes());
                config.add(jedisPoolEntry.getKey().getBytes());
                //config.addAll(jedis.configGet(pattern));@TODO revert!!!!
            } finally {
                jedis.close();
            }
        }
        return configMap;
    }


    @Override
    public Map<String, List<String>> clientList() {
        Map<String, List<String>> clientsMap = new LinkedHashMap<>();

        for (Map.Entry<String, ConnectionPool> entry : jedisCluster.getClusterNodes().entrySet()) {
            Jedis jedis = new Jedis(entry.getValue().getResource());
            try {
                clientsMap.put(entry.getKey(), Arrays.asList(jedis.clientList().split("\n")));
            } finally {
                jedis.close();
            }
        }
        return clientsMap;
    }

    @Override
    public Map<String, List<String>> info() {
        Map<String, List<String>> info = new LinkedHashMap<>();
        boolean commonDataDone = false;
        List<String> clusterInfo = new ArrayList<>();
        for (Map.Entry<String, ConnectionPool> jedisPoolEntry : jedisCluster.getClusterNodes().entrySet()) {
            Jedis jedis = new Jedis(jedisPoolEntry.getValue().getResource());
            try {
                if (!commonDataDone && jedis != null) {
                    commonDataDone = true;
                    info.put("ClusterInfo", clusterInfo);
                    clusterInfo.addAll(Arrays.asList(jedis.clusterInfo().split("\n")));
                    clusterInfo.addAll(Arrays.asList(jedis.clusterNodes().split("\n")));
                    // clusterInfo.addAll(Arrays.asList(jedis.clusterSlots().split("\n")));
                }
                List<String> infoList = new ArrayList<>();
                info.put(jedisPoolEntry.getKey(), infoList);
                infoList.add("name:" + jedisPoolEntry.getKey());
                infoList.addAll(Arrays.asList(jedis.info().split("\n")));
            } finally {
                jedis.close();
            }
        }


        return info;
    }

    @Override
    public void close() throws IOException {
        jedisCluster.close();
    }

    @Override
    public long dbSize() {
        long size = 0;
        for (ConnectionPool jedisPool : jedisCluster.getClusterNodes().values()) {
            Jedis jedis = new Jedis(jedisPool.getResource());
            try {
                size += jedis.dbSize();
            } finally {
                jedis.close();
            }
        }
        return size;
    }

    @Override
    public String auth(String password) {
        String r = "";
        for (ConnectionPool jedisPool : jedisCluster.getClusterNodes().values()) {
            Jedis jedis = new Jedis(jedisPool.getResource());
            try {
                r += jedis.auth(password);
            } finally {
                jedis.close();
            }
        }
        return r;
    }

    @Override
    public void connect() {
        //jedisCluster.c
    }

    @Override
    public boolean isCluster() {
        return true;
    }

    @Override
    public int amountOfShards() {
        return jedisCluster.getClusterNodes().size();
    }
}
