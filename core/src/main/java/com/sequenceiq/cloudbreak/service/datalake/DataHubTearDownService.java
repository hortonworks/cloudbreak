package com.sequenceiq.cloudbreak.service.datalake;

import java.util.Optional;

import jakarta.inject.Inject;

import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.reactor.handler.cluster.deregister.DeregisterPrePositionFactory;
import com.sequenceiq.cloudbreak.sdx.paas.LocalPaasDhTearDownService;
import com.sequenceiq.cloudbreak.service.stack.StackService;

@Service
public class DataHubTearDownService implements LocalPaasDhTearDownService {

    @Inject
    private StackService stackService;

    @Inject
    private DeregisterPrePositionFactory deregisterPrePositionFactory;

    @Override
    public void tearDownDataHub(String sdxCrn, String datahubCrn) {
        Stack stack = stackService.getByCrnWithLists(datahubCrn);
        Optional<Stack> dataLakeOptional = Optional.ofNullable(stackService.getByCrnOrElseNull(sdxCrn));
        if (dataLakeOptional.isPresent()) {
            deregisterPrePositionFactory.clusterApi(stack)
                    .clusterSecurityService()
                    .deregisterServices(stack.getName(), deregisterPrePositionFactory.datalakeDto(stack));
        }
    }
}
