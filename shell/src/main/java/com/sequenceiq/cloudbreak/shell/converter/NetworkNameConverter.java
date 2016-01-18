package com.sequenceiq.cloudbreak.shell.converter;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.shell.core.Completion;
import org.springframework.shell.core.MethodTarget;

import com.sequenceiq.cloudbreak.shell.completion.NetworkName;
import com.sequenceiq.cloudbreak.shell.model.CloudbreakClient;
import com.sequenceiq.cloudbreak.shell.transformer.ResponseTransformer;

public class NetworkNameConverter extends AbstractConverter<NetworkName> {

    @Autowired
    private CloudbreakClient cloudbreakClient;

    @Autowired
    private ResponseTransformer responseTransformer;

    public NetworkNameConverter() {
    }

    @Override
    public boolean supports(Class<?> type, String optionContext) {
        return NetworkName.class.isAssignableFrom(type);
    }

    @Override
    public boolean getAllPossibleValues(List<Completion> completions, Class<?> targetType, String existingData, String optionContext, MethodTarget target) {
        try {
            Map<String, String> networksMap = responseTransformer.transformToMap(cloudbreakClient.networkEndpoint().getPublics(), "id", "name");
            return getAllPossibleValues(completions, networksMap.values());
        } catch (Exception e) {
            return false;
        }
    }
}
