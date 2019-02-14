package com.sequenceiq.cloudbreak.converter.v4.stacks.cluster;

import static com.sequenceiq.cloudbreak.domain.ClusterAttributes.CUSTOM_QUEUE;

import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.ResourceStatus;
import com.sequenceiq.cloudbreak.api.endpoint.v4.database.responses.DatabaseV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.kerberos.responses.KerberosV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.ldaps.responses.LdapV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.proxies.responses.ProxyV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.cluster.ClusterV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.cluster.ambari.AmbariV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.cluster.customcontainer.CustomContainerV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.cluster.gateway.GatewayV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.cluster.gateway.topology.ClusterExposedServiceV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.cluster.storage.CloudStorageV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.workspace.responses.WorkspaceResourceV4Response;
import com.sequenceiq.cloudbreak.api.util.ConverterUtil;
import com.sequenceiq.cloudbreak.controller.exception.CloudbreakApiException;
import com.sequenceiq.cloudbreak.converter.AbstractConversionServiceAwareConverter;
import com.sequenceiq.cloudbreak.domain.KerberosConfig;
import com.sequenceiq.cloudbreak.domain.json.Json;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.service.ServiceEndpointCollector;
import com.sequenceiq.cloudbreak.service.clusterdefinition.ClusterDefinitionService;
import com.sequenceiq.cloudbreak.util.StackUtil;

@Component
public class ClusterToClusterV4ResponseConverter extends AbstractConversionServiceAwareConverter<Cluster, ClusterV4Response> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClusterToClusterV4ResponseConverter.class);

    private static final int SECONDS_PER_MINUTE = 60;

    private static final int MILLIS_PER_SECOND = 1000;

    @Inject
    private StackUtil stackUtil;

    @Inject
    private ServiceEndpointCollector serviceEndpointCollector;

    @Inject
    private ClusterDefinitionService clusterDefinitionService;

    @Inject
    private ConverterUtil converterUtil;

    @Override
    public ClusterV4Response convert(Cluster source) {
        ClusterV4Response clusterResponse = new ClusterV4Response();
        clusterResponse.setId(source.getId());
        clusterResponse.setName(source.getName());
        clusterResponse.setStatus(source.getStatus());
        clusterResponse.setStatusReason(source.getStatusReason());
        setUptime(source, clusterResponse);
        clusterResponse.setDescription(source.getDescription() == null ? "" : source.getDescription());
        if (clusterDefinitionService.isAmbariBlueprint(source.getClusterDefinition())) {
            String ambariIp = stackUtil.extractAmbariIp(source.getStack());
            Map<String, Collection<ClusterExposedServiceV4Response>> clusterExposedServicesForTopologies =
                    serviceEndpointCollector.prepareClusterExposedServices(source, ambariIp);
            clusterResponse.setExposedServices(clusterExposedServicesForTopologies);
        }
        clusterResponse.setLdap(getConversionService().convert(source.getLdapConfig(), LdapV4Response.class));
        convertCustomQueue(source, clusterResponse);
        convertNullableProperties(source, clusterResponse);
        convertContainerConfig(source, clusterResponse);
        clusterResponse.setCreationFinished(source.getCreationFinished());
        convertKerberosConfig(source, clusterResponse);
        decorateResponseWithProxyConfig(source, clusterResponse);
        clusterResponse.setCloudStorage(getCloudStorage(source));
        clusterResponse.setAmbari(getConversionService().convert(source, AmbariV4Response.class));
        clusterResponse.setDatabases(converterUtil.convertAll(source.getRdsConfigs().stream().filter(
                rds -> ResourceStatus.USER_MANAGED.equals(rds.getStatus())).collect(Collectors.toList()), DatabaseV4Response.class));
        clusterResponse.setWorkspace(getConversionService().convert(source.getWorkspace(), WorkspaceResourceV4Response.class));
        return clusterResponse;
    }

    private void setUptime(Cluster source, ClusterV4Response clusterResponse) {
        long uptime = stackUtil.getUptimeForCluster(source, source.isAvailable());
        int minutes = (int) ((uptime / (MILLIS_PER_SECOND * SECONDS_PER_MINUTE)) % SECONDS_PER_MINUTE);
        int hours = (int) (uptime / (MILLIS_PER_SECOND * SECONDS_PER_MINUTE * SECONDS_PER_MINUTE));
        clusterResponse.setUptime(uptime);
        clusterResponse.setHoursUp(hours);
        clusterResponse.setMinutesUp(minutes);
    }

    private void convertCustomQueue(Cluster source, ClusterV4Response clusterResponse) {
        if (source.getAttributes() != null) {
            Json fromVault = new Json(source.getAttributes());
            Map<String, Object> attributes = fromVault.getMap();
            Object customQueue = attributes.get(CUSTOM_QUEUE.name());
            if (customQueue != null) {
                clusterResponse.setCustomQueue(customQueue.toString());
            } else {
                clusterResponse.setCustomQueue("default");
            }
        }
    }

    private CloudStorageV4Response getCloudStorage(Cluster source) {
        if (source.getFileSystem() != null) {
            return getConversionService().convert(source.getFileSystem(), CloudStorageV4Response.class);
        }
        return null;
    }

    private void convertNullableProperties(Cluster source, ClusterV4Response clusterResponse) {
        if (source.getGateway() != null) {
            GatewayV4Response gatewayV4Response = getConversionService().convert(source.getGateway(), GatewayV4Response.class);
            clusterResponse.setGateway(gatewayV4Response);
        }
        if (source.getAttributes() != null) {
            Json fromVault = new Json(source.getAttributes());
            clusterResponse.setAttributes(fromVault.getMap());
        }
    }

    private void convertContainerConfig(Cluster source, ClusterV4Response clusterResponse) {
        Json customContainerDefinition = source.getCustomContainerDefinition();
        if (customContainerDefinition != null && StringUtils.isNoneEmpty(customContainerDefinition.getValue())) {
            try {
                Map<String, String> map = customContainerDefinition.get(Map.class);
                Map<String, String> result = new HashMap<>();

                for (Entry<String, String> stringStringEntry : map.entrySet()) {
                    result.put(stringStringEntry.getKey(), stringStringEntry.getValue());
                }
                CustomContainerV4Response customContainers = new CustomContainerV4Response();
                customContainers.setDefinitions(result);
                clusterResponse.setCustomContainers(customContainers);

            } catch (IOException e) {
                LOGGER.info("Failed to add customContainerDefinition to response", e);
                throw new CloudbreakApiException("Failed to add customContainerDefinition to response", e);
            }
        }
    }

    private void convertKerberosConfig(Cluster source, ClusterV4Response clusterResponse) {
        KerberosConfig kerberosConfig = source.getKerberosConfig();
        if (kerberosConfig != null) {
            clusterResponse.setKerberos(getConversionService().convert(kerberosConfig, KerberosV4Response.class));
        }
    }

    private void decorateResponseWithProxyConfig(Cluster source, ClusterV4Response clusterResponse) {
        if (source.getProxyConfig() != null) {
            clusterResponse.setProxy(getConversionService().convert(source.getProxyConfig(), ProxyV4Response.class));
        }
    }
}