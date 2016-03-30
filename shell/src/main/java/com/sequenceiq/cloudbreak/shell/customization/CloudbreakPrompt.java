package com.sequenceiq.cloudbreak.shell.customization;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.shell.plugin.PromptProvider;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.shell.model.ShellContext;

/**
 * Manages the text of the shell's prompt.
 */
@Component
public class CloudbreakPrompt implements PromptProvider {

    @Autowired
    private ShellContext context;

    @Override
    public String getProviderName() {
        return CloudbreakPrompt.class.getSimpleName();
    }

    @Override
    public String getPrompt() {
        return context.getPrompt();
    }
}
