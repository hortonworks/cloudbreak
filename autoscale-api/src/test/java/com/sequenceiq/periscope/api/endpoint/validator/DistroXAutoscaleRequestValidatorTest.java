package com.sequenceiq.periscope.api.endpoint.validator;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import javax.validation.ConstraintValidatorContext;
import javax.validation.ConstraintValidatorContext.ConstraintViolationBuilder;
import javax.validation.ConstraintValidatorContext.ConstraintViolationBuilder.NodeBuilderCustomizableContext;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import com.sequenceiq.periscope.api.model.AdjustmentType;
import com.sequenceiq.periscope.api.model.DistroXAutoscaleClusterRequest;
import com.sequenceiq.periscope.api.model.LoadAlertRequest;
import com.sequenceiq.periscope.api.model.ScalingPolicyRequest;
import com.sequenceiq.periscope.api.model.TimeAlertRequest;

@RunWith(MockitoJUnitRunner.class)
public class DistroXAutoscaleRequestValidatorTest {

    private DistroXAutoscaleRequestValidator underTest = new DistroXAutoscaleRequestValidator();

    private ConstraintViolationBuilder constraintViolationBuilder;

    private NodeBuilderCustomizableContext nodeBuilderCustomizableContext;

    private ConstraintValidatorContext validatorContext;

    @Before
    public void setupMocks() {
        constraintViolationBuilder = mock(ConstraintValidatorContext.ConstraintViolationBuilder.class);
        nodeBuilderCustomizableContext =
                mock(ConstraintValidatorContext.ConstraintViolationBuilder.NodeBuilderCustomizableContext.class);
        validatorContext = mock(ConstraintValidatorContext.class);

        when(validatorContext.buildConstraintViolationWithTemplate(any())).thenReturn(constraintViolationBuilder);
        when(constraintViolationBuilder.addPropertyNode(any())).thenReturn(nodeBuilderCustomizableContext);
        when(nodeBuilderCustomizableContext.addConstraintViolation()).thenReturn(validatorContext);
        doNothing().when(validatorContext).disableDefaultConstraintViolation();
    }

    @Test
    public void testIsValidWhenValidLoadAlertsThenTrue() {
        List<String> loadHostGroups = Arrays.asList("compute2", "hdfs1", "hdfs3");

        boolean underTestValid = underTest
                .isValid(getTestRequest(List.of(), loadHostGroups, Optional.empty()), validatorContext);
        assertTrue("Valid Load Alert Autoscale Policy", underTestValid);
    }

    @Test
    public void testIsValidWhenValidTimeAlertsThenTrue() {
        List<String> timeHostGroups = Arrays.asList("compute2", "hdfs1", "hdfs3");

        boolean underTestValid = underTest
                .isValid(getTestRequest(timeHostGroups, List.of(), Optional.empty()), validatorContext);
        assertTrue("Valid Time Alert Autoscale Policy", underTestValid);
    }

    @Test
    public void testIsValidWhenBothAlertsThenFalse() {
        List<String> timeHostGroups = Arrays.asList("hdfs1", "compute1", "hdfs3");
        List<String> loadHostGroups = Arrays.asList("compute2", "hdfs1", "hdfs3");

        boolean underTestValid = underTest
                .isValid(getTestRequest(timeHostGroups, loadHostGroups, Optional.empty()), validatorContext);
        assertFalse("Cluster can be configured with only one type of autoscale policy", underTestValid);
    }

    @Test
    public void testIsValidWhenDuplicateLoadAlertHostGroupThenFalse() {
        List<String> loadHostGroups = Arrays.asList("compute2", "compute2", "hdfs3");

        boolean underTestValid = underTest
                .isValid(getTestRequest(List.of(), loadHostGroups, Optional.empty()), validatorContext);
        assertFalse("Cluster can be configured with only one type of autoscale policy", underTestValid);
    }

    @Test
    public void testIsValidWhenLoadAlertWithInvalidAdjustmentTypeThenFalse() {
        List<String> loadHostGroups = Arrays.asList("compute2", "compute2", "hdfs3");

        boolean underTestValid = underTest
                .isValid(getTestRequest(List.of(), loadHostGroups, Optional.of(AdjustmentType.NODE_COUNT)), validatorContext);
        assertFalse("Cluster can be configured with only one type of autoscale policy", underTestValid);
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
                scalingPolicyRequest.setAdjustmentType(testAdjustmentType.orElse(AdjustmentType.NODE_COUNT));
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
