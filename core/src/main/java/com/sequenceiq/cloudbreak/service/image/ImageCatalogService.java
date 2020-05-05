package com.sequenceiq.cloudbreak.service.image;

import static com.sequenceiq.cloudbreak.exception.NotFoundException.notFound;
import static com.sequenceiq.cloudbreak.service.image.StatedImage.statedImage;
import static com.sequenceiq.cloudbreak.service.image.StatedImages.statedImages;
import static com.sequenceiq.cloudbreak.util.NameUtil.generateArchiveName;
import static java.util.Collections.emptyList;
import static java.util.Collections.emptySet;
import static java.util.stream.Collectors.partitioningBy;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.lang3.StringUtils.isNotEmpty;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
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
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import com.google.common.collect.ImmutableSet;
import com.sequenceiq.authorization.resource.AuthorizationResourceType;
import com.sequenceiq.authorization.service.ResourceBasedCrnProvider;
import com.sequenceiq.cloudbreak.api.endpoint.v4.dto.NameOrCrn;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.altus.Crn;
import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.cloud.VersionComparator;
import com.sequenceiq.cloudbreak.cloud.model.catalog.CloudbreakImageCatalogV3;
import com.sequenceiq.cloudbreak.cloud.model.catalog.CloudbreakVersion;
import com.sequenceiq.cloudbreak.cloud.model.catalog.Image;
import com.sequenceiq.cloudbreak.cloud.model.catalog.Images;
import com.sequenceiq.cloudbreak.cloud.model.component.StackType;
import com.sequenceiq.cloudbreak.core.CloudbreakImageCatalogException;
import com.sequenceiq.cloudbreak.core.CloudbreakImageNotFoundException;
import com.sequenceiq.cloudbreak.domain.ImageCatalog;
import com.sequenceiq.cloudbreak.domain.UserProfile;
import com.sequenceiq.cloudbreak.exception.BadRequestException;
import com.sequenceiq.cloudbreak.exception.NotFoundException;
import com.sequenceiq.cloudbreak.repository.ImageCatalogRepository;
import com.sequenceiq.cloudbreak.service.AbstractWorkspaceAwareResourceService;
import com.sequenceiq.cloudbreak.service.account.PreferencesService;
import com.sequenceiq.cloudbreak.service.user.UserProfileHandler;
import com.sequenceiq.cloudbreak.service.user.UserProfileService;
import com.sequenceiq.cloudbreak.workspace.model.User;
import com.sequenceiq.cloudbreak.workspace.model.Workspace;
import com.sequenceiq.cloudbreak.workspace.repository.workspace.WorkspaceResourceRepository;

@Component
public class ImageCatalogService extends AbstractWorkspaceAwareResourceService<ImageCatalog> implements ResourceBasedCrnProvider {

    public static final String UNDEFINED = "";

    static final String CLOUDBREAK_DEFAULT_CATALOG_NAME = "cloudbreak-default";

    static final String CDP_DEFAULT_CATALOG_NAME = "cdp-default";

    private static final Logger LOGGER = LoggerFactory.getLogger(ImageCatalogService.class);

    @Value("${info.app.version:}")
    private String cbVersion;

    @Value("${cb.image.catalog.url}")
    private String defaultCatalogUrl;

    @Value("${cb.image.catalog.legacy.enabled}")
    private boolean legacyCatalogEnabled;

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

    @Inject
    private EntitlementService entitlementService;

    @Override
    public Set<ImageCatalog> findAllByWorkspaceId(Long workspaceId) {
        Set<ImageCatalog> imageCatalogs = imageCatalogRepository.findAllByWorkspaceIdAndArchived(workspaceId, false);
        imageCatalogs.add(getCloudbreakDefaultImageCatalog());
        if (legacyCatalogEnabled) {
            imageCatalogs.add(getCloudbreakLegacyDefaultImageCatalog());
        }
        return imageCatalogs;
    }

    public ImageCatalog delete(NameOrCrn imageCatalogNameOrCrn, Long workspaceId) {
        ImageCatalog catalog = get(imageCatalogNameOrCrn, workspaceId);
        return delete(workspaceId, catalog.getName());
    }

    @Override
    public Set<ImageCatalog> findAllByWorkspace(Workspace workspace) {
        Set<ImageCatalog> imageCatalogs = repository().findAllByWorkspace(workspace);
        imageCatalogs.add(getCloudbreakDefaultImageCatalog());
        if (legacyCatalogEnabled) {
            imageCatalogs.add(getCloudbreakLegacyDefaultImageCatalog());
        }
        return imageCatalogs;
    }

    public ImageCatalog createForLoggedInUser(ImageCatalog imageCatalog, Long workspaceId, String accountId, String creator) {
        imageCatalog.setResourceCrn(createCRN(accountId));
        imageCatalog.setCreator(creator);
        return super.createForLoggedInUser(imageCatalog, workspaceId);
    }

    public ImageCatalog get(NameOrCrn imageCatalogNameOrCrn, Long workspaceId) {
        return imageCatalogNameOrCrn.hasName()
                ? get(workspaceId, imageCatalogNameOrCrn.getName())
                : findByResourceCrn(imageCatalogNameOrCrn.getCrn());
    }

    public ImageCatalog findByResourceCrn(String resourceCrn) {
        return imageCatalogRepository.findByResourceCrnAndArchivedFalse(resourceCrn).orElseThrow(notFound("ImageCatalog", resourceCrn));
    }

    public Images getImagesByCatalogName(Long workspaceId, String catalogName, String stackName, String platform) throws CloudbreakImageCatalogException {
        if (isNotEmpty(platform) && isNotEmpty(stackName)) {
            throw new BadRequestException("Both platform and existing stack name could not be present in the request.");
        }
        if (isNotEmpty(platform)) {
            return getImages(workspaceId, catalogName, platform).getImages();
        } else if (isNotEmpty(stackName)) {
            return stackImageFilterService.getApplicableImages(workspaceId, catalogName, stackName);
        } else {
            throw new BadRequestException("Either platform or stack name must be present in the request.");
        }
    }

    public Images getImagesFromDefault(Long workspaceId, String stackName, String platform, Set<String> operatingSystems)
            throws CloudbreakImageCatalogException {
        if (isNotEmpty(platform) && isNotEmpty(stackName)) {
            throw new BadRequestException("Platform or stackName cannot be filled in the same request.");
        }
        if (isNotEmpty(platform)) {
            User user = getLoggedInUser();
            ImageFilter imageFilter =
                    new ImageFilter(getDefaultImageCatalog(user), Set.of(platform), null, baseImageEnabled(), operatingSystems, null);
            return getStatedImagesFilteredByOperatingSystems(imageFilter, image -> true).getImages();
        } else if (isNotEmpty(stackName)) {
            return stackImageFilterService.getApplicableImages(workspaceId, stackName);
        } else {
            throw new BadRequestException("Either platform or stackName should be filled in request.");
        }
    }

    public StatedImages getStatedImagesFilteredByOperatingSystems(ImageFilter imageFilter, Predicate<Image> imageFilterPredicate)
            throws CloudbreakImageCatalogException {
        Set<String> platforms = imageFilter.getPlatforms();
        Set<String> operatingSystems = imageFilter.getOperatingSystems();
        ImageCatalog imageCatalog = imageFilter.getImageCatalog();
        boolean baseImageEnabled = imageFilter.isBaseImageEnabled();
        StatedImages images = getImages(new ImageFilter(imageCatalog, platforms, cbVersion, baseImageEnabled, null, null));
        if (!CollectionUtils.isEmpty(operatingSystems)) {
            Images rawImages = images.getImages();
            List<Image> baseImages = filterImagesByOperatingSystemsAndPackageVersion(rawImages.getBaseImages(), operatingSystems, imageFilterPredicate);
            List<Image> hdpImages = filterImagesByOperatingSystemsAndPackageVersion(rawImages.getHdpImages(), operatingSystems, imageFilterPredicate);
            List<Image> hdfImages = filterImagesByOperatingSystemsAndPackageVersion(rawImages.getHdfImages(), operatingSystems, imageFilterPredicate);
            List<Image> cdhImages = filterImagesByOperatingSystemsAndPackageVersion(rawImages.getCdhImages(), operatingSystems, imageFilterPredicate);
            images = statedImages(new Images(baseImages, hdpImages, hdfImages, cdhImages, rawImages.getSuppertedVersions()),
                    images.getImageCatalogUrl(), images.getImageCatalogName());
        }
        return images;
    }

    public StatedImage getLatestBaseImageDefaultPreferred(ImageFilter imageFilter, Predicate<Image> imageFilterPredicate)
            throws CloudbreakImageCatalogException, CloudbreakImageNotFoundException {
        String platform = imageFilter.getPlatforms().stream().findFirst().isPresent() ? imageFilter.getPlatforms().stream().findFirst().get() : "";
        StatedImages statedImages = getStatedImagesFilteredByOperatingSystems(imageFilter, imageFilterPredicate);
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

    public StatedImage getImagePrewarmedDefaultPreferred(ImageFilter imageFilter, Predicate<Image> imageFilterPredicate)
            throws CloudbreakImageNotFoundException, CloudbreakImageCatalogException {
        Set<String> platforms = imageFilter.getPlatforms();
        String platform = platforms.stream().findFirst().isPresent() ? platforms.stream().findFirst().get() : "";
        StatedImages statedImages = getStatedImagesFilteredByOperatingSystems(imageFilter, imageFilterPredicate);
        Optional<Image> selectedImage = getLatestPrewarmedImage(imageFilter.getClusterVersion(), statedImages);
        if (selectedImage.isEmpty()) {
            selectedImage = getLatestBaseImageDefaultPreferred(statedImages);
        }
        if (selectedImage.isEmpty()) {
            throw new CloudbreakImageNotFoundException(imageNotFoundErrorMessage(platform));
        }
        return statedImage(selectedImage.get(), statedImages.getImageCatalogUrl(), statedImages.getImageCatalogName());
    }

    private Optional<Image> getLatestPrewarmedImage(String clusterVersion, StatedImages statedImages) {
        List<Image> images = getImagesForClusterType(statedImages, StackType.CDH.name());
        Optional<Image> selectedImage = Optional.empty();
        if (!CollectionUtils.isEmpty(images)) {
            List<Image> matchingVersionImages = images.stream().filter(img -> {
                String[] repoIdParts = img.getStackDetails().getRepo().getStack().get("repoid").split("-");
                return repoIdParts.length > 1 && repoIdParts[1].equals(clusterVersion);
            }).collect(toList());
            selectedImage = getLatestImageDefaultPreferred(matchingVersionImages);
        }
        return selectedImage;
    }

    public StatedImage getLatestPrewarmedImageDefaultPreferred(ImageFilter imageFilter, Predicate<Image> imageFilterPredicate)
            throws CloudbreakImageNotFoundException, CloudbreakImageCatalogException {
        StatedImages statedImages = getStatedImagesFilteredByOperatingSystems(imageFilter, imageFilterPredicate);
        Optional<Image> selectedImage = getLatestPrewarmedImage(imageFilter.getClusterVersion(), statedImages);
        Set<String> platforms = imageFilter.getPlatforms();
        String platform = platforms.stream().findFirst().isPresent() ? platforms.stream().findFirst().get() : "";
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
            return getImages(new ImageFilter(imageCatalog, providers, cbVersion, baseImageEnabled(), null, null));
        } catch (NotFoundException ignore) {
            throw new CloudbreakImageCatalogException(String.format("The %s catalog does not exist or does not belongs to your account.", name));
        }
    }

    public StatedImage getImage(String imageId) throws CloudbreakImageNotFoundException, CloudbreakImageCatalogException {
        return getImage(defaultCatalogUrl, CDP_DEFAULT_CATALOG_NAME, imageId);
    }

    public StatedImage getImage(String catalogUrl, String catalogName, String imageId) throws CloudbreakImageNotFoundException,
            CloudbreakImageCatalogException {
        Images images = imageCatalogProvider.getImageCatalogV3(catalogUrl).getImages();
        Optional<? extends Image> image = getImage(imageId, images);
        if (image.isEmpty()) {
            images = imageCatalogProvider.getImageCatalogV3(catalogUrl, true).getImages();
            image = getImage(imageId, images);
        }
        if (image.isEmpty()) {
            throw new CloudbreakImageNotFoundException(String.format("Could not find any image with id: '%s' in catalog:. '%s'", imageId, catalogName));
        }
        return statedImage(image.get(), catalogUrl, catalogName);
    }

    public StatedImage getImageByCatalogName(Long workspaceId, String imageId, String catalogName) throws
            CloudbreakImageNotFoundException, CloudbreakImageCatalogException {
        StatedImage image;
        if (StringUtils.isEmpty(catalogName)) {
            image = getImage(imageId);
        } else {
            try {
                ImageCatalog imageCatalog = get(workspaceId, catalogName);
                image = getImage(imageCatalog.getImageCatalogUrl(), imageCatalog.getName(), imageId);
            } catch (NotFoundException e) {
                throw new CloudbreakImageCatalogException(e.getMessage());
            }
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
        return isEnvDefault(name) ? getDefaultCatalog(name) : getByNameForWorkspaceId(name, workspaceId);
    }

    public boolean isEnvDefault(String name) {
        return CDP_DEFAULT_CATALOG_NAME.equals(name)
                || (legacyCatalogEnabled && CLOUDBREAK_DEFAULT_CATALOG_NAME.equals(name));
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

    private ImageCatalog getDefaultCatalog(String name) {
        if (CDP_DEFAULT_CATALOG_NAME.equalsIgnoreCase(name)) {
            return getCloudbreakDefaultImageCatalog();
        } else {
            return getCloudbreakLegacyDefaultImageCatalog();
        }
    }

    private ImageCatalog getCloudbreakDefaultImageCatalog() {
        ImageCatalog imageCatalog = new ImageCatalog();
        imageCatalog.setName(CDP_DEFAULT_CATALOG_NAME);
        imageCatalog.setImageCatalogUrl(defaultCatalogUrl);
        imageCatalog.setResourceCrn(Crn.builder()
                .setResource(CDP_DEFAULT_CATALOG_NAME)
                .setPartition(Crn.Partition.CDP)
                .setService(Crn.Service.DATAHUB)
                .setAccountId(ThreadBasedUserCrnProvider.getAccountId())
                .setResourceType(Crn.ResourceType.IMAGE_CATALOG)
                .build()
                .toString());
        return imageCatalog;
    }

    private ImageCatalog getCloudbreakLegacyDefaultImageCatalog() {
        ImageCatalog imageCatalog = new ImageCatalog();
        imageCatalog.setName(CLOUDBREAK_DEFAULT_CATALOG_NAME);
        imageCatalog.setImageCatalogUrl(defaultCatalogUrl);
        imageCatalog.setResourceCrn(Crn.builder()
                .setResource(CLOUDBREAK_DEFAULT_CATALOG_NAME)
                .setPartition(Crn.Partition.CDP)
                .setService(Crn.Service.DATAHUB)
                .setAccountId(ThreadBasedUserCrnProvider.getAccountId())
                .setResourceType(Crn.ResourceType.IMAGE_CATALOG)
                .build()
                .toString());
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
        CloudbreakImageCatalogV3 imageCatalogV2 = imageCatalogProvider.getImageCatalogV3(imageFilter.getImageCatalog().getImageCatalogUrl());
        Set<String> suppertedVersions;
        if (imageCatalogV2 != null) {
            LOGGER.info("Image catalog found, filtering the images..");

            Set<String> vMImageUUIDs = new HashSet<>();
            Set<String> defaultVMImageUUIDs = new HashSet<>();
            List<CloudbreakVersion> cloudbreakVersions = imageCatalogV2.getVersions().getCloudbreakVersions();
            String cbv = versionFilter.isVersionUnspecified(imageFilter.getCbVersion())
                    ? versionFilter.latestCloudbreakVersion(cloudbreakVersions)
                    : imageFilter.getCbVersion();
            List<CloudbreakVersion> exactMatchedImgs = cloudbreakVersions.stream()
                    .filter(cloudbreakVersion -> cloudbreakVersion.getVersions().contains(cbv)).collect(toList());

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
            LOGGER.info("The following images are matching for CB version ({}): {} ", cbv, vMImageUUIDs);

            List<Image> baseImages = filterImagesByPlatforms(imageFilter.getPlatforms(), imageCatalogV2.getImages().getBaseImages(), vMImageUUIDs);
            List<Image> hdpImages = filterImagesByPlatforms(imageFilter.getPlatforms(), imageCatalogV2.getImages().getHdpImages(), vMImageUUIDs);
            List<Image> hdfImages = filterImagesByPlatforms(imageFilter.getPlatforms(), imageCatalogV2.getImages().getHdfImages(), vMImageUUIDs);
            List<Image> cdhImages = filterImagesByPlatforms(imageFilter.getPlatforms(), imageCatalogV2.getImages().getCdhImages(), vMImageUUIDs);

            Stream.of(baseImages.stream(), hdpImages.stream(), hdfImages.stream(), cdhImages.stream())
                    .reduce(Stream::concat)
                    .orElseGet(Stream::empty)
                    .forEach(img -> img.setDefaultImage(defaultVMImageUUIDs.contains(img.getUuid())));

            if (!imageFilter.isBaseImageEnabled()) {
                baseImages.clear();
            }
            images = statedImages(new Images(baseImages, hdpImages, hdfImages, cdhImages, suppertedVersions),
                    imageFilter.getImageCatalog().getImageCatalogUrl(),
                    imageFilter.getImageCatalog().getName());
        } else {
            LOGGER.warn("Image catalog {} not found, returning empty response", imageFilter.getImageCatalog());
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
                    StringUtils.join(collect, ",")));
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

    public boolean baseImageEnabled() {
        User loggedInUser = getLoggedInUser();
        String userCrn = loggedInUser.getUserCrn();
        String accountId = Crn.safeFromString(userCrn).getAccountId();
        boolean baseImageEnabled = entitlementService.baseImageEnabled(userCrn, accountId);
        LOGGER.info("The usage of base images is {}", baseImageEnabled ? "enabled" : "disabled");
        return baseImageEnabled;
    }

    private Optional<? extends Image> findFirstWithImageId(String imageId, Collection<? extends Image> images) {
        return images.stream()
                .filter(img -> img.getUuid().equals(imageId))
                .findFirst();
    }

    private List<Image> filterImagesByPlatforms(Collection<String> platforms, Collection<Image> images, Collection<String> vMImageUUIDs) {
        return images.stream()
                .filter(isPlatformMatching(platforms, vMImageUUIDs))
                .collect(toList());
    }

    private static Predicate<Image> isPlatformMatching(Collection<String> platforms, Collection<String> vMImageUUIDs) {
        return img -> vMImageUUIDs.contains(img.getUuid())
                && img.getImageSetsByProvider().keySet().stream().anyMatch(p -> platforms.stream().anyMatch(platform -> platform.equalsIgnoreCase(p)));
    }

    private List<Image> filterImagesByOperatingSystemsAndPackageVersion(List<Image> images, Set<String> operatingSystems,
            Predicate<Image> imageFilter) {
        Map<Boolean, List<Image>> partitionedImages = images
                .stream()
                .filter(imageFilter)
                .collect(partitioningBy(isMatchingOs(operatingSystems)));
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
                .collect(toList());

        versions = versions.stream().sorted((o1, o2) -> new VersionComparator().compare(() -> o2, () -> o1)).collect(toList());

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
        if (imageCatalog.getName() != null && !CDP_DEFAULT_CATALOG_NAME.equalsIgnoreCase(imageCatalog.getName())) {
            setImageCatalogAsDefault(null, user);
            imageCatalogRepository.save(imageCatalog);
        }
    }

    @Nonnull
    public ImageCatalog getDefaultImageCatalog(User user) {
        ImageCatalog imageCatalog = getUserProfile(user).getImageCatalog();
        if (imageCatalog == null) {
            imageCatalog = new ImageCatalog();
            imageCatalog.setImageCatalogUrl(defaultCatalogUrl);
            imageCatalog.setName(CDP_DEFAULT_CATALOG_NAME);
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
        return img -> operatingSystems.stream().anyMatch(os -> img.getOs().equalsIgnoreCase(os));
    }

    private Optional<Image> getLatestImageDefaultPreferred(List<Image> images) {
        List<Image> defaultImages = images.stream().filter(Image::isDefaultImage).collect(toList());
        return defaultImages.isEmpty() ? images.stream().max(getImageComparing(images)) : defaultImages.stream().max(getImageComparing(defaultImages));
    }

    private Comparator<Image> getImageComparing(List<Image> images) {
        return images.stream().map(Image::getCreated).anyMatch(Objects::isNull) ? Comparator.comparing(Image::getDate)
                : Comparator.comparing(Image::getCreated);
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
    protected void prepareDeletion(ImageCatalog resource) {

    }

    @Override
    protected void prepareCreation(ImageCatalog resource) {
        if (isEnvDefault(resource.getName())) {
            throw new BadRequestException(String
                    .format("%s cannot be created because it is an environment default image catalog.", resource.getName()));
        }
    }

    private String createCRN(String accountId) {
        return Crn.builder()
                .setService(Crn.Service.DATAHUB)
                .setAccountId(accountId)
                .setResourceType(Crn.ResourceType.IMAGE_CATALOG)
                .setResource(UUID.randomUUID().toString())
                .build()
                .toString();
    }

    @Override
    public String getResourceCrnByResourceName(String resourceName) {
        if (CDP_DEFAULT_CATALOG_NAME.equals(resourceName)) {
            return getCloudbreakDefaultImageCatalog().getResourceCrn();
        } else if (legacyCatalogEnabled && CLOUDBREAK_DEFAULT_CATALOG_NAME.equals(resourceName)) {
            return getCloudbreakLegacyDefaultImageCatalog().getResourceCrn();
        } else {
            return imageCatalogRepository.findResourceCrnByNameAndTenantId(resourceName, ThreadBasedUserCrnProvider.getAccountId())
                    .orElseThrow(() -> NotFoundException.notFoundException("Image catalog", resourceName));
        }
    }

    @Override
    public List<String> getResourceCrnListByResourceNameList(List<String> resourceNames) {
        Map<Boolean, List<String>> catalogs = resourceNames.stream()
                .collect(partitioningBy(name ->
                        CDP_DEFAULT_CATALOG_NAME.equals(name) || (legacyCatalogEnabled && CLOUDBREAK_DEFAULT_CATALOG_NAME.equals(name))));
        List<String> result = new ArrayList<>();

        List<String> defaultCatalogs = catalogs.get(true);
        if (!CollectionUtils.isEmpty(defaultCatalogs)) {
            result.addAll(defaultCatalogs.stream().map(this::getResourceCrnByResourceName).collect(toList()));
        }

        List<String> notDefaultCatalogs = catalogs.get(false);
        if (!CollectionUtils.isEmpty(notDefaultCatalogs)) {
            result.addAll(imageCatalogRepository.findAllResourceCrnsByNamesAndTenantId(notDefaultCatalogs, ThreadBasedUserCrnProvider.getAccountId()));
        }
        return result;
    }

    @Override
    public List<String> getResourceCrnsInAccount() {
        List<String> crns = new ArrayList<>();
        crns.add(getCloudbreakDefaultImageCatalog().getResourceCrn());
        if (legacyCatalogEnabled) {
            crns.add(getCloudbreakLegacyDefaultImageCatalog().getResourceCrn());
        }
        crns.addAll(imageCatalogRepository.findAllResourceCrnsByTenantId(ThreadBasedUserCrnProvider.getAccountId()));
        return crns;
    }

    public List<ImageCatalog> getDefaultImageCatalogs() {
        if (legacyCatalogEnabled) {
            return List.of(getCloudbreakDefaultImageCatalog(), getCloudbreakLegacyDefaultImageCatalog());
        } else {
            return List.of(getCloudbreakDefaultImageCatalog());
        }
    }

    @Override
    public AuthorizationResourceType getResourceType() {
        return AuthorizationResourceType.IMAGE_CATALOG;
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
