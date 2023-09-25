package com.sequenceiq.cloudbreak.service.idbroker;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.converter.IdBrokerConverterUtil;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.domain.stack.cluster.IdBroker;
import com.sequenceiq.cloudbreak.repository.IdBrokerRepository;
import com.sequenceiq.cloudbreak.service.cluster.ClusterService;

@ExtendWith(MockitoExtension.class)
public class IdBrokerServiceTest {

    private static final Long STACK_ID = 1L;

    private static final Long CLUSTER_ID = 0L;

    private static final Long IDBROKER_ID = 1L;

    @InjectMocks
    private IdBrokerService underTest;

    @Mock
    private IdBrokerRepository repository;

    @Mock
    private ClusterService clusterService;

    @Mock
    private IdBrokerConverterUtil idBrokerConverterUtil;

    @Test
    public void testGenerateIdBrokerSignKeyWhenKeysNotExist() {
        Cluster cluster = new Cluster();
        cluster.setId(CLUSTER_ID);
        IdBroker idBroker = new IdBroker();

        when(clusterService.findOneByStackIdOrNotFoundError(STACK_ID)).thenReturn(cluster);
        when(repository.findByClusterId(CLUSTER_ID)).thenReturn(null);
        when(idBrokerConverterUtil.generateIdBrokerSignKeys(CLUSTER_ID, null)).thenReturn(idBroker);
        ArgumentCaptor<IdBroker> argumentCaptor = ArgumentCaptor.forClass(IdBroker.class);
        underTest.generateIdBrokerSignKey(STACK_ID);
        verify(repository).save(argumentCaptor.capture());
        assertEquals(idBroker, argumentCaptor.getValue());
    }

    @Test
    public void testGenerateIdBrokerSignKeyWhenKeysExist() {
        Cluster cluster = new Cluster();
        IdBroker idBroker = new IdBroker();

        when(clusterService.findOneByStackIdOrNotFoundError(STACK_ID)).thenReturn(cluster);
        when(repository.findByClusterId(cluster.getId())).thenReturn(idBroker);

        underTest.generateIdBrokerSignKey(STACK_ID);

        verify(repository, never()).save(any());
        verify(idBrokerConverterUtil, never()).generateIdBrokerSignKeys(CLUSTER_ID, null);
    }

    @Test
    void testPutLegacyFieldsIntoVaultIfNecessary() {
        IdBroker input = new IdBroker();
        input.setId(IDBROKER_ID);
        input.setSignPubDeprecated("signPub");
        input.setSignCertDeprecated("signCert");
        when(repository.findById(IDBROKER_ID)).thenReturn(Optional.of(input));
        when(repository.save(input)).thenReturn(input);

        IdBroker result = underTest.putLegacyFieldsIntoVaultIfNecessary(input.getId());

        assertEquals("signPub", result.getSignPubSecret().getRaw());
        assertEquals("signCert", result.getSignCertSecret().getRaw());
        verify(repository, times(1)).save(input);
    }

    @Test
    void testSetLegacyFieldsForServiceRollback() {
        IdBroker input = new IdBroker();
        input.setId(IDBROKER_ID);
        input.setSignPub("signPub");
        input.setSignCert("signCert");
        when(repository.findById(IDBROKER_ID)).thenReturn(Optional.of(input));
        when(repository.save(input)).thenReturn(input);

        underTest.setLegacyFieldsForServiceRollback(input.getId());

        assertEquals(input.getSignCertSecret().getRaw(), input.getSignCertDeprecated());
        assertEquals(input.getSignPubSecret().getRaw(), input.getSignPubDeprecated());
        verify(repository, times(1)).save(input);
    }
}
