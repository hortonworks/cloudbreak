package com.sequenceiq.cloudbreak.service.upgrade.sync;

import java.util.HashSet;
import java.util.Set;

import javax.inject.Inject;

import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.domain.stack.Component;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.service.image.ComponentConverter;
import com.sequenceiq.cloudbreak.service.upgrade.sync.operationresult.CmParcelSyncOperationResult;
import com.sequenceiq.cloudbreak.service.upgrade.sync.operationresult.CmRepoSyncOperationResult;

@Service
public class CmSyncResultMergerService {

    @Inject
    private ComponentConverter componentConverter;

    Set<Component> merge(CmRepoSyncOperationResult cmRepoSyncOperationResult, CmParcelSyncOperationResult cmParcelSyncOperationResult, Stack stack) {
        Set<Component> syncedFromServer = new HashSet<>();
        cmRepoSyncOperationResult.getFoundClouderaManagerRepo()
                .map(cmRepo -> componentConverter.fromClouderaManagerRepo(cmRepo, stack))
                .ifPresent(syncedFromServer::add);
        syncedFromServer.addAll(componentConverter.fromClouderaManagerProductList(cmParcelSyncOperationResult.getFoundCmProducts(), stack));
        return syncedFromServer;
    }
}
