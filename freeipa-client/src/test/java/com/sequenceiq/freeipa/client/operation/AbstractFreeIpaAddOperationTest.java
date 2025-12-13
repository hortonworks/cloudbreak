package com.sequenceiq.freeipa.client.operation;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.googlecode.jsonrpc4j.JsonRpcClientException;
import com.sequenceiq.freeipa.client.FreeIpaClient;
import com.sequenceiq.freeipa.client.FreeIpaClientException;
import com.sequenceiq.freeipa.client.FreeIpaErrorCodes;

@ExtendWith(MockitoExtension.class)
public class AbstractFreeIpaAddOperationTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractFreeIpaAddOperationTest.class);

    private static final String OPERATION_NAME = "opName";

    private static final String NAME = "name";

    @Mock
    private FreeIpaClient freeIpaClient;

    @Test
    public void testReturnValue() throws FreeIpaClientException {
        AbstractFreeipaOperation victim = new FreeIpaAddOperation(NAME, null, FreeIpaAddOperationBehaviour.RETURN);

        Optional<Object> result = victim.invoke(freeIpaClient);

        assertTrue(result.isPresent());
    }

    @Test
    public void testReturnEmptyInCaseOfDuplicateAndMissingGetOperation() throws FreeIpaClientException {
        AbstractFreeipaOperation victim = new FreeIpaAddOperation(NAME, null, FreeIpaAddOperationBehaviour.THROWS_DUPLICATE_ENTRY_EXEPTION);

        Optional<Object> result = victim.invoke(freeIpaClient);

        assertTrue(result.isEmpty());
    }

    @Test
    public void testReturnValueByGetOperationInCaseOfDuplicate() throws FreeIpaClientException {
        AbstractFreeIpaAddOperation<Object> getOperation = mock(AbstractFreeIpaAddOperation.class);
        AbstractFreeipaOperation victim = new FreeIpaAddOperation(NAME, getOperation, FreeIpaAddOperationBehaviour.THROWS_DUPLICATE_ENTRY_EXEPTION);

        when(getOperation.invoke(freeIpaClient)).thenReturn(Optional.of(new Object()));

        Optional<Object> result = victim.invoke(freeIpaClient);

        assertTrue(result.isPresent());
    }

    @Test
    public void testThrowFreeIpaClientException() throws FreeIpaClientException {
        AbstractFreeipaOperation victim = new FreeIpaAddOperation(NAME, null, FreeIpaAddOperationBehaviour.THROWS_OTHER_EXCEPTION);

        assertThrows(FreeIpaClientException.class, () -> victim.invoke(freeIpaClient));
    }

    private enum FreeIpaAddOperationBehaviour {
        RETURN,
        THROWS_DUPLICATE_ENTRY_EXEPTION,
        THROWS_OTHER_EXCEPTION
    }

    private static class FreeIpaAddOperation extends AbstractFreeIpaAddOperation<Object> {

        private final FreeIpaAddOperationBehaviour behaviour;

        private FreeIpaAddOperation(String name, AbstractFreeipaOperation<Object> getOperation, FreeIpaAddOperationBehaviour behaviour) {
            super(name, getOperation, Object.class);
            this.behaviour = behaviour;
        }

        @Override
        protected Logger getLogger() {
            return LOGGER;
        }

        @Override
        public String getOperationName() {
            return OPERATION_NAME;
        }

        @Override
        protected Object invoke(FreeIpaClient freeipaClient, Class<Object> clazz) throws FreeIpaClientException {
            switch (behaviour) {
                case RETURN:
                    return new Object();
                case THROWS_DUPLICATE_ENTRY_EXEPTION:
                    throw new FreeIpaClientException("",
                            new JsonRpcClientException(FreeIpaErrorCodes.DUPLICATE_ENTRY.getValue(), null, null));
                case THROWS_OTHER_EXCEPTION:
                    throw new FreeIpaClientException("",
                            new JsonRpcClientException(FreeIpaErrorCodes.GENERIC_ERROR.getValue(), null, null));
                default:
                    throw new RuntimeException("Not implemented behaviour");
            }
        }
    }
}