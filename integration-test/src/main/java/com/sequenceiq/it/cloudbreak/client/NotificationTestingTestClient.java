package com.sequenceiq.it.cloudbreak.client;

import org.springframework.stereotype.Service;

import com.sequenceiq.it.cloudbreak.action.Action;
import com.sequenceiq.it.cloudbreak.action.v4.notificationtest.NotificationTestingAction;
import com.sequenceiq.it.cloudbreak.dto.util.NotificationTestingTestDto;
import com.sequenceiq.it.cloudbreak.microservice.CloudbreakClient;

@Service
public class NotificationTestingTestClient {

    public Action<NotificationTestingTestDto, CloudbreakClient> notificationTesting() {
        return new NotificationTestingAction();
    }

}