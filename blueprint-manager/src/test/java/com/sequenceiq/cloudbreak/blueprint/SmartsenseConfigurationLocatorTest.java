package com.sequenceiq.cloudbreak.blueprint;

import java.util.Optional;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.test.util.ReflectionTestUtils;

import com.sequenceiq.cloudbreak.domain.SmartSenseSubscription;

@RunWith(MockitoJUnitRunner.class)
public class SmartsenseConfigurationLocatorTest {

    @Rule
    public final ExpectedException thrown = ExpectedException.none();

    @InjectMocks
    private SmartsenseConfigurationLocator underTest;

    private String smartsenseId = "11111-22222-33333-44444";

    private Optional<String> defaultSmartsenseId = Optional.of(smartsenseId);

    private Optional<SmartSenseSubscription> defaultSmartsenseSubscription;

    @Before
    public void before() {

        SmartSenseSubscription smartSenseSubscription = new SmartSenseSubscription();
        smartSenseSubscription.setSubscriptionId(smartsenseId);
        defaultSmartsenseSubscription = Optional.of(smartSenseSubscription);
    }

    @Test
    public void testSmartsenseConfigurableBySubscriptionIdWhenSmartsenseIdPresentedAndBpContainsSmartsenseServerShouldReturnTrue() {
        ReflectionTestUtils.setField(underTest, "configureSmartSense", true);

        Assert.assertTrue(underTest.smartsenseConfigurableBySubscriptionId(defaultSmartsenseId));
    }

    @Test
    public void testSmartsenseConfigurableBySubscriptionIdWhenSmartsenseDisabledAndBpContainsSmartsenseServerShouldReturnFalse() {
        ReflectionTestUtils.setField(underTest, "configureSmartSense", false);

        Assert.assertFalse(underTest.smartsenseConfigurableBySubscriptionId(defaultSmartsenseId));
    }

    @Test
    public void testSmartsenseConfigurableWhenSmartsenseIdPresentedAndBpContainsSmartsenseServerShouldReturnTrue() {
        ReflectionTestUtils.setField(underTest, "configureSmartSense", true);

        Assert.assertTrue(underTest.smartsenseConfigurable(defaultSmartsenseSubscription));
    }

    @Test
    public void testSmartsenseConfigurableWhenSmartsenseDisabledAndBpContainsSmartsenseServerShouldReturnFalse() {
        ReflectionTestUtils.setField(underTest, "configureSmartSense", false);

        Assert.assertFalse(underTest.smartsenseConfigurable(defaultSmartsenseSubscription));
    }

}