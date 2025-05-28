package com.sequenceiq.freeipa.converter.stack;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import jakarta.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

import com.google.common.collect.Maps;
import com.sequenceiq.cloudbreak.cloud.model.StackTags;
import com.sequenceiq.cloudbreak.cloud.model.instance.AwsInstanceTemplate;
import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.common.network.NetworkConstants;
import com.sequenceiq.common.api.backup.request.BackupRequest;
import com.sequenceiq.common.api.telemetry.model.Features;
import com.sequenceiq.common.api.telemetry.model.Logging;
import com.sequenceiq.common.api.telemetry.model.Monitoring;
import com.sequenceiq.common.api.telemetry.model.Telemetry;
import com.sequenceiq.common.api.telemetry.model.WorkloadAnalytics;
import com.sequenceiq.common.api.telemetry.request.FeaturesRequest;
import com.sequenceiq.common.api.telemetry.request.LoggingRequest;
import com.sequenceiq.common.api.telemetry.request.MonitoringRequest;
import com.sequenceiq.common.api.telemetry.request.TelemetryRequest;
import com.sequenceiq.common.api.telemetry.request.WorkloadAnalyticsRequest;
import com.sequenceiq.common.model.Architecture;
import com.sequenceiq.freeipa.api.model.Backup;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.FreeIpaServerRequest;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.image.ImageSettingsRequest;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.instance.InstanceGroupNetworkRequest;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.instance.InstanceGroupRequest;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.instance.InstanceTemplateRequest;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.instance.VolumeRequest;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.instance.aws.AwsInstanceTemplateParameters;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.instance.aws.AwsInstanceTemplateSpotParameters;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.instance.aws.InstanceGroupAwsNetworkParameters;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.network.NetworkRequest;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.region.PlacementRequest;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.security.SecurityGroupRequest;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.security.SecurityRuleRequest;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.security.StackAuthenticationRequest;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.create.CreateFreeIpaRequest;
import com.sequenceiq.freeipa.entity.FreeIpa;
import com.sequenceiq.freeipa.entity.ImageEntity;
import com.sequenceiq.freeipa.entity.InstanceGroup;
import com.sequenceiq.freeipa.entity.InstanceGroupNetwork;
import com.sequenceiq.freeipa.entity.Network;
import com.sequenceiq.freeipa.entity.SecurityGroup;
import com.sequenceiq.freeipa.entity.SecurityRule;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.entity.StackAuthentication;
import com.sequenceiq.freeipa.entity.Template;
import com.sequenceiq.freeipa.service.freeipa.FreeIpaService;

@Component
public class StackToCreateFreeIpaRequestConverter implements Converter<Stack, CreateFreeIpaRequest> {

    private static final Logger LOGGER = LoggerFactory.getLogger(StackToCreateFreeIpaRequestConverter.class);

    private static final String DELETED_NAME_DELIMETER = "_";

    private static final String PATH_DELIMETER = "/";

    private static final String CLUSTER_BACKUP_PREFIX = "cluster-backups";

    private static final String CLUSTER_LOG_PREFIX = "cluster-logs";

    private static final String CLUSTER_TYPE = "freeipa";

    private static final String AZURE_BLOB_STORAGE_SCHEMA = "https";

    private static final String ORIGINAL_AZURE_BLOB_STORAGE_SCHEMA = "abfs://";

    @Inject
    private FreeIpaService freeIpaService;

    @Override
    public CreateFreeIpaRequest convert(Stack source) {
        LOGGER.debug("Creating a CreateFreeIpaRequest from {}", source);
        CreateFreeIpaRequest request = new CreateFreeIpaRequest();

        request.setEnvironmentCrn(source.getEnvironmentCrn());
        request.setName(getName(source));
        request.setPlacement(getPlacementRequest(source));
        request.setInstanceGroups(source.getInstanceGroups().stream()
                .map(this::getInstanceGroupRequst)
                .collect(Collectors.toList()));
        request.setAuthentication(getStackAuthenticationRequest(source.getStackAuthentication()));
        request.setNetwork(getNetworkRequest(source.getNetwork()));
        request.setImage(getImageSettingsRequest(source.getImage()));
        request.setArchitecture(Optional.ofNullable(source.getArchitecture()).map(Architecture::getName).orElse(null));
        request.setFreeIpa(getFreeIpaServerRequest(source));
        request.setGatewayPort(source.getGatewayport());
        request.setTelemetry(getTelemetry(source));
        request.setBackup(getBackup(source));
        request.setTags(getTags(source.getTags()));
        request.setUseCcm(source.getUseCcm());
        request.setTunnel(source.getTunnel());
        request.setVariant(source.getPlatformvariant());

        LOGGER.info("Created CreateFreeIpaRequest {} from the original stack {}", request, source);
        return request;
    }

    private String getName(Stack stack) {
        String name = stack.getName();
        name = stack.isDeleteCompleted() ? StringUtils.substringBeforeLast(name, DELETED_NAME_DELIMETER) : name;
        LOGGER.debug("Using the stack name {} from the original stack name {}",  name, stack.getName());
        return name;
    }

    private PlacementRequest getPlacementRequest(Stack stack) {
        PlacementRequest placementRequest = new PlacementRequest();
        placementRequest.setRegion(stack.getRegion());
        placementRequest.setAvailabilityZone(stack.getAvailabilityZone());
        return placementRequest;
    }

    private InstanceGroupRequest getInstanceGroupRequst(InstanceGroup instanceGroup) {
        InstanceGroupRequest request = new InstanceGroupRequest();
        request.setInstanceTemplateRequest(getInstanceTemplateRequest(instanceGroup.getTemplate()));
        request.setName(instanceGroup.getGroupName());
        request.setNetwork(getInstanceGroupNetworkRequest(instanceGroup.getInstanceGroupNetwork()));
        request.setNodeCount(instanceGroup.getNodeCount());
        request.setSecurityGroup(getSecurityGroupRequest(instanceGroup.getSecurityGroup()));
        request.setType(instanceGroup.getInstanceGroupType());
        LOGGER.debug("Created instance group request {} from the original instance group {}", request, instanceGroup);
        return request;
    }

    private InstanceTemplateRequest getInstanceTemplateRequest(Template template) {
        InstanceTemplateRequest request = null;
        if (template != null) {
            request = new InstanceTemplateRequest();
            request.setInstanceType(template.getInstanceType());
            Integer spotPercentage = template.getAttributes().getValue(AwsInstanceTemplate.EC2_SPOT_PERCENTAGE);
            Double maxPrice = template.getAttributes().getValue(AwsInstanceTemplate.EC2_SPOT_MAX_PRICE);
            if (spotPercentage != null) {
                LOGGER.debug("EC2 spot percentage found in the instances template attributes");
                AwsInstanceTemplateParameters aws = new AwsInstanceTemplateParameters();
                AwsInstanceTemplateSpotParameters spot = new AwsInstanceTemplateSpotParameters();
                spot.setPercentage(spotPercentage);
                if (maxPrice != null) {
                    LOGGER.debug("EC2 max price found in the instances template attributes");
                    spot.setMaxPrice(maxPrice);
                }
                aws.setSpot(spot);
                request.setAws(aws);
            }
            VolumeRequest volumeRequest = new VolumeRequest();
            volumeRequest.setType(template.getVolumeType());
            volumeRequest.setCount(template.getVolumeCount());
            volumeRequest.setSize(template.getVolumeSize());
            request.setAttachedVolumes(Optional.of(volumeRequest)
                    .filter(v -> v.getCount() != null && v.getCount() > 0 &&
                            v.getSize() != null && v.getSize() > 0)
                            .stream().collect(Collectors.toSet()));
        }
        LOGGER.debug("Created instance template request {} from instance template {}", request, template);
        return request;
    }

    private InstanceGroupNetworkRequest getInstanceGroupNetworkRequest(InstanceGroupNetwork instanceGroupNetwork) {
        InstanceGroupNetworkRequest request = null;
        if (instanceGroupNetwork != null) {
            request = new InstanceGroupNetworkRequest();
            List<String> subnetIds = instanceGroupNetwork.getAttributes().getValue(NetworkConstants.SUBNET_IDS);
            if (subnetIds != null) {
                LOGGER.debug("Subnet IDs found in instance group network");
                InstanceGroupAwsNetworkParameters aws = new InstanceGroupAwsNetworkParameters();
                aws.setSubnetIds(subnetIds);
                request.setAws(aws);
            }
        }
        LOGGER.debug("Created instance group network request {} from instance group network {}",
                request, instanceGroupNetwork);
        return request;
    }

    private SecurityGroupRequest getSecurityGroupRequest(SecurityGroup securityGroup) {
        SecurityGroupRequest request = null;
        if (securityGroup != null) {
            request = new SecurityGroupRequest();
            request.setSecurityGroupIds(securityGroup.getSecurityGroupIds());
            request.setSecurityRules(securityGroup.getSecurityRules().stream()
                    .map(this::getSecurityRuleRequest)
                    .collect(Collectors.toList()));
        }
        LOGGER.debug("Created security group request {} from security group {}", request, securityGroup);
        return request;
    }

    private SecurityRuleRequest getSecurityRuleRequest(SecurityRule securityRule) {
        SecurityRuleRequest request = null;
        if (securityRule != null) {
            request = new SecurityRuleRequest();
            request.setModifiable(securityRule.isModifiable());
            request.setPorts(Arrays.stream(securityRule.getPorts()).collect(Collectors.toList()));
            request.setProtocol(securityRule.getProtocol());
            request.setSubnet(securityRule.getCidr());
        }
        LOGGER.debug("Created security rule request {} from security rule {}", request, securityRule);
        return request;
    }

    private StackAuthenticationRequest getStackAuthenticationRequest(StackAuthentication stackAuthentication) {
        StackAuthenticationRequest request = null;
        if (stackAuthentication != null) {
            request = new StackAuthenticationRequest();
            request.setLoginUserName(stackAuthentication.getLoginUserName());
            request.setPublicKey(stackAuthentication.getPublicKey());
            request.setPublicKeyId(stackAuthentication.getPublicKeyId());
        }
        LOGGER.debug("Created stack authentication request {} from stack authentication {}",
                request, stackAuthentication);
        return request;
    }

    private NetworkRequest getNetworkRequest(Network network) {
        NetworkRequest request = null;
        if (network != null) {
            request = new NetworkRequest();
            Optional<CloudPlatform> cloudPlatform = Optional.ofNullable(network.cloudPlatform())
                    .or(() -> Optional.ofNullable(network.getAttributes().getValue(NetworkConstants.CLOUD_PLATFORM)))
                    .map(CloudPlatform::valueOf);
            request.setNetworkCidrs(network.getNetworkCidrs());
            request.setOutboundInternetTraffic(network.getOutboundInternetTraffic());
            if (cloudPlatform.isPresent()) {
                LOGGER.debug("Network request has cloud platform {}", cloudPlatform.get());
                request.setCloudPlatform(cloudPlatform.get());
                switch (cloudPlatform.get()) {
                    case AWS:
                        request.createAws().parse(network.getAttributes().getMap());
                        break;
                    case AZURE:
                        request.createAzure().parse(network.getAttributes().getMap());
                        break;
                    case GCP:
                        request.createGcp().parse(network.getAttributes().getMap());
                        break;
                    case MOCK:
                        request.createMock().parse(network.getAttributes().getMap());
                        break;
                    case YARN:
                        request.createYarn().parse(network.getAttributes().getMap());
                        break;
                    default:
                        break;
                }
            }
        }
        LOGGER.debug("Created network request {} from network {}", request, network);
        return request;
    }

    private ImageSettingsRequest getImageSettingsRequest(ImageEntity image) {
        ImageSettingsRequest request = null;
        if (image != null) {
            request = new ImageSettingsRequest();
            request.setCatalog(image.getImageCatalogUrl());
            request.setId(image.getImageId());
            request.setOs(image.getOs());
        }
        LOGGER.debug("Created image settings request {} from image entity {}", request, image);
        return request;
    }

    private FreeIpaServerRequest getFreeIpaServerRequest(Stack stack) {
        FreeIpaServerRequest request = new FreeIpaServerRequest();
        FreeIpa freeIpa = freeIpaService.findByStack(stack);
        if (freeIpa != null) {
            request.setAdminGroupName(freeIpa.getAdminGroupName());
            request.setAdminPassword(freeIpa.getAdminPassword());
            request.setDomain(freeIpa.getDomain());
            request.setHostname(freeIpa.getHostname());
        }
        LOGGER.debug("Created FreeIPA server request {} from FreeIPA {}", request, freeIpa);
        return request;
    }

    private TelemetryRequest getTelemetry(Stack stack) {
        TelemetryRequest request = null;
        Telemetry telemetry = stack.getTelemetry();
        if (telemetry != null) {
            request = new TelemetryRequest();
            request.setFluentAttributes(telemetry.getFluentAttributes());
            request.setLogging(getLoggingRequest(telemetry.getLogging()));
            request.setMonitoring(getMonitoringRequest(telemetry.getMonitoring()));
            request.setFeatures(getFeaturesRequest(telemetry.getFeatures()));
            request.setWorkloadAnalytics(getWorkloadAnalyticsRequest(telemetry.getWorkloadAnalytics()));
        }
        LOGGER.debug("Created telemetry request {} from telemetry {}", request, telemetry);
        return request;
    }

    private LoggingRequest getLoggingRequest(Logging logging) {
        LoggingRequest request = null;
        if (logging != null) {
            request = new LoggingRequest();
            request.setStorageLocation(getLoggingLocation(logging.getStorageLocation()));
            request.setS3(logging.getS3());
            request.setAdlsGen2(logging.getAdlsGen2());
            request.setGcs(logging.getGcs());
        }
        LOGGER.debug("Created logging request {} from logging {}", request, logging);
        return request;
    }

    private MonitoringRequest getMonitoringRequest(Monitoring monitoring) {
        MonitoringRequest request = null;
        if (monitoring != null) {
            request = new MonitoringRequest();
            request.setRemoteWriteUrl(monitoring.getRemoteWriteUrl());
        }
        LOGGER.debug("Created monitoring request {} from monitoring {}", request, monitoring);
        return request;
    }

    private FeaturesRequest getFeaturesRequest(Features features) {
        FeaturesRequest request = null;
        if (features != null) {
            request = new FeaturesRequest();
            request.setMonitoring(features.getMonitoring());
            request.setCloudStorageLogging(features.getCloudStorageLogging());
            request.setWorkloadAnalytics(features.getWorkloadAnalytics());
        }
        LOGGER.debug("Created features request {} from features {}", request, features);
        return request;
    }

    private WorkloadAnalyticsRequest getWorkloadAnalyticsRequest(WorkloadAnalytics workloadAnalytics) {
        WorkloadAnalyticsRequest request = null;
        if (workloadAnalytics != null) {
            request = new WorkloadAnalyticsRequest();
            request.setAttributes(workloadAnalytics.getAttributes());
        }
        LOGGER.debug("Created workload analytics request {} from workload analytics {}", request, workloadAnalytics);
        return request;
    }

    private BackupRequest getBackup(Stack stack) {
        BackupRequest request = null;
        Backup backup = stack.getBackup();
        if (backup != null) {
            request = new BackupRequest();
            request.setStorageLocation(getBackupLocation(stack, backup.getStorageLocation()));
            request.setS3(backup.getS3());
            request.setAdlsGen2(backup.getAdlsGen2());
            request.setGcs(backup.getGcs());
        }
        LOGGER.debug("Created backup request {} from backup {}", request, backup);
        return request;
    }

    private Map<String, String> getTags(Json tags) {
        Map<String, String> userDefined = Maps.newHashMap();
        if (tags != null) {
            try {
                StackTags stackTag = tags.get(StackTags.class);
                userDefined.putAll(stackTag.getUserDefinedTags());
            } catch (IOException e) {
                LOGGER.info("Exception during converting user defined tags.", e);
            }
        }
        LOGGER.debug("Created tags request {} from tags {}", userDefined, tags);
        return userDefined;
    }

    private String getLoggingLocation(String location) {
        return getLocation(location, CLUSTER_LOG_PREFIX);
    }

    String getBackupLocation(Stack stack, String location) {
        String originalLocation = getLocation(location, CLUSTER_BACKUP_PREFIX);

        // During provisioning, the URL for Azure is converted from abfs/abfss to https, this conversion needs to be reversed.
        // Example: convert https://storage1.dfs.core.windows.net/logs-fs into abfs://logs-fs@storage1.dfs.core.windows.net
        try {
            if (CloudPlatform.AZURE.equalsIgnoreCase(stack.getCloudPlatform()) && originalLocation != null) {
                URI uri = new URI(originalLocation);
                if (AZURE_BLOB_STORAGE_SCHEMA.equals(uri.getScheme())) {
                    String uriPath = uri.getPath();
                    int firstSeparator = uriPath.indexOf(PATH_DELIMETER);
                    if (firstSeparator != -1) {
                        int secondSeparator = uriPath.indexOf(PATH_DELIMETER, firstSeparator + 1);
                        String bucketName;
                        String bucketPath = "";
                        if (secondSeparator == -1) {
                            bucketName = uriPath.substring(firstSeparator + 1);
                        } else {
                            bucketName = uriPath.substring(firstSeparator + 1, secondSeparator);
                            bucketPath = uriPath.substring(secondSeparator);
                        }
                        originalLocation = String.format("%s%s@%s%s", ORIGINAL_AZURE_BLOB_STORAGE_SCHEMA, bucketName, uri.getHost(), bucketPath);
                    }
                }
            }
        } catch (URISyntaxException e) {
            String error = String.format("Unable to parse URI for backup location %s", originalLocation);
            LOGGER.error(error);
            throw new BadRequestException(error, e);
        }
        LOGGER.debug("Created backup location {} location {}", originalLocation, location);
        return originalLocation;
    }

    /**
     * The storage location has the following appended /PREFIX/CLUSTER_TYPE/CLUSTER_IDENTIFIER appended. This must be
     * removed in order to get the original path.
     */
    private String getLocation(String location, String prefix) {
        return StringUtils.substringBeforeLast(location, PATH_DELIMETER + prefix + PATH_DELIMETER + CLUSTER_TYPE);
    }
}
