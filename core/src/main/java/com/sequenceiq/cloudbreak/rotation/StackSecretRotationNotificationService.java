package com.sequenceiq.cloudbreak.rotation;

import static com.sequenceiq.redbeams.api.model.common.Status.UPDATE_IN_PROGRESS;

import java.util.List;

import jakarta.inject.Inject;

import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.event.ResourceEvent;
import com.sequenceiq.cloudbreak.rotation.service.notification.SecretRotationNotificationService;
import com.sequenceiq.cloudbreak.service.stack.StackDtoService;
import com.sequenceiq.cloudbreak.structuredevent.event.CloudbreakEventService;
import com.sequenceiq.cloudbreak.view.StackView;

@Primary
@Component
public class StackSecretRotationNotificationService extends SecretRotationNotificationService {

    @Inject
    private StackDtoService stackService;

    @Inject
    private CloudbreakEventService cloudbreakEventService;

    @Override
    protected void send(String resourceCrn, String message) {
        StackView stack = stackService.getStackViewByCrn(resourceCrn);
        cloudbreakEventService.fireCloudbreakEvent(stack.getId(), UPDATE_IN_PROGRESS.name(), ResourceEvent.SECRET_ROTATION_STEP, List.of(message));
    }
}
