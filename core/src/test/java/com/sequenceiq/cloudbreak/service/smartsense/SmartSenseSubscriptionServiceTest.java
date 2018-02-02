package com.sequenceiq.cloudbreak.service.smartsense;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.test.util.ReflectionTestUtils;

import com.sequenceiq.cloudbreak.TestUtil;
import com.sequenceiq.cloudbreak.common.model.user.IdentityUser;
import com.sequenceiq.cloudbreak.domain.SmartSenseSubscription;
import com.sequenceiq.cloudbreak.repository.SmartSenseSubscriptionRepository;

public class SmartSenseSubscriptionServiceTest {

    private static final String DEPLOYMENT_SMARTSENSE_ID = "A-99900000-C-00000000";

    private static final String OLD_SMARTSENSE_ID = "A-99900000-C-88888888";

    private IdentityUser user;

    @Mock
    private SmartSenseSubscriptionRepository repository;

    @InjectMocks
    private SmartSenseSubscriptionService underTest;

    @Before
    public void setUp() throws Exception {
        initMocks(this);
        user = TestUtil.cbUser();
    }

    @Test
    public void getDefaultForUserWhenDefaultSmartSenseIdIsSpecifiedAndDefaultSubscriptionCouldNotBeFound() {
        ReflectionTestUtils.setField(underTest, "defaultSmartsenseId", DEPLOYMENT_SMARTSENSE_ID);
        when(repository.findByAccountAndOwner(user.getAccount(), user.getUserId())).thenReturn(null);

        SmartSenseSubscription result = underTest.getDefaultForUser(user);

        verify(repository, times(1)).save(Mockito.<SmartSenseSubscription>any());
        Assert.assertEquals(DEPLOYMENT_SMARTSENSE_ID, result.getSubscriptionId());
    }

    @Test
    public void getDefaultForUserWhenDefaultSmartSenseIdIsSpecifiedAndDefaultSubscriptionNeedsToBeUpdated() {
        ReflectionTestUtils.setField(underTest, "defaultSmartsenseId", DEPLOYMENT_SMARTSENSE_ID, String.class);
        SmartSenseSubscription smartSenseSubscription = createSmartSenseSubscription();
        when(repository.findByAccountAndOwner(user.getAccount(), user.getUserId())).thenReturn(smartSenseSubscription);

        SmartSenseSubscription result = underTest.getDefaultForUser(user);

        verify(repository, times(1)).save(Mockito.<SmartSenseSubscription>any());
        Assert.assertEquals(DEPLOYMENT_SMARTSENSE_ID, result.getSubscriptionId());
    }

    @Test
    public void getDefaultForUserWhenDefaultSmartSenseIdIsNotSpecifiedAndDefaultSubscriptionCouldNotBeFound() {
        when(repository.findByAccountAndOwner(user.getAccount(), user.getUserId())).thenReturn(null);

        SmartSenseSubscription result = underTest.getDefaultForUser(user);

        Assert.assertNull(result);
    }

    @Test
    public void getDefaultForUserWhenDefaultSmartSenseIdIsNotSpecifiedAndDefaultSubscriptionCouldBeFound() {
        SmartSenseSubscription smartSenseSubscription = createSmartSenseSubscription();
        when(repository.findByAccountAndOwner(user.getAccount(), user.getUserId())).thenReturn(smartSenseSubscription);

        SmartSenseSubscription result = underTest.getDefaultForUser(user);

        Assert.assertEquals(smartSenseSubscription, result);
    }

    private SmartSenseSubscription createSmartSenseSubscription() {
        SmartSenseSubscription subscription = new SmartSenseSubscription();
        subscription.setSubscriptionId(OLD_SMARTSENSE_ID);
        subscription.setAccount(user.getAccount());
        subscription.setOwner(user.getUserId());
        subscription.setPublicInAccount(true);
        return subscription;
    }
}