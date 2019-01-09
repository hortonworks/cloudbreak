package com.sequenceiq.cloudbreak.converter.v4.environment;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import com.sequenceiq.cloudbreak.api.endpoint.v4.database.responses.DatabaseV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.kerberos.responses.KerberosV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.kubernetes.responses.KubernetesV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.ldaps.responses.LdapV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.proxies.responses.ProxyV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.environment.responses.DatalakeResourcesV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.environment.responses.DetailedEnvironmentV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.environment.responses.LocationV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.environment.responses.ServiceDescriptorV4Response;
import com.sequenceiq.cloudbreak.api.model.stack.StackViewResponse;
import com.sequenceiq.cloudbreak.api.endpoint.v4.workspace.responses.WorkspaceResourceV4Response;
import com.sequenceiq.cloudbreak.converter.AbstractConversionServiceAwareConverter;
import com.sequenceiq.cloudbreak.domain.environment.Environment;
import com.sequenceiq.cloudbreak.domain.stack.StackType;
import com.sequenceiq.cloudbreak.domain.stack.cluster.DatalakeResources;
import com.sequenceiq.cloudbreak.domain.stack.cluster.ServiceDescriptor;

@Component
public class EnvironmentToDetailedEnvironmentV4ResponseConverter extends AbstractConversionServiceAwareConverter<Environment, DetailedEnvironmentV4Response> {

    @Inject
    private RegionConverter regionConverter;

    @Override
    public DetailedEnvironmentV4Response convert(Environment source) {
        DetailedEnvironmentV4Response response = new DetailedEnvironmentV4Response();
        response.setId(source.getId());
        response.setName(source.getName());
        response.setDescription(source.getDescription());
        response.setRegions(regionConverter.convertRegions(source.getRegionSet()));
        response.setCloudPlatform(source.getCloudPlatform());
        response.setCredentialName(source.getCredential().getName());
        response.setWorkspace(getConversionService().convert(source.getWorkspace(), WorkspaceResourceV4Response.class));
        response.setLdaps(
                source.getLdapConfigs()
                        .stream()
                        .map(ldapConfig -> getConversionService().convert(ldapConfig, LdapV4Response.class))
                        .collect(Collectors.toSet()));
        response.setProxies(
                source.getProxyConfigs()
                        .stream()
                        .map(proxyConfig -> getConversionService().convert(proxyConfig, ProxyV4Response.class))
                        .collect(Collectors.toSet()));
        response.setDatabases(
                source.getRdsConfigs()
                        .stream()
                        .map(rdsConfig -> getConversionService().convert(rdsConfig, DatabaseV4Response.class))
                        .collect(Collectors.toSet()));
        response.setKubernetes(
                source.getKubernetesConfigs()
                        .stream()
                        .map(kubeConfig -> getConversionService().convert(kubeConfig, KubernetesV4Response.class))
                        .collect(Collectors.toSet()));
        response.setKerberoses(
                source.getKerberosConfigs()
                        .stream()
                        .map(kerberosConfig -> getConversionService().convert(kerberosConfig, KerberosV4Response.class))
                        .collect(Collectors.toSet()));
        response.setWorkloadClusters(
                source.getStacks()
                        .stream()
                        .filter(stack -> stack.getType() == StackType.WORKLOAD)
                        .map(workload -> getConversionService().convert(workload, StackViewResponse.class))
                        .collect(Collectors.toSet()));
        response.setDatalakeClusters(
                source.getStacks()
                        .stream()
                        .filter(stack -> stack.getType() == StackType.DATALAKE)
                        .map(stack -> getConversionService().convert(stack, StackViewResponse.class))
                        .collect(Collectors.toSet()));
        response.setLocation(getConversionService().convert(source, LocationV4Response.class));
        response.setWorkloadClusterNames(
                response.getWorkloadClusters()
                        .stream()
                        .map(StackViewResponse::getName)
                        .collect(Collectors.toSet()));
        response.setDatalakeClusterNames(
                response.getDatalakeClusters()
                        .stream()
                        .map(StackViewResponse::getName)
                        .collect(Collectors.toSet()));
        if (source.getDatalakeResources() != null) {
            DatalakeResources datalakeResources = source.getDatalakeResources();
            response.setDatalakeResourcesName(datalakeResources.getName());
            DatalakeResourcesV4Response datalakeResourcesV4Response = new DatalakeResourcesV4Response();
            datalakeResourcesV4Response.setAmbariUrl(datalakeResources.getDatalakeAmbariUrl());
            if (datalakeResources.getLdapConfig() != null) {
                datalakeResourcesV4Response.setLdapName(datalakeResources.getLdapConfig().getName());
            }
            if (datalakeResources.getKerberosConfig() != null) {
                datalakeResourcesV4Response.setKerberosName(datalakeResources.getKerberosConfig().getName());
            }
            if (!CollectionUtils.isEmpty(datalakeResources.getRdsConfigs())) {
                datalakeResourcesV4Response.setDatabaseNames(datalakeResources.getRdsConfigs().stream().map(rds -> rds.getName()).collect(Collectors.toSet()));
            }
            Map<String, ServiceDescriptorV4Response> serviceDescriptorResponses = new HashMap<>();
            for (ServiceDescriptor serviceDescriptor : datalakeResources.getServiceDescriptorMap().values()) {
                ServiceDescriptorV4Response serviceDescriptorV4Response = new ServiceDescriptorV4Response();
                serviceDescriptorV4Response.setServiceName(serviceDescriptor.getServiceName());
                serviceDescriptorV4Response.setBlueprintParams((Map) serviceDescriptor.getBlueprintParams().getMap());
                serviceDescriptorV4Response.setComponentHosts((Map) serviceDescriptor.getComponentsHosts().getMap());
                serviceDescriptorResponses.put(serviceDescriptor.getServiceName(), serviceDescriptorV4Response);
            }
            datalakeResourcesV4Response.setServiceDescriptorMap(serviceDescriptorResponses);
            response.setDatalakeResources(datalakeResourcesV4Response);
        }
        return response;
    }
}
