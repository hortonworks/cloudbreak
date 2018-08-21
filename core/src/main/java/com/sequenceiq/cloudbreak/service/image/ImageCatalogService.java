package com.sequenceiq.cloudbreak.service.image;

import java.util.Optional;
import java.util.Set;

import com.sequenceiq.cloudbreak.cloud.model.catalog.Image;
import com.sequenceiq.cloudbreak.cloud.model.catalog.Images;
import com.sequenceiq.cloudbreak.core.CloudbreakImageCatalogException;
import com.sequenceiq.cloudbreak.core.CloudbreakImageNotFoundException;
import com.sequenceiq.cloudbreak.domain.ImageCatalog;
import com.sequenceiq.cloudbreak.service.organization.OrganizationAwareResourceService;

public interface ImageCatalogService extends OrganizationAwareResourceService<ImageCatalog> {

    StatedImage getImageByCatalogName(Long organizationId, String imageId, String catalogName) throws
            CloudbreakImageNotFoundException, CloudbreakImageCatalogException;

    StatedImage getLatestBaseImageDefaultPreferred(String platform, String os) throws CloudbreakImageCatalogException, CloudbreakImageNotFoundException;

    Optional<Image> getLatestBaseImageDefaultPreferred(StatedImages statedImages, String os);

    StatedImage getPrewarmImageDefaultPreferred(String platform, String clusterType, String clusterVersion, String os)
            throws CloudbreakImageNotFoundException, CloudbreakImageCatalogException;

    Images propagateImagesIfRequested(Long organizationId, String name, boolean withImages);

    StatedImages getImages(Long organizationId, String name, Set<String> providers) throws CloudbreakImageCatalogException;

    StatedImages getImages(Long organizationId, String name, String provider) throws CloudbreakImageCatalogException;

    StatedImages getImagesOsFiltered(String provider, String os) throws CloudbreakImageCatalogException;

    ImageCatalog setAsDefault(Long organizationId, String name);

    boolean isEnvDefault(String name);

    String getDefaultImageCatalogName();

    String getImageDefaultCatalogUrl();

    ImageCatalog getCloudbreakDefaultImageCatalog();

    StatedImages getImages(ImageCatalog imageCatalog, String platform, String cbVersion) throws CloudbreakImageCatalogException;

    StatedImage getImage(String catalogUrl, String catalogName, String imageId) throws CloudbreakImageNotFoundException, CloudbreakImageCatalogException;
}
