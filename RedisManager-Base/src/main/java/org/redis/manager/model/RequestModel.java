package org.redis.manager.model;

import java.util.List;

/**
 * Created by
 *
 * @author Zhangduoli
 * @date 2018-9-21
 */
public class RequestModel {
    private String clusterId;
    private String param;
    private String hostAndPort;

    public String getClusterId() {
        return clusterId;
    }

    public void setClusterId(String clusterId) {
        this.clusterId = clusterId;
    }

    public String getParam() {
        return param;
    }

    public void setParam(String param) {
        this.param = param;
    }

    public String getHostAndPort() {
        return hostAndPort;
    }

    public void setHostAndPort(String hostAndPort) {
        this.hostAndPort = hostAndPort;
    }
}
