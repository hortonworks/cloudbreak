package com.sequenceiq.cloudbreak.service.stack.archive;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.sql.SQLException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

import org.hibernate.JDBCException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import com.sequenceiq.cloudbreak.common.service.TransactionService;
import com.sequenceiq.cloudbreak.converter.InstanceMetadataToArchivedInstanceMetadataConverter;
import com.sequenceiq.cloudbreak.domain.stack.instance.ArchivedInstanceMetaData;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceGroup;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceMetaData;
import com.sequenceiq.cloudbreak.domain.view.StackView;
import com.sequenceiq.cloudbreak.job.archiver.instancemetadata.ArchiveInstanceMetaDataConfig;
import com.sequenceiq.cloudbreak.repository.ArchivedInstanceMetaDataRepository;
import com.sequenceiq.cloudbreak.service.CloudbreakRuntimeException;
import com.sequenceiq.cloudbreak.service.stack.InstanceGroupService;
import com.sequenceiq.cloudbreak.service.stack.InstanceMetaDataService;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

@ExtendWith(MockitoExtension.class)
class ArchiveInstanceMetaDataServiceTest {

    private static final String RESOURCE_CRN = "crn:env";

    private static final long STACK_ID = 0L;

    private static final String INSTANCEID = "instanceid";

    private static final String INSTANCEID_2 = "instanceid2";

    private static final String GROUP_NAME = "group";

    @InjectMocks
    private ArchiveInstanceMetaDataService underTest;

    @Mock
    private InstanceMetaDataService instanceMetaDataService;

    @Mock
    private ArchivedInstanceMetaDataRepository archivedInstanceMetaDataRepository;

    @Mock
    private TransactionService transactionService;

    @Mock
    private ArchiveInstanceMetaDataConfig archiveInstanceMetaDataConfig;

    @Mock
    private InstanceGroupService instanceGroupService;

    @Spy
    @SuppressFBWarnings(value = "URF_UNREAD_FIELD", justification = "Injected by Mockito")
    private InstanceMetadataToArchivedInstanceMetadataConverter converter = new InstanceMetadataToArchivedInstanceMetadataConverter();

    @BeforeEach
    public void setUp() throws Exception {
        when(archiveInstanceMetaDataConfig.getArchiveOlderThanWeeks()).thenReturn(4);
        when(archiveInstanceMetaDataConfig.getPageSize()).thenReturn(1);

        lenient().doAnswer(invocation -> ((Supplier<?>) invocation.getArgument(0)).get()).when(transactionService).required(any(Supplier.class));
    }

    @Test
    void testArchiveWithNoTerminatedIMD() throws Exception {
        StackView stack = new StackView();
        stack.setResourceCrn(RESOURCE_CRN);
        stack.setId(STACK_ID);

        Page<InstanceMetaData> page = new PageImpl<>(List.of());
        when(instanceMetaDataService.getTerminatedInstanceMetaDataBefore(eq(STACK_ID), any(), any())).thenReturn(page);

        underTest.archive(stack);

        verify(archivedInstanceMetaDataRepository, times(0)).saveAll(any());
    }

    @Test
    void testArchiveWithExceptionWhenGettingTerminatedIMD() throws Exception {
        StackView stack = new StackView();
        stack.setResourceCrn(RESOURCE_CRN);
        stack.setId(STACK_ID);

        JDBCException exception = new JDBCException("Random SQL exception", new SQLException());
        when(instanceMetaDataService.getTerminatedInstanceMetaDataBefore(eq(STACK_ID), any(), any())).thenThrow(exception);

        ArchiveInstanceMetaDataException actual = assertThrows(ArchiveInstanceMetaDataException.class, () -> underTest.archive(stack));
        assertThat(actual.getMessage()).isEqualTo("Something unexpected went wrong with stack crn:env while archiving terminated InstanceMetaData");
    }

    @Test
    void testArchiveWithExceptionWhenTransactionExecutionExceptionThrown() throws Exception {
        StackView stack = new StackView();
        stack.setResourceCrn(RESOURCE_CRN);
        stack.setId(STACK_ID);

        TransactionService.TransactionExecutionException exception =
                new TransactionService.TransactionExecutionException("Transaction failed", new CloudbreakRuntimeException(""));
        when(transactionService.required(any(Supplier.class))).thenThrow(exception);

        ArchiveInstanceMetaDataException actual = assertThrows(ArchiveInstanceMetaDataException.class, () -> underTest.archive(stack));
        assertThat(actual.getMessage()).isEqualTo("Failed to archive the batch #0 of terminated instancemetadata for stack crn:env");
    }

    @Test
    void testArchiveWithSinglePageTerminatedIMD() throws Exception {
        StackView stack = new StackView();
        stack.setResourceCrn(RESOURCE_CRN);
        stack.setId(STACK_ID);

        InstanceGroup group = new InstanceGroup();
        group.setGroupName(GROUP_NAME);
        InstanceMetaData instance = new InstanceMetaData();
        instance.setInstanceGroup(group);
        instance.setInstanceId(INSTANCEID);
        group.setInstanceMetaData(new HashSet<>(Set.of(instance)));
        Page<InstanceMetaData> page = new PageImpl<>(List.of(instance));
        when(instanceMetaDataService.getTerminatedInstanceMetaDataBefore(eq(STACK_ID), any(), any())).thenReturn(page);

        underTest.archive(stack);

        ArgumentCaptor<List> archivedCaptor = ArgumentCaptor.forClass(List.class);
        verify(archivedInstanceMetaDataRepository, times(1)).saveAll(archivedCaptor.capture());
        List<ArchivedInstanceMetaData> archivedList = (List<ArchivedInstanceMetaData>) archivedCaptor.getValue();
        assertThat(archivedList.size()).isEqualTo(1);
        assertThat(archivedList.get(0).getInstanceId()).isEqualTo(INSTANCEID);

        ArgumentCaptor<Set> groupCaptor = ArgumentCaptor.forClass(Set.class);
        verify(instanceGroupService, times(1)).saveAll(groupCaptor.capture());
        Set<InstanceGroup> groupSet = (Set<InstanceGroup>) groupCaptor.getValue();
        assertThat(groupSet.size()).isEqualTo(1);
        assertThat(groupSet.iterator().next().getGroupName()).isEqualTo(GROUP_NAME);

        ArgumentCaptor<List> deletedCaptor = ArgumentCaptor.forClass(List.class);
        verify(instanceMetaDataService, times(1)).deleteAll(deletedCaptor.capture());
        List<InstanceMetaData> deletedList = (List<InstanceMetaData>) deletedCaptor.getValue();
        assertThat(deletedList.size()).isEqualTo(1);
        assertThat(deletedList.get(0).getInstanceId()).isEqualTo(INSTANCEID);
    }

    @Test
    void testArchiveWithMultiplePageTerminatedIMD() throws Exception {
        StackView stack = new StackView();
        stack.setResourceCrn(RESOURCE_CRN);
        stack.setId(STACK_ID);

        InstanceGroup group = new InstanceGroup();
        group.setGroupName(GROUP_NAME);
        InstanceMetaData instance1 = new InstanceMetaData();
        instance1.setInstanceGroup(group);
        instance1.setInstanceId(INSTANCEID);

        InstanceMetaData instance2 = new InstanceMetaData();
        instance2.setInstanceGroup(group);
        instance2.setInstanceId(INSTANCEID_2);

        group.setInstanceMetaData(new HashSet<>(Set.of(instance1, instance2)));
        Page<InstanceMetaData> page1 = new PageImpl<>(List.of(instance1), Pageable.ofSize(1), 2);
        Page<InstanceMetaData> page2 = new PageImpl<>(List.of(instance2), Pageable.ofSize(1), 2);
        when(instanceMetaDataService.getTerminatedInstanceMetaDataBefore(eq(STACK_ID), any(), any()))
                .thenReturn(page1)
                .thenReturn(page2);

        underTest.archive(stack);

        ArgumentCaptor<List<ArchivedInstanceMetaData>> archivedCaptor = ArgumentCaptor.forClass(List.class);
        verify(archivedInstanceMetaDataRepository, times(2)).saveAll(archivedCaptor.capture());
        List<List<ArchivedInstanceMetaData>> archivedList = archivedCaptor.getAllValues();
        assertThat(archivedList.size()).isEqualTo(2);
        assertThat(archivedList.get(0).get(0).getInstanceId()).isEqualTo(INSTANCEID);
        assertThat(archivedList.get(1).get(0).getInstanceId()).isEqualTo(INSTANCEID_2);

        ArgumentCaptor<Set<InstanceGroup>> groupCaptor = ArgumentCaptor.forClass(Set.class);
        verify(instanceGroupService, times(2)).saveAll(groupCaptor.capture());
        List<Set<InstanceGroup>> groupSet = groupCaptor.getAllValues();
        assertThat(groupSet.size()).isEqualTo(2);
        assertThat(groupSet.get(0).iterator().next().getGroupName()).isEqualTo(GROUP_NAME);
        assertThat(groupSet.get(1).iterator().next().getGroupName()).isEqualTo(GROUP_NAME);

        ArgumentCaptor<List<InstanceMetaData>> deletedCaptor = ArgumentCaptor.forClass(List.class);
        verify(instanceMetaDataService, times(2)).deleteAll(deletedCaptor.capture());
        List<List<InstanceMetaData>> deletedList = deletedCaptor.getAllValues();
        assertThat(deletedList.size()).isEqualTo(2);
        assertThat(deletedList.get(0).get(0).getInstanceId()).isEqualTo(INSTANCEID);
        assertThat(deletedList.get(1).get(0).getInstanceId()).isEqualTo(INSTANCEID_2);
    }
}