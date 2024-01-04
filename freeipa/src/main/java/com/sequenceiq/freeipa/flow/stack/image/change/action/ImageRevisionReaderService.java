package com.sequenceiq.freeipa.flow.stack.image.change.action;

import java.util.List;

import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

import org.hibernate.envers.AuditReader;
import org.hibernate.envers.AuditReaderFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.common.service.TransactionService;
import com.sequenceiq.cloudbreak.service.OperationException;
import com.sequenceiq.freeipa.entity.ImageEntity;

@Service
public class ImageRevisionReaderService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ImageRevisionReaderService.class);

    @PersistenceContext
    private EntityManager entityManager;

    @Inject
    private TransactionService transactionService;

    public List<Number> getRevisions(Long imageEntityId) {
        try {
            return transactionService.required(() -> {
                AuditReader auditReader = AuditReaderFactory.get(entityManager);
                return auditReader.getRevisions(ImageEntity.class, imageEntityId);
            });
        } catch (TransactionService.TransactionExecutionException e) {
            String message = String.format("Could not retrieve revisions for Freeipa Image: %s", e);
            LOGGER.error(message);
            throw new OperationException(message);
        }
    }

    public ImageEntity find(Long imageEntityId, Number revision) {
        try {
            return transactionService.required(() -> {
                AuditReader auditReader = AuditReaderFactory.get(entityManager);
                return auditReader.find(ImageEntity.class, imageEntityId, revision);
            });
        } catch (TransactionService.TransactionExecutionException e) {
            String message = String.format("Could not retrieve Freeipa image of revision %s: %s", revision, e);
            LOGGER.error(message);
            throw new OperationException(message);
        }
    }

}
