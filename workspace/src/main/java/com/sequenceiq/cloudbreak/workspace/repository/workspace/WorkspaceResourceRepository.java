package com.sequenceiq.cloudbreak.workspace.repository.workspace;

import java.io.Serializable;
import java.util.Optional;
import java.util.Set;

import javax.transaction.Transactional;
import javax.transaction.Transactional.TxType;

import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.NoRepositoryBean;

import com.sequenceiq.cloudbreak.workspace.model.Workspace;
import com.sequenceiq.cloudbreak.workspace.model.WorkspaceAwareResource;

@NoRepositoryBean
@Transactional(TxType.REQUIRED)
public interface WorkspaceResourceRepository<T extends WorkspaceAwareResource, ID extends Serializable> extends CrudRepository<T, ID> {

    Set<T> findAllByWorkspace(Workspace workspace);

    Set<T> findAllByWorkspaceId(Long workspaceId);

    Optional<T> findByNameAndWorkspace(String name, Workspace workspace);

    Optional<T> findByNameAndWorkspaceId(String name, Long workspaceId);

    Set<T> findByNameInAndWorkspaceId(Set<String> names, Long workspaceId);
}
