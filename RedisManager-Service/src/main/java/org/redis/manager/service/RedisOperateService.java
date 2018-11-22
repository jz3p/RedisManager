package org.redis.manager.service;

import org.redis.manager.shell.client.ShellClient;
import org.redis.manager.util.RedisOperateUtil;
import org.springframework.stereotype.Service;
import redis.clients.jedis.HostAndPort;

/**
 * Created by
 *
 * @author Zhangduoli
 * @date 2018-10-12
 */
@Service
public class RedisOperateService {
    private ShellClient client = null;
    private RedisOperateUtil operateUtil = null;

    public void init(HostAndPort hp, String user, String pass) throws Exception{
        client = new ShellClient(hp.getHost(), user, pass);
        operateUtil = new RedisOperateUtil(hp,client,null);
    }

    public void delete(String key) throws Exception{
        operateUtil.delete(key);
    }
}
