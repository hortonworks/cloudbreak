package com.sequenceiq.cloudbreak.shell.commands.base;

import java.util.Map;

import org.springframework.shell.core.annotation.CliAvailabilityIndicator;
import org.springframework.shell.core.annotation.CliCommand;
import org.springframework.shell.core.annotation.CliOption;

import com.sequenceiq.cloudbreak.api.model.AccountPreferencesJson;
import com.sequenceiq.cloudbreak.shell.commands.AccountPreferencesCommands;
import com.sequenceiq.cloudbreak.shell.model.ShellContext;
import com.sequenceiq.cloudbreak.shell.util.TagParser;

public class BaseAccountPreferencesCommands implements AccountPreferencesCommands {
    private static final String CREATE_SUCCESS_MSG = "Default tags are added to account";

    private ShellContext shellContext;

    public BaseAccountPreferencesCommands(ShellContext shellContext) {
        this.shellContext = shellContext;
    }

    @CliAvailabilityIndicator("defaulttags set")
    public boolean isDefaultTagsAvailable() {
        return true;
    }

    @CliCommand(value = "defaulttags set", help = "Add default tags to the account using the format 'key1=value1,key2=value2")
    @Override
    public String setDefaultTags(@CliOption(key = "tags", mandatory = true) String defaultTags) {
        try {
            Map<String, String> parsedTags = TagParser.parseTagsIntoMap(defaultTags);
            AccountPreferencesJson ap = shellContext.cloudbreakClient().accountPreferencesEndpoint().get();
            ap.setDefaultTags(parsedTags);
            shellContext.cloudbreakClient().accountPreferencesEndpoint().put(ap);
            return CREATE_SUCCESS_MSG;
        } catch (Exception ex) {
            throw shellContext.exceptionTransformer().transformToRuntimeException(ex);
        }
    }
}
