package com.sequenceiq.cloudbreak.service.messages;

import java.util.List;
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
        return messageSource.getMessage(code, null, Locale.getDefault());
    }

    public String getMessage(String code, List args) {
        return messageSource.getMessage(code, args.toArray(), Locale.getDefault());
    }
}
