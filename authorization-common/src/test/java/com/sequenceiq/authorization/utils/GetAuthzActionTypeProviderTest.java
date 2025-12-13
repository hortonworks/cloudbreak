package com.sequenceiq.authorization.utils;

import static com.sequenceiq.authorization.utils.GetAuthzActionTypeProvider.getActionsForResourceType;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.params.provider.EnumSource.Mode.EXCLUDE;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import com.sequenceiq.authorization.resource.AuthorizationResourceAction;
import com.sequenceiq.authorization.resource.AuthorizationResourceType;

class GetAuthzActionTypeProviderTest {

    @ParameterizedTest
    @EnumSource(value = AuthorizationResourceType.class, names = "CONSUMPTION", mode = EXCLUDE)
    void testAllAuthorizationResourceTypeShouldHaveAnEntryInTheResultMap(AuthorizationResourceType types) {
        List<String> missingTypesFromPairs = new ArrayList<>();
        for (AuthorizationResourceType resourceType : List.of(types)) {
            if (getActionsForResourceType(resourceType).isEmpty()) {
                missingTypesFromPairs.add(resourceType.name());
            }
        }

        assertTrue(missingTypesFromPairs.isEmpty(),
                String.format("The following %s(s) has no get/fetch action pair: [%s]", AuthorizationResourceAction.class.getSimpleName(),
                        String.join(", ", missingTypesFromPairs)));
    }

}