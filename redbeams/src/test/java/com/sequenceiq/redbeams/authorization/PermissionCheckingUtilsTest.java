package com.sequenceiq.redbeams.authorization;

import static org.mockito.Mockito.any;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import com.google.common.collect.ImmutableSet;
import com.sequenceiq.cloudbreak.auth.altus.Crn;
import com.sequenceiq.cloudbreak.auth.altus.GrpcUmsClient;
import com.sequenceiq.redbeams.domain.DatabaseConfig;

import java.util.Optional;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.security.access.AccessDeniedException;

public class PermissionCheckingUtilsTest {

    private static final String USER_CRN = "crn:altus:iam:us-west-1:cloudera:user:bob@cloudera.com";

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @InjectMocks
    private PermissionCheckingUtils underTest;

    @Mock
    private GrpcUmsClient grpcUmsClient;

    private DatabaseConfig db;

    private DatabaseConfig db2;

    @Before
    public void setUp() {
        initMocks(this);

        db = new DatabaseConfig();
        db.setId(1L);
        db.setName("mydb");
        db.setResourceCrn(Crn.safeFromString("crn:altus:redbeams:us-west-1:cloudera:database:mydb"));

        db2 = new DatabaseConfig();
        db2.setId(2L);
        db2.setName("mydb2");
        db2.setResourceCrn(Crn.safeFromString("crn:altus:redbeams:us-west-1:cloudera:database:mydb2"));
    }

    @Test
    public void testCheckPermissionsByTargetNull() {
        underTest.checkPermissionsByTarget(null, USER_CRN, ResourceAction.READ);
    }

    @Test
    public void testCheckPermissionsByTargetSinglePass() {
        when(grpcUmsClient.checkRight(eq(USER_CRN), eq(ResourceAction.READ.name()), eq(db.getResourceCrn().toString()), any(String.class)))
            .thenReturn(true);

        underTest.checkPermissionsByTarget(db, USER_CRN, ResourceAction.READ);

        verify(grpcUmsClient).checkRight(eq(USER_CRN), eq(ResourceAction.READ.name()), eq(db.getResourceCrn().toString()), any(String.class));
    }

    @Test
    public void testCheckPermissionsByTargetSingleFail() {
        thrown.expect(AccessDeniedException.class);
        when(grpcUmsClient.checkRight(eq(USER_CRN), eq(ResourceAction.READ.name()), eq(db.getResourceCrn().toString()), any(String.class)))
            .thenReturn(false);

        underTest.checkPermissionsByTarget(db, USER_CRN, ResourceAction.READ);
    }

    @Test
    public void testCheckPermissionsByTargetSingleOptionalPass() {
        when(grpcUmsClient.checkRight(eq(USER_CRN), eq(ResourceAction.READ.name()), eq(db.getResourceCrn().toString()), any(String.class)))
            .thenReturn(true);

        underTest.checkPermissionsByTarget(Optional.of(db), USER_CRN, ResourceAction.READ);

        verify(grpcUmsClient).checkRight(eq(USER_CRN), eq(ResourceAction.READ.name()), eq(db.getResourceCrn().toString()), any(String.class));
    }

    @Test
    public void testCheckPermissionsByTargetSingleOptionalFail() {
        thrown.expect(AccessDeniedException.class);
        when(grpcUmsClient.checkRight(eq(USER_CRN), eq(ResourceAction.READ.name()), eq(db.getResourceCrn().toString()), any(String.class)))
            .thenReturn(false);

        underTest.checkPermissionsByTarget(Optional.of(db), USER_CRN, ResourceAction.READ);
    }

    @Test
    public void testCheckPermissionsByTargetSingleOptionalEmptyPass() {
        underTest.checkPermissionsByTarget(Optional.empty(), USER_CRN, ResourceAction.READ);
    }

    @Test
    public void testCheckPermissionsByTargetLeelooDallasMultiPass() {
        when(grpcUmsClient.checkRight(eq(USER_CRN), eq(ResourceAction.READ.name()), eq(db.getResourceCrn().toString()), any(String.class)))
            .thenReturn(true);
        when(grpcUmsClient.checkRight(eq(USER_CRN), eq(ResourceAction.READ.name()), eq(db2.getResourceCrn().toString()), any(String.class)))
            .thenReturn(true);

        underTest.checkPermissionsByTarget(ImmutableSet.of(db, db2), USER_CRN, ResourceAction.READ);

        verify(grpcUmsClient).checkRight(eq(USER_CRN), eq(ResourceAction.READ.name()), eq(db.getResourceCrn().toString()), any(String.class));
        verify(grpcUmsClient).checkRight(eq(USER_CRN), eq(ResourceAction.READ.name()), eq(db2.getResourceCrn().toString()), any(String.class));
    }

    @Test
    public void testCheckPermissionsByTargetMultiFail() {
        thrown.expect(AccessDeniedException.class);
        when(grpcUmsClient.checkRight(eq(USER_CRN), eq(ResourceAction.READ.name()), eq(db.getResourceCrn().toString()), any(String.class)))
            .thenReturn(false);
        when(grpcUmsClient.checkRight(eq(USER_CRN), eq(ResourceAction.READ.name()), eq(db2.getResourceCrn().toString()), any(String.class)))
            .thenReturn(true);

        underTest.checkPermissionsByTarget(ImmutableSet.of(db, db2), USER_CRN, ResourceAction.READ);
    }

    @Test
    public void testCheckPermissionsByTargetNoCrn() {
        thrown.expect(IllegalStateException.class);

        underTest.checkPermissionsByTarget("nope", USER_CRN, ResourceAction.READ);
    }
}
