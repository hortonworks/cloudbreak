package com.sequenceiq.cloudbreak.controller.v4;

import static com.sequenceiq.authorization.resource.AuthorizationResourceAction.EDIT_IMAGE_CATALOG;
import static com.sequenceiq.authorization.resource.AuthorizationVariableType.CRN;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.transaction.Transactional.TxType;

import org.springframework.stereotype.Controller;
import org.springframework.util.StringUtils;

import com.sequenceiq.authorization.annotation.AccountIdNotNeeded;
import com.sequenceiq.authorization.annotation.CheckPermissionByAccount;
import com.sequenceiq.authorization.annotation.CheckPermissionByRequestProperty;
import com.sequenceiq.authorization.annotation.CheckPermissionByResourceCrn;
import com.sequenceiq.authorization.annotation.CheckPermissionByResourceName;
import com.sequenceiq.authorization.annotation.CheckPermissionByResourceNameList;
import com.sequenceiq.authorization.annotation.DisableCheckPermissions;
import com.sequenceiq.authorization.annotation.FilterListBasedOnPermissions;
import com.sequenceiq.authorization.annotation.InternalOnly;
import com.sequenceiq.authorization.annotation.ResourceName;
import com.sequenceiq.authorization.annotation.ResourceNameList;
import com.sequenceiq.authorization.resource.AuthorizationResourceAction;
import com.sequenceiq.cloudbreak.api.endpoint.v4.dto.NameOrCrn;
import com.sequenceiq.cloudbreak.api.endpoint.v4.imagecatalog.ImageCatalogV4Endpoint;
import com.sequenceiq.cloudbreak.api.endpoint.v4.imagecatalog.requests.ImageCatalogV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.imagecatalog.requests.ImageRecommendationV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.imagecatalog.requests.UpdateImageCatalogV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.imagecatalog.responses.ImageCatalogV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.imagecatalog.responses.ImageCatalogV4Responses;
import com.sequenceiq.cloudbreak.api.endpoint.v4.imagecatalog.responses.ImageRecommendationV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.imagecatalog.responses.ImageV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.imagecatalog.responses.ImagesV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.imagecatalog.responses.RuntimeVersionsV4Response;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.security.internal.AccountId;
import com.sequenceiq.cloudbreak.auth.security.internal.InitiatorUserCrn;
import com.sequenceiq.cloudbreak.auth.security.internal.RequestObject;
import com.sequenceiq.cloudbreak.auth.security.internal.ResourceCrn;
import com.sequenceiq.cloudbreak.authorization.ImageCatalogFiltering;
import com.sequenceiq.cloudbreak.cloud.model.catalog.Images;
import com.sequenceiq.cloudbreak.common.service.PlatformStringTransformer;
import com.sequenceiq.cloudbreak.common.type.ResourceEvent;
import com.sequenceiq.cloudbreak.controller.validation.RecommendedImageValidator;
import com.sequenceiq.cloudbreak.converter.UpdateImageCatalogRequestToImageCatalogConverter;
import com.sequenceiq.cloudbreak.converter.v4.imagecatalog.ImageCatalogToImageCatalogV4RequestConverter;
import com.sequenceiq.cloudbreak.converter.v4.imagecatalog.ImageCatalogToImageCatalogV4ResponseConverter;
import com.sequenceiq.cloudbreak.converter.v4.imagecatalog.ImageCatalogV4RequestToImageCatalogConverter;
import com.sequenceiq.cloudbreak.converter.v4.imagecatalog.ImageToImageV4ResponseConverter;
import com.sequenceiq.cloudbreak.converter.v4.imagecatalog.ImagesToImagesV4ResponseConverter;
import com.sequenceiq.cloudbreak.domain.ImageCatalog;
import com.sequenceiq.cloudbreak.service.image.DefaultImageCatalogService;
import com.sequenceiq.cloudbreak.service.image.ImageCatalogService;
import com.sequenceiq.cloudbreak.service.image.StatedImage;
import com.sequenceiq.cloudbreak.structuredevent.CloudbreakRestRequestThreadLocalService;
import com.sequenceiq.cloudbreak.workspace.controller.WorkspaceEntityType;
import com.sequenceiq.common.model.Architecture;

@Controller
@Transactional(TxType.NEVER)
@WorkspaceEntityType(ImageCatalog.class)
public class ImageCatalogV4Controller extends NotificationController implements ImageCatalogV4Endpoint {

    @Inject
    private ImageCatalogService imageCatalogService;

    @Inject
    private DefaultImageCatalogService defaultImageCatalogService;

    @Inject
    private ImageCatalogFiltering imageCatalogFiltering;

    @Inject
    private CloudbreakRestRequestThreadLocalService restRequestThreadLocalService;

    @Inject
    private ImageCatalogToImageCatalogV4ResponseConverter imageCatalogToImageCatalogV4ResponseConverter;

    @Inject
    private ImagesToImagesV4ResponseConverter imagesToImagesV4ResponseConverter;

    @Inject
    private ImageToImageV4ResponseConverter imageToImageV4ResponseConverter;

    @Inject
    private ImageCatalogToImageCatalogV4RequestConverter imageCatalogToImageCatalogV4RequestConverter;

    @Inject
    private ImageCatalogV4RequestToImageCatalogConverter imageCatalogV4RequestToImageCatalogConverter;

    @Inject
    private UpdateImageCatalogRequestToImageCatalogConverter updateImageCatalogRequestToImageCatalogConverter;

    @Inject
    private PlatformStringTransformer platformStringTransformer;

    @Inject
    private RecommendedImageValidator recommendedImageValidator;

    @Override
    @FilterListBasedOnPermissions
    public ImageCatalogV4Responses list(Long workspaceId, boolean customCatalogsOnly) {
        Set<ImageCatalog> allByWorkspaceId = imageCatalogFiltering.filterImageCatalogs(AuthorizationResourceAction.DESCRIBE_IMAGE_CATALOG, customCatalogsOnly);
        return new ImageCatalogV4Responses(allByWorkspaceId.stream()
                .map(i -> imageCatalogToImageCatalogV4ResponseConverter.convert(i))
                .collect(Collectors.toSet())
        );
    }

    @Override
    @CheckPermissionByResourceName(action = AuthorizationResourceAction.DESCRIBE_IMAGE_CATALOG)
    public ImageCatalogV4Response getByName(Long workspaceId, @ResourceName String name, Boolean withImages, Boolean applyVersionBasedFiltering) {
        ImageCatalog catalog = imageCatalogService.getImageCatalogByName(NameOrCrn.ofName(name), restRequestThreadLocalService.getRequestedWorkspaceId());
        ImageCatalogV4Response imageCatalogResponse = imageCatalogToImageCatalogV4ResponseConverter.convert(catalog);
        Images images = imageCatalogService.propagateImagesIfRequested(restRequestThreadLocalService.getRequestedWorkspaceId(), name,
                withImages, applyVersionBasedFiltering);
        if (images != null) {
            imageCatalogResponse.setImages(imagesToImagesV4ResponseConverter.convert(images));
        }
        return imageCatalogResponse;
    }

    @Override
    @InternalOnly
    public ImageCatalogV4Response getByNameInternal(Long workspaceId, @ResourceName String name, Boolean withImages, Boolean applyVersionBasedFiltering,
            @InitiatorUserCrn String initiatorUserCrn) {
        ImageCatalog catalog = imageCatalogService.getImageCatalogByName(NameOrCrn.ofName(name), restRequestThreadLocalService.getRequestedWorkspaceId());
        ImageCatalogV4Response imageCatalogResponse = imageCatalogToImageCatalogV4ResponseConverter.convert(catalog);
        Images images = imageCatalogService.propagateImagesIfRequested(restRequestThreadLocalService.getRequestedWorkspaceId(), name,
                withImages, applyVersionBasedFiltering);
        if (images != null) {
            imageCatalogResponse.setImages(imagesToImagesV4ResponseConverter.convert(images));
        }
        return imageCatalogResponse;
    }

    @Override
    @CheckPermissionByResourceCrn(action = AuthorizationResourceAction.DESCRIBE_IMAGE_CATALOG)
    public ImageCatalogV4Response getByCrn(Long workspaceId, @ResourceCrn String crn, Boolean withImages) {
        ImageCatalog catalog = imageCatalogService.getImageCatalogByName(NameOrCrn.ofCrn(crn), restRequestThreadLocalService.getRequestedWorkspaceId());
        ImageCatalogV4Response imageCatalogResponse = imageCatalogToImageCatalogV4ResponseConverter.convert(catalog);
        Images images = imageCatalogService.propagateImagesIfRequested(restRequestThreadLocalService.getRequestedWorkspaceId(), catalog.getName(), withImages);
        if (images != null) {
            imageCatalogResponse.setImages(imagesToImagesV4ResponseConverter.convert(images));
        }
        return imageCatalogResponse;
    }

    @Override
    @CheckPermissionByAccount(action = AuthorizationResourceAction.CREATE_IMAGE_CATALOG)
    public ImageCatalogV4Response create(Long workspaceId, ImageCatalogV4Request request) {
        String accountId = ThreadBasedUserCrnProvider.getAccountId();
        String creator = ThreadBasedUserCrnProvider.getUserCrn();
        ImageCatalog catalogToSave = imageCatalogV4RequestToImageCatalogConverter.convert(request);
        ImageCatalog imageCatalog = imageCatalogService.createForLoggedInUser(catalogToSave, restRequestThreadLocalService.getRequestedWorkspaceId(),
                accountId, creator);
        notify(ResourceEvent.IMAGE_CATALOG_CREATED);
        return imageCatalogToImageCatalogV4ResponseConverter.convert(imageCatalog);
    }

    @Override
    @CheckPermissionByResourceName(action = AuthorizationResourceAction.DELETE_IMAGE_CATALOG)
    public ImageCatalogV4Response deleteByName(Long workspaceId, @ResourceName String name) {
        ImageCatalog deleted = imageCatalogService.delete(NameOrCrn.ofName(name), restRequestThreadLocalService.getRequestedWorkspaceId());
        notify(ResourceEvent.IMAGE_CATALOG_DELETED);
        return imageCatalogToImageCatalogV4ResponseConverter.convert(deleted);
    }

    @Override
    @CheckPermissionByResourceCrn(action = AuthorizationResourceAction.DELETE_IMAGE_CATALOG)
    public ImageCatalogV4Response deleteByCrn(Long workspaceId, @ResourceCrn String crn) {
        ImageCatalog deleted = imageCatalogService.delete(NameOrCrn.ofCrn(crn), restRequestThreadLocalService.getRequestedWorkspaceId());
        notify(ResourceEvent.IMAGE_CATALOG_DELETED);
        return imageCatalogToImageCatalogV4ResponseConverter.convert(deleted);
    }

    @Override
    @CheckPermissionByResourceNameList(action = AuthorizationResourceAction.DELETE_IMAGE_CATALOG)
    public ImageCatalogV4Responses deleteMultiple(Long workspaceId, @ResourceNameList Set<String> names) {
        Set<ImageCatalog> deleted = imageCatalogService.deleteMultiple(restRequestThreadLocalService.getRequestedWorkspaceId(), names);
        notify(ResourceEvent.IMAGE_CATALOG_DELETED);
        return new ImageCatalogV4Responses(
                deleted.stream()
                        .map(i -> imageCatalogToImageCatalogV4ResponseConverter.convert(i))
                        .collect(Collectors.toSet())
        );
    }

    @Override
    @CheckPermissionByRequestProperty(path = "crn", type = CRN, action = EDIT_IMAGE_CATALOG)
    public ImageCatalogV4Response update(Long workspaceId, @RequestObject UpdateImageCatalogV4Request request) {
        ImageCatalog imageCatalog = imageCatalogService.update(restRequestThreadLocalService.getRequestedWorkspaceId(),
                updateImageCatalogRequestToImageCatalogConverter.convert(request));
        return imageCatalogToImageCatalogV4ResponseConverter.convert(imageCatalog);
    }

    @Override
    @CheckPermissionByResourceName(action = AuthorizationResourceAction.DESCRIBE_IMAGE_CATALOG)
    public ImageCatalogV4Request getRequest(Long workspaceId, @ResourceName String name) {
        ImageCatalog imageCatalog = imageCatalogService.getImageCatalogByName(restRequestThreadLocalService.getRequestedWorkspaceId(), name);
        return imageCatalogToImageCatalogV4RequestConverter.convert(imageCatalog);
    }

    @Override
    @DisableCheckPermissions
    @SuppressWarnings("checkstyle:ParameterNumber")
    public ImagesV4Response getImages(Long workspaceId, String stackName, String platform,
            String runtimeVersion, String imageType, boolean govCloud, boolean defaultOnly, String architecture) throws Exception {
        Images images = imageCatalogService.getImagesFromDefault(
                restRequestThreadLocalService.getRequestedWorkspaceId(),
                stackName,
                platformStringTransformer.getPlatformStringForImageCatalog(platform, govCloud),
                Collections.emptySet(),
                runtimeVersion,
                govCloud,
                defaultOnly,
                architecture);
        return imagesToImagesV4ResponseConverter.convert(images);
    }

    @Override
    @CheckPermissionByResourceName(action = AuthorizationResourceAction.DESCRIBE_IMAGE_CATALOG)
    @SuppressWarnings("checkstyle:ParameterNumber")
    public ImagesV4Response getImagesByName(Long workspaceId, @ResourceName String name, String stackName, String platform,
            String runtimeVersion, String imageType, boolean govCloud, boolean defaultOnly, String architecture) throws Exception {
        Images images = imageCatalogService.getImagesByCatalogName(
                restRequestThreadLocalService.getRequestedWorkspaceId(),
                name,
                stackName,
                platformStringTransformer.getPlatformStringForImageCatalog(platform, govCloud),
                runtimeVersion,
                govCloud,
                defaultOnly,
                architecture);
        return imagesToImagesV4ResponseConverter.convert(images);
    }

    @Override
    @DisableCheckPermissions
    public ImagesV4Response getImageByImageId(Long workspaceId, String imageId, @AccountId String accountId) throws Exception {
        StatedImage statedImage = imageCatalogService.getImageByCatalogName(restRequestThreadLocalService.getRequestedWorkspaceId(), imageId, "");
        Images images = getImagesFromSingleStatedImage(statedImage);
        return imagesToImagesV4ResponseConverter.convert(images);
    }

    @Override
    @CheckPermissionByResourceName(action = AuthorizationResourceAction.DESCRIBE_IMAGE_CATALOG)
    public ImagesV4Response getImageByCatalogNameAndImageId(Long workspaceId, @ResourceName String name,
            String imageId, @AccountId String accountId) throws Exception {
        StatedImage statedImage = imageCatalogService.getImageByCatalogName(restRequestThreadLocalService.getRequestedWorkspaceId(), imageId, name);
        Images images = getImagesFromSingleStatedImage(statedImage);
        return imagesToImagesV4ResponseConverter.convert(images);
    }

    private Images getImagesFromSingleStatedImage(StatedImage statedImage) {
        return statedImage.getImage().isPrewarmed()
                ? new Images(List.of(), List.of(statedImage.getImage()), List.of(), Set.of())
                : new Images(List.of(statedImage.getImage()), List.of(), List.of(), Set.of());
    }

    @Override
    @InternalOnly
    @CheckPermissionByResourceName(action = AuthorizationResourceAction.DESCRIBE_IMAGE_CATALOG)
    public ImageV4Response getSingleImageByCatalogNameAndImageIdInternal(Long workspaceId, @ResourceName String name, String imageId,
            @AccountId String accountId) throws Exception {
        StatedImage statedImage = imageCatalogService.getImageByCatalogName(restRequestThreadLocalService.getRequestedWorkspaceId(), imageId, name);
        return imageToImageV4ResponseConverter.convert(statedImage.getImage());
    }

    @Override
    @CheckPermissionByResourceName(action = AuthorizationResourceAction.DESCRIBE_IMAGE_CATALOG)
    public ImageV4Response getSingleImageByCatalogNameAndImageId(Long workspaceId, @ResourceName String name, String imageId) throws Exception {
        StatedImage statedImage = imageCatalogService.getImageByCatalogName(restRequestThreadLocalService.getRequestedWorkspaceId(), imageId, name);
        return imageToImageV4ResponseConverter.convert(statedImage.getImage());
    }

    @Override
    @AccountIdNotNeeded
    @DisableCheckPermissions
    public ImageV4Response getImageFromDefaultById(Long workspaceId, @ResourceName String imageId) throws Exception {
        StatedImage statedImage = defaultImageCatalogService.getImageFromDefaultCatalog(workspaceId, imageId);
        return imageToImageV4ResponseConverter.convert(statedImage.getImage());
    }

    @Override
    @AccountIdNotNeeded
    @DisableCheckPermissions
    public ImageV4Response getImageFromDefault(Long workspaceId, String type, String provider, String runtime, boolean govCloud, String architecture)
            throws Exception {
        StatedImage statedImage = defaultImageCatalogService.getImageFromDefaultCatalog(type,
                platformStringTransformer.getPlatformStringForImageCatalog(provider, govCloud), runtime, Architecture.fromStringWithFallback(architecture));
        return imageToImageV4ResponseConverter.convert(statedImage.getImage());
    }

    @Override
    @AccountIdNotNeeded
    @DisableCheckPermissions
    public ImageV4Response getImageFromDefault(Long workspaceId, String type, String provider) throws Exception {
        StatedImage statedImage = defaultImageCatalogService.getImageFromDefaultCatalog(type,
                platformStringTransformer.getPlatformStringForImageCatalog(provider, false));
        return imageToImageV4ResponseConverter.convert(statedImage.getImage());
    }

    @Override
    @AccountIdNotNeeded
    @DisableCheckPermissions
    public RuntimeVersionsV4Response getRuntimeVersionsFromDefault(Long workspaceId) throws Exception {
        List<String> runtimeVersions = imageCatalogService.getRuntimeVersionsFromDefault();
        return new RuntimeVersionsV4Response(runtimeVersions);
    }

    @Override
    @AccountIdNotNeeded
    @DisableCheckPermissions
    public ImageRecommendationV4Response validateRecommendedImageWithProvider(Long workspaceId, ImageRecommendationV4Request request) {
        RecommendedImageValidator.ValidationResult validationResult = recommendedImageValidator.validateRecommendedImage(
                workspaceId,
                restRequestThreadLocalService.getCloudbreakUser(),
                request);
        ImageRecommendationV4Response response = new ImageRecommendationV4Response();
        response.setHasValidationError(StringUtils.hasText(validationResult.getErrorMsg()));
        Stream.of(validationResult.getErrorMsg(), validationResult.getWarningMsg())
                .filter(StringUtils::hasText)
                .findFirst()
                .ifPresent(response::setValidationMessage);
        return response;
    }
}
