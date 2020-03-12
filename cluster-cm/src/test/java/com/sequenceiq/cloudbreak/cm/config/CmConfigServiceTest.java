package com.sequenceiq.cloudbreak.cm.config;

import static org.mockito.Mockito.verify;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.cloudera.api.swagger.model.ApiRoleList;
import com.sequenceiq.cloudbreak.domain.stack.Stack;

@ExtendWith(MockitoExtension.class)
class CmConfigServiceTest {

    @Mock
    private CmConfigServiceDelegate service1;

    @Mock
    private CmConfigServiceDelegate service2;

    private CmConfigService underTest;

    @BeforeEach
    public void setUp() {
        underTest = new CmConfigService(List.of(service1, service2));
    }

    @Test
    public void shouldDelegateToEachService() {
        Stack stack = new Stack();
        ApiRoleList apiRoleList = new ApiRoleList();

        underTest.setConfigs(stack, apiRoleList);

        verify(service1).setConfigs(stack, apiRoleList);
        verify(service2).setConfigs(stack, apiRoleList);
    }

}