package com.sequenceiq.cloudbreak.converter.environment;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import com.sequenceiq.cloudbreak.api.model.KerberosResponse;
import com.sequenceiq.cloudbreak.api.model.KubernetesConfigResponse;
import com.sequenceiq.cloudbreak.api.model.environment.response.DatalakeResourcesResponse;
import com.sequenceiq.cloudbreak.api.model.environment.response.DetailedEnvironmentResponse;
import com.sequenceiq.cloudbreak.api.model.environment.response.LocationResponse;
import com.sequenceiq.cloudbreak.api.model.environment.response.ServiceDescriptorResponse;
import com.sequenceiq.cloudbreak.api.model.ldap.LdapConfigResponse;
import com.sequenceiq.cloudbreak.api.model.proxy.ProxyConfigResponse;
import com.sequenceiq.cloudbreak.api.model.rds.RDSConfigResponse;
import com.sequenceiq.cloudbreak.api.model.stack.StackViewResponse;
import com.sequenceiq.cloudbreak.api.model.users.WorkspaceResourceResponse;
import com.sequenceiq.cloudbreak.converter.AbstractConversionServiceAwareConverter;
import com.sequenceiq.cloudbreak.domain.environment.Environment;
import com.sequenceiq.cloudbreak.domain.stack.StackType;
import com.sequenceiq.cloudbreak.domain.stack.cluster.DatalakeResources;
import com.sequenceiq.cloudbreak.domain.stack.cluster.ServiceDescriptor;

@Component
public class EnvironmentToDetailedEnvironmentResponseConverter extends AbstractConversionServiceAwareConverter<Environment, DetailedEnvironmentResponse> {

    @Inject
    private RegionConverter regionConverter;

    @Override
    public DetailedEnvironmentResponse convert(Environment source) {
        DetailedEnvironmentResponse response = new DetailedEnvironmentResponse();
        response.setId(source.getId());
        response.setName(source.getName());
        response.setDescription(source.getDescription());
        response.setRegions(regionConverter.convertRegions(source.getRegionSet()));
        response.setCloudPlatform(source.getCloudPlatform());
        response.setCredentialName(source.getCredential().getName());
        response.setWorkspace(getConversionService().convert(source.getWorkspace(), WorkspaceResourceResponse.class));
        response.setLdapConfigs(
                source.getLdapConfigs()
                        .stream()
                        .map(ldapConfig -> getConversionService().convert(ldapConfig, LdapConfigResponse.class))
                        .collect(Collectors.toSet()));
        response.setProxyConfigs(
                source.getProxyConfigs()
                        .stream()
                        .map(proxyConfig -> getConversionService().convert(proxyConfig, ProxyConfigResponse.class))
                        .collect(Collectors.toSet()));
        response.setRdsConfigs(
                source.getRdsConfigs()
                        .stream()
                        .map(rdsConfig -> getConversionService().convert(rdsConfig, RDSConfigResponse.class))
                        .collect(Collectors.toSet()));
        response.setKubernetesConfigs(
                source.getKubernetesConfigs()
                        .stream()
                        .map(kubeConfig -> getConversionService().convert(kubeConfig, KubernetesConfigResponse.class))
                        .collect(Collectors.toSet()));
        response.setKerberosConfigs(
                source.getKerberosConfigs()
                        .stream()
                        .map(kerberosConfig -> getConversionService().convert(kerberosConfig, KerberosResponse.class))
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
        response.setLocation(getConversionService().convert(source, LocationResponse.class));
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
        Set<String> datalakeResourcesNames = new HashSet<>();
        Set<DatalakeResourcesResponse> datalakeResourcesResponses = new HashSet<>();
        for (DatalakeResources datalakeResources : source.getDatalakeResources()) {
            datalakeResourcesNames.add(datalakeResources.getName());
            DatalakeResourcesResponse datalakeResourcesResponse = new DatalakeResourcesResponse();
            datalakeResourcesResponse.setAmbariUrl(datalakeResources.getDatalakeAmbariUrl());
            if (datalakeResources.getLdapConfig() != null) {
                datalakeResourcesResponse.setLdapName(datalakeResources.getLdapConfig().getName());
            }
            if (datalakeResources.getKerberosConfig() != null) {
                datalakeResourcesResponse.setKerberosName(datalakeResources.getKerberosConfig().getName());
            }
            if (!CollectionUtils.isEmpty(datalakeResources.getRdsConfigs())) {
                datalakeResourcesResponse.setRdsNames(datalakeResources.getRdsConfigs().stream().map(rds -> rds.getName()).collect(Collectors.toSet()));
            }
            Map<String, ServiceDescriptorResponse> serviceDescriptorResponses = new HashMap<>();
            for (ServiceDescriptor serviceDescriptor : datalakeResources.getServiceDescriptorMap().values()) {
                ServiceDescriptorResponse serviceDescriptorResponse = new ServiceDescriptorResponse();
                serviceDescriptorResponse.setServiceName(serviceDescriptor.getServiceName());
                serviceDescriptorResponse.setBlueprintParams((Map) serviceDescriptor.getBlueprintParams().getMap());
                serviceDescriptorResponse.setComponentHosts((Map) serviceDescriptor.getComponentsHosts().getMap());
                serviceDescriptorResponses.put(serviceDescriptor.getServiceName(), serviceDescriptorResponse);
            }
            datalakeResourcesResponse.setServiceDescriptorMap(serviceDescriptorResponses);
            datalakeResourcesResponses.add(datalakeResourcesResponse);
        }
        response.setDatalakeResourcesNames(datalakeResourcesNames);
        response.setDatalakeResourcesResponses(datalakeResourcesResponses);
        return response;
    }
}
