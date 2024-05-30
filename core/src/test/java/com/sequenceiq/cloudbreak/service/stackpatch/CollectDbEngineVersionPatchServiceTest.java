package com.sequenceiq.cloudbreak.service.stackpatch;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Optional;

import jakarta.ws.rs.NotFoundException;

import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.domain.stack.Database;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.StackPatchType;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorFailedException;
import com.sequenceiq.cloudbreak.sdx.common.PlatformAwareSdxConnector;
import com.sequenceiq.cloudbreak.sdx.common.model.SdxBasicView;
import com.sequenceiq.cloudbreak.service.database.EmbeddedDbVersionCollector;
import com.sequenceiq.cloudbreak.service.datalake.SdxClientService;
import com.sequenceiq.cloudbreak.service.externaldatabase.ExternalDbVersionCollector;
import com.sequenceiq.cloudbreak.service.stack.StackService;

@ExtendWith(MockitoExtension.class)
class CollectDbEngineVersionPatchServiceTest {

    private static final Long STACK_ID = 3L;

    private static final String STACK_CRN = "stackCrn";

    private static final String STACK_CRN_2 = "stackCrn2";

    private static final String ENV_CRN = "envCrn";

    private static final String STACK_NAME = "stackName";

    private static final String DB_CRN = "dbCrn";

    private static final String DB_VERSION = "10";

    @Mock
    private ExternalDbVersionCollector externalDbVersionCollector;

    @Mock
    private EmbeddedDbVersionCollector embeddedDbVersionCollector;

    @Mock
    private StackService stackService;

    @Mock
    private SdxClientService sdxClientService;

    @Mock
    private PlatformAwareSdxConnector platformAwareSdxConnector;

    @InjectMocks
    private CollectDbEngineVersionPatchService underTest;

    @Test
    public void testStackPatchType() {
        assertEquals(StackPatchType.COLLECT_DB_ENGINE_VERSION, underTest.getStackPatchType());
    }

    @Test
    public void testAffected() {
        Stack stack = new Stack();
        Database database = new Database();
        stack.setDatabase(database);
        assertTrue(underTest.isAffected(stack));
        database.setExternalDatabaseEngineVersion("asdf");
        assertFalse(underTest.isAffected(stack));
    }

    // @formatter:off
    // CHECKSTYLE:OFF
    static Object[][] scenarios() {
        return new Object[][]{
                // name                                     stackAvailability datalake dbCrn    dbVersion  expected
                {"Available embedded distrox with db version", true, false, null, DB_VERSION, true},
                {"Available embedded distrox without db version", true, false, null, null, false},
                {"Not available embedded distrox with db version", false, false, null, DB_VERSION, false},
                {"Not available embedded distrox without db version", false, false, null, null, false},
                {"Available external distrox with db version", true, false, DB_CRN, DB_VERSION, true},
                {"Available external distrox without db version", true, false, DB_CRN, null, false},
                {"Not available external distrox with db version", false, false, DB_CRN, DB_VERSION, true},
                {"Not available external distrox without db version", false, false, DB_CRN, null, false},
                {"Available embedded datalake with db version", true, true, null, DB_VERSION, true},
                {"Available embedded datalake without db version", true, true, null, null, false},
                {"Not available embedded datalake with db version", false, true, null, DB_VERSION, false},
                {"Not available embedded datalake without db version", false, true, null, null, false},
                {"Available external datalake with db version", true, true, DB_CRN, DB_VERSION, true},
                {"Available external datalake without db version", true, true, DB_CRN, null, false},
                {"Not available external datalake with db version", false, true, DB_CRN, DB_VERSION, true},
                {"Not available external datalake without db version", false, true, DB_CRN, null, false},
        };
    }
    // CHECKSTYLE:ON
    // @formatter:on

    @ParameterizedTest(name = "{0}")
    @MethodSource("scenarios")
    public void testApply(String name, boolean stackAvailability, boolean datalake, String databaseServerCrn, String dbVersion, boolean expected)
            throws CloudbreakOrchestratorFailedException, ExistingStackPatchApplyException {
        Stack stack = mock(Stack.class);
        when(stack.isAvailable()).thenReturn(stackAvailability);
        lenient().when(stack.getResourceCrn()).thenReturn(STACK_CRN);
        lenient().when(stack.getId()).thenReturn(STACK_ID);
        lenient().when(stack.isDatalake()).thenReturn(datalake);
        Cluster cluster = new Cluster();
        cluster.setDatabaseServerCrn(databaseServerCrn);
        when(stack.getCluster()).thenReturn(cluster);
        Optional<String> dbVersionResult = Optional.ofNullable(dbVersion);
        if (StringUtils.isBlank(databaseServerCrn)) {
            lenient().when(embeddedDbVersionCollector.collectDbVersion(stack)).thenReturn(dbVersionResult);
        } else {
            lenient().when(externalDbVersionCollector.collectDbVersion(databaseServerCrn)).thenReturn(dbVersionResult);
        }

        boolean result = underTest.doApply(stack);

        assertEquals(expected, result);
        if (expected) {
            if (datalake) {
                verify(sdxClientService).updateDatabaseEngineVersion(STACK_CRN, dbVersion);
            }
            verify(stackService).updateExternalDatabaseEngineVersion(STACK_ID, dbVersion);
        } else {
            verifyNoInteractions(sdxClientService, stackService);
        }
    }

    @Test
    public void testCrnDifferentForDatalake() throws ExistingStackPatchApplyException {
        Stack stack = mock(Stack.class);
        when(stack.isAvailable()).thenReturn(true);
        when(stack.getResourceCrn()).thenReturn(STACK_CRN);
        when(stack.getId()).thenReturn(STACK_ID);
        when(stack.isDatalake()).thenReturn(true);
        when(stack.getEnvironmentCrn()).thenReturn(ENV_CRN);
        Cluster cluster = new Cluster();
        cluster.setDatabaseServerCrn(DB_CRN);
        when(stack.getCluster()).thenReturn(cluster);
        Optional<String> dbVersionResult = Optional.of(DB_VERSION);
        when(externalDbVersionCollector.collectDbVersion(DB_CRN)).thenReturn(dbVersionResult);
        doThrow(new NotFoundException("nope")).when(sdxClientService).updateDatabaseEngineVersion(STACK_CRN, DB_VERSION);
        lenient().when(platformAwareSdxConnector.getSdxBasicViewByEnvironmentCrn(any())).thenReturn(
                Optional.of(new SdxBasicView(null, STACK_CRN_2, null, null, false, 1L, null)));

        boolean result = underTest.doApply(stack);

        assertTrue(result);
        verify(sdxClientService).updateDatabaseEngineVersion(STACK_CRN_2, DB_VERSION);
        verify(stackService).updateExternalDatabaseEngineVersion(STACK_ID, DB_VERSION);
    }

    @Test
    public void testDatalakeDoesntExist() {
        Stack stack = mock(Stack.class);
        when(stack.isAvailable()).thenReturn(true);
        when(stack.getResourceCrn()).thenReturn(STACK_CRN);
        when(stack.isDatalake()).thenReturn(true);
        when(stack.getEnvironmentCrn()).thenReturn(ENV_CRN);
        Cluster cluster = new Cluster();
        cluster.setDatabaseServerCrn(DB_CRN);
        when(stack.getCluster()).thenReturn(cluster);
        Optional<String> dbVersionResult = Optional.of(DB_VERSION);
        when(externalDbVersionCollector.collectDbVersion(DB_CRN)).thenReturn(dbVersionResult);
        doThrow(new NotFoundException("nope")).when(sdxClientService).updateDatabaseEngineVersion(STACK_CRN, DB_VERSION);
        when(platformAwareSdxConnector.getSdxBasicViewByEnvironmentCrn(any())).thenReturn(Optional.empty());

        assertThrows(com.sequenceiq.cloudbreak.common.exception.NotFoundException.class, () -> underTest.doApply(stack));

        verifyNoInteractions(stackService);
    }
}
