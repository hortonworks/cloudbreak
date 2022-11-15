package com.sequenceiq.authorization.service;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;

import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.authorization.resource.AuthorizationResourceAction;

@ExtendWith(MockitoExtension.class)
public class UmsRightProviderTest {

    @InjectMocks
    private UmsRightProvider underTest;

    @Test
    public void testIfEveryActionIsInMap() {
        assertTrue(Arrays.stream(AuthorizationResourceAction.values())
                .allMatch(action -> StringUtils.isNotBlank(underTest.getRight(action))));
    }
}
