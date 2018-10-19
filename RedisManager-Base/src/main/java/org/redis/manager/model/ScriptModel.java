package org.redis.manager.model;

/**
 * Created by
 *
 * @author Zhangduoli
 * @date 2018-9-21
 */
public class ScriptModel {
    private String cluster;
    private String script;

    public String getCluster() {
        return cluster;
    }

    public void setCluster(String cluster) {
        this.cluster = cluster;
    }

    public String getScript() {
        return script;
    }

    public void setScript(String script) {
        this.script = script;
    }
}
