package com.sequenceiq.periscope.api.endpoint.validator;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.ConstraintValidatorContext.ConstraintViolationBuilder;
import jakarta.validation.ConstraintValidatorContext.ConstraintViolationBuilder.NodeBuilderCustomizableContext;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.message.CloudbreakMessagesService;
import com.sequenceiq.periscope.api.model.AdjustmentType;
import com.sequenceiq.periscope.api.model.DistroXAutoscaleClusterRequest;
import com.sequenceiq.periscope.api.model.LoadAlertRequest;
import com.sequenceiq.periscope.api.model.ScalingPolicyRequest;
import com.sequenceiq.periscope.api.model.TimeAlertRequest;

@ExtendWith(MockitoExtension.class)
public class DistroXAutoscaleRequestValidatorTest {

    @InjectMocks
    private DistroXAutoscaleRequestValidator underTest = new DistroXAutoscaleRequestValidator();

    private ConstraintViolationBuilder constraintViolationBuilder;

    private NodeBuilderCustomizableContext nodeBuilderCustomizableContext;

    private ConstraintValidatorContext validatorContext;

    @Mock
    private CloudbreakMessagesService messagesService;

    @BeforeEach
    public void setupMocks() {
        constraintViolationBuilder = mock(ConstraintValidatorContext.ConstraintViolationBuilder.class);
        nodeBuilderCustomizableContext =
                mock(ConstraintValidatorContext.ConstraintViolationBuilder.NodeBuilderCustomizableContext.class);
        validatorContext = mock(ConstraintValidatorContext.class);

        lenient().when(validatorContext.buildConstraintViolationWithTemplate(any())).thenReturn(constraintViolationBuilder);
        lenient().when(constraintViolationBuilder.addPropertyNode(any())).thenReturn(nodeBuilderCustomizableContext);
        lenient().when(nodeBuilderCustomizableContext.addConstraintViolation()).thenReturn(validatorContext);
        lenient().doNothing().when(validatorContext).disableDefaultConstraintViolation();
    }

    @Test
    public void testIsValidWhenValidLoadAlertsThenTrue() {
        List<String> loadHostGroups = Arrays.asList("compute2");

        boolean underTestValid = underTest
                .isValid(getTestRequest(List.of(), loadHostGroups, Optional.empty()), validatorContext);
        assertTrue(underTestValid, "Valid Load Alert Autoscale Policy");
    }

    @Test
    public void testIsValidWhenValidTimeAlertsThenTrue() {
        List<String> timeHostGroups = Arrays.asList("compute2", "hdfs1", "hdfs3");

        boolean underTestValid = underTest
                .isValid(getTestRequest(timeHostGroups, List.of(), Optional.empty()), validatorContext);
        assertTrue(underTestValid, "Valid Time Alert Autoscale Policy");
    }

    @Test
    public void testIsValidWhenNegativeTargeTimeAlertsThenFalse() {
        List<String> timeHostGroups = Arrays.asList("compute2", "hdfs1", "hdfs3");
        DistroXAutoscaleClusterRequest  request = getTestRequest(timeHostGroups, List.of(), Optional.empty());
        request.getTimeAlertRequests().stream()
                .forEach(timeAlertRequest -> timeAlertRequest.getScalingPolicy().setScalingAdjustment(-10));

        boolean underTestValid = underTest.isValid(request, validatorContext);
        assertFalse(underTestValid, "Target for Time Alert with Exact Adjustment cannot be negative.");
    }

    @Test
    public void testIsValidWhenBothAlertsThenFalse() {
        List<String> timeHostGroups = Arrays.asList("hdfs1", "compute1", "hdfs3");
        List<String> loadHostGroups = Arrays.asList("compute2", "hdfs1", "hdfs3");

        boolean underTestValid = underTest
                .isValid(getTestRequest(timeHostGroups, loadHostGroups, Optional.empty()), validatorContext);
        assertFalse(underTestValid, "Cluster can be configured with only one type of autoscale policy");
    }

    @Test
    public void testIsValidWhenDuplicateLoadAlertHostGroupThenFalse() {
        List<String> loadHostGroups = Arrays.asList("compute2", "compute2", "hdfs3");

        boolean underTestValid = underTest
                .isValid(getTestRequest(List.of(), loadHostGroups, Optional.empty()), validatorContext);
        assertFalse(underTestValid, "Cluster can be configured with only one type of autoscale policy");
    }

    @Test
    public void testIsValidWhenLoadAlertWithInvalidAdjustmentTypeThenFalse() {
        List<String> loadHostGroups = Arrays.asList("compute2", "compute2", "hdfs3");

        boolean underTestValid = underTest
                .isValid(getTestRequest(List.of(), loadHostGroups, Optional.of(AdjustmentType.NODE_COUNT)), validatorContext);
        assertFalse(underTestValid, "Cluster can be configured with only one type of autoscale policy");
    }

    private DistroXAutoscaleClusterRequest getTestRequest(List<String> timeHostGroups,
            List<String> loadHostGroups, Optional<AdjustmentType> testAdjustmentType) {
        DistroXAutoscaleClusterRequest testRequest = new DistroXAutoscaleClusterRequest();
        List<TimeAlertRequest> timeAlertRequests = new ArrayList<>();
        List<LoadAlertRequest> loadAlertRequests = new ArrayList<>();

        if (timeHostGroups != null) {
            for (String hostGroup : timeHostGroups) {
                TimeAlertRequest request = new TimeAlertRequest();
                ScalingPolicyRequest scalingPolicyRequest = new ScalingPolicyRequest();
                scalingPolicyRequest.setHostGroup(hostGroup);
                scalingPolicyRequest.setAdjustmentType(testAdjustmentType.orElse(AdjustmentType.EXACT));
                scalingPolicyRequest.setScalingAdjustment(10);
                request.setScalingPolicy(scalingPolicyRequest);
                timeAlertRequests.add(request);
            }
            testRequest.setTimeAlertRequests(timeAlertRequests);
        }

        if (loadHostGroups != null) {
            for (String hostGroup : loadHostGroups) {
                LoadAlertRequest request = new LoadAlertRequest();
                ScalingPolicyRequest scalingPolicyRequest = new ScalingPolicyRequest();
                scalingPolicyRequest.setHostGroup(hostGroup);
                scalingPolicyRequest.setAdjustmentType(testAdjustmentType.orElse(AdjustmentType.LOAD_BASED));
                request.setScalingPolicy(scalingPolicyRequest);
                loadAlertRequests.add(request);
            }
            testRequest.setLoadAlertRequests(loadAlertRequests);
        }
        return testRequest;
    }
}
