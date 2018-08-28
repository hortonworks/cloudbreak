package com.sequenceiq.cloudbreak.cloud.aws;

import static com.sequenceiq.cloudbreak.cloud.aws.AwsInstanceConnector.INSTANCE_NOT_FOUND_ERROR_CODE;
import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import javax.inject.Inject;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockitoAnnotations;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.amazonaws.services.ec2.AmazonEC2Client;
import com.amazonaws.services.ec2.model.AmazonEC2Exception;
import com.amazonaws.services.ec2.model.DescribeInstancesRequest;
import com.sequenceiq.cloudbreak.cloud.InstanceConnector;
import com.sequenceiq.cloudbreak.cloud.aws.view.AwsCredentialView;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.CloudInstance;
import com.sequenceiq.cloudbreak.cloud.model.InstanceStatus;
import com.sequenceiq.cloudbreak.cloud.model.InstanceTemplate;
import com.sequenceiq.cloudbreak.cloud.model.Location;
import com.sequenceiq.cloudbreak.cloud.model.Region;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(classes = TestApplicationContext.class)
public class AwsInstanceConnectorTest {

    @Inject
    private InstanceConnector awsInstanceConnector;

    @Inject
    private AwsClient awsClient;

    @Inject
    private AmazonEC2Client ec2Client;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void check() {
        CloudContext cloudContext = new CloudContext(0L, "cctx-name", "cctx-platform", "cctx-owner", "variant",
                Location.location(Region.region("us-west-1")));
        CloudCredential cloudCredential = new CloudCredential(1L, "ccred-name", "ccred-pubkey", "ccred-login");

        AuthenticatedContext ac = new AuthenticatedContext(cloudContext, cloudCredential);

        List<CloudInstance> vms = new ArrayList<>();
        vms.add(createVM("i-016f8349ce980dcfd", InstanceStatus.STARTED));
        vms.add(createVM("i-026f8349ce980dcfd", InstanceStatus.TERMINATED));
        vms.add(createVM("i-036f8349ce980dcfd", InstanceStatus.TERMINATED));

        when(awsClient.createAccess(any(AwsCredentialView.class), eq("us-west-1"))).thenReturn(ec2Client);

        AmazonEC2Exception amazonEC2Exception =
                new AmazonEC2Exception("An error occurred (InvalidInstanceID.NotFound) when calling the DescribeInstances operation: "
                        + "The instance IDs 'i-026f8349ce980dcfd, i-036f8349ce980dcfd' do not exist");
        amazonEC2Exception.setErrorCode(INSTANCE_NOT_FOUND_ERROR_CODE);
        when(ec2Client.describeInstances(any(DescribeInstancesRequest.class)))
                .thenThrow(amazonEC2Exception);

        try {
            awsInstanceConnector.check(ac, vms);
        } catch (AmazonEC2Exception e) {
            assertEquals(amazonEC2Exception, e);
        } finally {
            assertEquals(1, vms.size());
            assertEquals("i-016f8349ce980dcfd", vms.get(0).getInstanceId());
            verify(awsClient, times(15)).createAccess(any(AwsCredentialView.class), eq("us-west-1"));
        }
    }

    private CloudInstance createVM(String instanceId, InstanceStatus started) {
        return new CloudInstance(instanceId,
                new InstanceTemplate("flavor", "group", 2L, Collections.emptyList(), started, new HashMap<>()));
    }
}