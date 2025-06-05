package com.sequenceiq.authorization.utils;

import static com.sequenceiq.authorization.resource.AuthorizationResourceAction.DESCRIBE_CLUSTER_DEFINITION;
import static com.sequenceiq.authorization.resource.AuthorizationResourceAction.DESCRIBE_CLUSTER_TEMPLATE;
import static com.sequenceiq.authorization.resource.AuthorizationResourceAction.DESCRIBE_CREDENTIAL;
import static com.sequenceiq.authorization.resource.AuthorizationResourceAction.DESCRIBE_CUSTOM_CONFIGS;
import static com.sequenceiq.authorization.resource.AuthorizationResourceAction.DESCRIBE_DATABASE;
import static com.sequenceiq.authorization.resource.AuthorizationResourceAction.DESCRIBE_DATABASE_SERVER;
import static com.sequenceiq.authorization.resource.AuthorizationResourceAction.DESCRIBE_DATAHUB;
import static com.sequenceiq.authorization.resource.AuthorizationResourceAction.DESCRIBE_DATALAKE;
import static com.sequenceiq.authorization.resource.AuthorizationResourceAction.DESCRIBE_ENCRYPTION_PROFILE;
import static com.sequenceiq.authorization.resource.AuthorizationResourceAction.DESCRIBE_ENVIRONMENT;
import static com.sequenceiq.authorization.resource.AuthorizationResourceAction.DESCRIBE_PROXY;
import static com.sequenceiq.authorization.resource.AuthorizationResourceAction.DESCRIBE_RECIPE;
import static com.sequenceiq.authorization.resource.AuthorizationResourceAction.STRUCTURED_EVENTS_READ;
import static com.sequenceiq.authorization.resource.AuthorizationResourceType.AUDIT_CREDENTIAL;
import static com.sequenceiq.authorization.resource.AuthorizationResourceType.CLUSTER_DEFINITION;
import static com.sequenceiq.authorization.resource.AuthorizationResourceType.CLUSTER_TEMPLATE;
import static com.sequenceiq.authorization.resource.AuthorizationResourceType.CREDENTIAL;
import static com.sequenceiq.authorization.resource.AuthorizationResourceType.CUSTOM_CONFIGURATIONS;
import static com.sequenceiq.authorization.resource.AuthorizationResourceType.DATABASE;
import static com.sequenceiq.authorization.resource.AuthorizationResourceType.DATABASE_SERVER;
import static com.sequenceiq.authorization.resource.AuthorizationResourceType.DATAHUB;
import static com.sequenceiq.authorization.resource.AuthorizationResourceType.DATALAKE;
import static com.sequenceiq.authorization.resource.AuthorizationResourceType.ENCRYPTION_PROFILE;
import static com.sequenceiq.authorization.resource.AuthorizationResourceType.ENVIRONMENT;
import static com.sequenceiq.authorization.resource.AuthorizationResourceType.FREEIPA;
import static com.sequenceiq.authorization.resource.AuthorizationResourceType.IMAGE_CATALOG;
import static com.sequenceiq.authorization.resource.AuthorizationResourceType.KERBEROS;
import static com.sequenceiq.authorization.resource.AuthorizationResourceType.LDAP;
import static com.sequenceiq.authorization.resource.AuthorizationResourceType.PROXY;
import static com.sequenceiq.authorization.resource.AuthorizationResourceType.RECIPE;
import static com.sequenceiq.authorization.resource.AuthorizationResourceType.STRUCTURED_EVENT;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.authorization.resource.AuthorizationResourceAction;
import com.sequenceiq.authorization.resource.AuthorizationResourceType;

public final class GetAuthzActionTypeProvider {

    private static final AuthorizationResourceAction NO_DEDICATED_ACTION = null;

    private static final Logger LOGGER = LoggerFactory.getLogger(GetAuthzActionTypeProvider.class);

    private static final Set<AuthzActionTypePair> PAIRS = new HashSet<>(AuthorizationResourceType.values().length);

    static {
        PAIRS.add(new AuthzActionTypePair(CLUSTER_DEFINITION, DESCRIBE_CLUSTER_DEFINITION));
        PAIRS.add(new AuthzActionTypePair(CUSTOM_CONFIGURATIONS, DESCRIBE_CUSTOM_CONFIGS));
        PAIRS.add(new AuthzActionTypePair(CLUSTER_TEMPLATE, DESCRIBE_CLUSTER_TEMPLATE));
        PAIRS.add(new AuthzActionTypePair(DATABASE_SERVER, DESCRIBE_DATABASE_SERVER));
        PAIRS.add(new AuthzActionTypePair(STRUCTURED_EVENT, STRUCTURED_EVENTS_READ));
        PAIRS.add(new AuthzActionTypePair(AUDIT_CREDENTIAL, DESCRIBE_ENVIRONMENT));
        PAIRS.add(new AuthzActionTypePair(IMAGE_CATALOG, DESCRIBE_CREDENTIAL));
        PAIRS.add(new AuthzActionTypePair(ENVIRONMENT, DESCRIBE_ENVIRONMENT));
        PAIRS.add(new AuthzActionTypePair(CREDENTIAL, DESCRIBE_CREDENTIAL));
        PAIRS.add(new AuthzActionTypePair(KERBEROS, NO_DEDICATED_ACTION));
        PAIRS.add(new AuthzActionTypePair(FREEIPA, NO_DEDICATED_ACTION));
        PAIRS.add(new AuthzActionTypePair(DATALAKE, DESCRIBE_DATALAKE));
        PAIRS.add(new AuthzActionTypePair(DATABASE, DESCRIBE_DATABASE));
        PAIRS.add(new AuthzActionTypePair(LDAP, NO_DEDICATED_ACTION));
        PAIRS.add(new AuthzActionTypePair(DATAHUB, DESCRIBE_DATAHUB));
        PAIRS.add(new AuthzActionTypePair(RECIPE, DESCRIBE_RECIPE));
        PAIRS.add(new AuthzActionTypePair(PROXY, DESCRIBE_PROXY));
        PAIRS.add(new AuthzActionTypePair(ENCRYPTION_PROFILE, DESCRIBE_ENCRYPTION_PROFILE));
    }

    private GetAuthzActionTypeProvider() {
    }

    public static Optional<AuthzActionTypePair> getActionsForResourceType(AuthorizationResourceType resourceType) {
        Optional<AuthzActionTypePair> resultPair = Optional.empty();
        if (resourceType != null) {
            LOGGER.debug("Looking up for {} based on the following {}: {}", AuthzActionTypePair.class.getSimpleName(),
                    AuthorizationResourceType.class.getSimpleName(), resourceType);
            resultPair = PAIRS.stream().filter(pair -> resourceType.equals(pair.getResourceType())).findFirst();
        }
        if (resultPair.isPresent()) {
            LOGGER.debug("The following {} found for the given {}[{}]: {}", AuthzActionTypePair.class.getSimpleName(),
                    AuthorizationResourceType.class.getSimpleName(), resourceType, resultPair.get());
        } else {
            LOGGER.debug("No {} has been found based on the given {}: {}", AuthzActionTypePair.class.getSimpleName(),
                    AuthorizationResourceType.class.getSimpleName(), resourceType);
        }
        return resultPair;
    }

    public static Set<AuthzActionTypePair> getPairs() {
        return PAIRS;
    }

}
