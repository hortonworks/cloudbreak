package com.sequenceiq.cloudbreak.service.image;

import static com.sequenceiq.cloudbreak.common.exception.NotFoundException.notFound;
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
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;
import javax.inject.Inject;

import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import com.sequenceiq.authorization.resource.AuthorizationResourceType;
import com.sequenceiq.authorization.service.OwnerAssignmentService;
import com.sequenceiq.authorization.service.ResourcePropertyProvider;
import com.sequenceiq.authorization.service.list.ResourceWithId;
import com.sequenceiq.cloudbreak.api.endpoint.v4.dto.NameOrCrn;
import com.sequenceiq.cloudbreak.auth.crn.RegionAwareCrnGenerator;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.crn.Crn;
import com.sequenceiq.cloudbreak.auth.crn.CrnResourceDescriptor;
import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.cloud.model.catalog.CloudbreakImageCatalogV3;
import com.sequenceiq.cloudbreak.cloud.model.catalog.Image;
import com.sequenceiq.cloudbreak.cloud.model.catalog.Images;
import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.cloudbreak.common.exception.NotFoundException;
import com.sequenceiq.cloudbreak.common.service.TransactionService;
import com.sequenceiq.cloudbreak.common.service.TransactionService.TransactionExecutionException;
import com.sequenceiq.cloudbreak.common.service.TransactionService.TransactionRuntimeExecutionException;
import com.sequenceiq.cloudbreak.core.CloudbreakImageCatalogException;
import com.sequenceiq.cloudbreak.core.CloudbreakImageNotFoundException;
import com.sequenceiq.cloudbreak.domain.CustomImage;
import com.sequenceiq.cloudbreak.domain.ImageCatalog;
import com.sequenceiq.cloudbreak.domain.UserProfile;
import com.sequenceiq.cloudbreak.logger.MDCUtils;
import com.sequenceiq.cloudbreak.repository.ImageCatalogRepository;
import com.sequenceiq.cloudbreak.service.AbstractWorkspaceAwareResourceService;
import com.sequenceiq.cloudbreak.service.account.PreferencesService;
import com.sequenceiq.cloudbreak.service.image.catalog.ImageCatalogServiceProxy;
import com.sequenceiq.cloudbreak.service.user.UserProfileHandler;
import com.sequenceiq.cloudbreak.service.user.UserProfileService;
import com.sequenceiq.cloudbreak.structuredevent.CloudbreakRestRequestThreadLocalService;
import com.sequenceiq.cloudbreak.workspace.model.User;
import com.sequenceiq.cloudbreak.workspace.model.Workspace;
import com.sequenceiq.cloudbreak.workspace.repository.workspace.WorkspaceResourceRepository;
import com.sequenceiq.common.api.type.ImageType;

@Component
public class ImageCatalogService extends AbstractWorkspaceAwareResourceService<ImageCatalog> implements ResourcePropertyProvider {

    public static final String UNDEFINED = "";

    public static final String CDP_DEFAULT_CATALOG_NAME = "cdp-default";

    public static final String FREEIPA_DEFAULT_CATALOG_NAME = "freeipa-default";

    static final String CLOUDBREAK_DEFAULT_CATALOG_NAME = "cloudbreak-default";

    private static final Logger LOGGER = LoggerFactory.getLogger(ImageCatalogService.class);

    @Value("${info.app.version:}")
    private String cbVersion;

    @Value("${cb.image.catalog.url}")
    private String defaultCatalogUrl;

    @Value("${cb.freeipa.image.catalog.url}")
    private String defaultFreeIpaCatalogUrl;

    @Value("${cb.image.catalog.legacy.enabled}")
    private boolean legacyCatalogEnabled;

    @Inject
    private ImageCatalogProvider imageCatalogProvider;

    @Inject
    private ImageCatalogRepository imageCatalogRepository;

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

    @Inject
    private OwnerAssignmentService ownerAssignmentService;

    @Inject
    private TransactionService transactionService;

    @Inject
    private ImageCatalogServiceProxy imageCatalogServiceProxy;

    @Inject
    private CustomImageProvider customImageProvider;

    @Inject
    private CloudbreakRestRequestThreadLocalService restRequestThreadLocalService;

    @Inject
    private RegionAwareCrnGenerator regionAwareCrnGenerator;

    public Set<ImageCatalog> findAllByWorkspaceId(Long workspaceId, boolean customCatalogsOnly) {

        Set<ImageCatalog> imageCatalogs = null;
        if (customCatalogsOnly) {
            imageCatalogs = imageCatalogRepository.findAllByWorkspaceIdAndArchivedAndImageCatalogUrlIsNull(workspaceId, false);
        } else {
            imageCatalogs = imageCatalogRepository.findAllByWorkspaceIdAndArchivedAndImageCatalogUrlIsNotNull(workspaceId, false);

            imageCatalogs.add(getCloudbreakDefaultImageCatalog());
            if (legacyCatalogEnabled) {
                imageCatalogs.add(getCloudbreakLegacyDefaultImageCatalog());
            }
        }
        return imageCatalogs;
    }

    @Override
    public Set<ImageCatalog> findAllByWorkspaceId(Long workspaceId) {
        return findAllByWorkspaceId(workspaceId, false);
    }

    public List<ResourceWithId> findAsAuthorizationResorcesInWorkspace(Long workspaceId, boolean customCatalogsOnly) {
        if (customCatalogsOnly) {
            return imageCatalogRepository.findCustomAsAuthorizationResourcesInWorkspace(workspaceId);
        } else {
            return imageCatalogRepository.findAsAuthorizationResourcesInWorkspace(workspaceId);
        }
    }

    public Set<ImageCatalog> findAllByIdsWithDefaults(List<Long> ids, boolean customCatalogsOnly) {
        Set<ImageCatalog> imageCatalogs = Sets.newLinkedHashSet(imageCatalogRepository.findAllByIdNotArchived(ids));

        // FIXME: We need to clean up this part
        // In contrast with the method name, this will only return the default catalog if the customCatalogsOnly
        // parameter is false. Right now this is intentional, but as soon as the support for JSON based custom catalogs
        // is gone, we'll have to clean up things like this.
        if (customCatalogsOnly) {
            imageCatalogs.removeIf(imageCatalog -> !StringUtils.isEmpty(imageCatalog.getImageCatalogUrl()));
        } else {
            imageCatalogs.add(getCloudbreakDefaultImageCatalog());
            if (legacyCatalogEnabled) {
                imageCatalogs.add(getCloudbreakLegacyDefaultImageCatalog());
            }
        }
        return imageCatalogs;
    }

    public ImageCatalog delete(NameOrCrn imageCatalogNameOrCrn, Long workspaceId) {
        ImageCatalog catalog = getImageCatalogByName(imageCatalogNameOrCrn, workspaceId);
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
        String resourceCrn = createCRN(accountId);
        imageCatalog.setResourceCrn(resourceCrn);
        imageCatalog.setCreator(creator);
        try {
            return transactionService.required(() -> {
                ImageCatalog created = super.createForLoggedInUser(imageCatalog, workspaceId);
                ownerAssignmentService.assignResourceOwnerRoleIfEntitled(creator, resourceCrn, accountId);
                return created;
            });
        } catch (TransactionExecutionException e) {
            throw new TransactionRuntimeExecutionException(e);
        }
    }

    public ImageCatalog getImageCatalogByName(NameOrCrn imageCatalogNameOrCrn, Long workspaceId) {
        return imageCatalogNameOrCrn.hasName()
                ? getImageCatalogByName(workspaceId, imageCatalogNameOrCrn.getName())
                : findByResourceCrn(imageCatalogNameOrCrn.getCrn());
    }

    public ImageCatalog findByResourceCrn(String resourceCrn) {
        return imageCatalogRepository.findByResourceCrnAndArchivedFalseAndImageCatalogUrlIsNotNull(resourceCrn)
                .orElseThrow(notFound("ImageCatalog", resourceCrn));
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
            ImageFilter imageFilter = new ImageFilter(getDefaultImageCatalog(user), Set.of(platform), null, baseImageEnabled(), operatingSystems, null);
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
            List<Image> cdhImages = filterImagesByOperatingSystemsAndPackageVersion(rawImages.getCdhImages(), operatingSystems, imageFilterPredicate);
            images = statedImages(new Images(baseImages, cdhImages, rawImages.getFreeIpaImages(), rawImages.getSuppertedVersions()),
                    images.getImageCatalogUrl(), images.getImageCatalogName());
        }
        return images;
    }

    public StatedImage getLatestBaseImageDefaultPreferred(ImageFilter imageFilter, Predicate<Image> imageFilterPredicate)
            throws CloudbreakImageCatalogException, CloudbreakImageNotFoundException {
        String platform = imageFilter.getPlatforms().stream().findFirst().isPresent() ? imageFilter.getPlatforms().stream().findFirst().get() : "";
        StatedImages statedImages = getStatedImagesFilteredByOperatingSystems(imageFilter, imageFilterPredicate);
        List<Image> baseImages = statedImages.getImages().getBaseImages();
        Optional<Image> defaultBaseImage = getLatestImageDefaultPreferred(baseImages);
        if (!defaultBaseImage.isPresent()) {
            throw new CloudbreakImageNotFoundException(imageNotFoundErrorMessage(platform));
        }
        return statedImage(defaultBaseImage.get(), statedImages.getImageCatalogUrl(), statedImages.getImageCatalogName());
    }

    public StatedImage getImagePrewarmedDefaultPreferred(ImageFilter imageFilter, Predicate<Image> imageFilterPredicate)
            throws CloudbreakImageNotFoundException, CloudbreakImageCatalogException {
        StatedImages statedImages = getStatedImagesFilteredByOperatingSystems(imageFilter, imageFilterPredicate);
        Optional<Image> selectedImage = getLatestPrewarmedImage(imageFilter.getClusterVersion(), statedImages);
        if (selectedImage.isEmpty()) {
            Set<String> platforms = imageFilter.getPlatforms();
            String platform = platforms.stream().findFirst().isPresent() ? platforms.stream().findFirst().get() : "";
            throw new CloudbreakImageNotFoundException(imageNotFoundErrorMessage(platform));
        }
        return statedImage(selectedImage.get(), statedImages.getImageCatalogUrl(), statedImages.getImageCatalogName());
    }

    private Optional<Image> getLatestPrewarmedImage(String clusterVersion, StatedImages statedImages) {
        List<Image> images = statedImages.getImages().getCdhImages();
        Optional<Image> selectedImage = Optional.empty();
        if (!CollectionUtils.isEmpty(images)) {
            List<Image> matchingVersionImages = filterImagesByRuntimeVersion(clusterVersion, images);
            selectedImage = getLatestImageDefaultPreferred(matchingVersionImages);
        }
        return selectedImage;
    }

    private List<Image> filterImagesByRuntimeVersion(String clusterVersion, List<Image> images) {
        List<Image> matchingVersionImages = images.stream().filter(img -> {
            String[] repoIdParts = img.getStackDetails().getRepo().getStack().get("repoid").split("-");
            return repoIdParts.length > 1 && repoIdParts[1].equals(clusterVersion);
        }).collect(toList());
        LOGGER.debug("Images matching runtime are: {}", matchingVersionImages.stream().map(Image::getUuid).collect(Collectors.joining(",")));
        return matchingVersionImages;
    }

    private String imageNotFoundErrorMessage(String platform) {
        return String.format("Could not find any image for platform '%s' and Cloudbreak version '%s'.", platform, cbVersion);
    }

    public StatedImages getImages(Long workspaceId, String imageCatalogName, String provider) throws CloudbreakImageCatalogException {
        return getImages(getLoggedInUser().getUserCrn(), workspaceId, imageCatalogName, ImmutableSet.of(provider));
    }

    public List<Image> getCdhImages(String userCrn, Long workspaceId, String imageCatalogName, String provider) throws CloudbreakImageCatalogException {
        return getImages(userCrn, workspaceId, imageCatalogName, ImmutableSet.of(provider)).getImages().getCdhImages();
    }

    public StatedImages getImages(String userCrn, Long workspaceId, String imageCatalogName, Set<String> providers) throws CloudbreakImageCatalogException {
        try {
            ImageCatalog imageCatalog = getImageCatalogByName(workspaceId, imageCatalogName);
            if (isCustomImageCatalog(imageCatalog)) {
                return getStatedImagesFromCustomImageCatalog(imageCatalog, providers);
            } else {
                return getImages(new ImageFilter(imageCatalog, providers, cbVersion, baseImageEnabled(userCrn), null, null));
            }
        } catch (NotFoundException ignore) {
            throw new CloudbreakImageCatalogException(String.format("The %s catalog does not exist or does not belongs to your account.", imageCatalogName));
        }
    }

    private boolean isCustomImageCatalog(ImageCatalog imageCatalog) {
        return imageCatalog != null && Strings.isNullOrEmpty(imageCatalog.getImageCatalogUrl()) && imageCatalog.getCustomImages() != null;
    }

    private StatedImages getStatedImagesFromCustomImageCatalog(ImageCatalog imageCatalog, Set<String> providers) throws CloudbreakImageCatalogException {
        try {
            List<Image> cbImages = getImages(Set.of(ImageType.DATALAKE, ImageType.DATAHUB), imageCatalog, providers);
            List<Image> freeIpaImages = getImages(Set.of(ImageType.FREEIPA), imageCatalog, providers);
            return statedImages(new Images(null, cbImages, freeIpaImages,
                    Set.of(cbVersion)), imageCatalog.getImageCatalogUrl(), imageCatalog.getName());
        } catch (CloudbreakImageNotFoundException ex) {
            throw new CloudbreakImageCatalogException(ex.getMessage());
        }
    }

    private List<Image> getImages(Set<ImageType> imageTypes, ImageCatalog imageCatalog, Set<String> providers)
            throws CloudbreakImageNotFoundException, CloudbreakImageCatalogException {
        List<Image> images = new ArrayList<>();
        for (CustomImage customImage : imageCatalog.getCustomImages()) {
            if (imageTypes.contains(customImage.getImageType())) {
                StatedImage sourceImage = getSourceImageByImageType(customImage);
                Optional<String> provider = sourceImage.getImage().getImageSetsByProvider().keySet().stream().findFirst();
                provider.ifPresent(p -> {
                    if (providers.stream().anyMatch(p::equalsIgnoreCase)) {
                        images.add(customImageProvider.mergeSourceImageAndCustomImageProperties(
                                sourceImage, customImage, imageCatalog.getImageCatalogUrl(), imageCatalog.getName()).getImage());
                    }
                });
            }
        }
        return images;
    }

    public StatedImage getImage(String imageId) throws CloudbreakImageNotFoundException, CloudbreakImageCatalogException {
        return getImage(defaultCatalogUrl, CDP_DEFAULT_CATALOG_NAME, imageId);
    }

    public StatedImage getImage(String catalogUrl, String catalogName, String imageId) throws CloudbreakImageNotFoundException,
            CloudbreakImageCatalogException {
        ImageCatalog imageCatalog = getImageCatalogByNameIfUrlIsEmpty(catalogUrl, catalogName);
        if (isCustomImageCatalog(imageCatalog)) {
            LOGGER.debug(String.format("'%s' image catalog is a custom image catalog.", catalogName));
            return getCustomStatedImage(imageCatalog, imageId);
        } else {
            LOGGER.debug(String.format("'%s' image catalog is not a custom image catalog, we should lookup images by image catalog url '%s'.",
                    catalogName, catalogUrl));
            return getImageByUrl(catalogUrl, catalogName, imageId);
        }
    }

    private ImageCatalog getImageCatalogByNameIfUrlIsEmpty(String catalogUrl, String catalogName) {
        if (Strings.isNullOrEmpty(catalogUrl)) {
            try {
                return getImageCatalogByName(restRequestThreadLocalService.getRequestedWorkspaceId(), catalogName);
            } catch (Exception ex) {
                LOGGER.debug(String.format("Failed to lookup '%s' image catalog by name.", catalogName));
                return null;
            }
        } else {
            return null;
        }
    }

    public StatedImage getImageByCatalogName(Long workspaceId, String imageId, String catalogName)
            throws CloudbreakImageNotFoundException, CloudbreakImageCatalogException {
        if (StringUtils.isEmpty(catalogName)) {
            return getImage(imageId);
        } else {
            try {
                return getCustomStatedImage(workspaceId, catalogName, imageId);
            } catch (NotFoundException e) {
                throw new CloudbreakImageCatalogException(e.getMessage());
            }
        }
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
        ImageCatalog imageCatalog = getImageCatalogByName(workspaceId, name);
        imageCatalog.setArchived(true);
        setImageCatalogAsDefault(null, user);
        imageCatalog.setName(generateArchiveName(name));
        imageCatalogRepository.save(imageCatalog);
        ownerAssignmentService.notifyResourceDeleted(imageCatalog.getResourceCrn(), MDCUtils.getRequestId());
        userProfileHandler.destroyProfileImageCatalogPreparation(imageCatalog);
        LOGGER.debug("Image catalog has been archived: {}", imageCatalog);
        return imageCatalog;
    }

    public ImageCatalog getImageCatalogByName(Long workspaceId, String name) {
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
        return Optional.ofNullable(getImageCatalogByName(workspaceId, name)).orElseThrow(notFound("Image catalog", name));
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
        imageCatalog.setResourceCrn(regionAwareCrnGenerator.generateCrnString(CrnResourceDescriptor.IMAGE_CATALOG, CDP_DEFAULT_CATALOG_NAME,
                ThreadBasedUserCrnProvider.getAccountId()));
        return imageCatalog;
    }

    private ImageCatalog getCloudbreakLegacyDefaultImageCatalog() {
        ImageCatalog imageCatalog = new ImageCatalog();
        imageCatalog.setName(CLOUDBREAK_DEFAULT_CATALOG_NAME);
        imageCatalog.setImageCatalogUrl(defaultCatalogUrl);
        imageCatalog.setResourceCrn(regionAwareCrnGenerator.generateCrnString(CrnResourceDescriptor.IMAGE_CATALOG, CLOUDBREAK_DEFAULT_CATALOG_NAME,
                ThreadBasedUserCrnProvider.getAccountId()));
        return imageCatalog;
    }

    private Images emptyImages() {
        return new Images(emptyList(), emptyList(), emptyList(), emptySet());
    }

    private Optional<? extends Image> getImage(String imageId, Images images) {
        Optional<? extends Image> image;
        if (!images.getFreeIpaImages().isEmpty()) {
            image = findFirstWithImageId(imageId, images.getFreeIpaImages());
        } else {
            image = findFirstWithImageId(imageId, images.getBaseImages());
            if (image.isEmpty()) {
                image = findFirstWithImageId(imageId, images.getCdhImages());
            }
        }
        return image;
    }

    public StatedImages getImages(ImageFilter imageFilter) throws CloudbreakImageCatalogException {
        LOGGER.info("Determine images for imageCatalogUrl: '{}', platforms: '{}' and Cloudbreak version: '{}'.",
                imageFilter.getImageCatalog().getImageCatalogUrl(), imageFilter.getPlatforms(), imageFilter.getCbVersion());
        validateRequestPlatforms(imageFilter.getPlatforms());

        CloudbreakImageCatalogV3 imageCatalogV3 = imageCatalogProvider.getImageCatalogV3(imageFilter.getImageCatalog().getImageCatalogUrl());
        if (imageCatalogV3 != null) {
            LOGGER.info("Image catalog found, filtering the images..");
            return imageCatalogServiceProxy.getImages(imageCatalogV3, imageFilter);
        } else {
            LOGGER.warn("Image catalog {} not found, returning empty response", imageFilter.getImageCatalog());
            return statedImages(emptyImages(),
                        imageFilter.getImageCatalog().getImageCatalogUrl(),
                        imageFilter.getImageCatalog().getName());
        }
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
                return getImages(getLoggedInUser().getUserCrn(), workspaceId, name, platforms).getImages();
            } catch (CloudbreakImageCatalogException e) {
                LOGGER.info("No images was found: ", e);
            }
        }
        return null;
    }

    public boolean baseImageEnabled() {
        return baseImageEnabled(getLoggedInUser().getUserCrn());
    }

    public boolean baseImageEnabled(String userCrn) {
        String accountId = Crn.safeFromString(userCrn).getAccountId();
        boolean baseImageEnabled = entitlementService.baseImageEnabled(accountId);
        LOGGER.info("The usage of base images is {}", baseImageEnabled ? "enabled" : "disabled");
        return baseImageEnabled;
    }

    private Optional<? extends Image> findFirstWithImageId(String imageId, Collection<? extends Image> images) {
        return images.stream()
                .filter(img -> img.getUuid().equals(imageId))
                .findFirst();
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
        //This predicate should be used after image burning generates the right OS into the image catalog
        //return img -> operatingSystems.stream().anyMatch(os -> img.getOs().equalsIgnoreCase(os));
        return img -> operatingSystems.stream().anyMatch(os -> img.getOs().equalsIgnoreCase(os) || img.getOsType().equalsIgnoreCase(os));
    }

    private Optional<Image> getLatestImageDefaultPreferred(List<Image> images) {
        List<Image> defaultImages = images.stream().filter(Image::isDefaultImage).collect(toList());
        return defaultImages.isEmpty() ? images.stream().max(getImageComparing(images)) : defaultImages.stream().max(getImageComparing(defaultImages));
    }

    private Comparator<Image> getImageComparing(List<Image> images) {
        return images.stream().map(Image::getCreated).anyMatch(Objects::isNull) ? Comparator.comparing(Image::getDate)
                : Comparator.comparing(Image::getCreated);
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
        return regionAwareCrnGenerator.generateCrnStringWithUuid(CrnResourceDescriptor.IMAGE_CATALOG, accountId);
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

    public List<ImageCatalog> getDefaultImageCatalogs() {
        if (legacyCatalogEnabled) {
            return List.of(getCloudbreakDefaultImageCatalog(), getCloudbreakLegacyDefaultImageCatalog());
        } else {
            return List.of(getCloudbreakDefaultImageCatalog());
        }
    }

    @Override
    public Optional<AuthorizationResourceType> getSupportedAuthorizationResourceType() {
        return Optional.of(AuthorizationResourceType.IMAGE_CATALOG);
    }

    @Override
    public Map<String, Optional<String>> getNamesByCrns(Collection<String> crns) {
        Map<String, Optional<String>> result = new HashMap<>();
        List<String> notDefault = new ArrayList<>();
        for (String crn : crns) {
            if (legacyCatalogEnabled) {
                ImageCatalog legacy = getCloudbreakLegacyDefaultImageCatalog();
                if (legacy.getResourceCrn().equals(crn)) {
                    result.put(crn, Optional.ofNullable(legacy.getName()));
                } else {
                    notDefault.add(crn);
                }
            } else {
                ImageCatalog cdpDefault = getCloudbreakDefaultImageCatalog();
                if (cdpDefault.getResourceCrn().equals(crn)) {
                    result.put(crn, Optional.ofNullable(cdpDefault.getName()));
                } else {
                    notDefault.add(crn);
                }
            }
        }
        imageCatalogRepository.findResourceNamesByCrnAndTenantId(notDefault, ThreadBasedUserCrnProvider.getAccountId()).stream()
                .forEach(nameAndCrn -> result.put(nameAndCrn.getCrn(), Optional.ofNullable(nameAndCrn.getName())));
        return result;
    }

    @Override
    public EnumSet<Crn.ResourceType> getSupportedCrnResourceTypes() {
        return EnumSet.of(Crn.ResourceType.IMAGE_CATALOG);
    }

    public StatedImage getCustomStatedImage(long workspaceId, String catalogName, String imageId)
            throws CloudbreakImageNotFoundException, CloudbreakImageCatalogException {
        ImageCatalog imageCatalog = getImageCatalogByName(workspaceId, catalogName);

        return getCustomStatedImage(imageCatalog, imageId);
    }

    private StatedImage getCustomStatedImage(ImageCatalog imageCatalog, String imageId)
            throws CloudbreakImageNotFoundException, CloudbreakImageCatalogException {
        Optional<CustomImage> optionalCustomImage = getCustomImage(imageCatalog, imageId);
        if (optionalCustomImage.isPresent()) {
            CustomImage customImage = optionalCustomImage.get();
            LOGGER.info("Custom image is available with id '{}'. Searching for source image '{}',", imageId);
            StatedImage sourceImage = getSourceImageByImageType(customImage);
            LOGGER.info("Custom image '{}' is a {} image '{}' customization.", imageId, customImage.getImageType(), customImage.getCustomizedImageId());
            return customImageProvider.mergeSourceImageAndCustomImageProperties(
                        sourceImage, customImage, imageCatalog.getImageCatalogUrl(), imageCatalog.getName());
        } else {
            return getImage(imageCatalog.getImageCatalogUrl(), imageCatalog.getName(), imageId);
        }

    }

    private Optional<CustomImage> getCustomImage(ImageCatalog imageCatalog, String imageId) {
        return imageCatalog.getCustomImages().stream().filter(i -> i.getName().equalsIgnoreCase(imageId)).findFirst();
    }

    public StatedImage getSourceImageByImageType(CustomImage customImage)
            throws CloudbreakImageNotFoundException, CloudbreakImageCatalogException {
        switch (customImage.getImageType()) {
            case FREEIPA:
                return getImageByUrl(defaultFreeIpaCatalogUrl, FREEIPA_DEFAULT_CATALOG_NAME, customImage.getCustomizedImageId());
            case DATAHUB:
            case DATALAKE:
                return getImage(customImage.getCustomizedImageId());
            default:
                throw new CloudbreakImageCatalogException("Image type is not supported.");
        }
    }

    private StatedImage getImageByUrl(String catalogUrl, String catalogName, String imageId) throws CloudbreakImageNotFoundException,
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
}
