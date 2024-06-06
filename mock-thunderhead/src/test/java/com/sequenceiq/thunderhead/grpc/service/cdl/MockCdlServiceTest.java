package com.sequenceiq.thunderhead.grpc.service.cdl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.cloudera.thunderhead.service.cdlcrud.CdlCrudProto;
import com.sequenceiq.cloudbreak.auth.crn.Crn;
import com.sequenceiq.cloudbreak.auth.crn.RegionAwareCrnGenerator;
import com.sequenceiq.thunderhead.entity.Cdl;
import com.sequenceiq.thunderhead.repository.CdlRespository;

import io.grpc.Status;
import io.grpc.StatusException;
import io.grpc.stub.StreamObserver;

@ExtendWith(MockitoExtension.class)
class MockCdlServiceTest {

    private static final String ENV_CRN = "crn:cdp:environments:us-west-1:accountId:environment:4c5ba74b-c35e-45e9-9f47-123456789876";

    private static final String CDL_CRN = "crn:cdp:sdxsvc:us-west-1:cloudera:instance:f22e7f31-a98d-424d-917a-a62a36cb3c9e";

    private static final String DB_SERVER_CRN = "crn:cdp:redbeams:us-west-1:default:databaseServer:e63520c8-aaf0-4bf3-b872-5613ce496ac3";

    @Mock
    private RegionAwareCrnGenerator regionAwareCrnGenerator;

    @Mock
    private CdlRespository cdlRespository;

    @InjectMocks
    private MockCdlService underTest;

    @Test
    void testCreate() {
        when(regionAwareCrnGenerator.generateCrn(any(), any(), any())).thenReturn(Crn.safeFromString(CDL_CRN));
        StreamObserver<CdlCrudProto.CreateDatalakeResponse> responseObserver = mock(StreamObserver.class);
        doNothing().when(responseObserver).onCompleted();
        ArgumentCaptor<CdlCrudProto.CreateDatalakeResponse> responseCaptor = ArgumentCaptor.forClass(CdlCrudProto.CreateDatalakeResponse.class);
        doNothing().when(responseObserver).onNext(responseCaptor.capture());
        when(cdlRespository.save(any())).thenReturn(new Cdl());

        underTest.createDatalake(CdlCrudProto.CreateDatalakeRequest.newBuilder().setEnvironmentName(ENV_CRN).build(), responseObserver);

        verify(responseObserver).onCompleted();
        verify(cdlRespository).save(any());
        assertEquals(responseCaptor.getValue().getCrn(), CDL_CRN);
    }

    @Test
    void findDatalakeWhenNotExists() {
        when(cdlRespository.findByEnvironmentCrn(any())).thenReturn(Optional.empty());
        StreamObserver<CdlCrudProto.DatalakeResponse> responseObserver = mock(StreamObserver.class);
        ArgumentCaptor<Throwable> errorCaptor = ArgumentCaptor.forClass(Throwable.class);
        doNothing().when(responseObserver).onError(errorCaptor.capture());

        underTest.findDatalake(CdlCrudProto.FindDatalakeRequest.newBuilder().setEnvironment(ENV_CRN).build(), responseObserver);

        verify(responseObserver).onError(any());
        assertEquals(errorCaptor.getValue().getClass(), StatusException.class);
        assertEquals(((StatusException) errorCaptor.getValue()).getStatus(), Status.NOT_FOUND);
    }

    @Test
    void findDatalakeWhenExists() {
        when(cdlRespository.findByEnvironmentCrn(any())).thenReturn(Optional.of(getCdl()));
        StreamObserver<CdlCrudProto.DatalakeResponse> responseObserver = mock(StreamObserver.class);
        doNothing().when(responseObserver).onCompleted();
        ArgumentCaptor<CdlCrudProto.DatalakeResponse> responseCaptor = ArgumentCaptor.forClass(CdlCrudProto.DatalakeResponse.class);
        doNothing().when(responseObserver).onNext(responseCaptor.capture());

        underTest.findDatalake(CdlCrudProto.FindDatalakeRequest.newBuilder().setEnvironment(ENV_CRN).build(), responseObserver);

        verify(responseObserver).onCompleted();
        assertEquals(responseCaptor.getValue().getCrn(), CDL_CRN);
    }

    @Test
    void describeDatalakeWhenNotExists() {
        when(cdlRespository.findByCrn(any())).thenReturn(Optional.empty());
        StreamObserver<CdlCrudProto.DescribeDatalakeResponse> responseObserver = mock(StreamObserver.class);
        ArgumentCaptor<Throwable> errorCaptor = ArgumentCaptor.forClass(Throwable.class);
        doNothing().when(responseObserver).onError(errorCaptor.capture());

        underTest.describeDatalake(CdlCrudProto.DescribeDatalakeRequest.newBuilder().setDatalake(CDL_CRN).build(), responseObserver);

        verify(responseObserver).onError(any());
        assertEquals(errorCaptor.getValue().getClass(), StatusException.class);
        assertEquals(((StatusException) errorCaptor.getValue()).getStatus(), Status.NOT_FOUND);
    }

    @Test
    void describeDatalakeWhenExists() {
        when(cdlRespository.findByCrn(any())).thenReturn(Optional.of(getCdl()));
        StreamObserver<CdlCrudProto.DescribeDatalakeResponse> responseObserver = mock(StreamObserver.class);
        doNothing().when(responseObserver).onCompleted();
        ArgumentCaptor<CdlCrudProto.DescribeDatalakeResponse> responseCaptor = ArgumentCaptor.forClass(CdlCrudProto.DescribeDatalakeResponse.class);
        doNothing().when(responseObserver).onNext(responseCaptor.capture());

        underTest.describeDatalake(CdlCrudProto.DescribeDatalakeRequest.newBuilder().setDatalake(CDL_CRN).build(), responseObserver);

        verify(responseObserver).onCompleted();
        assertEquals(responseCaptor.getValue().getCrn(), CDL_CRN);
    }

    @Test
    void deleteDatalakeWhenNotExists() {
        when(cdlRespository.findByCrn(any())).thenReturn(Optional.empty());
        StreamObserver<CdlCrudProto.DeleteDatalakeResponse> responseObserver = mock(StreamObserver.class);
        ArgumentCaptor<Throwable> errorCaptor = ArgumentCaptor.forClass(Throwable.class);
        doNothing().when(responseObserver).onError(errorCaptor.capture());

        underTest.deleteDatalake(CdlCrudProto.DeleteDatalakeRequest.newBuilder().setDatalake(CDL_CRN).build(), responseObserver);

        verify(responseObserver).onError(any());
        assertEquals(errorCaptor.getValue().getClass(), StatusException.class);
        assertEquals(((StatusException) errorCaptor.getValue()).getStatus(), Status.NOT_FOUND);
    }

    @Test
    void deleteDatalakeWhenExists() {
        when(cdlRespository.findByCrn(any())).thenReturn(Optional.of(getCdl()));
        StreamObserver<CdlCrudProto.DeleteDatalakeResponse> responseObserver = mock(StreamObserver.class);
        doNothing().when(responseObserver).onCompleted();
        ArgumentCaptor<CdlCrudProto.DeleteDatalakeResponse> responseCaptor = ArgumentCaptor.forClass(CdlCrudProto.DeleteDatalakeResponse.class);
        doNothing().when(responseObserver).onNext(responseCaptor.capture());

        underTest.deleteDatalake(CdlCrudProto.DeleteDatalakeRequest.newBuilder().setDatalake(CDL_CRN).build(), responseObserver);

        verify(responseObserver).onCompleted();
        assertEquals(responseCaptor.getValue().getCrn(), CDL_CRN);

        StreamObserver<CdlCrudProto.DatalakeResponse> findDlResponseObserver = mock(StreamObserver.class);
        ArgumentCaptor<Throwable> findDlErrorCaptor = ArgumentCaptor.forClass(Throwable.class);
        doNothing().when(findDlResponseObserver).onError(findDlErrorCaptor.capture());

        underTest.findDatalake(CdlCrudProto.FindDatalakeRequest.newBuilder().setEnvironment(ENV_CRN).build(), findDlResponseObserver);

        verify(findDlResponseObserver).onError(any());
        assertEquals(findDlErrorCaptor.getValue().getClass(), StatusException.class);
        assertEquals(((StatusException) findDlErrorCaptor.getValue()).getStatus(), Status.NOT_FOUND);
    }

    private static Cdl getCdl() {
        Cdl cdl = new Cdl();
        cdl.setCrn(CDL_CRN);
        cdl.setEnvironmentCrn(ENV_CRN);
        cdl.setDatabaseServerCrn("");
        return cdl;
    }

}
