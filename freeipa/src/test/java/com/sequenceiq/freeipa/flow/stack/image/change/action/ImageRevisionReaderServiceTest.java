package com.sequenceiq.freeipa.flow.stack.image.change.action;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.function.Supplier;

import jakarta.persistence.EntityManager;

import org.hibernate.envers.AuditReader;
import org.hibernate.envers.AuditReaderFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.stubbing.Answer;

import com.sequenceiq.cloudbreak.common.service.TransactionService;
import com.sequenceiq.freeipa.entity.ImageEntity;

@ExtendWith(MockitoExtension.class)
public class ImageRevisionReaderServiceTest {

    private static final long IMAGE_ENTITY_ID = 3L;

    private static final long REVISION = 5L;

    @Mock
    private EntityManager entityManager;

    @Mock
    private TransactionService transactionService;

    @Mock
    private AuditReader auditReader;

    @InjectMocks
    private ImageRevisionReaderService underTest;

    @Test
    void testGetRevisions() throws TransactionService.TransactionExecutionException {
        try (MockedStatic<AuditReaderFactory> auditReaderFactory = mockStatic(AuditReaderFactory.class)) {
            setupInvokeAuditReaderGetRevisionsInTransaction(auditReaderFactory);
            when(auditReader.getRevisions(ImageEntity.class, IMAGE_ENTITY_ID)).thenReturn(List.of(1, 2, 3));

            List<Number> revisions = underTest.getRevisions(IMAGE_ENTITY_ID);

            assertThat(revisions).hasSameElementsAs(List.of(1, 2, 3));
        }
    }

    @Test
    void testFind() throws TransactionService.TransactionExecutionException {
        try (MockedStatic<AuditReaderFactory> auditReaderFactory = mockStatic(AuditReaderFactory.class)) {
            ImageEntity imageEntity = new ImageEntity();
            imageEntity.setId(IMAGE_ENTITY_ID);
            setupInvokeAuditReaderFindInTransaction(auditReaderFactory);
            when(auditReader.find(ImageEntity.class, IMAGE_ENTITY_ID, REVISION)).thenReturn(imageEntity);

            ImageEntity returnedEntity = underTest.find(IMAGE_ENTITY_ID, REVISION);

            assertEquals(imageEntity, returnedEntity);
        }
    }

    private void setupInvokeAuditReaderGetRevisionsInTransaction(MockedStatic<AuditReaderFactory> auditReaderFactory)
            throws TransactionService.TransactionExecutionException {
        auditReaderFactory.when(() -> AuditReaderFactory.get(entityManager)).thenReturn(auditReader);
        when(transactionService.required(any(Supplier.class)))
                .thenAnswer((Answer<List<Number>>) invocation -> {
                    Supplier<List<Number>> listSupplier = invocation.getArgument(0);
                    return listSupplier.get();
                });
    }

    private void setupInvokeAuditReaderFindInTransaction(MockedStatic<AuditReaderFactory> auditReaderFactory)
            throws TransactionService.TransactionExecutionException {
        auditReaderFactory.when(() -> AuditReaderFactory.get(entityManager)).thenReturn(auditReader);
        when(transactionService.required(any(Supplier.class)))
                .thenAnswer((Answer<ImageEntity>) invocation -> {
                    Supplier<ImageEntity> listSupplier = invocation.getArgument(0);
                    return listSupplier.get();
                });
    }

}
