package com.sequenceiq.authorization.resource;

import java.util.Map;

public interface AuthorizationApiRequest {

    Map<String, AuthorizableFieldInfoModel> getAuthorizableFields();
}
