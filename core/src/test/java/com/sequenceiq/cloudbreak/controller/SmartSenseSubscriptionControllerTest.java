package com.sequenceiq.cloudbreak.controller;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.security.access.AccessDeniedException;

import com.sequenceiq.cloudbreak.api.model.SmartSenseSubscriptionJson;
import com.sequenceiq.cloudbreak.common.model.user.CloudbreakUser;
import com.sequenceiq.cloudbreak.converter.SmartSenseSubscriptionToSmartSenseSubscriptionJsonConverter;
import com.sequenceiq.cloudbreak.domain.SmartSenseSubscription;
import com.sequenceiq.cloudbreak.service.RestRequestThreadLocalService;
import com.sequenceiq.cloudbreak.service.smartsense.SmartSenseSubscriptionAccessDeniedException;
import com.sequenceiq.cloudbreak.service.smartsense.SmartSenseSubscriptionService;

@RunWith(MockitoJUnitRunner.class)
public class SmartSenseSubscriptionControllerTest {

    @Rule
    public final ExpectedException thrown = ExpectedException.none();

    @InjectMocks
    private SmartSenseSubscriptionController underTest;

    @Mock
    private RestRequestThreadLocalService restRequestThreadLocalService;

    @Mock
    private SmartSenseSubscriptionService smartSenseSubService;

    @Mock
    private SmartSenseSubscriptionToSmartSenseSubscriptionJsonConverter toJsonConverter;

    @Mock
    private CloudbreakUser cloudbreakUser;

    @Test
    public void testGetWhenSmartSenseNotAvailable() {
        String exceptionMessage = "Unable to identify SmartSense subscription for the user.";
        when(restRequestThreadLocalService.getCloudbreakUser()).thenReturn(cloudbreakUser);
        when(smartSenseSubService.getDefaultForUser(cloudbreakUser)).thenThrow(new SmartSenseSubscriptionAccessDeniedException(exceptionMessage));

        thrown.expect(SmartSenseSubscriptionAccessDeniedException.class);
        thrown.expectMessage(exceptionMessage);

        underTest.get();
        verify(restRequestThreadLocalService, times(1)).getCloudbreakUser();
        verify(smartSenseSubService, times(1)).getDefaultForUser(cloudbreakUser);
    }

    @Test
    public void testGetWhenSmartSenseSubscriptionCannotBeAuthenticated() {
        String exceptionMessage = "Access Denied";
        when(restRequestThreadLocalService.getCloudbreakUser()).thenReturn(cloudbreakUser);
        when(smartSenseSubService.getDefaultForUser(cloudbreakUser)).thenThrow(new AccessDeniedException(exceptionMessage));

        thrown.expect(AccessDeniedException.class);
        thrown.expectMessage(exceptionMessage);

        underTest.get();
        verify(restRequestThreadLocalService, times(1)).getCloudbreakUser();
        verify(smartSenseSubService, times(1)).getDefaultForUser(cloudbreakUser);
    }

    @Test
    public void testGetWhenSmartSenseSubscriptionIsValid() {
        SmartSenseSubscription subscription = createSmartSenseSubscription();
        when(restRequestThreadLocalService.getCloudbreakUser()).thenReturn(cloudbreakUser);
        when(smartSenseSubService.getDefaultForUser(cloudbreakUser)).thenReturn(subscription);
        when(toJsonConverter.convert(subscription)).thenCallRealMethod();

        SmartSenseSubscriptionJson json = underTest.get();

        Assert.assertNotNull(json);
        verify(restRequestThreadLocalService, times(1)).getCloudbreakUser();
        verify(smartSenseSubService, times(1)).getDefaultForUser(cloudbreakUser);
        verify(toJsonConverter, times(1)).convert(subscription);
    }

    private SmartSenseSubscription createSmartSenseSubscription() {
        SmartSenseSubscription subscription = new SmartSenseSubscription();
        subscription.setSubscriptionId("A-99900000-C-00000000");
        return subscription;
    }

}
