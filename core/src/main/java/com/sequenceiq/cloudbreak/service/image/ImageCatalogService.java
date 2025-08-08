package com.sequenceiq.cloudbreak.service.image;

import static com.sequenceiq.cloudbreak.common.exception.NotFoundException.notFound;
import static com.sequenceiq.cloudbreak.service.image.StatedImage.statedImage;
import static com.sequenceiq.cloudbreak.service.image.StatedImages.statedImages;
import static com.sequenceiq.cloudbreak.util.NameUtil.generateArchiveName;
import static com.sequenceiq.common.model.ImageCatalogPlatform.imageCatalogPlatform;
import static com.sequenceiq.common.model.OsType.RHEL9;
import static java.util.Collections.emptyList;
import static java.util.Collections.emptySet;
import static java.util.stream.Collectors.partitioningBy;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.lang3.StringUtils.isNotEmpty;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import jakarta.annotation.Nonnull;
import jakarta.inject.Inject;

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
import com.sequenceiq.authorization.service.CompositeAuthResourcePropertyProvider;
import com.sequenceiq.authorization.service.OwnerAssignmentService;
import com.sequenceiq.authorization.service.list.ResourceWithId;
import com.sequenceiq.cloudbreak.api.endpoint.v4.dto.NameOrCrn;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.auth.crn.Crn;
import com.sequenceiq.cloudbreak.auth.crn.CrnResourceDescriptor;
import com.sequenceiq.cloudbreak.auth.crn.RegionAwareCrnGenerator;
import com.sequenceiq.cloudbreak.cloud.model.catalog.CloudbreakImageCatalogV3;
import com.sequenceiq.cloudbreak.cloud.model.catalog.CloudbreakVersion;
import com.sequenceiq.cloudbreak.cloud.model.catalog.Image;
import com.sequenceiq.cloudbreak.cloud.model.catalog.ImageStackDetails;
import com.sequenceiq.cloudbreak.cloud.model.catalog.Images;
import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.cloudbreak.common.exception.NotFoundException;
import com.sequenceiq.cloudbreak.common.gov.CommonGovService;
import com.sequenceiq.cloudbreak.common.provider.ProviderPreferencesService;
import com.sequenceiq.cloudbreak.common.service.PlatformStringTransformer;
import com.sequenceiq.cloudbreak.core.CloudbreakImageCatalogException;
import com.sequenceiq.cloudbreak.core.CloudbreakImageNotFoundException;
import com.sequenceiq.cloudbreak.domain.CustomImage;
import com.sequenceiq.cloudbreak.domain.ImageCatalog;
import com.sequenceiq.cloudbreak.domain.UserProfile;
import com.sequenceiq.cloudbreak.repository.ImageCatalogRepository;
import com.sequenceiq.cloudbreak.service.AbstractWorkspaceAwareResourceService;
import com.sequenceiq.cloudbreak.service.image.catalog.ImageCatalogServiceProxy;
import com.sequenceiq.cloudbreak.service.upgrade.image.ImageFilterResult;
import com.sequenceiq.cloudbreak.service.user.UserProfileHandler;
import com.sequenceiq.cloudbreak.service.user.UserProfileService;
import com.sequenceiq.cloudbreak.workspace.model.User;
import com.sequenceiq.cloudbreak.workspace.model.Workspace;
import com.sequenceiq.cloudbreak.workspace.repository.workspace.WorkspaceResourceRepository;
import com.sequenceiq.common.api.type.ImageType;
import com.sequenceiq.common.model.Architecture;
import com.sequenceiq.common.model.ImageCatalogPlatform;

@Component
public class ImageCatalogService extends AbstractWorkspaceAwareResourceService<ImageCatalog> implements CompositeAuthResourcePropertyProvider {

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
    private ImageComparator imageComparator;

    @Inject
    private ImageCatalogProvider imageCatalogProvider;

    @Inject
    private ImageCatalogRepository imageCatalogRepository;

    @Inject
    private UserProfileService userProfileService;

    @Inject
    private UserProfileHandler userProfileHandler;

    @Inject
    private ProviderPreferencesService preferencesService;

    @Inject
    private StackImageFilterService stackImageFilterService;

    @Inject
    private EntitlementService entitlementService;

    @Inject
    private OwnerAssignmentService ownerAssignmentService;

    @Inject
    private ImageCatalogServiceProxy imageCatalogServiceProxy;

    @Inject
    private CustomImageProvider customImageProvider;

    @Inject
    private RegionAwareCrnGenerator regionAwareCrnGenerator;

    @Inject
    private PlatformStringTransformer platformStringTransformer;

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
        ownerAssignmentService.assignResourceOwnerRoleIfEntitled(creator, resourceCrn);
        try {
            return super.createForLoggedInUserInTransaction(imageCatalog, workspaceId);
        } catch (RuntimeException e) {
            ownerAssignmentService.notifyResourceDeleted(resourceCrn);
            throw e;
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

    @SuppressWarnings("checkstyle:ParameterNumber")
    public Images getImagesByCatalogName(Long workspaceId, String catalogName, String stackName,
            ImageCatalogPlatform platform, String runtimeVersion, boolean govCloud, boolean defaultOnly, String architecture)
            throws CloudbreakImageCatalogException {
        if (platform != null && isNotEmpty(platform.name()) && isNotEmpty(stackName)) {
            throw new BadRequestException("Both platform and existing stack name could not be present in the request.");
        }
        if (platform != null && isNotEmpty(platform.name())) {
            return getImages(workspaceId,
                    catalogName,
                    runtimeVersion,
                    platformStringTransformer.getPlatformStringForImageCatalog(platform.nameToLowerCase(), govCloud),
                    defaultOnly,
                    architecture)
                    .getImages();
        } else if (isNotEmpty(stackName)) {
            return stackImageFilterService.getApplicableImages(workspaceId, catalogName, stackName, defaultOnly);
        } else {
            throw new BadRequestException("Either platform or stack name must be present in the request.");
        }
    }

    @SuppressWarnings("checkstyle:ParameterNumber")
    public Images getImagesFromDefault(Long workspaceId, String stackName, ImageCatalogPlatform platform,
            Set<String> operatingSystems, String runtimeVersion, boolean govCloud, boolean defaultOnly, String architecture)
            throws CloudbreakImageCatalogException {
        if (platform != null && isNotEmpty(platform.name()) && isNotEmpty(stackName)) {
            throw new BadRequestException("Platform or stackName cannot be filled in the same request.");
        }
        if (platform != null && isNotEmpty(platform.name())) {
            User user = getLoggedInUser();
            Set<ImageCatalogPlatform> platforms = Set.of(platformStringTransformer.getPlatformStringForImageCatalog(platform, govCloud));
            ImageFilter imageFilter = ImageFilter.builder()
                    .withImageCatalog(getDefaultImageCatalog(user))
                    .withPlatforms(platforms)
                    .withCbVersion(cbVersion)
                    .withBaseImageEnabled(baseImageEnabled())
                    .withOperatingSystems(operatingSystems)
                    .withDefaultOnly(defaultOnly)
                    .withArchitecture(architecture == null ? null : Architecture.fromStringWithFallback(architecture))
                    .build();
            return getImages(imageFilter).getImages();
        } else if (isNotEmpty(stackName)) {
            return stackImageFilterService.getApplicableImages(workspaceId, stackName, defaultOnly);
        } else {
            throw new BadRequestException("Either platform or stackName should be filled in request.");
        }
    }

    public StatedImage getLatestImageDefaultPreferred(ImageFilter imageFilter, boolean selectBaseImage)
            throws CloudbreakImageCatalogException, CloudbreakImageNotFoundException {
        LOGGER.info("Trying to select a {} image.", selectBaseImage ? "base" : "prewarmed");
        StatedImages statedImages = getImages(imageFilter.withCbVersion(cbVersion));
        List<Image> images = selectBaseImage ? statedImages.getImages().getBaseImages() : statedImages.getImages().getCdhImages();
        Image defaultBaseImage = getLatestImageDefaultPreferred(imageFilter, images);
        return statedImage(defaultBaseImage, statedImages.getImageCatalogUrl(), statedImages.getImageCatalogName());
    }

    public StatedImage getImagePrewarmedDefaultPreferred(ImageFilter imageFilter) throws CloudbreakImageNotFoundException, CloudbreakImageCatalogException {
        return getLatestImageDefaultPreferred(imageFilter, false);
    }

    private String imageNotFoundErrorMessage(ImageFilter imageFilter) {
        return String.format(
                "Could not find any image for platform '%s %s', runtime '%s' and Cloudbreak version '%s' in '%s' image catalog.",
                getPlatform(imageFilter), getOses(imageFilter), imageFilter.getClusterVersion(), cbVersion,
                getImageCatalogName(imageFilter));
    }

    private String getPlatform(ImageFilter imageFilter) {
        return imageFilter.getPlatforms()
                .stream()
                .map(ImageCatalogPlatform::nameToLowerCase)
                .findFirst()
                .orElse("");
    }

    private String getOses(ImageFilter imageFilter) {
        return Optional.ofNullable(imageFilter.getOperatingSystems())
                .stream()
                .flatMap(Set::stream)
                .map(os -> imageFilter.getArchitecture() != null ? String.format("%s-%s", os, imageFilter.getArchitecture().getName()) : os)
                .collect(Collectors.joining(","));
    }

    private String getImageCatalogName(ImageFilter imageFilter) {
        return Optional.ofNullable(imageFilter.getImageCatalog()).map(ImageCatalog::getName).orElse(null);
    }

    public StatedImages getImages(Long workspaceId, String imageCatalogName, String runtimeVersion, ImageCatalogPlatform provider, boolean defaultOnly,
            String architecture)
            throws CloudbreakImageCatalogException {
        return getImages(getLoggedInUser(), workspaceId, imageCatalogName, runtimeVersion, ImmutableSet.of(provider), true, defaultOnly,
                architecture);
    }

    public List<Image> getAllCdhImages(String accountId, Long workspaceId, String imageCatalogName, Set<ImageCatalogPlatform> provider)
            throws CloudbreakImageCatalogException {
        return getImages(accountId, workspaceId, imageCatalogName, provider, null, false, false, null)
                .getImages().getCdhImages();
    }

    @SuppressWarnings("checkstyle:ParameterNumber")
    StatedImages getImages(User user, Long workspaceId, String imageCatalogName, String runtimeVersion,
            Set<ImageCatalogPlatform> providers, boolean applyVersionBasedFiltering, boolean defaultOnly, String architecture)
            throws CloudbreakImageCatalogException {
        String accountId = Crn.safeFromString(user.getUserCrn()).getAccountId();
        return getImages(accountId, workspaceId, imageCatalogName, providers, runtimeVersion, applyVersionBasedFiltering, defaultOnly, architecture);
    }

    @SuppressWarnings("checkstyle:ParameterNumber")
    StatedImages getImages(String accountId, Long workspaceId, String imageCatalogName,
            Set<ImageCatalogPlatform> providers, String runtimeVersion, boolean applyVersionBasedFiltering, boolean defaultOnly, String architecture)
            throws CloudbreakImageCatalogException {
        try {
            ImageCatalog imageCatalog = getImageCatalogByName(workspaceId, imageCatalogName);
            if (isCustomImageCatalog(imageCatalog)) {
                return getStatedImagesFromCustomImageCatalog(imageCatalog, providers);
            } else {
                ImageFilter imageFilter = ImageFilter.builder()
                        .withImageCatalog(imageCatalog)
                        .withPlatforms(providers)
                        .withCbVersion(applyVersionBasedFiltering ? cbVersion : null)
                        .withBaseImageEnabled(baseImageEnabled(accountId))
                        .withClusterVersion(runtimeVersion)
                        .withDefaultOnly(defaultOnly)
                        .withArchitecture(architecture == null ? null : Architecture.fromStringWithFallback(architecture))
                        .build();
                return getImages(imageFilter);
            }
        } catch (NotFoundException ignore) {
            throw new CloudbreakImageCatalogException(String.format("The %s catalog does not exist or does not belongs to your account.", imageCatalogName));
        }
    }

    public boolean isCustomImageCatalog(ImageCatalog imageCatalog) {
        return imageCatalog != null && Strings.isNullOrEmpty(imageCatalog.getImageCatalogUrl()) && imageCatalog.getCustomImages() != null;
    }

    private StatedImages getStatedImagesFromCustomImageCatalog(ImageCatalog imageCatalog,
            Set<ImageCatalogPlatform> providers) throws CloudbreakImageCatalogException {
        try {
            List<Image> cbImages = getImages(ImageType.RUNTIME, imageCatalog, providers);
            List<Image> freeIpaImages = getImages(ImageType.FREEIPA, imageCatalog, providers);
            return statedImages(new Images(null, cbImages, freeIpaImages,
                    Set.of(cbVersion)), imageCatalog.getImageCatalogUrl(), imageCatalog.getName());
        } catch (CloudbreakImageNotFoundException ex) {
            throw new CloudbreakImageCatalogException(ex.getMessage());
        }
    }

    private List<Image> getImages(ImageType imageType, ImageCatalog imageCatalog, Set<ImageCatalogPlatform> providers)
            throws CloudbreakImageNotFoundException, CloudbreakImageCatalogException {
        List<Image> images = new ArrayList<>();
        for (CustomImage customImage : imageCatalog.getCustomImages()) {
            if (imageType == customImage.getImageType()) {
                StatedImage sourceImage = getSourceImageByImageType(customImage);
                Optional<String> provider = sourceImage.getImage().getImageSetsByProvider().keySet().stream().findFirst();
                provider.ifPresent(p -> {
                    if (providers.stream().anyMatch(prv -> prv.name().equalsIgnoreCase(p))) {
                        images.add(customImageProvider.mergeSourceImageAndCustomImageProperties(
                                sourceImage, customImage, imageCatalog.getImageCatalogUrl(), imageCatalog.getName()).getImage());
                    }
                });
            }
        }
        return images;
    }

    public StatedImage getImage(Long workspaceId, String imageId) throws CloudbreakImageNotFoundException, CloudbreakImageCatalogException {
        return getImage(workspaceId, defaultCatalogUrl, CDP_DEFAULT_CATALOG_NAME, imageId);
    }

    public StatedImage getImage(Long workspaceId, String catalogUrl, String catalogName, String imageId) throws CloudbreakImageNotFoundException,
            CloudbreakImageCatalogException {
        ImageCatalog imageCatalog = getImageCatalogByNameIfUrlIsEmpty(workspaceId, catalogUrl, catalogName);
        if (isCustomImageCatalog(imageCatalog)) {
            LOGGER.debug(String.format("'%s' image catalog is a custom image catalog.", catalogName));
            return getCustomStatedImage(imageCatalog, imageId);
        } else {
            LOGGER.debug(String.format("'%s' image catalog is not a custom image catalog, we should lookup images by image catalog url '%s'.",
                    catalogName, catalogUrl));
            return getImageByUrl(catalogUrl, catalogName, imageId);
        }
    }

    private ImageCatalog getImageCatalogByNameIfUrlIsEmpty(Long workspaceId, String catalogUrl, String catalogName) {
        if (Strings.isNullOrEmpty(catalogUrl)) {
            try {
                return getImageCatalogByName(workspaceId, catalogName);
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
            return getImage(workspaceId, imageId);
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
        ownerAssignmentService.notifyResourceDeleted(imageCatalog.getResourceCrn());
        userProfileHandler.destroyProfileImageCatalogPreparation(imageCatalog);
        LOGGER.debug("Image catalog has been archived: {}", imageCatalog);
        return imageCatalog;
    }

    public ImageCatalog getImageCatalogByName(Long workspaceId, String name) {
        return isEnvDefault(name) ? getDefaultCatalog(name) : getByNameForWorkspaceId(name, workspaceId);
    }

    public boolean isEnvDefault(String name) {
        return CDP_DEFAULT_CATALOG_NAME.equals(name)
                || legacyCatalogEnabled && CLOUDBREAK_DEFAULT_CATALOG_NAME.equals(name);
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

    public ImageCatalog getCloudbreakDefaultImageCatalog() {
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

    StatedImages getImages(ImageFilter imageFilter) throws CloudbreakImageCatalogException {
        LOGGER.info("Determine images for {}", imageFilter);
        validateRequestPlatforms(imageFilter.getPlatforms());

        CloudbreakImageCatalogV3 imageCatalogV3 = imageCatalogProvider.getImageCatalogV3(imageFilter.getImageCatalog().getImageCatalogUrl());
        if (imageCatalogV3 != null) {
            LOGGER.info("Image catalog found, filtering the images..");
            StatedImages images = imageCatalogServiceProxy.getImages(imageCatalogV3, imageFilter);
            if (imageFilter.isDefaultOnly()) {
                images = filterImages(images, "default only", Image::isDefaultImage);
            }
            if (imageFilter.getArchitecture() != null) {
                String filterName = "architecture=" + imageFilter.getArchitecture();
                images = filterImages(images, filterName, isMatchingArchitecture(imageFilter.getArchitecture()));
            }
            if (!CollectionUtils.isEmpty(imageFilter.getOperatingSystems())) {
                String filterName = String.format("operating system in (%s)", String.join(",", imageFilter.getOperatingSystems()));
                images = filterImages(images, filterName, isMatchingOs(imageFilter.getOperatingSystems()));
            }
            boolean rhel9Enabled = entitlementService.isEntitledToUseOS(ThreadBasedUserCrnProvider.getAccountId(), RHEL9);
            if (!rhel9Enabled) {
                String filterName = "architecture=redhat9";
                images = filterImages(images, filterName, isMatchingOs(Collections.singleton(RHEL9.getOs())).negate());
            }
            if (!Strings.isNullOrEmpty(imageFilter.getClusterVersion())) {
                String filterName = "runtime version=" + imageFilter.getClusterVersion();
                images = filterImages(images, filterName, filterImagesByRuntimeVersion(imageFilter.getClusterVersion()));
            }
            if (imageFilter.getAdditionalPredicate() != null) {
                images = filterImages(images, "additional predicate", imageFilter.getAdditionalPredicate());
            }
            return images;
        } else {
            LOGGER.warn("Image catalog {} not found, returning empty response", imageFilter.getImageCatalog());
            return statedImages(emptyImages(),
                    imageFilter.getImageCatalog().getImageCatalogUrl(),
                    imageFilter.getImageCatalog().getName());
        }
    }

    private static Predicate<Image> isMatchingArchitecture(Architecture architecture) {
        return image -> Architecture.fromStringWithFallback(image.getArchitecture()) == architecture;
    }

    private static Predicate<Image> filterImagesByRuntimeVersion(String clusterVersion) {
        return image -> {
            ImageStackDetails stackDetails = image.getStackDetails();
            if (stackDetails != null) {
                String[] repoIdParts = stackDetails.getRepo().getStack().get("repoid").split("-");
                return repoIdParts.length > 1 && repoIdParts[1].equals(clusterVersion);
            } else {
                // base images should not be filtered for runtime
                return true;
            }
        };
    }

    private StatedImages filterImages(StatedImages images, String filterName, Predicate<Image> imageFilterPredicate) {
        Images rawImages = images.getImages();
        List<Image> baseImagesFiltered = filterImages(rawImages.getBaseImages(), filterName, imageFilterPredicate);
        List<Image> cdhImagesFiltered = filterImages(rawImages.getCdhImages(), filterName, imageFilterPredicate);
        return statedImages(new Images(baseImagesFiltered, cdhImagesFiltered, rawImages.getFreeIpaImages(),
                rawImages.getSupportedVersions()), images.getImageCatalogUrl(), images.getImageCatalogName());
    }

    private List<Image> filterImages(List<Image> images, String filterName, Predicate<Image> imageFilterPredicate) {
        Map<Boolean, List<Image>> filteredImages = images.stream().collect(partitioningBy(imageFilterPredicate));
        if (!filteredImages.get(false).isEmpty()) {
            LOGGER.debug("Used filter: {} | Images filtered: {}", filterName,
                    filteredImages.get(false).stream().map(Image::shortDescriptionFormat).collect(Collectors.joining(", ")));
        }
        return filteredImages.get(true);
    }

    private void validateRequestPlatforms(Set<ImageCatalogPlatform> platforms) throws CloudbreakImageCatalogException {
        Set<String> collect = platforms.stream()
                .filter(requestedPlatform -> preferencesService.enabledPlatforms().stream()
                        .filter(enabledPlatform -> enabledPlatform.equalsIgnoreCase(requestedPlatform.nameToLowerCase()))
                        .collect(Collectors.toSet()).isEmpty())
                .collect(Collectors.toSet())
                .stream()
                .filter(requestedPlatform -> preferencesService.enabledGovPlatforms().stream()
                        .filter(enabledPlatform -> enabledPlatform
                                .equalsIgnoreCase(requestedPlatform.nameToLowerCase()
                                        .replace(CommonGovService.GOV, "")))
                        .collect(Collectors.toSet()).isEmpty())
                .collect(Collectors.toSet())
                .stream()
                .map(ImageCatalogPlatform::nameToLowerCase)
                .collect(Collectors.toSet());
        if (!collect.isEmpty()) {
            throw new CloudbreakImageCatalogException(String.format("Platform(s) %s are not supported by the current catalog",
                    StringUtils.join(collect, ",")));
        }
    }

    public Images propagateImagesIfRequested(Long workspaceId, String name, Boolean withImages) {
        return propagateImagesIfRequested(workspaceId, name, withImages, true);
    }

    public Images propagateImagesIfRequested(Long workspaceId, String name, Boolean withImages, Boolean applyVersionBasedFiltering) {
        if (BooleanUtils.isTrue(withImages)) {
            Set<ImageCatalogPlatform> platforms = new HashSet<>();
            platforms.addAll(preferencesService.enabledPlatforms()
                    .stream()
                    .map(ImageCatalogPlatform::imageCatalogPlatform)
                    .collect(Collectors.toSet()));
            platforms.addAll(preferencesService.enabledGovPlatforms()
                    .stream()
                    .map(e -> imageCatalogPlatform(e.concat("_GOV").toUpperCase(Locale.ROOT)))
                    .collect(Collectors.toSet()));
            try {
                return getImages(getLoggedInUser(), workspaceId, name, null, platforms, applyVersionBasedFiltering, false, null).getImages();
            } catch (CloudbreakImageCatalogException e) {
                LOGGER.info("No images was found: ", e);
            }
        }
        return null;
    }

    public boolean baseImageEnabled() {
        String accountId = Crn.safeFromString(getLoggedInUser().getUserCrn()).getAccountId();
        return baseImageEnabled(accountId);
    }

    public boolean baseImageEnabled(String accountId) {
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
        return img -> operatingSystems.stream().anyMatch(os -> os.equalsIgnoreCase(img.getOs()) || os.equalsIgnoreCase(img.getOsType()));
    }

    private Image getLatestImageDefaultPreferred(ImageFilter imageFilter, List<Image> images) throws CloudbreakImageNotFoundException {
        List<Image> defaultImages = images.stream().filter(Image::isDefaultImage).collect(toList());
        List<Image> comparableImages = !defaultImages.isEmpty() ? defaultImages : images;
        return comparableImages.stream()
                .max(imageComparator)
                .orElseThrow(() -> new CloudbreakImageNotFoundException(imageNotFoundErrorMessage(imageFilter)));
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
                        CDP_DEFAULT_CATALOG_NAME.equals(name) || legacyCatalogEnabled && CLOUDBREAK_DEFAULT_CATALOG_NAME.equals(name)));
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
    public AuthorizationResourceType getSupportedAuthorizationResourceType() {
        return AuthorizationResourceType.IMAGE_CATALOG;
    }

    @Override
    public Map<String, Optional<String>> getNamesByCrnsForMessage(Collection<String> crns) {
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
            LOGGER.info("Custom image is available with id '{}'. Searching for source image '{}'.", imageId, customImage.getCustomizedImageId());
            StatedImage sourceImage = getSourceImageByImageType(customImage);
            LOGGER.info("Custom image '{}' is a {} image '{}' customization.", imageId, customImage.getImageType(), customImage.getCustomizedImageId());
            return customImageProvider.mergeSourceImageAndCustomImageProperties(
                    sourceImage, customImage, imageCatalog.getImageCatalogUrl(), imageCatalog.getName());
        } else if (Strings.isNullOrEmpty(imageCatalog.getImageCatalogUrl())) {
            throw new CloudbreakImageNotFoundException(
                    String.format("Could not find any custom image with id: '%s' in catalog: '%s'", imageId, imageCatalog.getName()));
        } else {
            return getImageByUrl(imageCatalog.getImageCatalogUrl(), imageCatalog.getName(), imageId);
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
            case RUNTIME:
                return getImage(customImage.getImageCatalog().getWorkspace().getId(), customImage.getCustomizedImageId());
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
            throw new CloudbreakImageNotFoundException(String.format("Could not find any image with id: '%s' in catalog: '%s'", imageId, catalogName));
        }
        return statedImage(image.get(), catalogUrl, catalogName);
    }

    public List<String> getRuntimeVersionsFromDefault()
            throws CloudbreakImageCatalogException {
        return imageCatalogProvider.getImageCatalogMetaData(defaultCatalogUrl).getRuntimeVersions();
    }

    public ImageFilterResult getImageFilterResult(Long workspaceId, String imageCatalogName, ImageCatalogPlatform imageCatalogPlatform, boolean getAllImages,
            String currentImageId) throws CloudbreakImageCatalogException {
        ImageCatalog imageCatalog = getImageCatalogByName(workspaceId, imageCatalogName);
        if (isCustomImageCatalog(imageCatalog)) {
            StatedImages statedImages = getImages(workspaceId, imageCatalogName, null, imageCatalogPlatform, !getAllImages, null);
            return new ImageFilterResult(statedImages.getImages().getCdhImages());
        } else {
            CloudbreakImageCatalogV3 v3ImageCatalog = imageCatalogProvider.getImageCatalogV3(imageCatalog.getImageCatalogUrl());
            if (getAllImages) {
                return new ImageFilterResult(v3ImageCatalog.getImages().getCdhImages());
            } else {
                Set<String> defaultImages = v3ImageCatalog.getVersions().getCloudbreakVersions().stream()
                        .map(CloudbreakVersion::getDefaults)
                        .flatMap(List::stream)
                        .collect(Collectors.toSet());
                List<Image> images = v3ImageCatalog.getImages().getCdhImages().stream()
                        .filter(image -> defaultImages.contains(image.getUuid()) || currentImageId.equals(image.getUuid()))
                        .toList();
                return new ImageFilterResult(images);
            }
        }
    }
}
