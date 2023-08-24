package com.sequenceiq.cloudbreak.cloud.aws;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;
import com.sequenceiq.cloudbreak.cloud.model.Location;
import com.sequenceiq.cloudbreak.cloud.model.Region;

import software.amazon.awssdk.services.autoscaling.model.AutoScalingGroup;

@ExtendWith(MockitoExtension.class)
class AwsUserDataServiceTest {

    @Mock
    private CloudFormationStackUtil cfStackUtil;

    @Mock
    private AutoScalingGroupHandler autoScalingGroupHandler;

    @Mock
    private AwsCloudFormationClient awsCloudFormationClient;

    @Mock
    private AwsLaunchTemplateUpdateService awsLaunchTemplateUpdateService;

    @InjectMocks
    private AwsUserDataService underTest;

    @Mock
    private CloudStack cloudStack;

    private AuthenticatedContext ac;

    @BeforeEach
    void setUp() {
        Location location = Location.location(Region.region("region"));
        CloudContext context = CloudContext.Builder.builder()
                .withId(123L)
                .withName("cloudContext")
                .withCrn("crn")
                .withPlatform("AWS")
                .withVariant("variant")
                .withLocation(location)
                .withWorkspaceId(234L)
                .build();
        CloudCredential cred = new CloudCredential("crn", "cc", "account");
        ac = new AuthenticatedContext(context, cred);
    }

    @Test
    void testGetUserData() {
        AutoScalingGroup asgA = AutoScalingGroup.builder().autoScalingGroupName("asgA").build();
        AutoScalingGroup asgB = AutoScalingGroup.builder().autoScalingGroupName("asgB").build();
        Map<String, AutoScalingGroup> asgMap = Map.of("asgA", asgA, "asgB", asgB);
        when(autoScalingGroupHandler.autoScalingGroupByName(any(), any(), any())).thenReturn(asgMap);

        Map<String, String> groupToAsgMap = Map.of("hostgroup1", "asgA", "hostgroup2", "asgB");
        when(cfStackUtil.getGroupNameToAutoscalingGroupName(any(), any(), any())).thenReturn(groupToAsgMap);

        when(awsLaunchTemplateUpdateService.getUserDataFromAutoScalingGroup(ac, asgA)).thenReturn("userdata-A");
        when(awsLaunchTemplateUpdateService.getUserDataFromAutoScalingGroup(ac, asgB)).thenReturn("userdata-B");

        Map<String, String> result = underTest.getUserData(ac, cloudStack);

        assertThat(result).containsExactlyInAnyOrderEntriesOf(Map.of("hostgroup1", "userdata-A", "hostgroup2", "userdata-B"));
    }
}
