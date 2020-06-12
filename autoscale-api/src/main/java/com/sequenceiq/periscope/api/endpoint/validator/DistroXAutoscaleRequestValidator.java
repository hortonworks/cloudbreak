package com.sequenceiq.periscope.api.endpoint.validator;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

import com.sequenceiq.cloudbreak.validation.ValidatorUtil;
import com.sequenceiq.periscope.api.model.AdjustmentType;
import com.sequenceiq.periscope.api.model.DistroXAutoscaleClusterRequest;

public class DistroXAutoscaleRequestValidator
        implements ConstraintValidator<ValidDistroXAutoscaleRequest, DistroXAutoscaleClusterRequest> {

    @Override
    public boolean isValid(DistroXAutoscaleClusterRequest request, ConstraintValidatorContext context) {

        if (!request.getLoadAlertRequests().isEmpty() &&
                !request.getTimeAlertRequests().isEmpty()) {
            String message = String.format("Cluster can be configured with only one type of autoscaling policies.");
            ValidatorUtil.addConstraintViolation(context, message, "autoscalingPolicy")
                    .disableDefaultConstraintViolation();
            return false;
        }

        if (!request.getLoadAlertRequests().isEmpty()) {
            return isValidLoadAlertRequests(request, context);
        }

        return true;
    }

    private Boolean isValidLoadAlertRequests(DistroXAutoscaleClusterRequest request, ConstraintValidatorContext context) {
        Set<String> distinctLoadBasedHostGroups = new HashSet<>();
        Set<AdjustmentType> distinctLoadBasedAdjustmentTypes = new HashSet<>();

        if (request.getLoadAlertRequests().size() > 1) {
            String message = String.format("LoadBased autoscaling currently supports a single HostGroup in a Cluster.");
            ValidatorUtil.addConstraintViolation(context, message, "loadAlertRequests")
                    .disableDefaultConstraintViolation();
            return false;
        }

        Set<String> duplicateHostGroups =
                request.getLoadAlertRequests().stream()
                        .map(loadAlertRequest -> loadAlertRequest.getScalingPolicy())
                        .map(scalingPolicyRequest -> {
                            distinctLoadBasedAdjustmentTypes.add(scalingPolicyRequest.getAdjustmentType());
                            return scalingPolicyRequest.getHostGroup();
                        })
                        .filter(n -> !distinctLoadBasedHostGroups.add(n))
                        .collect(Collectors.toSet());

        if (distinctLoadBasedAdjustmentTypes.size() > 1 || !distinctLoadBasedAdjustmentTypes.contains(AdjustmentType.LOAD_BASED)) {
            String message = String.format("LoadBased Autoscale policy does not support AdjustmentType of type %s.",
                    distinctLoadBasedAdjustmentTypes.toString());
            ValidatorUtil.addConstraintViolation(context, message, "adjustmentType")
                    .disableDefaultConstraintViolation();
            return false;
        }

        if (duplicateHostGroups.size() > 0) {
            String message = String.format("Hostgroup(s) %s configured with multiple LoadBased Autoscaling policies.",
                    duplicateHostGroups.toString());
            ValidatorUtil.addConstraintViolation(context, message, "hostGroup")
                    .disableDefaultConstraintViolation();
            return false;
        }

        return true;
    }
}
