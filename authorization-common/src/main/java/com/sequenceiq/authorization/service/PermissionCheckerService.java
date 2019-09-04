package com.sequenceiq.authorization.service;

import java.lang.annotation.Annotation;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.authorization.annotation.CheckPermissionByAccount;
import com.sequenceiq.authorization.annotation.CheckPermissionByResourceCrn;
import com.sequenceiq.authorization.annotation.CheckPermissionByResourceCrnList;
import com.sequenceiq.authorization.annotation.CheckPermissionByResourceName;
import com.sequenceiq.authorization.annotation.CheckPermissionByResourceNameList;
import com.sequenceiq.authorization.annotation.CheckPermissionByResourceObject;
import com.sequenceiq.authorization.annotation.DisableCheckPermissions;

@Service
public class PermissionCheckerService extends AbstractPermissionCheckerService {

    private static final Logger LOGGER = LoggerFactory.getLogger(PermissionCheckerService.class);

    private static final List<Class<? extends Annotation>> POSSIBLE_METHOD_ANNOTATIONS =
            List.of(CheckPermissionByResourceCrn.class, CheckPermissionByResourceName.class, CheckPermissionByAccount.class,
                    DisableCheckPermissions.class, CheckPermissionByResourceCrnList.class, CheckPermissionByResourceNameList.class,
                    CheckPermissionByResourceObject.class);

    @Override
    protected List<Class<? extends Annotation>> getPossibleMethodAnnotations() {
        return POSSIBLE_METHOD_ANNOTATIONS;
    }
}
