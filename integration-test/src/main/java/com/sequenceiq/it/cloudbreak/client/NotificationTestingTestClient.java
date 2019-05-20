package com.sequenceiq.it.cloudbreak.client;

import org.springframework.stereotype.Service;

import com.sequenceiq.it.cloudbreak.action.Action;
import com.sequenceiq.it.cloudbreak.action.v4.notificationtest.NotificationTestingAction;
import com.sequenceiq.it.cloudbreak.dto.util.NotificationTestingTestDto;

@Service
public class NotificationTestingTestClient {

    public Action<NotificationTestingTestDto> notificationTesting() {
        return new NotificationTestingAction();
    }

}