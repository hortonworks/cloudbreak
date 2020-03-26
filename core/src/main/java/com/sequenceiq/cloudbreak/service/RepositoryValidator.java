package com.sequenceiq.cloudbreak.service;

import java.io.IOException;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.api.model.stack.StackDescriptor;
import com.sequenceiq.cloudbreak.cloud.VersionComparator;
import com.sequenceiq.cloudbreak.cloud.model.AmbariRepo;
import com.sequenceiq.cloudbreak.cloud.model.Image;
import com.sequenceiq.cloudbreak.cloud.model.component.StackRepoDetails;
import com.sequenceiq.cloudbreak.controller.exception.BadRequestException;
import com.sequenceiq.cloudbreak.domain.stack.Component;
import com.sequenceiq.cloudbreak.domain.stack.cluster.ClusterComponent;

@Service
public class RepositoryValidator {

    private static final Logger LOGGER = LoggerFactory.getLogger(RepositoryValidator.class);

    @Inject
    private StackMatrixService stackMatrixService;

    public void checkRepositories(ClusterComponent ambariRepoComponent, ClusterComponent stackRepoComponent, Component imageComponent, boolean strictCheck)
            throws IOException {
        AmbariRepo ambariRepo = ambariRepoComponent.getAttributes().get(AmbariRepo.class);
        StackRepoDetails stackRepoDetails = stackRepoComponent.getAttributes().get(StackRepoDetails.class);
        Image image = imageComponent.getAttributes().get(Image.class);
        String stackVersion = stackRepoDetails.getHdpVersion();
        String stackType = getStackType(stackRepoDetails);

        StackDescriptor stackDescriptor = stackMatrixService.getStackDescriptor(stackType, stackVersion);
        if (stackDescriptor != null) {
            boolean hasDefaultStackRepoUrlForOsType = stackDescriptor.getRepo().getStack().containsKey(image.getOsType());
            boolean hasDefaultAmbariRepoUrlForOsType = stackDescriptor.getAmbari().getRepo().containsKey(image.getOsType());
            boolean compatibleAmbari = isCompatibleAmbari(ambariRepo, stackDescriptor);
            if (!hasDefaultAmbariRepoUrlForOsType || !hasDefaultStackRepoUrlForOsType || !compatibleAmbari) {
                String message = String.format("The given repository information seems to be incompatible."
                        + " Ambari version: %s, Stack type: %s, Stack version: %s, Image Id: %s, Os type: %s.", ambariRepo.getVersion(),
                        stackType, stackVersion, image.getImageId(), image.getOsType());
                if (strictCheck) {
                    LOGGER.error(message);
                    throw new BadRequestException(message);
                } else {
                    LOGGER.warn(message);
                }
            }
        }
    }

    private boolean isCompatibleAmbari(AmbariRepo ambariRepo, StackDescriptor stackDescriptor) {
        return new VersionComparator().compare(() -> ambariRepo.getVersion().substring(0, stackDescriptor.getMinAmbari().length()),
                stackDescriptor::getMinAmbari) >= 0;
    }

    private String getStackType(StackRepoDetails stackRepoDetails) {
        String stackType = stackRepoDetails.getStack().get(StackRepoDetails.REPO_ID_TAG);
        return stackType.contains("-") ? stackType.substring(0, stackType.indexOf('-')) : stackType;
    }
}
