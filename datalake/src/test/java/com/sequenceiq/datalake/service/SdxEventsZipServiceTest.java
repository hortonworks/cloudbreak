package com.sequenceiq.datalake.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.PlatformTransactionManager;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sequenceiq.cloudbreak.api.endpoint.v4.events.EventV4Endpoint;
import com.sequenceiq.cloudbreak.api.endpoint.v4.events.responses.CloudbreakEventV4Response;
import com.sequenceiq.cloudbreak.structuredevent.event.StructuredEventType;
import com.sequenceiq.cloudbreak.structuredevent.event.cdp.CDPOperationDetails;
import com.sequenceiq.cloudbreak.structuredevent.event.cdp.CDPStructuredEvent;
import com.sequenceiq.cloudbreak.structuredevent.event.cdp.CDPStructuredNotificationEvent;
import com.sequenceiq.cloudbreak.structuredevent.service.db.CDPStructuredEventDBService;
import com.sequenceiq.datalake.entity.SdxCluster;

@ExtendWith(MockitoExtension.class)
class SdxEventsZipServiceTest {

    private static final String ENVIRONMENT_CRN = "crn:cdp:environments:us-west-1:460c0d8f-ae8e-4dce-9cd7-2351762eb9ac:environment:" +
            "6b2b1600-8ac6-4c26-aa34-dab36f4bd243";

    private static final String DATALAKE_CRN = "crn:cdp:datalake:us-west-1:460c0d8f-ae8e-4dce-9cd7-2351762eb9ac:datalake:" +
            "6b2b1600-8ac6-4c26-aa34-dab36f4bd243";

    private static final int DEFAULT_MAX_SIZE = 20000;

    @Mock
    private CDPStructuredEventDBService cdpStructuredEventDBService;

    @Mock
    private EventV4Endpoint eventV4Endpoint;

    @Mock
    private SdxEventsHelper sdxEventsHelper;

    @Mock
    private PlatformTransactionManager transactionManager;

    @InjectMocks
    private SdxEventsZipService underTest;

    private SdxCluster datalake;

    @BeforeEach
    void setUp() {
        datalake = createSdxCluster(DATALAKE_CRN, DATALAKE_CRN, ENVIRONMENT_CRN);
        when(sdxEventsHelper.getAvailableAndDetachedDatalakes(any())).thenReturn(List.of(datalake));
        when(transactionManager.getTransaction(any())).thenReturn(mock(org.springframework.transaction.TransactionStatus.class));
    }

    @Test
    void streamsAllDatalakeEventsWhenUnderMaxSize() throws IOException {
        setMaxSize(DEFAULT_MAX_SIZE);
        stubDatalakeEvents(1200);
        stubEmptyCloudbreakEvents();

        List<CDPStructuredNotificationEvent> result = executeAndReadEvents();

        assertEquals(1200, result.size());
    }

    @Test
    void stopsAtMaxSizeAndSkipsCloudbreakEvents() throws IOException {
        setMaxSize(750);
        stubDatalakeEvents(1000);

        List<CDPStructuredNotificationEvent> result = executeAndReadEvents();

        assertEquals(750, result.size());
        verify(eventV4Endpoint, never()).getPagedCloudbreakEventListByCrn(anyString(), anyInt(), anyInt(), anyBoolean());
    }

    @Test
    void includesCloudbreakEventsAfterDatalakeEvents() throws IOException {
        setMaxSize(DEFAULT_MAX_SIZE);
        stubDatalakeEvents(3);
        stubCloudbreakEventsWithOneEvent();

        List<CDPStructuredNotificationEvent> result = executeAndReadEvents();

        assertEquals(4, result.size());
    }

    @Test
    void producesValidZipWithSingleJsonEntry() throws IOException {
        setMaxSize(DEFAULT_MAX_SIZE);
        stubEmptyDatalakeEvents();
        stubEmptyCloudbreakEvents();

        ByteArrayOutputStream outputStream = executeZip();

        try (ZipInputStream zipIn = new ZipInputStream(new ByteArrayInputStream(outputStream.toByteArray()))) {
            ZipEntry entry = zipIn.getNextEntry();
            assertNotNull(entry);
            assertEquals("struct-events.json", entry.getName());
        }
    }

    @Test
    void cloudbreakApiFailureProducesValidZipWithDatalakeEventsOnly() throws IOException {
        setMaxSize(DEFAULT_MAX_SIZE);
        stubDatalakeEvents(5);
        when(sdxEventsHelper.getCloudbreakCrn(any())).thenReturn(DATALAKE_CRN);
        when(eventV4Endpoint.getPagedCloudbreakEventListByCrn(anyString(), anyInt(), anyInt(), anyBoolean()))
                .thenThrow(new RuntimeException("Connection refused"));

        List<CDPStructuredNotificationEvent> result = executeAndReadEvents();

        assertEquals(5, result.size());
    }

    @Test
    void cloudbreakEventsNoDuplicatesWhenBudgetNotMultipleOfPageSize() throws IOException {
        // maxSize=250 with 0 datalake events → remaining=250 for cloudbreak.
        // Cluster has 300 events. Without the fix, page 2 would use size=50 (remaining)
        // giving offset page*50=100 instead of page*200=200 → duplicates.
        setMaxSize(250);
        stubEmptyDatalakeEvents();
        when(sdxEventsHelper.getCloudbreakCrn(any())).thenReturn(DATALAKE_CRN);

        // Page 0 (size=200): returns 200 events with timestamps 0..199
        // Page 1 (size=200): returns 100 events with timestamps 200..299
        when(eventV4Endpoint.getPagedCloudbreakEventListByCrn(anyString(), eq(0), eq(200), anyBoolean()))
                .thenReturn(createCloudbreakEvents(200, 0));
        when(eventV4Endpoint.getPagedCloudbreakEventListByCrn(anyString(), eq(1), eq(200), anyBoolean()))
                .thenReturn(createCloudbreakEvents(100, 200));
        when(sdxEventsHelper.convert(any(CloudbreakEventV4Response.class), anyString()))
                .thenAnswer(invocation -> {
                    CloudbreakEventV4Response resp = invocation.getArgument(0);
                    return createNotificationEvent(resp.getEventTimestamp());
                });

        List<CDPStructuredNotificationEvent> result = executeAndReadEvents();

        // Should get exactly 250 events (budget), not 200+50-duplicates
        assertEquals(250, result.size());
        // Verify page 1 was requested with full page size (200), not the shrunk remaining (50)
        verify(eventV4Endpoint).getPagedCloudbreakEventListByCrn(anyString(), eq(1), eq(200), anyBoolean());
    }

    @Test
    void skipsDeletedDatalakesForCloudbreakEvents() throws IOException {
        setMaxSize(DEFAULT_MAX_SIZE);
        datalake.setDeleted(1L);
        stubEmptyDatalakeEvents();

        List<CDPStructuredNotificationEvent> result = executeAndReadEvents();

        assertEquals(0, result.size());
        verify(eventV4Endpoint, never()).getPagedCloudbreakEventListByCrn(anyString(), anyInt(), anyInt(), anyBoolean());
    }

    // --- Setup helpers ---

    private void setMaxSize(int maxSize) {
        ReflectionTestUtils.setField(underTest, "maxSize", maxSize);
    }

    private void stubDatalakeEvents(int count) {
        List<CDPStructuredEvent> events = createEventList(count);
        when(cdpStructuredEventDBService.streamEventsOfResources(any(), any()))
                .thenReturn(events.stream());
    }

    private void stubEmptyDatalakeEvents() {
        when(cdpStructuredEventDBService.streamEventsOfResources(any(), any()))
                .thenReturn(Stream.empty());
    }

    private void stubEmptyCloudbreakEvents() {
        when(sdxEventsHelper.getCloudbreakCrn(any())).thenReturn(DATALAKE_CRN);
        when(eventV4Endpoint.getPagedCloudbreakEventListByCrn(anyString(), anyInt(), anyInt(), anyBoolean()))
                .thenReturn(List.of());
    }

    private void stubCloudbreakEventsWithOneEvent() {
        when(sdxEventsHelper.getCloudbreakCrn(any())).thenReturn(DATALAKE_CRN);
        CloudbreakEventV4Response cbEvent = new CloudbreakEventV4Response();
        cbEvent.setEventTimestamp(5000L);
        cbEvent.setClusterName("test-datalake");
        cbEvent.setClusterId(1L);
        cbEvent.setEventType("NOTIFICATION");
        cbEvent.setEventMessage("test notification");
        when(eventV4Endpoint.getPagedCloudbreakEventListByCrn(anyString(), anyInt(), anyInt(), anyBoolean()))
                .thenReturn(List.of(cbEvent))
                .thenReturn(List.of());
        when(sdxEventsHelper.convert(any(CloudbreakEventV4Response.class), anyString()))
                .thenReturn(createNotificationEvent(5000L));
    }

    // --- Execution helpers ---

    private ByteArrayOutputStream executeZip() throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        underTest.streamDatalakeAuditEventsAsZip(ENVIRONMENT_CRN, outputStream);
        return outputStream;
    }

    private List<CDPStructuredNotificationEvent> executeAndReadEvents() throws IOException {
        ByteArrayOutputStream outputStream = executeZip();
        return readEventsFromZip(outputStream);
    }

    private List<CDPStructuredNotificationEvent> readEventsFromZip(ByteArrayOutputStream outputStream) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        try (ZipInputStream zipIn = new ZipInputStream(new ByteArrayInputStream(outputStream.toByteArray()))) {
            zipIn.getNextEntry();
            return mapper.readValue(zipIn, new TypeReference<>() { });
        }
    }

    // --- Factory helpers ---

    private List<CDPStructuredEvent> createEventList(int count) {
        List<CDPStructuredEvent> events = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            events.add(createNotificationEvent(1000L + i));
        }
        return events;
    }

    private CDPStructuredEvent createNotificationEvent(long timestamp) {
        CDPOperationDetails operationDetails = new CDPOperationDetails();
        operationDetails.setResourceCrn(DATALAKE_CRN);
        operationDetails.setResourceType("datalake");
        operationDetails.setTimestamp(timestamp);
        operationDetails.setEventType(StructuredEventType.NOTIFICATION);

        CDPStructuredNotificationEvent event = new CDPStructuredNotificationEvent() {
            @Override
            public String getStatus() {
                return SENT;
            }

            @Override
            public Long getDuration() {
                return 1L;
            }
        };
        event.setOperation(operationDetails);
        return event;
    }

    private List<CloudbreakEventV4Response> createCloudbreakEvents(int count, long startTimestamp) {
        List<CloudbreakEventV4Response> events = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            CloudbreakEventV4Response event = new CloudbreakEventV4Response();
            event.setEventTimestamp(startTimestamp + i);
            event.setClusterName("test-datalake");
            event.setClusterId(1L);
            event.setEventType("NOTIFICATION");
            event.setEventMessage("event-" + (startTimestamp + i));
            events.add(event);
        }
        return events;
    }

    private SdxCluster createSdxCluster(String crn, String stackCrn, String envCrn) {
        SdxCluster cluster = new SdxCluster();
        cluster.setCrn(crn);
        cluster.setStackCrn(stackCrn);
        cluster.setEnvCrn(envCrn);
        return cluster;
    }
}
