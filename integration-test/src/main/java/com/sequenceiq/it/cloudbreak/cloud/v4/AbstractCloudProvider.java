package com.sequenceiq.it.cloudbreak.cloud.v4;

import static com.sequenceiq.common.model.OsType.RHEL8;
import static java.lang.String.format;

import java.util.Comparator;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import com.sequenceiq.cloudbreak.api.endpoint.v4.imagecatalog.responses.BaseImageV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.imagecatalog.responses.ImageV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.instancegroup.network.InstanceGroupNetworkV4Request;
import com.sequenceiq.common.api.telemetry.request.LoggingRequest;
import com.sequenceiq.common.api.type.ServiceEndpointCreation;
import com.sequenceiq.distrox.api.v1.distrox.model.network.InstanceGroupNetworkV1Request;
import com.sequenceiq.environment.api.v1.environment.model.request.AttachedFreeIpaRequest;
import com.sequenceiq.environment.api.v1.environment.model.request.EnvironmentAuthenticationRequest;
import com.sequenceiq.environment.api.v1.environment.model.response.DetailedEnvironmentResponse;
import com.sequenceiq.it.cloudbreak.cloud.HostGroupType;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.ClusterTestDto;
import com.sequenceiq.it.cloudbreak.dto.ImageSettingsTestDto;
import com.sequenceiq.it.cloudbreak.dto.PlacementSettingsTestDto;
import com.sequenceiq.it.cloudbreak.dto.SubnetId;
import com.sequenceiq.it.cloudbreak.dto.distrox.DistroXTestDtoBase;
import com.sequenceiq.it.cloudbreak.dto.distrox.cluster.DistroXClusterTestDto;
import com.sequenceiq.it.cloudbreak.dto.distrox.image.DistroXImageTestDto;
import com.sequenceiq.it.cloudbreak.dto.environment.EnvironmentAuthenticationTestDto;
import com.sequenceiq.it.cloudbreak.dto.environment.EnvironmentNetworkTestDto;
import com.sequenceiq.it.cloudbreak.dto.environment.EnvironmentSecurityAccessTestDto;
import com.sequenceiq.it.cloudbreak.dto.environment.EnvironmentTestDto;
import com.sequenceiq.it.cloudbreak.dto.freeipa.FreeIpaTestDto;
import com.sequenceiq.it.cloudbreak.dto.imagecatalog.ImageCatalogTestDto;
import com.sequenceiq.it.cloudbreak.dto.sdx.SdxCustomTestDto;
import com.sequenceiq.it.cloudbreak.dto.sdx.SdxInternalTestDto;
import com.sequenceiq.it.cloudbreak.dto.sdx.SdxRepairTestDto;
import com.sequenceiq.it.cloudbreak.dto.sdx.SdxTestDto;
import com.sequenceiq.it.cloudbreak.dto.stack.StackTestDtoBase;
import com.sequenceiq.it.cloudbreak.dto.telemetry.TelemetryTestDto;
import com.sequenceiq.it.cloudbreak.exception.TestFailException;
import com.sequenceiq.it.cloudbreak.log.Log;
import com.sequenceiq.it.cloudbreak.microservice.CloudbreakClient;
import com.sequenceiq.it.util.TestParameter;
import com.sequenceiq.sdx.api.model.SdxClusterShape;

public abstract class AbstractCloudProvider implements CloudProvider {
    private static final String DEFAULT_SUBNET_CIDR = "10.0.0.0/16";

    private static final String DEFAULT_ACCESS_CIDR = "0.0.0.0/0";

    private static final String DUMMY_SSH_KEY = "ssh-rsa "
            + "AAAAB3NzaC1yc2EAAAADAQABAAABAQC0Rfl2G2vDs6yc19RxCqReunFgpYj+ucyLobpTCBtfDwzIbJot2Fmife6M42mBtiTmAK6x8kc"
            + "UEeab6CB4MUzsqF7vGTFUjwWirG/XU5pYXFUBhi8xzey+KS9KVrQ+UuKJh/AN9iSQeMV+rgT1yF5+etVH+bK1/37QCKp3+mCqjFzPyQOrvkGZv4sYyRwX7BKBLleQmIVWpofpj"
            + "T7BfcCxH877RzC5YMIi65aBc82Dl6tH6OEiP7mzByU52yvH6JFuwZ/9fWj1vXCWJzxx2w0F1OU8Zwg8gNNzL+SVb9+xfBE7xBHMpYFg72hBWPh862Ce36F4NZd3MpWMSjMmpDPh"
            + " centos";

    private static final int OBJECT_NAME_MAX_LENGTH = 63;

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractCloudProvider.class);

    @Inject
    private TestParameter testParameter;

    @Inject
    private CommonCloudProperties commonCloudProperties;

    @Inject
    private CommonClusterManagerProperties commonClusterManagerProperties;

    protected TestParameter getTestParameter() {
        return testParameter;
    }

    protected CommonCloudProperties commonCloudProperties() {
        return commonCloudProperties;
    }

    protected CommonClusterManagerProperties commonClusterManagerProperties() {
        return commonClusterManagerProperties;
    }

    @Override
    public ImageCatalogTestDto imageCatalog(ImageCatalogTestDto imageCatalog) {
        return imageCatalog.withName(commonCloudProperties.getImageCatalogName());
    }

    @Override
    public ImageSettingsTestDto imageSettings(ImageSettingsTestDto imageSettings) {
        imageSettings.withImageCatalog(commonCloudProperties.getImageCatalogName());
        return imageSettings;
    }

    @Override
    public DistroXImageTestDto imageSettings(DistroXImageTestDto imageSettings) {
        imageSettings.withImageCatalog(commonCloudProperties.getImageCatalogName());
        return imageSettings;
    }

    @Override
    public ServiceEndpointCreation serviceEndpoint() {
        return ServiceEndpointCreation.DISABLED;
    }

    @Override
    public EnvironmentTestDto setS3Guard(EnvironmentTestDto environmentTestDto, String tableName) {
        LOGGER.info("S3guard is ignored on cloudplatform {}.", getCloudPlatform());
        return environmentTestDto;
    }

    @Override
    public EnvironmentTestDto withResourceGroup(EnvironmentTestDto environmentTestDto, String resourceGroupUsage, String resourceGroupName) {
        return environmentTestDto;
    }

    @Override
    public EnvironmentTestDto withResourceEncryption(EnvironmentTestDto environmentTestDto) {
        return environmentTestDto;
    }

    @Override
    public DistroXTestDtoBase withResourceEncryption(DistroXTestDtoBase distroXTestDtoBase) {
        return distroXTestDtoBase;
    }

    @Override
    public EnvironmentTestDto environment(EnvironmentTestDto environment) {
        AttachedFreeIpaRequest attachedFreeIpaRequest = new AttachedFreeIpaRequest();
        attachedFreeIpaRequest.setCreate(Boolean.FALSE);
        return environment
                .withLocation(location())
                .withFreeIpa(attachedFreeIpaRequest);
    }

    @Override
    public PlacementSettingsTestDto placement(PlacementSettingsTestDto placement) {
        return placement.withRegion(region())
                .withAvailabilityZone(availabilityZone());
    }

    @Override
    public String getSubnetCIDR() {
        String subnetCIDR = commonCloudProperties.getSubnetCidr();
        return subnetCIDR == null ? DEFAULT_SUBNET_CIDR : subnetCIDR;
    }

    @Override
    public String getAccessCIDR() {
        String accessCIDR = commonCloudProperties.getAccessCidr();
        return accessCIDR == null ? DEFAULT_ACCESS_CIDR : accessCIDR;
    }

    @Override
    public Map<String, String> getTags() {
        return commonCloudProperties.getTags();
    }

    @Override
    public SdxClusterShape getClusterShape() {
        return commonClusterManagerProperties.getClusterShape();
    }

    @Override
    public SdxClusterShape getInternalClusterShape() {
        return commonClusterManagerProperties.getInternalClusterShape();
    }

    @Override
    public LoggingRequest loggingRequest(TelemetryTestDto dto) {
        return null;
    }

    @Override
    public InstanceGroupNetworkV4Request instanceGroupNetworkV4Request(SubnetId subnetId) {
        return null;
    }

    @Override
    public InstanceGroupNetworkV1Request instanceGroupNetworkV1Request(SubnetId subnetId) {
        return null;
    }

    @Override
    public Integer gatewayPort(StackTestDtoBase stackEntity) {
        return commonCloudProperties.getGatewayPort();
    }

    @Override
    public Integer gatewayPort(FreeIpaTestDto stackEntity) {
        return commonCloudProperties.getGatewayPort();
    }

    @Override
    public void setImageCatalogName(String name) {
        commonCloudProperties().setImageCatalogName(name);
    }

    @Override
    public void setImageCatalogUrl(String url) {
        commonCloudProperties().setImageCatalogUrl(url);
    }

    @Override
    public String getImageCatalogName() {
        return commonCloudProperties().getImageCatalogName();
    }

    @Override
    public final ClusterTestDto cluster(ClusterTestDto clusterTestDto) {
        clusterTestDto.withUserName(commonClusterManagerProperties.getClouderaManager().getDefaultUser())
                .withPassword(commonClusterManagerProperties.getClouderaManager().getDefaultPassword());
        return withCluster(clusterTestDto);
    }

    @Override
    public final DistroXClusterTestDto cluster(DistroXClusterTestDto clusterTestDto) {
        clusterTestDto.withUserName(commonClusterManagerProperties.getClouderaManager().getDefaultUser())
                .withPassword(commonClusterManagerProperties.getClouderaManager().getDefaultPassword());
        return withCluster(clusterTestDto);
    }

    @Override
    public final SdxTestDto sdx(SdxTestDto sdx) {
        sdx.withTags(commonCloudProperties.getTags());
        return sdx;
    }

    @Override
    public SdxInternalTestDto sdxInternal(SdxInternalTestDto sdxInternal) {
        sdxInternal.withDefaultSDXSettings();
        return sdxInternal;
    }

    @Override
    public SdxCustomTestDto sdxCustom(SdxCustomTestDto sdxCustom) {
        sdxCustom.withTags(commonCloudProperties.getTags());
        return sdxCustom;
    }

    @Override
    public final SdxRepairTestDto sdxRepair(SdxRepairTestDto sdxRepair) {
        if (sdxRepair.getRequest().getHostGroupNames() == null || sdxRepair.getRequest().getHostGroupNames().isEmpty()) {
            sdxRepair.withHostGroupNames(List.of(HostGroupType.MASTER.getName(), HostGroupType.IDBROKER.getName()));
        }
        return sdxRepair;
    }

    @Override
    public EnvironmentAuthenticationTestDto environmentAuthentication(EnvironmentAuthenticationTestDto environmentAuthenticationEntity) {
        EnvironmentAuthenticationRequest request = environmentAuthenticationEntity.getRequest();
        environmentAuthenticationEntity.withPublicKey(StringUtils.isNotBlank(commonCloudProperties.getSshPublicKey())
                ? commonCloudProperties.getSshPublicKey()
                : DUMMY_SSH_KEY);
        environmentAuthenticationEntity.withPublicKeyId(request.getPublicKeyId());
        environmentAuthenticationEntity.withLoginUserName(StringUtils.isBlank(request.getLoginUserName())
                ? "cloudbreak"
                : request.getLoginUserName());
        return environmentAuthenticationEntity;
    }

    @Override
    public EnvironmentSecurityAccessTestDto environmentSecurityAccess(EnvironmentSecurityAccessTestDto environmentSecurityAccessTestDto) {
        return environmentSecurityAccessTestDto;
    }

    @Override
    public String getFreeIpaImageCatalogUrl() {
        return null;
    }

    @Override
    public String getVariant() {
        return null;
    }

    protected abstract ClusterTestDto withCluster(ClusterTestDto cluster);

    protected abstract DistroXClusterTestDto withCluster(DistroXClusterTestDto cluster);

    @Override
    public String getLatestMarketplacePreWarmedImageID(TestContext testContext, ImageCatalogTestDto imageCatalogTestDto, CloudbreakClient cloudbreakClient,
            String runtimeVersion) {
        throw new TestFailException("Marketplace images are not supported on this platform");
    }

    public String getLatestPreWarmedImage(ImageCatalogTestDto imageCatalogTestDto, CloudbreakClient cloudbreakClient, String platform, boolean govCloud) {
        return getLatestPreWarmedImageByRuntimeVersion(imageCatalogTestDto, cloudbreakClient, platform, govCloud,
                commonClusterManagerProperties.getRuntimeVersion());
    }

    public String getLatestPreWarmedImageByRuntimeVersion(ImageCatalogTestDto imageCatalogTestDto, CloudbreakClient cloudbreakClient, String platform,
            boolean govCloud, String runtimeVersion) {
        String osType = govCloud ? RHEL8.getOs() : "redhat7";
        try {
            List<ImageV4Response> prewarmedImagesForRuntime = cloudbreakClient
                    .getDefaultClient()
                    .imageCatalogV4Endpoint()
                    .getImagesByName(cloudbreakClient.getWorkspaceId(), imageCatalogTestDto.getRequest().getName(), null,
                            platform, null, null, govCloud)
                    .getCdhImages().stream()
                    .filter(image -> StringUtils.equalsIgnoreCase(image.getStackDetails().getVersion(), runtimeVersion))
                    .filter(imageV4Response -> StringUtils.equalsIgnoreCase(imageV4Response.getOsType(), osType))
                    .sorted(Comparator.comparing(ImageV4Response::getPublished))
                    .toList();

            if (prewarmedImagesForRuntime.isEmpty()) {
                throw new TestFailException(format("Cannot find pre-warmed images at '%s' provider for '%s' runtime version!", platform,
                        runtimeVersion));
            }
            ImageV4Response latestPrewarmedImage = prewarmedImagesForRuntime.get(prewarmedImagesForRuntime.size() - 1);
            Log.log(LOGGER, format(" Image Catalog Name: %s ", imageCatalogTestDto.getRequest().getName()));
            Log.log(LOGGER, format(" Image Catalog URL: %s ", imageCatalogTestDto.getRequest().getUrl()));
            Log.log(LOGGER, format(" Selected Pre-warmed Image Date: %s | ID: %s | OS: %s | Description: %s ", latestPrewarmedImage.getDate(),
                    latestPrewarmedImage.getUuid(), latestPrewarmedImage.getOs(), latestPrewarmedImage.getDescription()));
            return latestPrewarmedImage.getUuid();
        } catch (TestFailException e) {
            throw e;
        } catch (Exception e) {
            LOGGER.error("Cannot fetch pre-warmed images from '{}' image catalog, because of: ", imageCatalogTestDto.getRequest().getName(), e);
            throw new TestFailException(format("Cannot fetch pre-warmed images from '%s' image catalog!", imageCatalogTestDto.getRequest().getName()), e);
        }
    }

    public String getLatestBaseImage(ImageCatalogTestDto imageCatalogTestDto, CloudbreakClient cloudbreakClient, String platform, boolean govCloud) {
        try {
            List<BaseImageV4Response> images = cloudbreakClient
                    .getDefaultClient()
                    .imageCatalogV4Endpoint()
                    .getImagesByName(cloudbreakClient.getWorkspaceId(), imageCatalogTestDto.getRequest().getName(), null,
                            platform, null, null, govCloud).getBaseImages();

            if (images.isEmpty()) {
                throw new TestFailException("Images are empty, there is not any base image on provider " + platform);
            }
            BaseImageV4Response baseImage = images.get(images.size() - 1);
            Log.log(LOGGER, format(" Image Catalog Name: %s ", imageCatalogTestDto.getRequest().getName()));
            Log.log(LOGGER, format(" Image Catalog URL: %s ", imageCatalogTestDto.getRequest().getUrl()));
            Log.log(LOGGER, format(" Selected Base Image Date: %s | ID: %s | Description: %s ", baseImage.getDate(),
                    baseImage.getUuid(), baseImage.getDescription()));

            return baseImage.getUuid();
        } catch (TestFailException e) {
            throw e;
        } catch (Exception e) {
            LOGGER.error("Cannot fetch base images of {} image catalog, because of ", imageCatalogTestDto.getRequest().getName(), e);
            throw new TestFailException(" Cannot fetch base images of " + imageCatalogTestDto.getRequest().getName() + " image catalog", e);
        }
    }

    @Override
    public String getFreeIpaUpgradeImageId() {
        LOGGER.warn("'freeIpaUpgradeImageId' is not implemented");
        return null;
    }

    @Override
    public String getFreeIpaMarketplaceUpgradeImageId() {
        LOGGER.warn("'freeIpaMarketplaceUpgradeImageId' is not implemented");
        return null;
    }

    @Override
    public String getFreeIpaUpgradeImageCatalog() {
        LOGGER.warn("'freeIpaUpgradeImageCatalog' is not implemented");
        return null;
    }

    @Override
    public String getFreeIpaMarketplaceUpgradeImageCatalog() {
        LOGGER.warn("'freeIpaMarketplaceUpgradeImageCatalog' is not implemented");
        return null;
    }

    @Override
    public String getSdxMarketplaceUpgradeImageId() {
        LOGGER.warn("'sdxMarketplaceUpgradeImageId' is not implemented");
        return null;
    }

    @Override
    public String getSdxMarketplaceUpgradeImageCatalog() {
        LOGGER.warn("'sdxMarketplaceUpgradeImageCatalog' is not implemented");
        return null;
    }

    @Override
    public void verifyDiskEncryptionKey(DetailedEnvironmentResponse environment, String environmentName) {
        LOGGER.warn("Disk encryption feature is not available currently");
    }

    @Override
    public void verifyVolumeEncryptionKey(List<String> volumeEncryptionKeyIds, String environmentName) {
        LOGGER.warn("Disk encryption feature is not available currently");
    }

    @Override
    public boolean getGovCloud() {
        return false;
    }

    public boolean isMultiAZ() {
        return false;
    }

    @Override
    public boolean isExternalDatabaseSslEnforcementSupported() {
        return false;
    }

    @Override
    public EnvironmentNetworkTestDto newNetwork(EnvironmentNetworkTestDto network) {
        return network;
    }

    protected String getSuiteName() {
        String suiteName = trimObjectName(getTestLabels()[0]);
        if (StringUtils.isBlank(suiteName)) {
            throw new IllegalArgumentException("Cloud Storage base location path cannot be generated, because of the Test Suite Name is null or empty!");
        } else {
            return suiteName;
        }
    }

    protected String getTestName() {
        String testName = trimObjectName(getTestLabels()[1]);
        if (StringUtils.isBlank(testName)) {
            throw new IllegalArgumentException("Cloud Storage base location path cannot be generated, because of the Test Name is null or empty!");
        } else {
            return testName;
        }
    }

    protected String[] getTestLabels() {
        String testLabel = StringUtils.isBlank(MDC.get("testlabel")) ? testParameter.get("testlabel") : MDC.get("testlabel");
        return StringUtils.split(StringUtils.lowerCase(testLabel), ".");
    }

    protected String trimObjectName(String name) {
        return (name.length() > OBJECT_NAME_MAX_LENGTH) ? name.substring(0, OBJECT_NAME_MAX_LENGTH) : name;
    }
}
