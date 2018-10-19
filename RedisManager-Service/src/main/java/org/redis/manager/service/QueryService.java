package org.redis.manager.service;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.redis.manager.cluster.RedisClusterScan;
import org.redis.manager.leveldb.D_ClusterInfo;
import org.redis.manager.leveldb.D_ClusterNode_Master;
import org.redis.manager.leveldb.D_ClusterNode_Tree;
import org.redis.manager.leveldb.D_RedisClusterNode;
import org.redis.manager.model.ScanPage;
import org.redis.manager.model.ScriptModel;
import org.redis.manager.model.enums.RedisNodeStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.JedisCluster;

@Service
public class QueryService {

    @Autowired
    ClusterNodeService clusterNodeService;
    @Autowired
    ClusterInfoService clusterInfoService;
    @Autowired
    RedisOperateService operateService;

    public ScanPage scan(String cluster, ScanPage scanPage) throws Exception {
        if (scanPage.getQuery().contains("*")) {
            return query(cluster, scanPage);
        } else {
            return exist(cluster, scanPage);
        }
    }

    private ScanPage exist(String cluster, ScanPage scanPage) throws Exception {
        scanPage.setHasMore(false);
        List<D_RedisClusterNode> nodes = clusterNodeService.getAllClusterNodes(cluster);
        Set<HostAndPort> masters = new HashSet<HostAndPort>();
        nodes.forEach(node -> {
            masters.add(new HostAndPort(node.getHost(), node.getPort()));
        });
        JedisCluster jedis = new JedisCluster(masters);
        try {
            if (jedis.exists(scanPage.getQuery())) {
                scanPage.setKeys(new HashSet<>());
                scanPage.getKeys().add(scanPage.getQuery());
            }
        } finally {
            jedis.close();
        }
        return scanPage;
    }

    public ScanPage query(String cluster, ScanPage scanPage) throws Exception {
        Set<HostAndPort> masters = getMasters(cluster);
        RedisClusterScan scan = new RedisClusterScan(masters);
        scanPage.setKeys(null);
        return scan.scan(scanPage);
    }

    public Object flushAll(String cluster) throws Exception {
        Set<HostAndPort> masters = getMasters(cluster);
        RedisClusterScan scan = new RedisClusterScan(masters);
        scan.flushDB();
        return true;
    }

    public Object deleteKeysByRegex(ScriptModel model) throws Exception{
        Set<HostAndPort> masters = getMasters(model.getCluster());
        for (HostAndPort hostAndPort : masters){
            operateService.init(hostAndPort,"root" , "1qaz@WSX");
            operateService.delete(model.getScript());
        }
        RedisClusterScan scan = new RedisClusterScan(masters);
        return scan.deleteKeysByRegex(model.getScript());
    }

    public Object scriptExecute(String cluster,String script) throws Exception{
        Set<HostAndPort> masters = getMasters(cluster);
        RedisClusterScan scan = new RedisClusterScan(masters);
        return scan.scriptExecute(script);
    }

    public Object get(String cluster, String key) throws Exception {
        List<D_RedisClusterNode> nodes = clusterNodeService.getAllClusterNodes(cluster);
        Set<HostAndPort> masters = new HashSet<HostAndPort>();
        nodes.forEach(node -> {
            masters.add(new HostAndPort(node.getHost(), node.getPort()));
        });
        Object value = null;
        JedisCluster jedis = new JedisCluster(masters);
        try {
            String type = jedis.type(key);
            switch (type) {
                case "string":
                    value = jedis.get(key);
                    break;
                case "list":
                    value = jedis.lrange(key, 0, -1);
                    break;
                case "set":
                    value = jedis.smembers(key);
                    break;
                case "zset":
                    value = jedis.zrange(key, 0, -1);
                    break;
                case "hash":
                    value = jedis.hgetAll(key);
                    break;
                default:
                    break;
            }
        } finally {
            jedis.close();
        }
        return value;
    }

    public void delete(String cluster, String key) throws Exception {
        List<D_RedisClusterNode> nodes = clusterNodeService.getAllClusterNodes(cluster);
        Set<HostAndPort> masters = new HashSet<HostAndPort>();
        nodes.forEach(node -> {
            masters.add(new HostAndPort(node.getHost(), node.getPort()));
        });
        JedisCluster jedis = new JedisCluster(masters);
        try {
            jedis.del(key);
        } finally {
            jedis.close();
        }
    }

    /**
     * 获取master列表
     *
     * @param cluster
     * @return
     * @throws Exception
     */
    private Set<HostAndPort> getMasters(String cluster) throws Exception {
        D_ClusterNode_Tree tree = clusterNodeService.getClusterTree(cluster);
        Set<HostAndPort> masters = new HashSet<HostAndPort>();
        //每一个分片获取一个节点
        for (D_ClusterNode_Master nodes : tree.getMasters()) {
            D_RedisClusterNode node = nodes.getMaster();
            if (node.getStatus() == RedisNodeStatus.CONNECT) {
                masters.add(new HostAndPort(node.getHost(), node.getPort()));
            } else {
                for (D_RedisClusterNode slave : nodes.getSlaves()) {
                    if (slave.getStatus() == RedisNodeStatus.CONNECT) {
                        masters.add(new HostAndPort(slave.getHost(), slave.getPort()));
                        break;
                    }
                }
            }
        }
        return masters;
    }
}
