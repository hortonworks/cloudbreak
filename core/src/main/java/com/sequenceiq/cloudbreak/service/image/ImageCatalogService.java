package com.sequenceiq.cloudbreak.service.image;

import static com.sequenceiq.cloudbreak.controller.exception.NotFoundException.notFound;
import static com.sequenceiq.cloudbreak.service.image.StatedImage.statedImage;
import static com.sequenceiq.cloudbreak.service.image.StatedImages.statedImages;
import static com.sequenceiq.cloudbreak.util.NameUtil.generateArchiveName;
import static java.util.Collections.emptyList;
import static java.util.Collections.emptySet;

import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.annotation.Nonnull;
import javax.inject.Inject;

import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import com.google.common.collect.ImmutableSet;
import com.sequenceiq.cloudbreak.authorization.WorkspaceResource;
import com.sequenceiq.cloudbreak.cloud.VersionComparator;
import com.sequenceiq.cloudbreak.cloud.model.catalog.CloudbreakImageCatalogV2;
import com.sequenceiq.cloudbreak.cloud.model.catalog.CloudbreakVersion;
import com.sequenceiq.cloudbreak.cloud.model.catalog.Image;
import com.sequenceiq.cloudbreak.cloud.model.catalog.Images;
import com.sequenceiq.cloudbreak.controller.exception.BadRequestException;
import com.sequenceiq.cloudbreak.controller.exception.NotFoundException;
import com.sequenceiq.cloudbreak.core.CloudbreakImageCatalogException;
import com.sequenceiq.cloudbreak.core.CloudbreakImageNotFoundException;
import com.sequenceiq.cloudbreak.domain.ImageCatalog;
import com.sequenceiq.cloudbreak.domain.UserProfile;
import com.sequenceiq.cloudbreak.domain.workspace.User;
import com.sequenceiq.cloudbreak.domain.workspace.Workspace;
import com.sequenceiq.cloudbreak.repository.ImageCatalogRepository;
import com.sequenceiq.cloudbreak.repository.workspace.WorkspaceResourceRepository;
import com.sequenceiq.cloudbreak.service.AbstractWorkspaceAwareResourceService;
import com.sequenceiq.cloudbreak.service.account.PreferencesService;
import com.sequenceiq.cloudbreak.service.user.UserProfileHandler;
import com.sequenceiq.cloudbreak.service.user.UserProfileService;

@Component
public class ImageCatalogService extends AbstractWorkspaceAwareResourceService<ImageCatalog> {

    public static final String UNDEFINED = "";

    public static final String CLOUDBREAK_DEFAULT_CATALOG_NAME = "cloudbreak-default";

    private static final Logger LOGGER = LoggerFactory.getLogger(ImageCatalogService.class);

    @Value("${info.app.version:}")
    private String cbVersion;

    @Value("${cb.image.catalog.url}")
    private String defaultCatalogUrl;

    @Inject
    private ImageCatalogProvider imageCatalogProvider;

    @Inject
    private ImageCatalogRepository imageCatalogRepository;

    @Inject
    private ImageCatalogVersionFilter versionFilter;

    @Inject
    private UserProfileService userProfileService;

    @Inject
    private UserProfileHandler userProfileHandler;

    @Inject
    private PreferencesService preferencesService;

    @Inject
    private StackImageFilterService stackImageFilterService;

    @Override
    public Set<ImageCatalog> findAllByWorkspaceId(Long workspaceId) {
        Set<ImageCatalog> imageCatalogs = imageCatalogRepository.findAllByWorkspaceIdAndArchived(workspaceId, false);
        imageCatalogs.add(getCloudbreakDefaultImageCatalog());
        return imageCatalogs;
    }

    @Override
    public Set<ImageCatalog> findAllByWorkspace(Workspace workspace) {
        Set<ImageCatalog> imageCatalogs = repository().findAllByWorkspace(workspace);
        imageCatalogs.add(getCloudbreakDefaultImageCatalog());
        return imageCatalogs;
    }

    public Images getImagesByCatalogName(Long workspaceId, String catalogName, String stackName, String platform) throws CloudbreakImageCatalogException {
        if (StringUtils.isNotEmpty(platform) && StringUtils.isNotEmpty(stackName)) {
            throw new BadRequestException("Platform or stackName cannot be filled in the same request.");
        }
        if (StringUtils.isNotEmpty(platform)) {
            return getImages(workspaceId, catalogName, platform).getImages();
        } else if (StringUtils.isNotEmpty(stackName)) {
            return stackImageFilterService.getApplicableImages(workspaceId, catalogName, stackName);
        } else {
            throw new BadRequestException("Either platform or stackName should be filled in request.");
        }
    }

    public Images getImagesFromDefault(Long workspaceId, String stackName, String platform, Set<String> operatingSystems)
            throws CloudbreakImageCatalogException {
        if (StringUtils.isNotEmpty(platform) && StringUtils.isNotEmpty(stackName)) {
            throw new BadRequestException("Platform or stackName cannot be filled in the same request.");
        }
        if (StringUtils.isNotEmpty(platform)) {
            User user = getLoggedInUser();
            return getStatedImagesFilteredByOperatingSystems(platform, operatingSystems, user).getImages();
        } else if (StringUtils.isNotEmpty(stackName)) {
            return stackImageFilterService.getApplicableImages(workspaceId, stackName);
        } else {
            throw new BadRequestException("Either platform or stackName should be filled in request.");
        }
    }

    public StatedImages getStatedImagesFilteredByOperatingSystems(String provider, Set<String> operatingSystems, User user)
            throws CloudbreakImageCatalogException {
        StatedImages images = getImages(new ImageFilter(getDefaultImageCatalog(user), Collections.singleton(provider), cbVersion));
        if (!CollectionUtils.isEmpty(operatingSystems)) {
            Images rawImages = images.getImages();
            List<Image> baseImages = filterImagesByOperatingSystems(rawImages.getBaseImages(), operatingSystems);
            List<Image> hdpImages = filterImagesByOperatingSystems(rawImages.getHdpImages(), operatingSystems);
            List<Image> hdfImages = filterImagesByOperatingSystems(rawImages.getHdfImages(), operatingSystems);
            List<Image> cdhImages = filterImagesByOperatingSystems(rawImages.getCdhImages(), operatingSystems);
            images = statedImages(new Images(baseImages, hdpImages, hdfImages, cdhImages, rawImages.getSuppertedVersions()),
                    images.getImageCatalogUrl(), images.getImageCatalogName());
        }
        return images;
    }

    public StatedImage getLatestBaseImageDefaultPreferred(String platform, Set<String> operatingSystems, User user)
            throws CloudbreakImageCatalogException, CloudbreakImageNotFoundException {
        StatedImages statedImages = getStatedImagesFilteredByOperatingSystems(platform, operatingSystems, user);
        Optional<Image> defaultBaseImage = getLatestBaseImageDefaultPreferred(statedImages);
        if (defaultBaseImage.isPresent()) {
            return statedImage(defaultBaseImage.get(), statedImages.getImageCatalogUrl(), statedImages.getImageCatalogName());
        } else {
            throw new CloudbreakImageNotFoundException(imageNotFoundErrorMessage(platform));
        }
    }

    private Optional<Image> getLatestBaseImageDefaultPreferred(StatedImages statedImages) {
        List<Image> baseImages = statedImages.getImages().getBaseImages();
        return getLatestImageDefaultPreferred(baseImages);
    }

    public StatedImage getPrewarmImageDefaultPreferred(String platform, String clusterType, String clusterVersion, Set<String> operatingSystems, User user)
            throws CloudbreakImageNotFoundException, CloudbreakImageCatalogException {
        StatedImages statedImages = getStatedImagesFilteredByOperatingSystems(platform, operatingSystems, user);
        List<Image> images = getImagesForClusterType(statedImages, clusterType);
        Optional<Image> selectedImage = Optional.empty();
        if (!CollectionUtils.isEmpty(images)) {
            List<Image> matchingVersionImages = images.stream().filter(img -> {
                String[] repoIdParts = img.getStackDetails().getRepo().getStack().get("repoid").split("-");
                return repoIdParts.length > 1 && repoIdParts[1].equals(clusterVersion);
            }).collect(Collectors.toList());
            selectedImage = getLatestImageDefaultPreferred(matchingVersionImages);
        }
        if (selectedImage.isEmpty()) {
            selectedImage = getLatestBaseImageDefaultPreferred(statedImages);
        }
        if (selectedImage.isEmpty()) {
            throw new CloudbreakImageNotFoundException(imageNotFoundErrorMessage(platform));
        }
        return statedImage(selectedImage.get(), statedImages.getImageCatalogUrl(), statedImages.getImageCatalogName());
    }

    private String imageNotFoundErrorMessage(String platform) {
        return String.format("Could not find any image for platform '%s' and Cloudbreak version '%s'.", platform, cbVersion);
    }

    public StatedImages getImages(Long workspaceId, String name, String provider) throws CloudbreakImageCatalogException {
        return getImages(workspaceId, name, ImmutableSet.of(provider));
    }

    public StatedImages getImages(Long workspaceId, String name, Set<String> providers) throws CloudbreakImageCatalogException {
        try {
            ImageCatalog imageCatalog = get(workspaceId, name);
            return getImages(new ImageFilter(imageCatalog, providers, cbVersion));
        } catch (AccessDeniedException | NotFoundException ignore) {
            throw new CloudbreakImageCatalogException(String.format("The %s catalog does not exist or does not belongs to your account.", name));
        }
    }

    public StatedImage getImage(String imageId) throws CloudbreakImageNotFoundException, CloudbreakImageCatalogException {
        return getImage(defaultCatalogUrl, CLOUDBREAK_DEFAULT_CATALOG_NAME, imageId);
    }

    public StatedImage getImage(String catalogUrl, String catalogName, String imageId) throws CloudbreakImageNotFoundException,
            CloudbreakImageCatalogException {
        Images images = imageCatalogProvider.getImageCatalogV2(catalogUrl).getImages();
        Optional<? extends Image> image = getImage(imageId, images);
        if (image.isEmpty()) {
            images = imageCatalogProvider.getImageCatalogV2(catalogUrl, true).getImages();
            image = getImage(imageId, images);
        }
        if (image.isEmpty()) {
            throw new CloudbreakImageNotFoundException(String.format("Could not find any image with id: '%s'.", imageId));
        }
        return statedImage(image.get(), catalogUrl, catalogName);
    }

    public StatedImage getImageByCatalogName(Long workspaceId, String imageId, String catalogName) throws
            CloudbreakImageNotFoundException, CloudbreakImageCatalogException {
        StatedImage image;
        if (StringUtils.isEmpty(catalogName)) {
            image = getImage(imageId);
        } else {
            ImageCatalog imageCatalog = get(workspaceId, catalogName);
            image = getImage(imageCatalog.getImageCatalogUrl(), imageCatalog.getName(), imageId);
        }
        return image;
    }

    public ImageCatalog delete(Long workspaceId, String name) {
        if (isEnvDefault(name)) {
            throw new BadRequestException(String.format("%s cannot be deleted because it is an environment default image catalog.", name));
        }

        return deleteNonDefault(workspaceId, name);
    }

    public Set<ImageCatalog> deleteMultiple(Long workspaceId, Set<String> names) {
        Set<String> envDefaults = names.stream()
            .filter(this::isEnvDefault)
            .collect(Collectors.toSet());
        if (!envDefaults.isEmpty()) {
            throw new BadRequestException(String.format("The following image catalogs cannot be deleted because they are environment defaults: %s", names));
        }

        return names.stream()
            .map(name -> deleteNonDefault(workspaceId, name))
            .collect(Collectors.toSet());
    }

    private ImageCatalog deleteNonDefault(Long workspaceId, String name) {
        User user = getLoggedInUser();
        ImageCatalog imageCatalog = get(workspaceId, name);
        imageCatalog.setArchived(true);
        setImageCatalogAsDefault(null, user);
        imageCatalog.setName(generateArchiveName(name));
        imageCatalogRepository.save(imageCatalog);
        userProfileHandler.destroyProfileImageCatalogPreparation(imageCatalog);
        LOGGER.debug("Image catalog has been archived: {}", imageCatalog);
        return imageCatalog;
    }

    public ImageCatalog get(Long workspaceId, String name) {
        return isEnvDefault(name) ? getCloudbreakDefaultImageCatalog() : getByNameForWorkspaceId(name, workspaceId);
    }

    public ImageCatalog setAsDefault(Long workspaceId, String name) {
        User user = getLoggedInUser();
        removeDefaultFlag(user);
        if (!isEnvDefault(name)) {
            ImageCatalog imageCatalog = get(workspaceId, name);
            checkImageCatalog(imageCatalog, name);
            setImageCatalogAsDefault(imageCatalog, user);
            return imageCatalog;
        }
        return getCloudbreakDefaultImageCatalog();
    }

    public boolean isEnvDefault(String name) {
        return CLOUDBREAK_DEFAULT_CATALOG_NAME.equals(name);
    }

    private void setImageCatalogAsDefault(ImageCatalog imageCatalog, User user) {
        UserProfile userProfile = getUserProfile(user);
        userProfile.setImageCatalog(imageCatalog);
        userProfileService.save(userProfile);
    }

    public ImageCatalog update(Long workspaceId, ImageCatalog source) {
        User user = getLoggedInUser();
        ImageCatalog imageCatalog = findImageCatalog(workspaceId, source.getName());
        checkImageCatalog(imageCatalog, source.getId());
        imageCatalog.setName(source.getName());
        imageCatalog.setImageCatalogUrl(source.getImageCatalogUrl());
        return create(imageCatalog, workspaceId, user);
    }

    private ImageCatalog findImageCatalog(Long workspaceId, String name) {
        return Optional.ofNullable(get(workspaceId, name)).orElseThrow(notFound("Image catalog", name));
    }

    private void checkImageCatalog(ImageCatalog imageCatalog, Object filter) {
        if (imageCatalog == null) {
            throw new NotFoundException(String.format("Resource not found with filter [%s]", filter));
        }
    }

    public ImageCatalog getCloudbreakDefaultImageCatalog() {
        ImageCatalog imageCatalog = new ImageCatalog();
        imageCatalog.setName(CLOUDBREAK_DEFAULT_CATALOG_NAME);
        imageCatalog.setImageCatalogUrl(defaultCatalogUrl);
        return imageCatalog;
    }

    private Images emptyImages() {
        return new Images(emptyList(), emptyList(), emptyList(), emptyList(), emptySet());
    }

    private Optional<? extends Image> getImage(String imageId, Images images) {
        Optional<? extends Image> image = findFirstWithImageId(imageId, images.getBaseImages());
        if (image.isEmpty()) {
            image = findFirstWithImageId(imageId, images.getHdpImages());
        }
        if (image.isEmpty()) {
            image = findFirstWithImageId(imageId, images.getHdfImages());
        }
        if (image.isEmpty()) {
            image = findFirstWithImageId(imageId, images.getCdhImages());
        }
        return image;
    }

    public StatedImages getImages(ImageFilter imageFilter) throws CloudbreakImageCatalogException {
        LOGGER.info("Determine images for imageCatalogUrl: '{}', platforms: '{}' and Cloudbreak version: '{}'.",
                imageFilter.getImageCatalog().getImageCatalogUrl(), imageFilter.getPlatforms(), imageFilter.getCbVersion());
        StatedImages images;
        validateRequestPlatforms(imageFilter.getPlatforms());
        CloudbreakImageCatalogV2 imageCatalogV2 = imageCatalogProvider.getImageCatalogV2(imageFilter.getImageCatalog().getImageCatalogUrl());
        Set<String> suppertedVersions;
        if (imageCatalogV2 != null) {
            Set<String> vMImageUUIDs = new HashSet<>();
            Set<String> defaultVMImageUUIDs = new HashSet<>();
            List<CloudbreakVersion> cloudbreakVersions = imageCatalogV2.getVersions().getCloudbreakVersions();
            String cbv = versionFilter.isVersionUnspecified(imageFilter.getCbVersion())
                    ? versionFilter.latestCloudbreakVersion(cloudbreakVersions) : imageFilter.getCbVersion();
            List<CloudbreakVersion> exactMatchedImgs = cloudbreakVersions.stream()
                    .filter(cloudbreakVersion -> cloudbreakVersion.getVersions().contains(cbv)).collect(Collectors.toList());

            if (!exactMatchedImgs.isEmpty()) {
                for (CloudbreakVersion exactMatchedImg : exactMatchedImgs) {
                    vMImageUUIDs.addAll(exactMatchedImg.getImageIds());
                    defaultVMImageUUIDs.addAll(exactMatchedImg.getDefaults());
                }
                suppertedVersions = Collections.singleton(cbv);
            } else {
                LOGGER.debug("No image found with exact match for version {} Trying prefix matching", cbv);
                PrefixMatchImages prefixMatchImages = prefixMatchForCBVersion(imageFilter.getCbVersion(), cloudbreakVersions);
                vMImageUUIDs.addAll(prefixMatchImages.vMImageUUIDs);
                defaultVMImageUUIDs.addAll(prefixMatchImages.defaultVMImageUUIDs);
                suppertedVersions = prefixMatchImages.supportedVersions;
            }

            List<Image> baseImages = filterImagesByPlatforms(imageFilter.getPlatforms(), imageCatalogV2.getImages().getBaseImages(), vMImageUUIDs);
            List<Image> hdpImages = filterImagesByPlatforms(imageFilter.getPlatforms(), imageCatalogV2.getImages().getHdpImages(), vMImageUUIDs);
            List<Image> hdfImages = filterImagesByPlatforms(imageFilter.getPlatforms(), imageCatalogV2.getImages().getHdfImages(), vMImageUUIDs);
            List<Image> cdhImages = filterImagesByPlatforms(imageFilter.getPlatforms(), imageCatalogV2.getImages().getCdhImages(), vMImageUUIDs);

            Stream.of(baseImages.stream(), hdpImages.stream(), hdfImages.stream(), cdhImages.stream())
                    .reduce(Stream::concat)
                    .orElseGet(Stream::empty)
                    .forEach(img -> img.setDefaultImage(defaultVMImageUUIDs.contains(img.getUuid())));

            images = statedImages(new Images(baseImages, hdpImages, hdfImages, cdhImages, suppertedVersions),
                    imageFilter.getImageCatalog().getImageCatalogUrl(),
                    imageFilter.getImageCatalog().getName());
        } else {
            images = statedImages(emptyImages(),
                    imageFilter.getImageCatalog().getImageCatalogUrl(),
                    imageFilter.getImageCatalog().getName());
        }
        return images;
    }

    private void validateRequestPlatforms(Set<String> platforms) throws CloudbreakImageCatalogException {
        Set<String> collect = platforms.stream()
                .filter(requestedPlatform -> preferencesService.enabledPlatforms().stream()
                        .filter(enabledPlatform -> enabledPlatform.equalsIgnoreCase(requestedPlatform))
                        .collect(Collectors.toSet()).isEmpty())
                .collect(Collectors.toSet());
        if (!collect.isEmpty()) {
            throw new CloudbreakImageCatalogException(String.format("Platform(s) %s are not supported by the current catalog",
                    org.apache.commons.lang3.StringUtils.join(collect, ",")));
        }
    }

    public Images propagateImagesIfRequested(Long workspaceId, String name, Boolean withImages) {
        if (BooleanUtils.isTrue(withImages)) {
            Set<String> platforms = preferencesService.enabledPlatforms();
            try {
                return getImages(workspaceId, name, platforms).getImages();
            } catch (CloudbreakImageCatalogException e) {
                LOGGER.info("No images was found: ", e);
            }
        }
        return null;
    }

    private Optional<? extends Image> findFirstWithImageId(String imageId, Collection<? extends Image> images) {
        return images.stream()
                .filter(img -> img.getUuid().equals(imageId))
                .findFirst();
    }

    private List<Image> filterImagesByPlatforms(Collection<String> platforms, Collection<Image> images, Collection<String> vMImageUUIDs) {
        return images.stream()
                .filter(isPlatformMatching(platforms, vMImageUUIDs))
                .collect(Collectors.toList());
    }

    private static Predicate<Image> isPlatformMatching(Collection<String> platforms, Collection<String> vMImageUUIDs) {
        return img -> vMImageUUIDs.contains(img.getUuid())
                && img.getImageSetsByProvider().keySet().stream().anyMatch(p -> platforms.stream().anyMatch(platform -> platform.equalsIgnoreCase(p)));
    }

    private List<Image> filterImagesByOperatingSystems(List<Image> images, Set<String> operatingSystems) {
        Map<Boolean, List<Image>> partitionedImages = images.stream().collect(Collectors.partitioningBy(isMatchingOs(operatingSystems)));
        if (!partitionedImages.get(false).isEmpty()) {
            LOGGER.debug("Used filter OS: | {} | Images filtered: {}", operatingSystems,
                    partitionedImages.get(false).stream().map(Image::shortOsDescriptionFormat).collect(Collectors.joining(", ")));
        }
        return partitionedImages.get(true);
    }

    private PrefixMatchImages prefixMatchForCBVersion(String cbVersion, Collection<CloudbreakVersion> cloudbreakVersions) {
        Set<String> supportedVersions = new HashSet<>();
        Set<String> vMImageUUIDs = new HashSet<>();
        Set<String> defaultVMImageUUIDs = new HashSet<>();
        String unReleasedVersion = versionFilter.extractUnreleasedVersion(cbVersion);
        boolean versionIsReleased = unReleasedVersion.equals(cbVersion);

        if (!versionIsReleased) {
            Set<CloudbreakVersion> unReleasedCbVersions = versionFilter.filterUnreleasedVersions(cloudbreakVersions, unReleasedVersion);
            supportedVersions = getSupportedVersions(vMImageUUIDs, defaultVMImageUUIDs, unReleasedCbVersions);
        }

        if (versionIsReleased || vMImageUUIDs.isEmpty()) {
            String releasedVersion = versionFilter.extractReleasedVersion(cbVersion);
            Set<CloudbreakVersion> releasedCbVersions = versionFilter.filterReleasedVersions(cloudbreakVersions, releasedVersion);

            Integer accumulatedImageCount = accumulateImageCount(releasedCbVersions);
            if (releasedCbVersions.isEmpty() || accumulatedImageCount == 0) {
                releasedCbVersions = previousCbVersion(releasedVersion, cloudbreakVersions);
            }
            supportedVersions = getSupportedVersions(vMImageUUIDs, defaultVMImageUUIDs, releasedCbVersions);
        }
        return new PrefixMatchImages(vMImageUUIDs, defaultVMImageUUIDs, supportedVersions);
    }

    private Set<String> getSupportedVersions(Set<String> vMImageUUIDs, Set<String> defaultVMImageUUIDs, Set<CloudbreakVersion> unReleasedCbVersions) {
        for (CloudbreakVersion unReleasedCbVersion : unReleasedCbVersions) {
            vMImageUUIDs.addAll(unReleasedCbVersion.getImageIds());
            defaultVMImageUUIDs.addAll(unReleasedCbVersion.getDefaults());
        }
        return unReleasedCbVersions.stream().map(CloudbreakVersion::getVersions).flatMap(List::stream).collect(Collectors.toSet());
    }

    private Set<CloudbreakVersion> previousCbVersion(String releasedVersion, Collection<CloudbreakVersion> cloudbreakVersions) {
        List<String> versions = cloudbreakVersions.stream()
                .map(CloudbreakVersion::getVersions)
                .flatMap(List::stream)
                .distinct()
                .collect(Collectors.toList());

        versions = versions.stream().sorted((o1, o2) -> new VersionComparator().compare(() -> o2, () -> o1)).collect(Collectors.toList());

        Predicate<String> ealierVersionPredicate = ver -> new VersionComparator().compare(() -> ver, () -> releasedVersion) < 0;
        Predicate<String> releaseVersionPredicate = ver -> versionFilter.extractExtendedUnreleasedVersion(ver).equals(ver);
        Predicate<String> versionHasImagesPredicate = ver -> accumulateImageCount(cloudbreakVersions) > 0;
        Optional<String> applicableVersion = versions.stream()
                .filter(ealierVersionPredicate)
                .filter(releaseVersionPredicate)
                .filter(versionHasImagesPredicate)
                .findAny();

        return applicableVersion
                .map(ver -> cloudbreakVersions.stream().filter(cbVer -> cbVer.getVersions().contains(ver)).collect(Collectors.toSet()))
                .orElse(emptySet());
    }

    private Integer accumulateImageCount(Collection<CloudbreakVersion> cloudbreakVersions) {
        return cloudbreakVersions.stream()
                .map(CloudbreakVersion::getImageIds)
                .map(List::size)
                .reduce((x, y) -> x + y)
                .orElse(0);
    }

    private void removeDefaultFlag(User user) {
        ImageCatalog imageCatalog = getDefaultImageCatalog(user);
        if (imageCatalog.getName() != null && !CLOUDBREAK_DEFAULT_CATALOG_NAME.equalsIgnoreCase(imageCatalog.getName())) {
            setImageCatalogAsDefault(null, user);
            imageCatalogRepository.save(imageCatalog);
        }
    }

    @Nonnull
    private ImageCatalog getDefaultImageCatalog(User user) {
        ImageCatalog imageCatalog = getUserProfile(user).getImageCatalog();
        if (imageCatalog == null) {
            imageCatalog = new ImageCatalog();
            imageCatalog.setImageCatalogUrl(defaultCatalogUrl);
            imageCatalog.setName(CLOUDBREAK_DEFAULT_CATALOG_NAME);
        }
        return imageCatalog;
    }

    public String getDefaultImageCatalogName(User user) {
        return getDefaultImageCatalog(user).getName();
    }

    public String getImageDefaultCatalogUrl(User user) {
        return getDefaultImageCatalog(user).getImageCatalogUrl();
    }

    private UserProfile getUserProfile(User user) {
        return userProfileService.getOrCreate(user);
    }

    private static Predicate<Image> isMatchingOs(Set<String> operatingSystems) {
        return img -> operatingSystems.stream().anyMatch(os -> img.getOsType().equalsIgnoreCase(os));
    }

    private Optional<Image> getLatestImageDefaultPreferred(List<Image> images) {
        List<Image> defaultImages = images.stream().filter(Image::isDefaultImage).collect(Collectors.toList());
        return defaultImages.isEmpty() ? images.stream().max(Comparator.comparing(Image::getDate))
                : defaultImages.stream().max(Comparator.comparing(Image::getDate));
    }

    private List<Image> getImagesForClusterType(StatedImages statedImages, String clusterType) {
        switch (clusterType) {
            case "HDP":
                return statedImages.getImages().getHdpImages();
            case "HDF":
                return statedImages.getImages().getHdfImages();
            case "CDH":
                return statedImages.getImages().getCdhImages();
            default:
                return emptyList();
        }
    }

    @Override
    public WorkspaceResourceRepository<ImageCatalog, Long> repository() {
        return imageCatalogRepository;
    }

    @Override
    public WorkspaceResource resource() {
        return WorkspaceResource.IMAGECATALOG;
    }

    @Override
    protected void prepareDeletion(ImageCatalog resource) {

    }

    @Override
    protected void prepareCreation(ImageCatalog resource) {
        if (isEnvDefault(resource.getName())) {
            throw new BadRequestException(String
                    .format("%s cannot be created because it is an environment default image catalog.", resource.getName()));
        }
    }

    private static class PrefixMatchImages {
        private final Set<String> vMImageUUIDs;

        private final Set<String> defaultVMImageUUIDs;

        private final Set<String> supportedVersions;

        private PrefixMatchImages(Set<String> vMImageUUIDs, Set<String> defaultVMImageUUIDs, Set<String> supportedVersions) {
            this.vMImageUUIDs = vMImageUUIDs;
            this.defaultVMImageUUIDs = defaultVMImageUUIDs;
            this.supportedVersions = supportedVersions;
        }
    }
}
