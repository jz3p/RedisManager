package org.redis.manager.cluster;

import java.util.*;
import java.util.logging.Logger;

import org.redis.manager.model.ScanPage;

import org.redis.manager.shell.client.LogMessage;
import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.ScanParams;
import redis.clients.jedis.ScanResult;

public class RedisClusterScan{

	HostAndPort[] nodes;
	
	public RedisClusterScan(Set<HostAndPort> masters) {
		HostAndPort[] nodes = masters.toArray(new HostAndPort[masters.size()]);
		Arrays.sort(nodes, (o1, o2)->{
			return (o1.getHost() + ":" + o1.getPort()).hashCode() - (o2.getHost() + ":" + o2.getPort()).hashCode();
		});
		this.nodes = nodes;
	}
	
	public ScanPage scan(ScanPage scan) {
		Set<String> keys = new HashSet<String>();
        //是否满足当前页面的个数
		boolean flag = false;
        //如果没有满足查询个数，则循环下一个节点继续查询
		while(!flag && scan.getHasMore()){
			HostAndPort hp = nodes[scan.getClient()];
			Jedis jedis = new Jedis(hp.getHost(), hp.getPort(),3000);
			try {
				ScanResult<String>  result = jedis.scan(scan.getCursor(), new ScanParams().count(scan.getPageSize() - keys.size()).match(scan.getQuery()));
				scan.setCursor(result.getStringCursor());
                //当前结点没有数据了
				if(scan.getCursor() == null || "".equals(scan.getCursor()) || "0".equals(scan.getCursor())){
					scan.setClient(scan.getClient() + 1);
					if(scan.getClient() >= nodes.length){
						scan.setHasMore(false);
					}
					scan.setCursor("0");
				}else{
					scan.setHasMore(true);
				}
				keys.addAll(result.getResult());
			} finally {
				jedis.close();
			}
			if(keys.size() >= scan.getPageSize()){
				flag = true;
			}
		}
		scan.setKeys(keys);
		return scan;
	}

	public boolean flushDB(){
        //如果没有满足查询个数，则循环下一个节点继续查询
        boolean result=false;
        for (int i=0;i<nodes.length;i++){
            HostAndPort hp = nodes[i];
            Jedis jedis = new Jedis(hp.getHost(), hp.getPort(),3000);
            try {
                jedis.flushAll();
                result = true;
            }catch (Exception e){
                System.out.println("lua will execute...");
                jedis.eval("return redis.call('flushAll'), 0");
            } finally {
                jedis.close();
            }
        }
        return result;
    }

    public Object scriptExecute(String script){
        Map<String,Object> data = new HashMap<>(16);
        for (int i=0;i<nodes.length;i++){
            HostAndPort hp = nodes[i];
            Jedis jedis = new Jedis(hp.getHost(), hp.getPort(),3000);
            try {
                Object eval = jedis.eval(script);
                data.put(hp.getHost()+":"+hp.getPort(),eval);
            }catch (Exception e){
                e.printStackTrace();
            } finally {
                jedis.close();
            }
        }
        return data;
    }

    public Object deleteKeysByRegex(String regex){
	    Map<String,Object> result = new HashMap<>(16);
        for (int i=0;i<nodes.length;i++){
            HostAndPort hp = nodes[i];
            Jedis jedis = new Jedis(hp.getHost(), hp.getPort(),3000);
            try {
                Set<String> keys  = jedis.keys(regex);
                for (String key : keys){
                    jedis.del(key);
                }
                result.put(hp.getHost()+":"+hp.getPort(),keys.size());
            }catch (Exception e){
                e.printStackTrace();
            } finally {
                jedis.close();
            }
        }
        return result;
    }
}