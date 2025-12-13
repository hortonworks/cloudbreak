package com.sequenceiq.cloudbreak.message;

import java.util.Collection;
import java.util.Locale;

import jakarta.inject.Inject;

import org.springframework.context.MessageSource;
import org.springframework.stereotype.Component;

/**
 * Wraps a message source used by the cloudbreak core.
 * It provides defaults and extends the interface with application specific methods if any
 */
@Component
public class CloudbreakMessagesService {

    @Inject
    private MessageSource messageSource;

    public String getMessage(String code) {
        return getMessageWithArgs(code, null);
    }

    public String getMessage(String code, Collection<?> args) {
        return getMessageWithArgs(code, args == null ? null : args.toArray());
    }

    public String getMessageWithArgs(String code, Object... args) {
        try {
            return messageSource.getMessage(code, args, Locale.getDefault());
        } catch (Exception e) {
            return e.getMessage();
        }
    }
}
