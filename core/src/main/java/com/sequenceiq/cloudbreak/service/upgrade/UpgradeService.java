package com.sequenceiq.cloudbreak.service.upgrade;

import static com.sequenceiq.cloudbreak.exception.NotFoundException.notFoundException;

import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.google.common.collect.Sets;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.image.ImageSettingsV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackViewV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackViewV4Responses;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.UpgradeOption;
import com.sequenceiq.cloudbreak.cloud.model.Image;
import com.sequenceiq.cloudbreak.common.service.TransactionService;
import com.sequenceiq.cloudbreak.common.service.TransactionService.TransactionExecutionException;
import com.sequenceiq.cloudbreak.common.service.TransactionService.TransactionRuntimeExecutionException;
import com.sequenceiq.cloudbreak.core.CloudbreakImageCatalogException;
import com.sequenceiq.cloudbreak.core.CloudbreakImageNotFoundException;
import com.sequenceiq.cloudbreak.domain.Blueprint;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.exception.BadRequestException;
import com.sequenceiq.cloudbreak.service.ComponentConfigProviderService;
import com.sequenceiq.cloudbreak.service.cluster.ClusterService;
import com.sequenceiq.cloudbreak.service.image.ImageCatalogService;
import com.sequenceiq.cloudbreak.service.image.ImageService;
import com.sequenceiq.cloudbreak.service.image.StatedImage;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.cloudbreak.workspace.model.User;
import com.sequenceiq.distrox.api.v1.distrox.endpoint.DistroXV1Endpoint;

@Component
public class UpgradeService {

    private static final Set<Status> UPGRADEABLE_ATTACHED_DISTRO_X_STATES = Sets.immutableEnumSet(Status.STOPPED, Status.DELETE_COMPLETED);

    @Inject
    private StackService stackService;

    @Inject
    private ImageService imageService;

    @Inject
    private ImageCatalogService imageCatalogService;

    @Inject
    private ComponentConfigProviderService componentConfigProviderService;

    @Inject
    private ClusterService clusterService;

    @Inject
    private DistroXV1Endpoint distroXV1Endpoint;

    @Inject
    private TransactionService transactionService;

    public UpgradeOption getUpgradeOptionByName(Long workspaceId, String name, User user) {
        try {
            return transactionService.required(() -> {
                Optional<Stack> stack = stackService.findStackByNameAndWorkspaceId(name, workspaceId);
                if (stack.isPresent()) {
                    return getUpgradeOption(stack.get(), workspaceId, user);
                } else {
                    throw notFoundException("Stack", name);
                }
            });
        } catch (TransactionExecutionException e) {
            throw new TransactionRuntimeExecutionException(e);
        }
    }

    private UpgradeOption getUpgradeOption(Stack stack, Long workspaceId, User user) {
        if (clusterService.repairSupported(stack) && attachedClustersStoppedOrDeleted(stack)) {
            try {
                Blueprint blueprint = stack.getCluster().getBlueprint();
                Image image = componentConfigProviderService.getImage(stack.getId());
                ImageSettingsV4Request imageSettingsV4Request = toImageSettingsRequest(image);
                boolean baseImage = useBaseImage(image);
                StatedImage latestImage = imageService
                        .determineImageFromCatalog(workspaceId, imageSettingsV4Request, stack.getCloudPlatform().toLowerCase(), blueprint, baseImage, user);
                if (!Objects.equals(image.getImageId(), latestImage.getImage().getUuid())) {
                    return new UpgradeOption(latestImage.getImage().getUuid(), latestImage.getImageCatalogName());
                } else {
                    return new UpgradeOption();
                }
            } catch (CloudbreakImageNotFoundException | CloudbreakImageCatalogException e) {
                throw new BadRequestException(e.getMessage());
            }
        } else {
            return new UpgradeOption();
        }
    }

    private boolean useBaseImage(Image image) throws CloudbreakImageNotFoundException, CloudbreakImageCatalogException {
        return !imageCatalogService.getImage(image.getImageCatalogUrl(), image.getImageCatalogName(), image.getImageId()).getImage().isPrewarmed();
    }

    private boolean attachedClustersStoppedOrDeleted(Stack stack) {
        StackViewV4Responses stackViewV4Responses = distroXV1Endpoint.list(null, stack.getEnvironmentCrn());
        for (StackViewV4Response stackViewV4Response : stackViewV4Responses.getResponses()) {
            if (!(UPGRADEABLE_ATTACHED_DISTRO_X_STATES.contains(stack.getStatus())
                    || UPGRADEABLE_ATTACHED_DISTRO_X_STATES.contains(getClusterStatus(stackViewV4Response)))) {
                return false;
            }
        }
        return true;
    }

    private Status getClusterStatus(StackViewV4Response stack) {
        if (stack.getCluster() == null) {
            return null;
        } else {
            return stack.getCluster().getStatus();
        }
    }

    private ImageSettingsV4Request toImageSettingsRequest(Image image) {
        ImageSettingsV4Request imageSettingsV4Request = new ImageSettingsV4Request();
        imageSettingsV4Request.setOs(image.getOs());
        imageSettingsV4Request.setCatalog(image.getImageCatalogUrl());
        return imageSettingsV4Request;
    }
}
