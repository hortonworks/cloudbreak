package com.sequenceiq.periscope.api.endpoint.validator;

import static com.sequenceiq.periscope.common.MessageCode.VALIDATION_LOAD_HOST_GROUP_DUPLICATE_CONFIG;
import static com.sequenceiq.periscope.common.MessageCode.VALIDATION_LOAD_SINGLE_HOST_GROUP;
import static com.sequenceiq.periscope.common.MessageCode.VALIDATION_LOAD_UNSUPPORTED_ADJUSTMENT;
import static com.sequenceiq.periscope.common.MessageCode.VALIDATION_SINGLE_TYPE;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

import com.sequenceiq.cloudbreak.message.CloudbreakMessagesService;
import com.sequenceiq.cloudbreak.validation.ValidatorUtil;
import com.sequenceiq.periscope.api.model.AdjustmentType;
import com.sequenceiq.periscope.api.model.DistroXAutoscaleClusterRequest;

public class DistroXAutoscaleRequestValidator
        implements ConstraintValidator<ValidDistroXAutoscaleRequest, DistroXAutoscaleClusterRequest> {

    @Inject
    private CloudbreakMessagesService messagesService;

    @Override
    public boolean isValid(DistroXAutoscaleClusterRequest request, ConstraintValidatorContext context) {

        if (!request.getLoadAlertRequests().isEmpty() &&
                !request.getTimeAlertRequests().isEmpty()) {
            ValidatorUtil.addConstraintViolation(context, messagesService.getMessage(VALIDATION_SINGLE_TYPE), "autoscalingPolicy")
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
            ValidatorUtil.addConstraintViolation(context, messagesService.getMessage(VALIDATION_LOAD_SINGLE_HOST_GROUP),
                    "loadAlertRequests").disableDefaultConstraintViolation();
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
            ValidatorUtil.addConstraintViolation(context,
                    messagesService.getMessage(VALIDATION_LOAD_UNSUPPORTED_ADJUSTMENT, distinctLoadBasedAdjustmentTypes), "adjustmentType")
                    .disableDefaultConstraintViolation();
            return false;
        }

        if (duplicateHostGroups.size() > 0) {
            ValidatorUtil.addConstraintViolation(context,
                    messagesService.getMessage(VALIDATION_LOAD_HOST_GROUP_DUPLICATE_CONFIG, duplicateHostGroups), "hostGroup")
                    .disableDefaultConstraintViolation();
            return false;
        }

        return true;
    }
}
