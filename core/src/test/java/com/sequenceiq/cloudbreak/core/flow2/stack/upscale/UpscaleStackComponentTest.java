package com.sequenceiq.cloudbreak.core.flow2.stack.upscale;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.Collections;

import org.junit.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import com.sequenceiq.cloudbreak.cloud.aws.CloudFormationStackUtil;
import com.sequenceiq.cloudbreak.cloud.event.CloudPlatformRequest;
import com.sequenceiq.cloudbreak.cloud.exception.QuotaExceededException;
import com.sequenceiq.cloudbreak.cloud.handler.AbstractComponentTest;
import com.sequenceiq.cloudbreak.cloud.handler.testcontext.TestApplicationContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudResourceStatus;
import com.sequenceiq.cloudbreak.cloud.model.ResourceStatus;
import com.sequenceiq.cloudbreak.common.service.TransactionService.TransactionExecutionException;
import com.sequenceiq.cloudbreak.reactor.UpscaleStackHandler;
import com.sequenceiq.cloudbreak.reactor.api.event.stack.UpscaleStackRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.stack.UpscaleStackResult;
import com.sequenceiq.common.api.adjustment.AdjustmentTypeWithThreshold;
import com.sequenceiq.common.api.type.AdjustmentType;

@SpringBootTest(classes = { TestApplicationContext.class, UpscaleStackComponentTest.TestConfig.class },
        properties = "spring.main.allow-bean-definition-overriding=true")
public class UpscaleStackComponentTest extends AbstractComponentTest<UpscaleStackResult> {

    @MockBean
    private StackUpscaleService stackUpscaleService;

    @MockBean
    private CloudFormationStackUtil cfStackUtil;

    @Test
    public void testUpscaleStack() throws QuotaExceededException, TransactionExecutionException {
        when(stackUpscaleService.upscale(any(), any(), any())).thenReturn(Collections.singletonList(new CloudResourceStatus(null, ResourceStatus.UPDATED)));
        UpscaleStackResult result = sendCloudRequest();


        assertEquals(ResourceStatus.UPDATED, result.getResourceStatus());
        assertEquals(1L, result.getResults().size());
        assertEquals(ResourceStatus.UPDATED, result.getResults().get(0).getStatus());
        assertFalse(result.isFailed());
        assertNull(result.getErrorDetails());
    }

    @Override
    protected String getTopicName() {
        return "UPSCALESTACKREQUEST";
    }

    @Override
    protected CloudPlatformRequest<UpscaleStackResult> getRequest() {
        return new UpscaleStackRequest<>(g().createCloudContext(), g().createCloudCredential(), g().createCloudStack(),
                g().createCloudResourceList(), new AdjustmentTypeWithThreshold(AdjustmentType.BEST_EFFORT, 5L), false);
    }

    @Configuration
    @Import({UpscaleStackHandler.class})
    public static class TestConfig {
    }
}
