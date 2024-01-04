package com.sequenceiq.datalake.service.rotation.notification;

import java.util.List;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.exception.NotFoundException;
import com.sequenceiq.cloudbreak.event.ResourceEvent;
import com.sequenceiq.cloudbreak.rotation.service.notification.SecretRotationNotificationService;
import com.sequenceiq.datalake.repository.SdxClusterRepository;
import com.sequenceiq.datalake.service.sdx.SdxNotificationService;

@Primary
@Component
public class SdxSecretRotationNotificationService extends SecretRotationNotificationService {

    private static final Logger LOGGER = LoggerFactory.getLogger(SdxSecretRotationNotificationService.class);

    @Inject
    private SdxClusterRepository sdxClusterRepository;

    @Inject
    private SdxNotificationService sdxNotificationService;

    @Override
    protected void send(String resourceCrn, String message) {
        sdxClusterRepository.findByCrnAndDeletedIsNull(resourceCrn)
                .ifPresentOrElse(cluster -> {
                    LOGGER.info("Send rotation step notification {}", message);
                    sdxNotificationService.send(ResourceEvent.SECRET_ROTATION_STEP, List.of(message), cluster);
                }, () -> {
                    throw new NotFoundException("SdxCluster was not found with crn: " + resourceCrn);
                });
    }
}
