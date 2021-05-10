package com.sequenceiq.cloudbreak.cloud.azure.image.identity;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.junit.Test;

import com.microsoft.azure.PagedList;
import com.microsoft.azure.ProxyResource;
import com.microsoft.azure.management.Azure;
import com.microsoft.azure.management.graphrbac.RoleAssignment;
import com.microsoft.azure.management.graphrbac.implementation.RoleAssignmentInner;
import com.microsoft.azure.management.msi.implementation.IdentityInner;
import com.sequenceiq.cloudbreak.cloud.azure.image.AzureTestCredentials;

public class IdentityTest {

    @Test
    public void iterateIdentities() {
        AzureTestCredentials azureTestCredentials = new AzureTestCredentials();
        Azure azure = azureTestCredentials.getAzure();

        List<IdentityInner> identityInnerList= azure.identities().inner().list().stream().collect(Collectors.toList());
        identityInnerList.stream().map(ProxyResource::name).collect(Collectors.toList());

        Optional<IdentityInner> identityInnerOptional = identityInnerList.stream().filter(i -> "mock-idbroker-admin-identity".equals(i.name())).findFirst();
        IdentityInner identityInner = identityInnerOptional.get();

        // get role assignment for mock-idbroker-admin-identity
        List<RoleAssignmentInner> roleAssignments = azure.identities().manager().graphRbacManager().roleAssignments().manager()
                .roleInner().withSubscriptionId(azureTestCredentials.getSubscriptionId()).roleAssignments().list().stream().collect(Collectors.toList());
//        azure.identities().manager().graphRbacManager().roleAssignments().listByScope()
        List<RoleAssignmentInner> roleAssignmentOfMockIdBroker = roleAssignments.stream().filter(a -> identityInner.principalId().equals(a.principalId())).collect(Collectors.toList());

        PagedList<RoleAssignment> roleAssignmentsByScope1 = azure.identities().manager().graphRbacManager().roleAssignments().listByScope("/subscriptions/3ddda1c7-d1f5-4e7b-ac81-0523f483b3b3/resourceGroups/rg-gpapp-single-rg");
        PagedList<RoleAssignment> roleAssignmentsByScope = azure.accessManagement().roleAssignments().listByScope("/subscriptions/3ddda1c7-d1f5-4e7b-ac81-0523f483b3b3/resourceGroups/rg-gpapp-single-rg");
        System.out.println("identities retrieved");
    }
}
