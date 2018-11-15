package com.sequenceiq.cloudbreak.service.smartsense;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.MockitoAnnotations.initMocks;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.util.ReflectionTestUtils;

import com.sequenceiq.cloudbreak.TestUtil;
import com.sequenceiq.cloudbreak.aspect.PermissionType;
import com.sequenceiq.cloudbreak.common.model.user.CloudbreakUser;
import com.sequenceiq.cloudbreak.domain.SmartSenseSubscription;
import com.sequenceiq.cloudbreak.repository.SmartSenseSubscriptionRepository;
import com.sequenceiq.cloudbreak.service.AuthorizationService;

@Ignore
public class SmartSenseSubscriptionServiceTest {

    private static final String DEPLOYMENT_SMARTSENSE_ID = "A-99900000-C-00000000";

    private static final String OLD_SMARTSENSE_ID = "A-99900000-C-88888888";

    @Rule
    public final ExpectedException thrown = ExpectedException.none();

    @Mock
    private SmartSenseSubscriptionRepository repository;

    @InjectMocks
    private SmartSenseSubscriptionService underTest;

    @Mock
    private AuthorizationService authorizationService;

    private CloudbreakUser user;

    @Before
    public void setUp() {
        initMocks(this);
        user = TestUtil.cbUser();
    }

    @Test
    public void getDefaultForUserWhenDefaultSmartSenseIdIsSpecifiedAndDefaultSubscriptionCouldNotBeFound() {
        ReflectionTestUtils.setField(underTest, "defaultSmartsenseId", DEPLOYMENT_SMARTSENSE_ID);

        SmartSenseSubscription result = underTest.getDefaultForUser(user);

        verify(repository, times(1)).save(ArgumentMatchers.<SmartSenseSubscription>any());
        Assert.assertEquals(DEPLOYMENT_SMARTSENSE_ID, result.getSubscriptionId());
    }

    @Test
    public void getDefaultForUserWhenDefaultSmartSenseIdIsSpecifiedAndDefaultSubscriptionNeedsToBeUpdated() {
        ReflectionTestUtils.setField(underTest, "defaultSmartsenseId", DEPLOYMENT_SMARTSENSE_ID, String.class);

        SmartSenseSubscription result = underTest.getDefaultForUser(user);

        verify(repository, times(1)).save(ArgumentMatchers.<SmartSenseSubscription>any());
        Assert.assertEquals(DEPLOYMENT_SMARTSENSE_ID, result.getSubscriptionId());
    }

    @Test
    public void getDefaultForUserWhenDefaultSmartSenseIdIsNotSpecifiedAndDefaultSubscriptionCouldNotBeFound() {

        thrown.expect(SmartSenseSubscriptionAccessDeniedException.class);
        thrown.expectMessage("Unable to identify SmartSense subscription for the user.");

        underTest.getDefaultForUser(user);
    }

    @Test
    public void getDefaultForUserWhenDefaultSmartSenseIdIsNotSpecifiedAndDefaultSubscriptionCouldBeFound() {
        SmartSenseSubscription smartSenseSubscription = createSmartSenseSubscription();

        SmartSenseSubscription result = underTest.getDefaultForUser(user);

        Assert.assertEquals(smartSenseSubscription, result);
    }

    @Test
    public void testGetDefaultForUserWhenDefaultSmartSenseIdIsNotSpecifiedAndDefaultSubscriptionCouldBeFoundButNotAuthorized() {
        SmartSenseSubscription smartSenseSubscription = createSmartSenseSubscription();
        String exceptionMessage = "Unable to identify SmartSense subscription for the user.";
        doThrow(new AccessDeniedException(exceptionMessage)).when(authorizationService).hasPermission(smartSenseSubscription, PermissionType.READ.name());

        thrown.expect(SmartSenseSubscriptionAccessDeniedException.class);
        thrown.expectMessage(exceptionMessage);

        underTest.getDefaultForUser(user);
        verify(authorizationService, times(1)).hasPermission(Matchers.<SmartSenseSubscription>any(), anyString());
    }

    @Test
    public void getDefaultForUserWhenDefaultSmartSenseIdIsSpecifiedButAndItsEqualsToTheSubscriptionId() {
        ReflectionTestUtils.setField(underTest, "defaultSmartsenseId", DEPLOYMENT_SMARTSENSE_ID);
        SmartSenseSubscription subscription = createSmartSenseSubscription();
        subscription.setSubscriptionId(DEPLOYMENT_SMARTSENSE_ID);

        SmartSenseSubscription result = underTest.getDefaultForUser(user);

        Assert.assertEquals("The given SmartSenseSubscription instance has got a unexpected update", subscription, result);
    }

    private SmartSenseSubscription createSmartSenseSubscription() {
        SmartSenseSubscription subscription = new SmartSenseSubscription();
        subscription.setSubscriptionId(OLD_SMARTSENSE_ID);
        return subscription;
    }

}