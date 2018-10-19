package org.redis.manager.util;

import org.redis.manager.notify.Notify;
import org.redis.manager.shell.LinuxUtil;
import org.redis.manager.shell.client.ShellClient;
import redis.clients.jedis.HostAndPort;

/**
 * Created by
 *
 * @author Zhangduoli
 * @date 2018-10-12
 */
public class RedisOperateUtil extends LinuxUtil{
    private HostAndPort hp;
    public RedisOperateUtil(HostAndPort hp, ShellClient client, Notify notify) {
        super(client, notify);
        this.hp =hp;
    }

    public void delete(String keyRegex) throws Exception{
        String path = findPath("/data/bigdata", "redis-cli");
        String connectInfo = path+" -c -h "+hp.getHost()+" -p "+hp.getPort();
        client.exec(connectInfo+" keys "+keyRegex+" | xargs "+connectInfo+" del", 1000);
    }
}
