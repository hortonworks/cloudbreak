package com.sequenceiq.redbeams.service.stack;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.common.api.type.CommonStatus;
import com.sequenceiq.common.api.type.ResourceType;
import com.sequenceiq.redbeams.converter.spi.DBResourceToCloudResourceConverter;
import com.sequenceiq.redbeams.domain.stack.DBResource;
import com.sequenceiq.redbeams.repository.DBResourceRepository;

@ExtendWith(MockitoExtension.class)
class DBResourceServiceTest {

    private static final long STACK_ID = 1L;

    @InjectMocks
    private DBResourceService underTest;

    @Mock
    private DBResourceRepository dbResourceRepository;

    @Mock
    private DBResourceToCloudResourceConverter dbResourceToCloudResourceConverter;

    @Mock
    private DBResource dbResource;

    @Test
    void testGetAllAsCloudResourceShouldReturnResourcesWhenAvailable() {
        CloudResource cloudResource = mock(CloudResource.class);
        when(dbResourceRepository.findAllByStackId(STACK_ID)).thenReturn(List.of(dbResource));
        when(dbResourceToCloudResourceConverter.convert(dbResource)).thenReturn(cloudResource);

        List<CloudResource> actual = underTest.getAllAsCloudResource(STACK_ID);

        assertEquals(List.of(cloudResource), actual);
    }

    @Test
    void testGetAllAsCloudResourceShouldReturnEmptyListWhenThereAraNoResourcesAvailable() {
        when(dbResourceRepository.findAllByStackId(STACK_ID)).thenReturn(Collections.emptyList());

        List<CloudResource> actual = underTest.getAllAsCloudResource(STACK_ID);

        assertTrue(actual.isEmpty());
        verifyNoInteractions(dbResourceToCloudResourceConverter);
    }

    @Test
    void testSaveShouldSaveTheResourcesSuccessfully() {
        when(dbResourceRepository.save(dbResource)).thenReturn(dbResource);

        DBResource actual = underTest.save(dbResource);

        assertEquals(dbResource, actual);
    }

    @Test
    void testDeleteShouldDeleteTheResourcesSuccessfully() {
        underTest.delete(dbResource);
        verify(dbResourceRepository).delete(dbResource);
    }

    @Test
    void testFindByStackAndNameAndTypeShouldReturnDbResource() {
        Long id = 334L;
        String name = "db-srv";

        when(dbResourceRepository.findByStackIdAndNameAndType(id, name, ResourceType.AZURE_DATABASE)).thenReturn(Optional.of(dbResource));

        Optional<DBResource> actual = underTest.findByStackAndNameAndType(id, name, ResourceType.AZURE_DATABASE);

        assertEquals(Optional.of(dbResource), actual);
    }

    @Test
    void testExistsByStackAndNameAndTypeShouldReturnTrueWhenTheDatabaseExists() {
        String name = "db-srv";
        when(dbResourceRepository.existsByStackAndNameAndType(STACK_ID, name, ResourceType.AZURE_DATABASE)).thenReturn(true);

        assertTrue(underTest.existsByStackAndNameAndType(STACK_ID, name, ResourceType.AZURE_DATABASE));
    }

    @Test
    void testFindByStatusAndTypeAndStackShouldReturnADbResource() {
        when(dbResourceRepository.findByResourceStatusAndResourceTypeAndDbStack(CommonStatus.CREATED, ResourceType.AZURE_DATABASE, STACK_ID))
                .thenReturn(Optional.of(dbResource));

        Optional<DBResource> actual = underTest.findByStatusAndTypeAndStack(CommonStatus.CREATED, ResourceType.AZURE_DATABASE, STACK_ID);

        assertEquals(Optional.of(dbResource), actual);
    }

}