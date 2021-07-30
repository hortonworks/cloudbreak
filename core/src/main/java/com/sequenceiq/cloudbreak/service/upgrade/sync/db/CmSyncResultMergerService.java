package com.sequenceiq.cloudbreak.service.upgrade.sync.db;

import java.util.HashSet;
import java.util.Set;

import javax.inject.Inject;

import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.domain.stack.Component;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.service.image.ComponentConverter;
import com.sequenceiq.cloudbreak.service.upgrade.sync.operationresult.CmSyncOperationResult;

@Service
public class CmSyncResultMergerService {

    @Inject
    private ComponentConverter componentConverter;

    Set<Component> merge(Stack stack, CmSyncOperationResult cmSyncOperationResult) {
        Set<Component> syncedFromServer = new HashSet<>();
        cmSyncOperationResult.getCmRepoSyncOperationResult().getFoundClouderaManagerRepo()
                .map(cmRepo -> componentConverter.fromClouderaManagerRepo(cmRepo, stack))
                .ifPresent(syncedFromServer::add);
        syncedFromServer.addAll(componentConverter.fromClouderaManagerProductList(
                cmSyncOperationResult.getCmParcelSyncOperationResult().getFoundCmProducts(), stack));
        return syncedFromServer;
    }

}
