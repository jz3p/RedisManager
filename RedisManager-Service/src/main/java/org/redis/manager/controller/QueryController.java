package org.redis.manager.controller;

import org.redis.manager.model.ClusterServerCache;
import org.redis.manager.model.RequestModel;
import org.redis.manager.model.ScanPage;
import org.redis.manager.service.QueryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/query")
public class QueryController extends BaseController {


    @Autowired
    QueryService queryService;

    @RequestMapping(value = "/scan/{cluster:.+}", method = RequestMethod.POST)
    @ResponseBody
    public Object scan(@PathVariable String cluster, ScanPage page) throws Exception {
        if (!ClusterServerCache.clusterExist(cluster)) {
            return null;
        }
        return queryService.scan(cluster, page);
    }

    @RequestMapping(value = "/get/{cluster}/{key:.+}", method = RequestMethod.GET)
    @ResponseBody
    public Object get(@PathVariable String cluster, @PathVariable String key) throws Exception {
        if (!ClusterServerCache.clusterExist(cluster)) {
            return null;
        }
        return SUCCESS(queryService.get(cluster, key));
    }

    @RequestMapping(value = "/delete/{cluster}/{key:.+}", method = RequestMethod.POST)
    @ResponseBody
    public Object delete(@PathVariable String cluster, @PathVariable String key) throws Exception {
        if (!ClusterServerCache.clusterExist(cluster)) {
            return null;
        }
        queryService.delete(cluster, key);
        return SUCCESS();
    }

    @GetMapping("/flush/{cluster}")
    @ResponseBody
    public Object flushDb(@PathVariable String cluster) throws Exception {
        queryService.flushAll(cluster);
        return SUCCESS();
    }

    @RequestMapping(value = "/deletes", method = RequestMethod.POST)
    @ResponseBody
    public Object deleteByRegex(RequestModel model) throws Exception {
        Object data = queryService.deleteByLinuxCommand(model);
        return SUCCESS(data);
    }

    @RequestMapping(value = "/keys", method = RequestMethod.POST)
    @ResponseBody
    public Object execute(RequestModel model) throws Exception {
        Object data = queryService.scriptExecute(model.getClusterId(), model.getParam());
        return SUCCESS(data);
    }


    @RequestMapping(value = "/deleteKeys", method = RequestMethod.POST)
    @ResponseBody
    public Object deleteByParam(@RequestBody RequestModel model) throws Exception {
        Object data = queryService.deleteKeysByRegex(model);
        return SUCCESS(data);
    }
}
