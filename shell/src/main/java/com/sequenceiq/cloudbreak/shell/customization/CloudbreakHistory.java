package com.sequenceiq.cloudbreak.shell.customization;

import org.springframework.shell.plugin.HistoryFileNameProvider;
import org.springframework.stereotype.Component;

/**
 * Specifies the name of the CloudBreak command log. Later this log can be used
 * to re-execute the commands with either the --cmdfile option at startup
 * or with the script --file command.
 */
@Component
public class CloudbreakHistory implements HistoryFileNameProvider {

    @Override
    public String getHistoryFileName() {
        return "cloud-break.cbh";
    }

    @Override
    public String getProviderName() {
        return "CloudbreakShell";
    }
}
