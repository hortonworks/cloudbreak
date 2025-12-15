package com.sequenceiq.freeipa.service.crossrealm;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import java.util.Optional;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import com.sequenceiq.cloudbreak.common.type.KdcType;
import com.sequenceiq.cloudbreak.orchestrator.host.HostOrchestrator;
import com.sequenceiq.freeipa.entity.CrossRealmTrust;
import com.sequenceiq.freeipa.kerberos.KerberosConfigService;
import com.sequenceiq.freeipa.repository.CrossRealmTrustRepository;
import com.sequenceiq.freeipa.service.crossrealm.commands.activedirectory.ActiveDirectoryBaseClusterTrustCommandsBuilder;
import com.sequenceiq.freeipa.service.crossrealm.commands.activedirectory.ActiveDirectoryTrustInstructionsBuilder;
import com.sequenceiq.freeipa.service.crossrealm.commands.mit.MitBaseClusterTrustCommandsBuilder;
import com.sequenceiq.freeipa.service.crossrealm.commands.mit.MitTrustInstructionsBuilder;
import com.sequenceiq.freeipa.service.freeipa.FreeIpaClientFactory;
import com.sequenceiq.freeipa.service.freeipa.dns.DnsZoneService;
import com.sequenceiq.freeipa.service.freeipa.trust.setup.ActiveDirectoryTrustService;
import com.sequenceiq.freeipa.service.freeipa.trust.setup.MitKdcTrustService;
import com.sequenceiq.freeipa.service.freeipa.trust.setup.TrustProvider;
import com.sequenceiq.freeipa.service.freeipa.trust.statusvalidation.TrustStatusValidationService;
import com.sequenceiq.freeipa.service.rotation.SaltStateParamsService;
import com.sequenceiq.freeipa.service.stack.StackService;

@ExtendWith(SpringExtension.class)
@TestPropertySource(properties = {
        "freeipa.max.salt.trustsetup.maxretry=90",
        "freeipa.max.salt.trustsetup.maxerrorretry=5"
})
class CrossRealmTrustServiceTest {

    private static final Long STACK_ID = 1L;

    @Inject
    private CrossRealmTrustService underTest;

    @MockBean
    private CrossRealmTrustRepository crossRealmTrustRepository;

    @Mock
    private CrossRealmTrust crossRealmTrust;

    @MockBean
    private SaltStateParamsService saltStateParamsService;

    @MockBean
    private DnsZoneService dnsZoneService;

    @MockBean
    private FreeIpaClientFactory freeIpaClientFactory;

    @MockBean
    private HostOrchestrator hostOrchestrator;

    @MockBean
    private StackService stackService;

    @MockBean
    private TrustStatusValidationService trustStatusValidationService;

    @MockBean
    private KerberosConfigService kerberosConfigService;

    @MockBean
    private StackHelper stackHelper;

    @MockBean
    private MitBaseClusterTrustCommandsBuilder mitBaseClusterTrustCommandsBuilder;

    @MockBean
    private MitTrustInstructionsBuilder mitTrustInstructionsBuilder;

    @MockBean
    private ActiveDirectoryTrustInstructionsBuilder activeDirectoryTrustInstructionsBuilder;

    @MockBean
    private ActiveDirectoryBaseClusterTrustCommandsBuilder activeDirectoryBaseClusterTrustCommandsBuilder;

    @Test
    void testGetTrustProvider() {
        when(crossRealmTrustRepository.findByStackId(STACK_ID)).thenReturn(Optional.of(crossRealmTrust));
        when(crossRealmTrust.getKdcType()).thenReturn(KdcType.ACTIVE_DIRECTORY);

        TrustProvider adTrustProvider = underTest.getTrustProvider(STACK_ID);
        assertTrue(adTrustProvider instanceof ActiveDirectoryTrustService);

        when(crossRealmTrust.getKdcType()).thenReturn(KdcType.MIT);
        TrustProvider mitTrustProvider = underTest.getTrustProvider(STACK_ID);
        assertTrue(mitTrustProvider instanceof MitKdcTrustService);
    }

    @TestConfiguration
    @Import(value = {
            CrossRealmTrustService.class,
            MitKdcTrustService.class,
            ActiveDirectoryTrustService.class,
    })
    static class Config {
    }
}