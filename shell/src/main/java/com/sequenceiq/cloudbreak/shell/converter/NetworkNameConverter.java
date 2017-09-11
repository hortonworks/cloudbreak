package com.sequenceiq.cloudbreak.shell.converter;

import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.springframework.shell.core.Completion;
import org.springframework.shell.core.MethodTarget;

import com.sequenceiq.cloudbreak.client.CloudbreakClient;
import com.sequenceiq.cloudbreak.shell.completion.NetworkName;
import com.sequenceiq.cloudbreak.shell.transformer.ResponseTransformer;

public class NetworkNameConverter extends AbstractConverter<NetworkName> {

    @Inject
    private CloudbreakClient cloudbreakClient;

    @Inject
    private ResponseTransformer responseTransformer;

    @Override
    public boolean supports(Class<?> type, String optionContext) {
        return NetworkName.class.isAssignableFrom(type);
    }

    @Override
    public boolean getAllPossibleValues(List<Completion> completions, Class<?> targetType, String existingData, String optionContext, MethodTarget target) {
        try {
            Map<String, String> networksMap = responseTransformer.transformToMap(cloudbreakClient.networkEndpoint().getPublics(), "id", "name");
            return getAllPossibleValues(completions, networksMap.values());
        } catch (RuntimeException e) {
            return false;
        }
    }
}
