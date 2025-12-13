package com.sequenceiq.cloudbreak.cm.polling.task;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import com.cloudera.api.swagger.client.ApiException;
import com.sequenceiq.cloudbreak.cluster.service.ClusterEventService;
import com.sequenceiq.cloudbreak.cm.client.ClouderaManagerApiPojoFactory;
import com.sequenceiq.cloudbreak.cm.exception.ClouderaManagerOperationFailedException;
import com.sequenceiq.cloudbreak.cm.polling.ClouderaManagerCommandPollerObject;

class AbstractClouderaManagerApiCheckerTaskTest {

    private static final int TOLERATED_ERROR_LIMIT = 5;

    @Test
    public void testCheckStatus() {
        AbstractClouderaManagerApiCheckerTask<ClouderaManagerCommandPollerObject> abstractClouderaManagerApiCheckerTask =
                new AbstractClouderaManagerApiCheckerTask<>(mock(ClouderaManagerApiPojoFactory.class), mock(ClusterEventService.class)) {
                    @Override
                    protected boolean doStatusCheck(ClouderaManagerCommandPollerObject pollerObject) throws ApiException {
                        throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR.value(), "something went wrong");
                    }

                    @Override
                    protected String getPollingName() {
                        return "echo";
                    }
                };
        ClouderaManagerOperationFailedException clouderaManagerOperationFailedException =
                assertThrows(ClouderaManagerOperationFailedException.class, () -> {
            for (int i = 0; i <= TOLERATED_ERROR_LIMIT; i++) {
                abstractClouderaManagerApiCheckerTask.checkStatus(mock(ClouderaManagerCommandPollerObject.class));
            }
        });
        assertEquals("API checking [echo] failed with a tolerated error 'something went wrong' for the 5. time(s). Operation is considered failed.",
                clouderaManagerOperationFailedException.getMessage());
    }
}