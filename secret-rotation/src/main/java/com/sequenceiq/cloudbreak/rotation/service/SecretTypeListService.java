package com.sequenceiq.cloudbreak.rotation.service;

import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;

import jakarta.inject.Inject;

import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.rotation.SecretType;
import com.sequenceiq.cloudbreak.rotation.entity.SecretRotationHistory;
import com.sequenceiq.cloudbreak.rotation.response.BaseSecretTypeResponse;
import com.sequenceiq.cloudbreak.rotation.service.history.SecretRotationHistoryService;
import com.sequenceiq.cloudbreak.rotation.service.notification.SecretListField;
import com.sequenceiq.cloudbreak.rotation.service.notification.SecretRotationNotificationService;

@Service
public class SecretTypeListService<T extends BaseSecretTypeResponse> {

    @Inject
    private SecretRotationNotificationService notificationService;

    @Inject
    private SecretRotationHistoryService historyService;

    @Inject
    private List<SecretType> enabledSecretTypes;

    public List<T> listRotatableSecretType(String resourceCrn, Function<BaseSecretTypeResponse, T> converter) {
        // further improvement needed to query secret types for resource
        List<SecretType> secretTypes = enabledSecretTypes.stream()
                .filter(Predicate.not(SecretType::internal))
                .toList();
        List<SecretRotationHistory> rotationHistory = historyService.getHistoryForResource(resourceCrn);
        return secretTypes
                .stream()
                .map(type -> new BaseSecretTypeResponse(type.value(),
                        notificationService.getMessage(type, SecretListField.DISPLAY_NAME),
                        notificationService.getMessage(type, SecretListField.DESCRIPTION),
                        rotationHistory.stream()
                                .filter(history -> history.getSecretType().equals(type))
                                .map(SecretRotationHistory::getLastUpdated)
                                .findFirst()
                                .orElse(null)))
                .map(converter)
                .toList();
    }
}
