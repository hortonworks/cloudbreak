package com.sequenceiq.cloudbreak.cm.polling.task;

import static org.mockito.Mockito.when;

import org.mockito.Mock;

import com.sequenceiq.cloudbreak.cloud.model.ClouderaManagerProduct;
import com.sequenceiq.cloudbreak.dto.StackDto;

class ClouderaManagerSingleParcelActivationListenerTaskTest
        extends AbstractClouderaManagerParcelListenerTaskTest<ClouderaManagerSingleParcelActivationListenerTask> {

    @Mock
    private StackDto stackDto;

    @Override
    ClouderaManagerSingleParcelActivationListenerTask setUpUnderTest() {
        when(stackDto.getName()).thenReturn(CLUSTER_NAME);
        when(pollerObject.getStack()).thenReturn(stackDto);

        ClouderaManagerProduct product = new ClouderaManagerProduct();
        product.setName(PRODUCT);
        product.setVersion(VERSION);

        return new ClouderaManagerSingleParcelActivationListenerTask(clouderaManagerApiPojoFactory, clusterEventService, product);
    }
}
