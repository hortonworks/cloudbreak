package com.sequenceiq.cloudbreak.ambari.filter;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;
import com.sequenceiq.cloudbreak.common.json.JsonUtil;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceMetaData;
import com.sequenceiq.cloudbreak.filter.ConfigParam;

@Component
public class AppMasterFilter implements HostFilter {

    private static final String AM_KEY = "amHostHttpAddress";

    private static final String APPS_NODE = "apps";

    private static final String APP_NODE = "app";

    @Inject
    private Client restClient;

    @Override
    public List<InstanceMetaData> filter(long clusterId, Map<String, String> config, List<InstanceMetaData> hosts) throws HostFilterException {
        List<InstanceMetaData> result = new ArrayList<>(hosts);
        try {
            String resourceManagerAddress = config.get(ConfigParam.YARN_RM_WEB_ADDRESS.key());
            WebTarget target = restClient.target("http://" + resourceManagerAddress + HostFilterService.RM_WS_PATH).path("apps").queryParam("state", "RUNNING");
            String appResponse = target.request(MediaType.APPLICATION_JSON).get(String.class);
            JsonNode jsonNode = JsonUtil.readTree(appResponse);
            JsonNode apps = jsonNode.get(APPS_NODE);
            if (apps != null && apps.has(APP_NODE)) {
                JsonNode app = apps.get(APP_NODE);
                Collection<String> hostsWithAM = new HashSet<>();
                for (JsonNode node : app) {
                    String hostName = node.get(AM_KEY).textValue();
                    hostsWithAM.add(hostName.substring(0, hostName.lastIndexOf(':')));
                }
                result.removeIf(host -> hostsWithAM.contains(host.getDiscoveryFQDN()));
            }
        } catch (Exception e) {
            throw new HostFilterException("Error filtering based on ApplicationMaster location", e);
        }
        return result;
    }

}
