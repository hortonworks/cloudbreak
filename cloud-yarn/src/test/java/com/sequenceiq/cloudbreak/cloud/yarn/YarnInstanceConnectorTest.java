package com.sequenceiq.cloudbreak.cloud.yarn;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.exception.CloudOperationNotSupportedException;
import com.sequenceiq.cloudbreak.cloud.model.CloudInstance;
import com.sequenceiq.cloudbreak.cloud.model.CloudVmInstanceStatus;
import com.sequenceiq.cloudbreak.cloud.model.CloudVmMetaDataStatus;

@ExtendWith(MockitoExtension.class)
class YarnInstanceConnectorTest {

    @Mock
    private YarnApplicationDetailsService yarnApplicationDetailsService;

    @InjectMocks
    private YarnInstanceConnector underTest;

    @Mock
    private AuthenticatedContext authenticatedContext;

    @Test
    void checkWithoutApplicationName() {
        assertThatThrownBy(() -> underTest.check(authenticatedContext, List.of(mock(CloudInstance.class))))
                .isInstanceOf(CloudOperationNotSupportedException.class)
                .hasMessage("Instances' states check operation is not supported on YARN without application name");
    }

    @Test
    void checkWithMultipleApplicationNames() {
        CloudInstance vm1 = mock();
        when(vm1.hasParameter(CloudInstance.APPLICATION_NAME)).thenReturn(true);
        when(vm1.getStringParameter(CloudInstance.APPLICATION_NAME)).thenReturn("app1");
        CloudInstance vm2 = mock();
        when(vm2.hasParameter(CloudInstance.APPLICATION_NAME)).thenReturn(true);
        when(vm2.getStringParameter(CloudInstance.APPLICATION_NAME)).thenReturn("app2");

        CloudVmMetaDataStatus vm1Status = mock();
        CloudVmInstanceStatus vm1InstanceStatus = mock();
        when(vm1Status.getCloudVmInstanceStatus()).thenReturn(vm1InstanceStatus);
        CloudVmMetaDataStatus vm2Status = mock();
        CloudVmInstanceStatus vm2InstanceStatus = mock();
        when(vm2Status.getCloudVmInstanceStatus()).thenReturn(vm2InstanceStatus);

        when(yarnApplicationDetailsService.collect(authenticatedContext, "app1", List.of(vm1)))
                .thenReturn(List.of(vm1Status));
        when(yarnApplicationDetailsService.collect(authenticatedContext, "app2", List.of(vm2)))
                .thenReturn(List.of(vm2Status));

        List<CloudVmInstanceStatus> result = underTest.check(authenticatedContext, List.of(vm1, vm2));

        assertThat(result).containsAll(List.of(vm1InstanceStatus, vm2InstanceStatus));
    }
}
