package com.sequenceiq.notification.sender.dto;

import java.util.List;

import com.sequenceiq.notification.domain.Subscription;

/**
 * Result object containing both sent notifications and associated subscriptions (distribution lists).
 */
public record NotificationSendingResult(
        List<NotificationDto> notifications,
        List<? extends Subscription> subscriptions
) {
}

