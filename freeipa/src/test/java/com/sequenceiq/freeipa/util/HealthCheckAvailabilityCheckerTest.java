package com.sequenceiq.freeipa.util;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.image.Image;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.service.image.ImageService;

@ExtendWith(MockitoExtension.class)
public class HealthCheckAvailabilityCheckerTest {

    private static final String AVAILABLE_VERSION = "2.32.0";

    private static final String UNAVAILABLE_VERSION = "2.31.0";

    private static final String ACCOUNT_ID = "account-id";

    @InjectMocks
    private HealthCheckAvailabilityChecker underTest;

    @Mock
    private ImageService imageService;

    @Test
    public void testAvailable() {
        Stack stack = new Stack();
        setHealthCheckPackageVersion(stack, "0.1.1");

        stack.setAppVersion(AVAILABLE_VERSION);
        stack.setAccountId(ACCOUNT_ID);
        assertTrue(underTest.isCdpFreeIpaHeathAgentAvailable(stack));
    }

    @Test
    public void testUnavailableVersion() {
        Stack stack = new Stack();

        stack.setAppVersion(UNAVAILABLE_VERSION);
        stack.setAccountId(ACCOUNT_ID);
        assertFalse(underTest.isCdpFreeIpaHeathAgentAvailable(stack));
    }

    @Test
    public void testUnavailablePackageVersion() {
        Stack stack = new Stack();
        stack.setAppVersion(AVAILABLE_VERSION);
        stack.setAccountId(ACCOUNT_ID);

        setHealthCheckPackageVersion(stack, null);
        assertFalse(underTest.isCdpFreeIpaHeathAgentAvailable(stack));

        setHealthCheckPackageVersion(stack, "");
        assertFalse(underTest.isCdpFreeIpaHeathAgentAvailable(stack));
    }

    @Test
    public void testAppVersionIsBlank() {
        Stack stack = new Stack();
        assertFalse(underTest.isCdpFreeIpaHeathAgentAvailable(stack));

        stack.setAppVersion("");
        assertFalse(underTest.isCdpFreeIpaHeathAgentAvailable(stack));

        stack.setAppVersion(" ");
        assertFalse(underTest.isCdpFreeIpaHeathAgentAvailable(stack));
    }

    private void setHealthCheckPackageVersion(Stack stack, String version) {
        Image image = mock(Image.class);
        when(imageService.getImageForStack(stack)).thenReturn(image);
        Map<String, String> packageVersions = new HashMap<>();
        when(image.getPackageVersions()).thenReturn(packageVersions);
        if (version != null) {
            packageVersions.put(HealthCheckAvailabilityChecker.HEALTH_CHECK_PACKAGE_NAME, version);
        }
    }
}