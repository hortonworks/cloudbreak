package com.sequenceiq.authorization.service;

import static org.junit.Assert.assertTrue;

import java.util.Arrays;

import org.junit.Test;
import org.junit.platform.commons.util.StringUtils;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.junit.MockitoJUnitRunner;

import com.sequenceiq.authorization.resource.AuthorizationResourceAction;

@RunWith(MockitoJUnitRunner.class)
public class UmsRightProviderTest {

    @InjectMocks
    private UmsRightProvider underTest;

    @Test
    public void testIfEveryActionIsInMap() {
        assertTrue(Arrays.stream(AuthorizationResourceAction.values())
                .allMatch(action -> StringUtils.isNotBlank(underTest.getRight(action))));
    }
}
