package com.sequenceiq.cloudbreak.service.image;

import static com.sequenceiq.cloudbreak.service.image.StatedImage.statedImage;
import static com.sequenceiq.cloudbreak.service.image.StatedImages.statedImages;
import static com.sequenceiq.cloudbreak.util.NameUtil.generateArchiveName;
import static com.sequenceiq.cloudbreak.util.SqlUtil.getProperSqlErrorMessage;
import static java.util.Collections.emptyList;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.sequenceiq.cloudbreak.cloud.model.Versioned;
import com.sequenceiq.cloudbreak.cloud.model.catalog.CloudbreakImageCatalogV2;
import com.sequenceiq.cloudbreak.cloud.model.catalog.CloudbreakVersion;
import com.sequenceiq.cloudbreak.cloud.model.catalog.Image;
import com.sequenceiq.cloudbreak.cloud.model.catalog.Images;
import com.sequenceiq.cloudbreak.common.model.user.IdentityUser;
import com.sequenceiq.cloudbreak.common.type.APIResourceType;
import com.sequenceiq.cloudbreak.controller.AuthenticatedUserService;
import com.sequenceiq.cloudbreak.controller.BadRequestException;
import com.sequenceiq.cloudbreak.controller.NotFoundException;
import com.sequenceiq.cloudbreak.core.CloudbreakImageCatalogException;
import com.sequenceiq.cloudbreak.core.CloudbreakImageNotFoundException;
import com.sequenceiq.cloudbreak.domain.ImageCatalog;
import com.sequenceiq.cloudbreak.domain.UserProfile;
import com.sequenceiq.cloudbreak.repository.ImageCatalogRepository;
import com.sequenceiq.cloudbreak.service.AuthorizationService;
import com.sequenceiq.cloudbreak.service.user.UserProfileService;

@Component
public class ImageCatalogService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ImageCatalogService.class);

    private static final String RELEASED_VERSION_PATTERN = "^\\d+\\.\\d+\\.\\d+";

    private static final String UNRELEASED_VERSION_PATTERN = "^\\d+\\.\\d+\\.\\d+-[d,r][c,e][v]?";

    private static final String UNSPECIFIED_VERSION = "unspecified";

    private static final String CLOUDBREAK_DEFAULT_CATALOG_NAME = "cloudbreak-default";

    @Value("${info.app.version:}")
    private String cbVersion;

    @Value("${cb.image.catalog.url}")
    private String defaultCatalogUrl;

    @Inject
    private ImageCatalogProvider imageCatalogProvider;

    @Inject
    private ImageCatalogRepository imageCatalogRepository;

    @Inject
    private AuthorizationService authorizationService;

    @Inject
    private AuthenticatedUserService authenticatedUserService;

    @Inject
    private UserProfileService userProfileService;

    public StatedImages getImages(String provider) throws CloudbreakImageCatalogException {
        return getImages(getImageDefaultCatalogUrl(), provider, cbVersion);
    }

    public StatedImage getBaseImage(String platform) throws CloudbreakImageNotFoundException, CloudbreakImageCatalogException {
        StatedImages images = getImages(platform);
        List<Image> baseImages = images.getImages().getBaseImages();
        if (baseImages.isEmpty()) {
            String msg = String.format("Could not find any base image for platform '%s' and Cloudbreak version '%s'.", platform, cbVersion);
            throw new CloudbreakImageNotFoundException(msg);
        }
        return statedImage(baseImages.stream().findFirst().get(), images.getImageCatalogUrl());
    }

    public StatedImages getImages(String name, String provider) throws CloudbreakImageCatalogException {
        ImageCatalog imageCatalog = null;
        try {
            imageCatalog = get(name);
        } catch (NotFoundException ignore) {

        }

        if (imageCatalog == null) {
            return statedImages(emptyImages(), null);
        }
        return getImages(imageCatalog.getImageCatalogUrl(), provider, cbVersion);
    }

    public StatedImage getImage(String imageId) throws CloudbreakImageNotFoundException, CloudbreakImageCatalogException {
        return getImage(defaultCatalogUrl, imageId);
    }

    public StatedImage getImage(String catalogUrl, String imageId) throws CloudbreakImageNotFoundException, CloudbreakImageCatalogException {
        Images images = imageCatalogProvider.getImageCatalogV2(catalogUrl).getImages();
        Optional<? extends Image> image = getImage(imageId, images);
        if (!image.isPresent()) {
            images = imageCatalogProvider.getImageCatalogV2(catalogUrl, true).getImages();
            image = getImage(imageId, images);
        }
        if (!image.isPresent()) {
            throw new CloudbreakImageNotFoundException(String.format("Could not find any image with id: '%s'.", imageId));
        }
        return statedImage(image.get(), catalogUrl);
    }

    public StatedImage getImageByCatalogName(String imageId, String catalogName) throws CloudbreakImageNotFoundException, CloudbreakImageCatalogException {
        StatedImage image;
        if (StringUtils.isEmpty(catalogName)) {
            image = getImage(imageId);
        } else {
            ImageCatalog imageCatalog = get(catalogName);
            if (imageCatalog != null) {
                image = getImage(imageCatalog.getImageCatalogUrl(), imageId);
            } else {
                String msg = String.format("The specified image catalog '%s' could not be found.", catalogName);
                LOGGER.error(msg);
                throw new CloudbreakImageNotFoundException(msg);
            }
        }
        return image;
    }

    public ImageCatalog create(ImageCatalog imageCatalog) throws CloudbreakImageCatalogException {
        try {
            if (isEnvDefault(imageCatalog.getImageCatalogName())) {
                throw new BadRequestException(String
                        .format("%s cannot be created because it is an environment default image catalog.", imageCatalog.getImageCatalogName()));
            }
            return imageCatalogRepository.save(imageCatalog);
        } catch (DataIntegrityViolationException ex) {
            String msg = String.format("Error with resource [%s], error: [%s]", APIResourceType.IMAGE_CATALOG, getProperSqlErrorMessage(ex));
            throw new BadRequestException(msg);
        }
    }

    public void delete(String name) {
        if (isEnvDefault(name)) {
            throw new BadRequestException(String.format("%s cannot be deleted because it is an environment default image catalog.", name));
        }
        ImageCatalog imageCatalog = get(name);
        authorizationService.hasWritePermission(imageCatalog);
        imageCatalog.setArchived(true);
        setImageCatalogAsDefault(null);
        imageCatalog.setImageCatalogName(generateArchiveName(name));
        imageCatalogRepository.save(imageCatalog);
        LOGGER.info("Image catalog has been archived: {}", imageCatalog);
    }

    public ImageCatalog get(String name) {
        if (isEnvDefault(name)) {
            return getCloudbreakDefaultImageCatalog();
        } else {
            IdentityUser user = authenticatedUserService.getCbUser();
            return imageCatalogRepository.findByName(name, user.getUserId(), user.getAccount());
        }
    }

    public ImageCatalog setAsDefault(String name) {

        removeDefaultFlag();

        if (!isEnvDefault(name)) {
            ImageCatalog imageCatalog = get(name);
            checkImageCatalog(imageCatalog, name);

            authorizationService.hasWritePermission(imageCatalog);

            setImageCatalogAsDefault(imageCatalog);

            return imageCatalog;
        }
        return getCloudbreakDefaultImageCatalog();
    }

    public boolean isEnvDefault(String name) {
        return CLOUDBREAK_DEFAULT_CATALOG_NAME.equals(name);
    }

    private void setImageCatalogAsDefault(ImageCatalog imageCatalog) {
        UserProfile userProfile = getUserProfile();
        userProfile.setImageCatalog(imageCatalog);
        userProfileService.save(userProfile);
    }

    public ImageCatalog update(ImageCatalog source) throws CloudbreakImageCatalogException {

        ImageCatalog imageCatalog = imageCatalogRepository.findOne(source.getId());
        checkImageCatalog(imageCatalog, source.getId());
        imageCatalog.setImageCatalogName(source.getImageCatalogName());
        imageCatalog.setImageCatalogUrl(source.getImageCatalogUrl());
        return create(imageCatalog);
    }

    private void checkImageCatalog(ImageCatalog imageCatalog, Object filter) {
        if (imageCatalog == null) {
            throw new NotFoundException(String.format("Resource not found with filter [%s]", filter));
        }
    }

    public Iterable<ImageCatalog> getAllPublicInAccount() {
        IdentityUser cbUser = authenticatedUserService.getCbUser();
        List<ImageCatalog> allPublicInAccount = imageCatalogRepository.findAllPublicInAccount(cbUser.getUserId(), cbUser.getAccount());
        allPublicInAccount.add(getCloudbreakDefaultImageCatalog());
        return allPublicInAccount;
    }

    private ImageCatalog getCloudbreakDefaultImageCatalog() {
        ImageCatalog imageCatalog = new ImageCatalog();
        imageCatalog.setImageCatalogName(CLOUDBREAK_DEFAULT_CATALOG_NAME);
        imageCatalog.setImageCatalogUrl(defaultCatalogUrl);
        imageCatalog.setPublicInAccount(true);
        return imageCatalog;
    }

    private Images emptyImages() {
        return new Images(emptyList(), emptyList(), emptyList());
    }

    private Optional<? extends Image> getImage(String imageId, Images images) throws CloudbreakImageNotFoundException,
            CloudbreakImageCatalogException {
        Optional<? extends Image> image = findFirstWithImageId(imageId, images.getBaseImages());
        if (!image.isPresent()) {
            image = findFirstWithImageId(imageId, images.getHdpImages());
        }
        if (!image.isPresent()) {
            image = findFirstWithImageId(imageId, images.getHdfImages());
        }
        return image;
    }

    public StatedImages getImages(String imageCatalogUrl, String platform, String cbVersion) throws CloudbreakImageCatalogException {
        LOGGER.info("Determine images for imageCatalogUrl: '{}', platform: '{}' and Cloudbreak version: '{}'.", platform, cbVersion);
        StatedImages images;
        CloudbreakImageCatalogV2 imageCatalog = imageCatalogProvider.getImageCatalogV2(imageCatalogUrl);
        if (imageCatalog != null) {
            Set<String> vMImageUUIDs = new HashSet<>();
            List<CloudbreakVersion> cloudbreakVersions = imageCatalog.getVersions().getCloudbreakVersions();
            String cbv = UNSPECIFIED_VERSION.equals(cbVersion) ? latestCloudbreakVersion(cloudbreakVersions) : cbVersion;
            List<CloudbreakVersion> exactMatchedImgs = cloudbreakVersions.stream()
                    .filter(cloudbreakVersion -> cloudbreakVersion.getVersions().contains(cbv)).collect(Collectors.toList());

            if (!exactMatchedImgs.isEmpty()) {
                exactMatchedImgs.forEach(cloudbreakVersion -> vMImageUUIDs.addAll(cloudbreakVersion.getImageIds()));
            } else {
                vMImageUUIDs.addAll(prefixMatchForCBVersion(cbVersion, cloudbreakVersions));
            }

            List<Image> baseImages = filterImagesByPlatform(platform, imageCatalog.getImages().getBaseImages(), vMImageUUIDs);
            List<Image> hdpImages = filterImagesByPlatform(platform, imageCatalog.getImages().getHdpImages(), vMImageUUIDs);
            List<Image> hdfImages = filterImagesByPlatform(platform, imageCatalog.getImages().getHdfImages(), vMImageUUIDs);

            images = statedImages(new Images(baseImages, hdpImages, hdfImages), imageCatalogUrl);
        } else {
            images = statedImages(emptyImages(), imageCatalogUrl);
        }
        return images;
    }

    private Optional<? extends Image> findFirstWithImageId(String imageId, Collection<? extends Image> images) {
        return images.stream()
                .filter(img -> img.getUuid().equals(imageId))
                .findFirst();
    }

    private List<Image> filterImagesByPlatform(String platform, List<Image> images, Set<String> vMImageUUIDs) {
        return images.stream()
                .filter(img -> vMImageUUIDs.contains(img.getUuid()))
                .filter(img -> img.getImageSetsByProvider().keySet().stream().anyMatch(p -> p.equalsIgnoreCase(platform)))
                .collect(Collectors.toList());
    }

    private String latestCloudbreakVersion(List<CloudbreakVersion> cloudbreakVersions) {
        SortedMap<Versioned, CloudbreakVersion> sortedCloudbreakVersions = new TreeMap<>(new VersionComparator());
        for (CloudbreakVersion cbv : cloudbreakVersions) {
            cbv.getVersions().forEach(cbvs -> sortedCloudbreakVersions.put(() -> cbvs, cbv));
        }
        return sortedCloudbreakVersions.lastKey().getVersion();
    }

    private Set<String> prefixMatchForCBVersion(String cbVersion, List<CloudbreakVersion> cloudbreakVersions) {
        Set<String> vMImageUUIDs = new HashSet<>();
        String unReleasedVersion = extractCbVersion(UNRELEASED_VERSION_PATTERN, cbVersion);
        boolean versionIsReleased = unReleasedVersion.equals(cbVersion);

        if (!versionIsReleased) {
            Set<CloudbreakVersion> unReleasedCbVersions = cloudbreakVersions.stream()
                    .filter(cloudbreakVersion -> cloudbreakVersion.getVersions().stream().anyMatch(aVersion -> aVersion.startsWith(unReleasedVersion)))
                    .collect(Collectors.toSet());
            unReleasedCbVersions.stream().forEach(cloudbreakVersion -> vMImageUUIDs.addAll(cloudbreakVersion.getImageIds()));
        }

        if (versionIsReleased || vMImageUUIDs.isEmpty()) {
            String releasedVersion = extractCbVersion(RELEASED_VERSION_PATTERN, cbVersion);
            Set<CloudbreakVersion> releasedCbVersions = cloudbreakVersions.stream()
                    .filter(cloudbreakVersion -> cloudbreakVersion.getVersions().contains(releasedVersion)).collect(Collectors.toSet());
            releasedCbVersions.stream().forEach(cloudbreakVersion -> vMImageUUIDs.addAll(cloudbreakVersion.getImageIds()));
        }
        return vMImageUUIDs;
    }

    private String extractCbVersion(String pattern, String cbVersion) {
        Matcher matcher = Pattern.compile(pattern).matcher(cbVersion);
        if (matcher.find()) {
            return matcher.group(0);
        }
        return cbVersion;
    }

    private void removeDefaultFlag() {
        ImageCatalog imageCatalog = getDefaultImageCatalog();
        if (imageCatalog != null) {
            setImageCatalogAsDefault(null);
            imageCatalogRepository.save(imageCatalog);
        }
    }

    private ImageCatalog getDefaultImageCatalog() {
        IdentityUser user = authenticatedUserService.getCbUser();
        return userProfileService.get(user.getAccount(), user.getUserId()).getImageCatalog();
    }

    public String getDefaultImageCatalogName() {
        ImageCatalog imageCatalog = getDefaultImageCatalog();
        return imageCatalog == null ? null : imageCatalog.getImageCatalogName();
    }

    public String getImageDefaultCatalogUrl() {
        ImageCatalog defaultImageCatalog = getDefaultImageCatalog();
        return (defaultImageCatalog == null) ? defaultCatalogUrl : defaultImageCatalog.getImageCatalogUrl();
    }

    private UserProfile getUserProfile() {
        IdentityUser cbUser = authenticatedUserService.getCbUser();
        return userProfileService.get(cbUser.getAccount(), cbUser.getUserId());
    }
}