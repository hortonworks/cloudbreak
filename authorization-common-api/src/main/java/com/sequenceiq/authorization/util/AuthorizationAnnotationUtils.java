package com.sequenceiq.authorization.util;

import java.lang.annotation.Annotation;
import java.util.List;

import com.sequenceiq.authorization.annotation.CheckPermissionByAccount;
import com.sequenceiq.authorization.annotation.CheckPermissionByResourceCrn;
import com.sequenceiq.authorization.annotation.CheckPermissionByResourceCrnList;
import com.sequenceiq.authorization.annotation.CheckPermissionByResourceName;
import com.sequenceiq.authorization.annotation.CheckPermissionByResourceNameList;
import com.sequenceiq.authorization.annotation.CheckPermissionByResourceObject;
import com.sequenceiq.authorization.annotation.CustomPermissionCheck;
import com.sequenceiq.authorization.annotation.DisableCheckPermissions;
import com.sequenceiq.authorization.annotation.FilterListBasedOnPermissions;
import com.sequenceiq.authorization.annotation.InternalOnly;

public class AuthorizationAnnotationUtils {

    private AuthorizationAnnotationUtils() {

    }

    public static List<Class<? extends Annotation>> getPossibleMethodAnnotations() {
        return List.of(CheckPermissionByResourceCrn.class, CheckPermissionByResourceName.class, CheckPermissionByAccount.class,
                DisableCheckPermissions.class, CheckPermissionByResourceCrnList.class, CheckPermissionByResourceNameList.class,
                CheckPermissionByResourceObject.class, FilterListBasedOnPermissions.class, InternalOnly.class,
                CustomPermissionCheck.class);
    }
}
