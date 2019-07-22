package com.sequenceiq.authorization.service;

import java.lang.annotation.Annotation;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.google.common.collect.Lists;
import com.sequenceiq.authorization.repository.BaseCrudRepository;
import com.sequenceiq.authorization.repository.BaseJpaRepository;
import com.sequenceiq.authorization.repository.CheckPermission;
import com.sequenceiq.authorization.repository.DisableCheckPermissions;

@Service
public class PermissionCheckerService extends AbstractPermissionCheckerService {

    private static final Logger LOGGER = LoggerFactory.getLogger(PermissionCheckerService.class);

    private static final List<Class<? extends Annotation>> POSSIBLE_METHOD_ANNOTATIONS =
            List.of(CheckPermission.class, DisableCheckPermissions.class);

    @Override
    protected List<Class<? extends Annotation>> getPossibleMethodAnnotations() {
        return POSSIBLE_METHOD_ANNOTATIONS;
    }

    @Override
    protected List<Class> getRepositoryClass() {
        return Lists.newArrayList(BaseCrudRepository.class, BaseJpaRepository.class);
    }
}
