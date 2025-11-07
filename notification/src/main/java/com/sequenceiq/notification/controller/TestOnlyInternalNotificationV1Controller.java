package com.sequenceiq.notification.controller;

import java.util.List;

import jakarta.inject.Inject;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Controller;

import com.sequenceiq.authorization.annotation.AccountIdNotNeeded;
import com.sequenceiq.authorization.annotation.InternalOnly;
import com.sequenceiq.cloudbreak.auth.security.internal.RequestObject;
import com.sequenceiq.notification.domain.DistributionList;
import com.sequenceiq.notification.domain.test.TestOnlyInternalRegisterAzureOutboundNotificationRequest;
import com.sequenceiq.notification.endpoint.TestOnlyInternalNotificationV1Endpoint;
import com.sequenceiq.notification.service.TestOnlyInternalNotificationService;

@Controller
@ConditionalOnProperty(name = "thunderheadnotification.enabled", havingValue = "true")
public class TestOnlyInternalNotificationV1Controller implements TestOnlyInternalNotificationV1Endpoint {

    @Inject
    private TestOnlyInternalNotificationService testOnlyInternalNotificationService;

    @Override
    @InternalOnly
    @AccountIdNotNeeded
    public void testSendWeeklyNotification(@RequestObject TestOnlyInternalRegisterAzureOutboundNotificationRequest testOnly) {
        testOnlyInternalNotificationService.testSendWeeklyNotification(testOnly);
    }

    @Override
    @InternalOnly
    @AccountIdNotNeeded
    public void testRegisterAzureDefaultOutbound(@RequestObject TestOnlyInternalRegisterAzureOutboundNotificationRequest request) {
        testOnlyInternalNotificationService.testRegisterAzureDefaultOutbound(request);
    }

    @Override
    @InternalOnly
    @AccountIdNotNeeded
    public void testCreateOrUpdateDistributionLists(String resourceCrn) {
        testOnlyInternalNotificationService.testCreateOrUpdateDistributionLists(resourceCrn);
    }

    @Override
    @InternalOnly
    @AccountIdNotNeeded
    public void testDeleteDistributionLists(String resourceCrn) {
        testOnlyInternalNotificationService.testDeleteDistributionLists(resourceCrn);
    }

    @Override
    @InternalOnly
    @AccountIdNotNeeded
    public List<DistributionList> testListDistributionLists(String resourceCrn) {
        return testOnlyInternalNotificationService.testListDistributionLists(resourceCrn);
    }

}
