package com.sequenceiq.freeipa.service.crossrealm.commands;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.message.CloudbreakMessagesService;
import com.sequenceiq.cloudbreak.util.DocumentationLinkProvider;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.crossrealm.commands.BaseClusterTrustSetupCommands;
import com.sequenceiq.freeipa.entity.CrossRealmTrust;
import com.sequenceiq.freeipa.entity.FreeIpa;
import com.sequenceiq.freeipa.entity.LoadBalancer;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.service.crossrealm.TrustCommandType;

@ExtendWith(MockitoExtension.class)
class BaseClusterTrustCommandsBuilderTest {
    @Mock
    private CloudbreakMessagesService messagesService;

    @InjectMocks
    private final BaseClusterTrustCommandsBuilder underTest = new BaseClusterTrustCommandsBuilder() {
        @Override
        protected String buildKrb5Conf(String resourceName, TrustCommandType trustCommandType, FreeIpa freeIpa, CrossRealmTrust crossRealmTrust,
                LoadBalancer loadBalancer) {
            return "krb5conf";
        }
    };

    @Test
    void testBuildBaseClusterSetupCommands() {
        Stack stack = new Stack();
        FreeIpa freeIpa = new FreeIpa();
        CrossRealmTrust crossRealmTrust = new CrossRealmTrust();
        LoadBalancer loadBalancer = new LoadBalancer();
        Mockito.when(messagesService.getMessage("trust.addrealm.explanation")).thenReturn("trust.addrealm.explanation");

        BaseClusterTrustSetupCommands actualResult = underTest.buildBaseClusterCommands(stack, TrustCommandType.SETUP, freeIpa, crossRealmTrust, loadBalancer);

        assertEquals("krb5conf", actualResult.getKrb5Conf());
        assertEquals("trust.addrealm.explanation", actualResult.getClouderaManagerSetupInstructions().getExplanation());
        assertEquals(DocumentationLinkProvider.onPremisesTrustedRealmsLink(), actualResult.getClouderaManagerSetupInstructions().getDocs());
    }

    @Test
    void testBuildBaseClusterCleanupCommands() {
        Stack stack = new Stack();
        FreeIpa freeIpa = new FreeIpa();
        CrossRealmTrust crossRealmTrust = new CrossRealmTrust();
        LoadBalancer loadBalancer = new LoadBalancer();
        Mockito.when(messagesService.getMessage("trust.removerealm.explanation")).thenReturn("trust.removerealm.explanation");

        BaseClusterTrustSetupCommands actualResult =
                underTest.buildBaseClusterCommands(stack, TrustCommandType.CLEANUP, freeIpa, crossRealmTrust, loadBalancer);

        assertEquals("krb5conf", actualResult.getKrb5Conf());
        assertEquals("trust.removerealm.explanation", actualResult.getClouderaManagerSetupInstructions().getExplanation());
        assertNull(actualResult.getClouderaManagerSetupInstructions().getDocs());
    }
}
