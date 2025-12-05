package com.sequenceiq.notification.scheduled.register;

import java.util.List;

import com.sequenceiq.notification.domain.Subscription;

/**
 * Optional callback interface for scheduled notification jobs to process subscriptions.
 * Implementing this interface allows jobs to access them during notification sending.
 */
public interface SubscriptionAware {

    /**
     * Called after notifications are sent successfully with the associated subscriptions (distribution lists or resource subscriptions).
     *
     * @param subscriptions the list of subscriptions associated with the sent notifications
     */
    void onSubscriptionsProcessed(List<? extends Subscription> subscriptions);
}

