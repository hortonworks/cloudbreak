package com.sequenceiq.cloudbreak.ambari.filter;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;
import com.sequenceiq.cloudbreak.domain.stack.cluster.host.HostMetadata;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceMetaData;
import com.sequenceiq.cloudbreak.filter.ConfigParam;
import com.sequenceiq.cloudbreak.util.JsonUtil;

@Component
public class AppMasterFilter implements HostFilter {

    private static final String AM_KEY = "amHostHttpAddress";

    private static final String APPS_NODE = "apps";

    private static final String APP_NODE = "app";

    @Inject
    private Client restClient;

    @Override
    public List<HostMetadata> filter(long clusterId, Map<String, String> config, List<HostMetadata> hosts, Set<InstanceMetaData> instanceMetaDatasInStack)
            throws HostFilterException {
        List<HostMetadata> result = new ArrayList<>(hosts);
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
                result = filter(hostsWithAM, result);
            }
        } catch (Exception e) {
            throw new HostFilterException("Error filtering based on ApplicationMaster location", e);
        }
        return result;
    }

    private List<HostMetadata> filter(Collection<String> hostsWithAM, List<HostMetadata> hosts) {
        hosts.removeIf(host -> hostsWithAM.contains(host.getHostName()));
        return hosts;
    }

}
