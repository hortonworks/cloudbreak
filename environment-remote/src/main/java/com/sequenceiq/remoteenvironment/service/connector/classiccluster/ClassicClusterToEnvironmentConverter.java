package com.sequenceiq.remoteenvironment.service.connector.classiccluster;

import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import com.cloudera.thunderhead.service.environments2api.model.Application;
import com.cloudera.thunderhead.service.environments2api.model.Environment;
import com.cloudera.thunderhead.service.environments2api.model.Instance;
import com.cloudera.thunderhead.service.environments2api.model.KerberosInfo;
import com.cloudera.thunderhead.service.environments2api.model.PrivateDatalakeDetails;
import com.cloudera.thunderhead.service.environments2api.model.PvcEnvironmentDetails;
import com.cloudera.thunderhead.service.environments2api.model.Service;
import com.cloudera.thunderhead.service.environments2api.model.ServiceEndPoint;
import com.cloudera.thunderhead.service.onpremises.OnPremisesApiProto;
import com.sequenceiq.cloudbreak.common.network.NetworkUtil;

@Component
public class ClassicClusterToEnvironmentConverter {
    public Environment createEnvironment(OnPremisesApiProto.Cluster cluster) {
        Environment environment = new Environment();
        environment.setEnvironmentName(cluster.getName());
        environment.setCrn(cluster.getClusterCrn());
        environment.setCreated(Instant.ofEpochMilli(cluster.getLastCreateTime()).atOffset(ZoneOffset.UTC));
        OnPremisesApiProto.OnPremEnvironmentDetails onPremEnvironmentDetails = cluster.getOnPremEnvironmentDetails();
        environment.setStatus(onPremEnvironmentDetails.getOnPremDatalakeDetails().getStatus().name());
        environment.setClouderaManagerClusterUuid(cluster.getCmClusterUuid());
        environment.setClouderaManagerVersion(onPremEnvironmentDetails.getCmVersion());
        environment.setCdpRuntimeVersion(onPremEnvironmentDetails.getClouderaRuntimeVersion());
        environment.setPvcEnvironmentDetails(new PvcEnvironmentDetails()
                .cmHost(onPremEnvironmentDetails.getCmHost())
                .knoxGatewayUrl(onPremEnvironmentDetails.getKnoxGatewayUrl())
                .applications(convertApplicationMap(onPremEnvironmentDetails.getApplicationsMap()))
                .privateDatalakeDetails(convertPrivateDatalakeDetails(onPremEnvironmentDetails.getOnPremDatalakeDetails())));
        return environment;
    }

    private PrivateDatalakeDetails convertPrivateDatalakeDetails(OnPremisesApiProto.OnPremDatalakeDetails datalakeDetails) {
        return new PrivateDatalakeDetails()
                .datalakeName(datalakeDetails.getDatalakeName())
                .creationTimeEpochMillis(datalakeDetails.getCreationTimeEpochMillis())
                .cmFQDN(datalakeDetails.getCmFqdn())
                .cmIP(datalakeDetails.getCmIp())
                .cmServerId(datalakeDetails.getCmServerId())
                .enableRangerRaz(datalakeDetails.getEnableRangerRaz())
                .status(convertDatalakeStatus(datalakeDetails.getStatus()))
                .kerberosInfo(convertKerberosInfo(datalakeDetails.getKerberosInfo()))
                .instances(convertInstances(datalakeDetails.getInstancesList()));
    }

    private List<Instance> convertInstances(List<OnPremisesApiProto.Instance> instances) {
        return instances.stream()
                .map(instance -> new Instance()
                        .discoveryFQDN(instance.getFqdn())
                        .instanceId(instance.getId())
                        .privateIp(instance.getPrivateIp()))
                .toList();
    }

    private KerberosInfo convertKerberosInfo(OnPremisesApiProto.KerberosInfo kerberosInfo) {
        return new KerberosInfo()
                .kerberized(kerberosInfo.getKerberized())
                .kdcType(kerberosInfo.getKdcType())
                .kdcHost(kerberosInfo.getKdcHost())
                .kdcHostIp(StringUtils.isNotBlank(kerberosInfo.getKdcHostIp()) ? kerberosInfo.getKdcHostIp() :
                        NetworkUtil.resolveHostAddress(kerberosInfo.getKdcHost()).orElse(null))
                .kerberosRealm(kerberosInfo.getKerberosRealm());
    }

    private PrivateDatalakeDetails.StatusEnum convertDatalakeStatus(OnPremisesApiProto.DatalakeStatus.Value datalakeStatus) {
        return datalakeStatus == OnPremisesApiProto.DatalakeStatus.Value.AVAILABLE ? PrivateDatalakeDetails.StatusEnum.AVAILABLE :
                PrivateDatalakeDetails.StatusEnum.NOT_AVAILABLE;
    }

    private Map<String, Application> convertApplicationMap(Map<String, OnPremisesApiProto.Application> applicationMap) {
        return applicationMap.entrySet().stream()
                .map(appEntry -> Map.entry(appEntry.getKey(), convertApplication(appEntry.getValue())))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    private Application convertApplication(OnPremisesApiProto.Application application) {
        return new Application()
                .name(application.getName())
                .config(application.getConfigMap())
                .services(convertServiceMap(application.getServicesMap()));
    }

    private Map<String, Service> convertServiceMap(Map<String, OnPremisesApiProto.Service> serviceMap) {
        return serviceMap.entrySet().stream()
                .map(serviceEntry -> Map.entry(serviceEntry.getKey(), convertService(serviceEntry.getValue())))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    private Service convertService(OnPremisesApiProto.Service service) {
        return new Service()
                .type(service.getType())
                .config(service.getConfigMap())
                .endpoints(convertServiceEndpoints(service.getEndpointsList()));
    }

    private List<ServiceEndPoint> convertServiceEndpoints(List<OnPremisesApiProto.ServiceEndpoint> serviceEndpoints) {
        return serviceEndpoints.stream()
                .map(endpoint -> new ServiceEndPoint()
                        .uri(endpoint.getUri())
                        .host(endpoint.getHost())
                        .port(endpoint.getPort()))
                .toList();
    }
}
