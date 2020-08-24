package com.sequenceiq.cloudbreak.cloud.aws.connector.resource;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.Date;
import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.amazonaws.services.autoscaling.AmazonAutoScalingClient;
import com.amazonaws.services.autoscaling.model.Activity;
import com.amazonaws.services.autoscaling.model.DescribeScalingActivitiesRequest;
import com.amazonaws.services.autoscaling.model.DescribeScalingActivitiesResult;
import com.amazonaws.waiters.Waiter;
import com.sequenceiq.cloudbreak.cloud.aws.client.AmazonAutoScalingRetryClient;
import com.sequenceiq.cloudbreak.cloud.aws.scheduler.CustomAmazonWaiterProvider;

@ExtendWith(MockitoExtension.class)
public class AwsAutoScalingServiceTest {

    @InjectMocks
    private AwsAutoScalingService underTest;

    @Mock
    private CustomAmazonWaiterProvider customAmazonWaiterProvider;

    @Mock
    private AmazonAutoScalingClient amazonAutoScalingClient;

    @Mock
    private AmazonAutoScalingRetryClient amazonAutoScalingRetryClient;

    @Mock
    private Waiter<DescribeScalingActivitiesRequest> describeScalingActivitiesRequestWaiter;

    @Test
    public void testCheckLastScalingActivityWhenActivitiesFailed() {
        DescribeScalingActivitiesResult result = new DescribeScalingActivitiesResult();
        Activity activity1 = new Activity();
        activity1.setStatusMessage("Status");
        activity1.setDescription("Description");
        activity1.setCause("Cause");
        activity1.setStatusCode("FAILED");
        result.setActivities(List.of(activity1));
        when(amazonAutoScalingRetryClient.describeScalingActivities(any(DescribeScalingActivitiesRequest.class))).thenReturn(result);
        when(customAmazonWaiterProvider.getAutoscalingActivitiesWaiter(any(), any())).thenReturn(describeScalingActivitiesRequestWaiter);

        Date date = new Date();
        AmazonAutoscalingFailed expected = Assertions.assertThrows(AmazonAutoscalingFailed.class, () ->
                underTest.checkLastScalingActivity(amazonAutoScalingClient, amazonAutoScalingRetryClient, "asGroup", date));

        Assertions.assertEquals(expected.getMessage(), "Description Cause");
    }

    @Test
    public void testCheckLastScalingActivityWhenActivitiesSuccessThenNoException() throws AmazonAutoscalingFailed {
        DescribeScalingActivitiesResult result = new DescribeScalingActivitiesResult();
        Activity activity1 = new Activity();
        activity1.setStatusMessage("Status");
        activity1.setDescription("Description");
        activity1.setCause("Cause");
        activity1.setStatusCode("success");
        result.setActivities(List.of(activity1));
        when(amazonAutoScalingRetryClient.describeScalingActivities(any(DescribeScalingActivitiesRequest.class))).thenReturn(result);
        when(customAmazonWaiterProvider.getAutoscalingActivitiesWaiter(any(), any())).thenReturn(describeScalingActivitiesRequestWaiter);

        Date date = new Date();
        underTest.checkLastScalingActivity(amazonAutoScalingClient, amazonAutoScalingRetryClient, "asGroup", date);

    }
}
