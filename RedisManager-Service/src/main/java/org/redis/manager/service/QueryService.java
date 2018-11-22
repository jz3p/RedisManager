package org.redis.manager.service;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.redis.manager.cluster.RedisClusterScan;
import org.redis.manager.cluster.RedisClusterTerminal;
import org.redis.manager.leveldb.D_ClusterNode_Master;
import org.redis.manager.leveldb.D_ClusterNode_Tree;
import org.redis.manager.leveldb.D_RedisClusterNode;
import org.redis.manager.model.RequestModel;
import org.redis.manager.model.ScanPage;
import org.redis.manager.model.enums.RedisClusterRole;
import org.redis.manager.model.enums.RedisNodeStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.JedisCluster;

@Service
public class QueryService {

    protected static Logger logger = LoggerFactory.getLogger(QueryService.class);

    @Value("${linux.user}")
    private String user;

    @Value("${linux.pass}")
    private String pass;

    @Autowired
    ClusterNodeService clusterNodeService;
    @Autowired
    ClusterInfoService clusterInfoService;
    @Autowired
    RedisOperateService operateService;

    public ScanPage scan(String cluster, ScanPage scanPage) throws Exception {
        scanPage.setClient(0);
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

    public Object deleteKeysByRegex(RequestModel model) throws Exception {
        Set<HostAndPort> masters;
        if (StringUtils.isBlank(model.getClusterId())) {
            List<D_RedisClusterNode> nodes = getClusterNodes(model.getHostAndPort());
            if (Objects.nonNull(nodes)){
                masters = getMasters(nodes);
            }else {
                return null;
            }
        } else {
            masters = getMasters(model.getClusterId());
        }
        RedisClusterScan scan = new RedisClusterScan(masters);
        return scan.deleteKeysByRegex(model.getParam());
    }

    public Object scriptExecute(String cluster, String script) throws Exception {
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

    /**
     * 获取master列表
     *
     * @param cluster
     * @return
     * @throws Exception
     */
    private Set<HostAndPort> getMasters(List<D_RedisClusterNode> cluster) throws Exception {
        Set<HostAndPort> masters = new HashSet<HostAndPort>();
        //每一个分片获取一个节点
        for (D_RedisClusterNode nodes : cluster) {
            RedisClusterRole role = nodes.getRole();
            if (RedisClusterRole.MASTER.name().equals(role.name())) {
                masters.add(new HostAndPort(nodes.getHost(), nodes.getPort()));
            }
        }
        return masters;
    }

    private List<D_RedisClusterNode> getClusterNodes(String hostAndPort) throws Exception {
        if (Objects.isNull(hostAndPort)) {
            return null;
        }
        String[] hosts = hostAndPort.split(",");
        String[] hostPort = hosts[0].split(":");
        RedisClusterTerminal terminal = new RedisClusterTerminal(hostPort[0], Integer.valueOf(hostPort[1]));
        return clusterNodeService.getClusterNodesByRedis(null, terminal);
    }
    
    public Object deleteByLinuxCommand(RequestModel model) throws Exception {
        Set<HostAndPort> masters = getMasters(model.getClusterId());
        for (HostAndPort hostAndPort : masters) {
            operateService.init(hostAndPort, user, pass);
            logger.info("connect to host:[{}:{}]:",hostAndPort.getHost(),hostAndPort.getPort());
            operateService.delete(model.getParam());
            logger.info("delete success");
        }
        return 0;
    }
}
