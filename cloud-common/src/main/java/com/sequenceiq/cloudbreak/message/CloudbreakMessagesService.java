package com.sequenceiq.cloudbreak.message;

import java.util.Collection;
import java.util.Collections;
import java.util.Locale;

import javax.inject.Inject;

import org.springframework.context.MessageSource;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.events.responses.NotificationEventType;
import com.sequenceiq.cloudbreak.authorization.WorkspaceResource;

/**
 * Wraps a message source used by the cloudbreak core.
 * It provides defaults and extends the interface with application specific methods if any
 */
@Component
public class CloudbreakMessagesService {

    private static final String RESOURCE_PREFIX = "resource.";

    private static final String DOT = ".";

    @Inject
    private MessageSource messageSource;

    public String getMessage(String code) {
        return messageSource.getMessage(code, null, Locale.getDefault());
    }

    public String getMessage(String code, Collection<?> args) {
        return messageSource.getMessage(code, args.toArray(), Locale.getDefault());
    }

    public String getMessage(NotificationEventType eventType) {
        return getMessage(eventType, Collections.emptyList());
    }

    public String getMessage(NotificationEventType eventType, Collection<?> args) {
        String code = RESOURCE_PREFIX + normalize(eventType.name());
        return messageSource.getMessage(code, args.toArray(), Locale.getDefault());
    }

    public String getMessage(WorkspaceResource resource, NotificationEventType eventType) {
        return getMessage(resource, eventType, Collections.emptyList());
    }

    public String getMessage(WorkspaceResource resource, NotificationEventType eventType, Collection<?> args) {
        String code = RESOURCE_PREFIX + resource.getShortName() + DOT + normalize(eventType.name());
        return messageSource.getMessage(code, args.toArray(), Locale.getDefault());
    }

    private String normalize(String value) {
        return value.replaceAll("_", DOT).toLowerCase();
    }
}
