package com.sequenceiq.cloudbreak.service.cluster.filter;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestOperations;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sequenceiq.cloudbreak.domain.HostMetadata;
import com.sequenceiq.cloudbreak.service.cluster.ConfigParam;

@Component
public class AppMasterFilter implements AmbariHostFilter {

    private static final String AM_KEY = "amHostHttpAddress";
    private static final String APPS_NODE = "apps";
    private static final String APP_NODE = "app";

    @Autowired
    private RestOperations restTemplate;

    @Override
    @SuppressWarnings("unchecked")
    public List<HostMetadata> filter(Map<String, String> config, List<HostMetadata> hosts) throws HostFilterException {
        List<HostMetadata> result = new ArrayList<>(hosts);
        try {
            String resourceManagerAddress = config.get(ConfigParam.YARN_RM_WEB_ADDRESS.key());
            String appResponse = restTemplate.exchange(
                    String.format("http://%s", resourceManagerAddress + AmbariHostFilterService.RM_WS_PATH + "/apps?state=RUNNING"),
                    HttpMethod.GET, null, String.class).getBody();
            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode jsonNode = objectMapper.readTree(appResponse);
            JsonNode apps = jsonNode.get(APPS_NODE);
            if (apps != null && apps.has(APP_NODE)) {
                JsonNode app = apps.get(APP_NODE);
                Set<String> hostsWithAM = new HashSet<>();
                for (JsonNode node : app) {
                    String hostName = node.get(AM_KEY).textValue();
                    hostsWithAM.add(hostName.substring(0, hostName.lastIndexOf(':')));
                }
                result = filter(hostsWithAM, result);
            }
        } catch (Exception e) {
            throw new HostFilterException("Error filtering based on ApplicationMaster location", e);
        }
        return result;
    }

    private List<HostMetadata> filter(Set<String> hostsWithAM, List<HostMetadata> hosts) {
        Iterator<HostMetadata> iterator = hosts.iterator();
        while (iterator.hasNext()) {
            HostMetadata host = iterator.next();
            if (hostsWithAM.contains(host.getHostName())) {
                iterator.remove();
            }
        }
        return hosts;
    }

}
