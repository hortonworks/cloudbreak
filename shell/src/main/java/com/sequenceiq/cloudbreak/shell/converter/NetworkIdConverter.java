package com.sequenceiq.cloudbreak.shell.converter;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.shell.core.Completion;
import org.springframework.shell.core.MethodTarget;

import com.sequenceiq.cloudbreak.shell.completion.NetworkId;
import com.sequenceiq.cloudbreak.shell.model.ShellContext;

public class NetworkIdConverter extends AbstractConverter<NetworkId> {

    @Autowired
    private ShellContext context;

    public NetworkIdConverter() {
    }

    @Override
    public boolean supports(Class<?> type, String optionContext) {
        return NetworkId.class.isAssignableFrom(type);
    }

    @Override
    public boolean getAllPossibleValues(List<Completion> completions, Class<?> targetType, String existingData, String optionContext, MethodTarget target) {
        try {
            return getAllPossibleValues(completions, context.getNetworksByProvider().keySet());
        } catch (Exception e) {
            return false;
        }
    }
}
