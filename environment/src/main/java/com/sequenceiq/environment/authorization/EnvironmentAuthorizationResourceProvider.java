package com.sequenceiq.environment.authorization;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.sequenceiq.authorization.service.list.ResourceListProvider;
import com.sequenceiq.authorization.service.list.Resource;

@Component
public class EnvironmentAuthorizationResourceProvider implements ResourceListProvider {

    @Override
    public List<Resource> findResources(String accountId, List<String> resourceCrns) {
        return resourceCrns.stream()
                .map(c -> new Resource(c, Optional.empty()))
                .collect(Collectors.toList());
    }
}
