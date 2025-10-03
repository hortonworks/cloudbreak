package com.sequenceiq.remoteenvironment.service.connector.classiccluster;

import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import jakarta.inject.Inject;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.cloudera.api.swagger.CdpResourceApi;
import com.cloudera.api.swagger.ClouderaManagerResourceApi;
import com.cloudera.api.swagger.ClustersResourceApi;
import com.cloudera.api.swagger.ParcelsResourceApi;
import com.cloudera.api.swagger.client.ApiClient;
import com.cloudera.api.swagger.client.ApiException;
import com.cloudera.api.swagger.model.ApiCluster;
import com.cloudera.api.swagger.model.ApiCmServer;
import com.cloudera.api.swagger.model.ApiCmServerList;
import com.cloudera.api.swagger.model.ApiEndPoint;
import com.cloudera.api.swagger.model.ApiEndPointHost;
import com.cloudera.api.swagger.model.ApiEntityStatus;
import com.cloudera.api.swagger.model.ApiHostList;
import com.cloudera.api.swagger.model.ApiKerberosInfo;
import com.cloudera.api.swagger.model.ApiMapEntry;
import com.cloudera.api.swagger.model.ApiParcel;
import com.cloudera.api.swagger.model.ApiRemoteDataContext;
import com.cloudera.api.swagger.model.ApiVersionInfo;
import com.cloudera.thunderhead.service.environments2api.model.Application;
import com.cloudera.thunderhead.service.environments2api.model.Environment;
import com.cloudera.thunderhead.service.environments2api.model.Instance;
import com.cloudera.thunderhead.service.environments2api.model.KerberosInfo;
import com.cloudera.thunderhead.service.environments2api.model.PrivateDatalakeDetails;
import com.cloudera.thunderhead.service.environments2api.model.PvcEnvironmentDetails;
import com.cloudera.thunderhead.service.environments2api.model.Service;
import com.cloudera.thunderhead.service.environments2api.model.ServiceEndPoint;
import com.cloudera.thunderhead.service.onpremises.OnPremisesApiProto;
import com.sequenceiq.cloudbreak.cm.client.retry.ClouderaManagerApiFactory;
import com.sequenceiq.remoteenvironment.DescribeEnvironmentPropertiesV2Response;
import com.sequenceiq.remoteenvironment.DescribeEnvironmentV2Response;
import com.sequenceiq.remoteenvironment.RemoteEnvironmentException;

@Component
class ClassicClusterDescribeService {
    private static final Logger LOGGER = LoggerFactory.getLogger(ClassicClusterDescribeService.class);

    private static final String KNOX_SERVICE = "KNOX";

    private static final String KNOX_GATEWAY_ROLE = "KNOX_GATEWAY";

    private static final String CDP_PARCEL_PRODUCT_NAME = "CDH";

    private static final String CDP_PARCEL_ACTIVATED = "ACTIVATED";

    @Inject
    private ClassicClusterClouderaManagerApiClientProvider apiClientProvider;

    @Inject
    private ClouderaManagerApiFactory clouderaManagerApiFactory;

    public DescribeEnvironmentV2Response describe(OnPremisesApiProto.Cluster cluster) {
        try {
            DescribeEnvironmentV2Response describeEnvironmentV2Response = new DescribeEnvironmentV2Response();
            CMResources cmResources = populateCmResources(cluster);
            describeEnvironmentV2Response.setAdditionalProperties(createEnvironmentProperties(cluster));
            describeEnvironmentV2Response.setEnvironment(createEnvironment(cluster, cmResources));
            return describeEnvironmentV2Response;
        } catch (ApiException apiException) {
            String errorMsg = "Cannot collect information from the on premise CM server which is needed to describe the cluster";
            LOGGER.error(errorMsg, apiException);
            throw new RemoteEnvironmentException(errorMsg, apiException);
        }
    }

    private Environment createEnvironment(OnPremisesApiProto.Cluster cluster, CMResources cmResources) {
        Environment environment = new Environment();
        environment.setEnvironmentName(cluster.getName());
        environment.setCrn(cluster.getClusterCrn());
        environment.setCreated(Instant.ofEpochMilli(cluster.getLastCreateTime()).atOffset(ZoneOffset.UTC));
        PvcEnvironmentDetails pvcEnvironmentDetails = createPvcEnvironmentDetails(cluster, cmResources);
        environment.setPvcEnvironmentDetails(pvcEnvironmentDetails);
        environment.setStatus(pvcEnvironmentDetails.getPrivateDatalakeDetails().getStatus().name());
        populateVersions(cluster, environment, cmResources);
        return environment;
    }

    private PvcEnvironmentDetails createPvcEnvironmentDetails(OnPremisesApiProto.Cluster cluster, CMResources cmResources) {
        PvcEnvironmentDetails pvcEnvironmentDetails = new PvcEnvironmentDetails();
        Map<String, Application> applicationMap = createDatalakeServices(cmResources.remoteDataContext());
        pvcEnvironmentDetails.setApplications(applicationMap);
        pvcEnvironmentDetails.setCmHost(cluster.getManagerUri());
        pvcEnvironmentDetails.setKnoxGatewayUrl(getKnoxGatewayUrl(applicationMap));
        pvcEnvironmentDetails.setPrivateDatalakeDetails(getPrivateDatalakeDetails(cluster, cmResources));
        return pvcEnvironmentDetails;
    }

    private DescribeEnvironmentPropertiesV2Response createEnvironmentProperties(OnPremisesApiProto.Cluster cluster) {
        DescribeEnvironmentPropertiesV2Response envProperties = new DescribeEnvironmentPropertiesV2Response();
        envProperties.setRemoteEnvironmentUrl(
                cluster.getKnoxEnabled() && StringUtils.isNotBlank(cluster.getKnoxUrl()) ? cluster.getKnoxUrl() : cluster.getManagerUri());
        return envProperties;
    }

    private Map<String, Application> createDatalakeServices(ApiRemoteDataContext remoteDataContext) {
        List<ApiEndPoint> endPoints = remoteDataContext.getEndPoints();
        if (CollectionUtils.isNotEmpty(endPoints)) {
            Map<String, Application> applicationsMap = new HashMap<>();
            for (ApiEndPoint endPoint : endPoints) {
                Application application = new Application();
                application.setConfig(createConfigMap(endPoint.getServiceConfigs()));
                createServiceItems(endPoint.getEndPointHostList(), application);
                applicationsMap.put(endPoint.getServiceType(), application);
            }
            return applicationsMap;
        } else {
            return Map.of();
        }
    }

    private Map<String, String> createConfigMap(List<ApiMapEntry> configs) {
        return configs.stream()
                .filter(config -> !Strings.CI.containsAny(config.getKey(), "password"))
                .collect(Collectors.toMap(ApiMapEntry::getKey, ApiMapEntry::getValue));
    }

    private void createServiceItems(List<ApiEndPointHost> endpointHosts, Application application) {
        for (ApiEndPointHost host : endpointHosts) {
            try {
                Service svc = new Service();
                URI uri = new URI(host.getUri());
                ServiceEndPoint svcEndpoint = new ServiceEndPoint();
                svcEndpoint.setUri(uri.toString());
                svcEndpoint.setHost(uri.getHost());
                svcEndpoint.setPort(uri.getPort());
                svc.addEndpointsItem(svcEndpoint);
                application.putServicesItem(host.getType(), svc);
            } catch (URISyntaxException e) {
                LOGGER.warn("Uri syntax exception, endpoint collection is skipped for {}", host.getUri(), e);
            }
        }
    }

    private PrivateDatalakeDetails getPrivateDatalakeDetails(OnPremisesApiProto.Cluster cluster, CMResources cmResources) {
        PrivateDatalakeDetails privateDatalakeDetails = new PrivateDatalakeDetails();
        privateDatalakeDetails.setDatalakeName(cluster.getName());
        // Ranger Raz is not yet supported in Private Cloud
        privateDatalakeDetails.setEnableRangerRaz(false);
        privateDatalakeDetails.setCreationTimeEpochMillis(cluster.getLastCreateTime());
        privateDatalakeDetails.setKerberosInfo(getKerberosInfo(cmResources.apiKerberosInfo()));

        // fetch the first CM server instance info assuming CM HA is not enabled
        ApiCmServer apiCmServer = cmResources.apiCmServerList().getItems().getFirst();
        privateDatalakeDetails.setCmIP(apiCmServer.getIpAddress());
        privateDatalakeDetails.setCmFQDN(apiCmServer.getName());
        privateDatalakeDetails.setCmServerId(apiCmServer.getCmServerId());

        // Map CM API status to proto enum; GOOD_HEALTH maps to AVAILABLE, all other statuses map to NOT_AVAILABLE
        ApiEntityStatus clusterStatusFromCM = cmResources.apiCluster().getEntityStatus();
        PrivateDatalakeDetails.StatusEnum status = clusterStatusFromCM == ApiEntityStatus.GOOD_HEALTH
                ? PrivateDatalakeDetails.StatusEnum.AVAILABLE
                : PrivateDatalakeDetails.StatusEnum.NOT_AVAILABLE;
        privateDatalakeDetails.setStatus(status);

        privateDatalakeDetails.setInstances(getDatalakeInstances(cmResources.apiHostList()));
        return privateDatalakeDetails;
    }

    private KerberosInfo getKerberosInfo(ApiKerberosInfo apiKerberosInfo) {
        KerberosInfo kerberosInfo = new KerberosInfo();
        if (apiKerberosInfo.isKerberized()) {
            kerberosInfo.setKerberized(true);
            kerberosInfo.setKdcType(apiKerberosInfo.getKdcType());
            kerberosInfo.setKerberosRealm(apiKerberosInfo.getKerberosRealm());
            String kdcHost = apiKerberosInfo.getKdcHost();
            kerberosInfo.setKdcHost(kdcHost);
            kerberosInfo.setKdcHostIp(resolveKdcHostAddress(kdcHost));
        } else {
            kerberosInfo.setKerberized(false);
        }
        return kerberosInfo;
    }

    private List<Instance> getDatalakeInstances(ApiHostList apiHostList) {
        return apiHostList.getItems().stream()
                .map(apiHost -> {
                    Instance instance = new Instance();
                    instance.setDiscoveryFQDN(apiHost.getHostname());
                    instance.setPrivateIp(apiHost.getIpAddress());
                    instance.setInstanceId(apiHost.getHostId());
                    return instance;
                })
                .toList();
    }

    private String getKnoxGatewayUrl(Map<String, Application> applications) {
        return Optional.ofNullable(applications.get(KNOX_SERVICE))
                .map(Application::getServices)
                .map(services -> services.get(KNOX_GATEWAY_ROLE))
                .map(Service::getEndpoints)
                .filter(list -> !list.isEmpty())
                .map(List::getFirst)
                .map(ServiceEndPoint::getUri)
                .orElse("");
    }

    private void populateVersions(OnPremisesApiProto.Cluster cluster, Environment environment, CMResources cmResources) {
        environment.setClouderaManagerVersion(cmResources.apiVersionInfo().getVersion());
        if (cmResources.cdpParcel() != null) {
            environment.setCdpRuntimeVersion(cmResources.cdpParcel().getVersion());
        }
    }

    private String resolveKdcHostAddress(String kdcHostAddress) {
        try {
            LOGGER.debug("Trying to resolve KDC host address " + kdcHostAddress);
            return InetAddress.getByName(kdcHostAddress).getHostAddress();
        } catch (Exception e) {
            LOGGER.warn("Failed to resolve KDC host address " + kdcHostAddress, e);
            return null;
        }
    }

    private CMResources populateCmResources(OnPremisesApiProto.Cluster cluster) throws ApiException {
        ApiClient apiClient = apiClientProvider.getClouderaManagerRootClient(cluster);
        ApiClient apiV51Client = apiClientProvider.getClouderaManagerV51Client(cluster);
        ClustersResourceApi clustersResourceApi = clouderaManagerApiFactory.getClustersResourceApi(apiV51Client);
        ClouderaManagerResourceApi cmResourceApi = clouderaManagerApiFactory.getClouderaManagerResourceApi(apiV51Client);
        ParcelsResourceApi parcelsResourceApi = clouderaManagerApiFactory.getParcelsResourceApi(apiV51Client);
        CdpResourceApi cdpResourceApi = clouderaManagerApiFactory.getCdpResourceApi(apiClient);
        String clusterName = cluster.getName();
        ApiParcel cdpParcel = parcelsResourceApi.readParcels(clusterName, "SUMMARY").getItems()
                .stream()
                .filter(parcel -> CDP_PARCEL_PRODUCT_NAME.equals(parcel.getProduct()) && CDP_PARCEL_ACTIVATED.equals(parcel.getStage()))
                .findFirst().orElse(null);
        return new CMResources(cmResourceApi.getVersion(), cdpParcel, clustersResourceApi.listHosts(clusterName, null, null, "SUMMARY"),
                clustersResourceApi.readCluster(clusterName), cdpResourceApi.getRemoteContextByCluster(cluster.getName()),
                cmResourceApi.readInstances(), clustersResourceApi.getKerberosInfo(clusterName));
    }

    private record CMResources(
            ApiVersionInfo apiVersionInfo,
            ApiParcel cdpParcel,
            ApiHostList apiHostList,
            ApiCluster apiCluster,
            ApiRemoteDataContext remoteDataContext,
            ApiCmServerList apiCmServerList,
            ApiKerberosInfo apiKerberosInfo) {
    }
}
