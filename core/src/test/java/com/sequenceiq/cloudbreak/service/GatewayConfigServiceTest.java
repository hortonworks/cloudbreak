package com.sequenceiq.cloudbreak.service;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.common.exception.NotFoundException;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceMetaData;
import com.sequenceiq.cloudbreak.dto.StackDtoDelegate;
import com.sequenceiq.cloudbreak.view.StackView;

@ExtendWith(MockitoExtension.class)
public class GatewayConfigServiceTest {

    @InjectMocks
    private GatewayConfigService underTest;

    @Test
    void testGetAllGatewayConfigIfSecurityConfigIsNull() {
        StackDtoDelegate stackDtoDelegate = mock(StackDtoDelegate.class);
        StackView stackView = mock(StackView.class);
        when(stackDtoDelegate.getSecurityConfig()).thenReturn(null);
        when(stackDtoDelegate.getReachableGatewayInstanceMetadata()).thenReturn(List.of(new InstanceMetaData()));
        when(stackDtoDelegate.getStack()).thenReturn(stackView);

        assertThrows(NotFoundException.class, () -> underTest.getAllGatewayConfigs(stackDtoDelegate),
                "Cannot find security config for the given gateway instance.");
    }
}
