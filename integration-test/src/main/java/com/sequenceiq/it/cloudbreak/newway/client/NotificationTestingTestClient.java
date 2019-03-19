package com.sequenceiq.it.cloudbreak.newway.client;

import org.springframework.stereotype.Service;

import com.sequenceiq.it.cloudbreak.newway.action.Action;
import com.sequenceiq.it.cloudbreak.newway.action.v4.notificationtest.NotificationTestingAction;
import com.sequenceiq.it.cloudbreak.newway.dto.util.NotificationTestingTestDto;

@Service
public class NotificationTestingTestClient {

    public Action<NotificationTestingTestDto> notificationTesting() {
        return new NotificationTestingAction();
    }

}