package com.sequenceiq.cloudbreak.service.gateway;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.sequenceiq.cloudbreak.common.exception.NotFoundException;
import com.sequenceiq.cloudbreak.domain.stack.cluster.gateway.Gateway;
import com.sequenceiq.cloudbreak.repository.GatewayRepository;
import com.sequenceiq.cloudbreak.view.GatewayView;

@ExtendWith(MockitoExtension.class)
class GatewayServiceTest {

    private static final Long GATEWAY_ID = 1L;

    @Mock
    private GatewayRepository gatewayRepository;

    @InjectMocks
    private GatewayService underTest;

    @Test
    void testGenerateAndUpdateSignKeys() {
        ReflectionTestUtils.setField(underTest, "httpsPort", "666");
        Gateway input = new Gateway();
        input.setId(GATEWAY_ID);
        when(gatewayRepository.findById(GATEWAY_ID)).thenReturn(Optional.of(input));
        when(gatewayRepository.save(input)).thenReturn(input);

        underTest.generateAndUpdateSignKeys(input);

        assertNotNull(input.getSignPub());
        assertNotNull(input.getSignCert());
        assertNotNull(input.getSignKey());
        verify(gatewayRepository, times(1)).save(input);
    }

    @Test
    void testGenerateAndUpdateSignKeysException() {
        assertThrows(NotFoundException.class, () -> underTest.generateAndUpdateSignKeys(new Gateway()));
    }

    @Test
    void testGenerateSignKeys() {
        Gateway input = new Gateway();
        input.setId(GATEWAY_ID);
        Gateway gatewayWithKeys = underTest.generateSignKeys(input);
        assertNotNull(gatewayWithKeys.getSignKeySecret());
        assertEquals(gatewayWithKeys.getSignKeySecret().getRaw(), gatewayWithKeys.getSignKey());
        assertNotNull(gatewayWithKeys.getSignPubSecret());
        assertEquals(gatewayWithKeys.getSignPubSecret().getRaw(), gatewayWithKeys.getSignPub());
        assertEquals(gatewayWithKeys.getSignPubDeprecated(), gatewayWithKeys.getSignPub());
        assertNotNull(gatewayWithKeys.getSignCertSecret());
        assertEquals(gatewayWithKeys.getSignCertSecret().getRaw(), gatewayWithKeys.getSignCert());
        assertEquals(gatewayWithKeys.getSignCertDeprecated(), gatewayWithKeys.getSignCert());
    }

    @Test
    void testPutLegacyFieldsIntoVaultIfNecessary() {
        ReflectionTestUtils.setField(underTest, "httpsPort", "666");
        Gateway input = new Gateway();
        input.setId(GATEWAY_ID);
        input.setSignPubDeprecated("signPub");
        input.setSignCertDeprecated("signCert");
        when(gatewayRepository.findById(GATEWAY_ID)).thenReturn(Optional.of(input));
        when(gatewayRepository.save(input)).thenReturn(input);

        GatewayView result = underTest.putLegacyFieldsIntoVaultIfNecessary(input);

        assertEquals("signPub", result.getSignPubSecret().getRaw());
        assertEquals("signCert", result.getSignCertSecret().getRaw());
        verify(gatewayRepository, times(1)).save(input);
    }

    @Test
    void testSetLegacyFieldsForServiceRollback() {
        ReflectionTestUtils.setField(underTest, "httpsPort", "666");
        Gateway input = new Gateway();
        input.setId(GATEWAY_ID);
        input.setSignPub("signPub");
        input.setSignCert("signCert");
        when(gatewayRepository.findById(GATEWAY_ID)).thenReturn(Optional.of(input));
        when(gatewayRepository.save(input)).thenReturn(input);

        underTest.setLegacyFieldsForServiceRollback(input);

        assertEquals(input.getSignCertSecret().getRaw(), input.getSignCertDeprecated());
        assertEquals(input.getSignPubSecret().getRaw(), input.getSignPubDeprecated());
        verify(gatewayRepository, times(1)).save(input);
    }

}