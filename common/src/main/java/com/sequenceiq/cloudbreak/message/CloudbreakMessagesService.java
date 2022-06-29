package com.sequenceiq.cloudbreak.message;

import java.util.Collection;
import java.util.Locale;

import javax.inject.Inject;

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
        try {
            return messageSource.getMessage(code, null, Locale.getDefault());
        } catch (Exception e) {
            return e.getMessage();
        }
    }

    public String getMessage(String code, Collection<?> args) {
        try {
            return messageSource.getMessage(code, args == null ? null : args.toArray(), Locale.getDefault());
        } catch (Exception e) {
            return e.getMessage();
        }
    }
}
