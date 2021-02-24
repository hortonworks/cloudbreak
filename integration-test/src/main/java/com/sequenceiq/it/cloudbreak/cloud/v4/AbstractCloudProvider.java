package com.sequenceiq.it.cloudbreak.cloud.v4;

import static java.lang.String.format;

import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.cloudbreak.api.endpoint.v4.imagecatalog.responses.BaseImageV4Response;
import com.sequenceiq.common.api.type.ServiceEndpointCreation;
import com.sequenceiq.environment.api.v1.environment.model.request.AttachedFreeIpaRequest;
import com.sequenceiq.it.TestParameter;
import com.sequenceiq.it.cloudbreak.CloudbreakClient;
import com.sequenceiq.it.cloudbreak.cloud.HostGroupType;
import com.sequenceiq.it.cloudbreak.dto.ClusterTestDto;
import com.sequenceiq.it.cloudbreak.dto.ImageSettingsTestDto;
import com.sequenceiq.it.cloudbreak.dto.PlacementSettingsTestDto;
import com.sequenceiq.it.cloudbreak.dto.distrox.cluster.DistroXClusterTestDto;
import com.sequenceiq.it.cloudbreak.dto.distrox.image.DistroXImageTestDto;
import com.sequenceiq.it.cloudbreak.dto.environment.EnvironmentAuthenticationTestDto;
import com.sequenceiq.it.cloudbreak.dto.environment.EnvironmentSecurityAccessTestDto;
import com.sequenceiq.it.cloudbreak.dto.environment.EnvironmentTestDto;
import com.sequenceiq.it.cloudbreak.dto.freeipa.FreeIpaTestDto;
import com.sequenceiq.it.cloudbreak.dto.imagecatalog.ImageCatalogTestDto;
import com.sequenceiq.it.cloudbreak.dto.sdx.SdxCustomTestDto;
import com.sequenceiq.it.cloudbreak.dto.sdx.SdxInternalTestDto;
import com.sequenceiq.it.cloudbreak.dto.sdx.SdxRepairTestDto;
import com.sequenceiq.it.cloudbreak.dto.sdx.SdxTestDto;
import com.sequenceiq.it.cloudbreak.dto.stack.StackTestDtoBase;
import com.sequenceiq.it.cloudbreak.exception.TestFailException;
import com.sequenceiq.it.cloudbreak.log.Log;
import com.sequenceiq.sdx.api.model.SdxClusterShape;

public abstract class AbstractCloudProvider implements CloudProvider {
    private static final String DEFAULT_SUBNET_CIDR = "10.0.0.0/16";

    private static final String DEFAULT_ACCESS_CIDR = "0.0.0.0/0";

    private static final String DUMMY_SSH_KEY = "ssh-rsa "
            + "AAAAB3NzaC1yc2EAAAADAQABAAABAQC0Rfl2G2vDs6yc19RxCqReunFgpYj+ucyLobpTCBtfDwzIbJot2Fmife6M42mBtiTmAK6x8kc"
            + "UEeab6CB4MUzsqF7vGTFUjwWirG/XU5pYXFUBhi8xzey+KS9KVrQ+UuKJh/AN9iSQeMV+rgT1yF5+etVH+bK1/37QCKp3+mCqjFzPyQOrvkGZv4sYyRwX7BKBLleQmIVWpofpj"
            + "T7BfcCxH877RzC5YMIi65aBc82Dl6tH6OEiP7mzByU52yvH6JFuwZ/9fWj1vXCWJzxx2w0F1OU8Zwg8gNNzL+SVb9+xfBE7xBHMpYFg72hBWPh862Ce36F4NZd3MpWMSjMmpDPh"
            + " centos";

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
        sdxRepair.withHostGroupNames(List.of(HostGroupType.MASTER.getName(), HostGroupType.IDBROKER.getName()));
        return sdxRepair;
    }

    @Override
    public EnvironmentAuthenticationTestDto environmentAuthentication(EnvironmentAuthenticationTestDto environmentAuthenticationEntity) {
        return environmentAuthenticationEntity.withPublicKey(
                StringUtils.isNotEmpty(commonCloudProperties.getSshPublicKey())
                        ? commonCloudProperties.getSshPublicKey()
                        : DUMMY_SSH_KEY
        );
    }

    @Override
    public EnvironmentSecurityAccessTestDto environmentSecurityAccess(EnvironmentSecurityAccessTestDto environmentSecurityAccessTestDto) {
        return environmentSecurityAccessTestDto;
    }

    @Override
    public String getFreeIpaImageCatalogUrl() {
        return null;
    }

    protected abstract ClusterTestDto withCluster(ClusterTestDto cluster);

    protected abstract DistroXClusterTestDto withCluster(DistroXClusterTestDto cluster);

    public String getLatestBaseImage(ImageCatalogTestDto imageCatalogTestDto, CloudbreakClient cloudbreakClient, String platform) {
        try {
            List<BaseImageV4Response> images = cloudbreakClient
                    .getDefaultClient()
                    .imageCatalogV4Endpoint()
                    .getImagesByName(cloudbreakClient.getWorkspaceId(), imageCatalogTestDto.getRequest().getName(), null,
                            platform).getBaseImages();

            if (images.size() == 0) {
                throw new IllegalStateException("Images are empty, there is not any base image on provider " + platform);
            }
            BaseImageV4Response baseImage = images.get(images.size() - 1);
            Log.log(LOGGER, format(" Image Catalog Name: %s ", imageCatalogTestDto.getRequest().getName()));
            Log.log(LOGGER, format(" Image Catalog URL: %s ", imageCatalogTestDto.getRequest().getUrl()));
            Log.log(LOGGER, format(" Selected Base Image Date: %s | ID: %s | Description: %s ", baseImage.getDate(),
                    baseImage.getUuid(), baseImage.getDescription()));

            return baseImage.getUuid();
        } catch (Exception e) {
            LOGGER.error("Cannot fetch base images of {} image catalog, because of {}", imageCatalogTestDto.getRequest().getName(), e);
            throw new TestFailException(" Cannot fetch base images of " + imageCatalogTestDto.getRequest().getName() + " image catalog", e);
        }
    }
}
