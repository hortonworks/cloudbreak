package com.sequenceiq.datalake.service;

import static java.util.stream.Collectors.toList;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import com.fasterxml.jackson.core.JsonGenerator;
import com.sequenceiq.cloudbreak.api.endpoint.v4.events.EventV4Endpoint;
import com.sequenceiq.cloudbreak.api.endpoint.v4.events.responses.CloudbreakEventV4Response;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.common.json.JsonUtil;
import com.sequenceiq.cloudbreak.structuredevent.event.StructuredEventType;
import com.sequenceiq.cloudbreak.structuredevent.event.cdp.CDPStructuredEvent;
import com.sequenceiq.cloudbreak.structuredevent.service.db.CDPStructuredEventDBService;
import com.sequenceiq.datalake.entity.SdxCluster;

/**
 * Streams datalake and cloudbreak audit events into a zip archive.
 *
 * Events are written directly to the output stream (no full in-memory collection)
 * to keep memory usage bounded regardless of event count.
 */
@Service
public class SdxEventsZipService {

    private static final Logger LOGGER = LoggerFactory.getLogger(SdxEventsZipService.class);

    private static final int FLUSH_INTERVAL = 500;

    private static final int CB_PAGE_SIZE = 200;

    @Value("${cdp.structuredevent.maxsize:20000}")
    private int maxSize;

    @Inject
    private CDPStructuredEventDBService cdpStructuredEventDBService;

    @Inject
    private EventV4Endpoint eventV4Endpoint;

    @Inject
    private SdxEventsHelper sdxEventsHelper;

    @Inject
    private PlatformTransactionManager transactionManager;

    /**
     * Writes all audit events for the given environment into a zip stream containing a single
     * JSON array file ({@code struct-events.json}). Datalake events are written first,
     * then cloudbreak (core) events, up to {@link #maxSize} total.
     */
    public void streamDatalakeAuditEventsAsZip(String environmentCrn, OutputStream outputStream) throws IOException {
        LOGGER.info("Streaming datalake audit events as zip for environment [{}]", environmentCrn);
        sdxEventsHelper.ensureNonDeletedNonDetachedDatalakeExists(environmentCrn);

        List<SdxCluster> datalakes = sdxEventsHelper.getAvailableAndDetachedDatalakes(environmentCrn);
        List<String> datalakeCrns = datalakes.stream().map(SdxCluster::getCrn).collect(toList());

        try (ZipOutputStream zipOut = new ZipOutputStream(outputStream)) {
            zipOut.putNextEntry(new ZipEntry("struct-events.json"));
            writeEventsAsJsonArray(datalakeCrns, datalakes, zipOut, environmentCrn);
            zipOut.closeEntry();
        }
    }

    private void writeEventsAsJsonArray(List<String> datalakeCrns, List<SdxCluster> datalakes,
            ZipOutputStream zipOut, String environmentCrn) throws IOException {
        JsonGenerator generator = JsonUtil.getFactory().createGenerator(zipOut);
        generator.configure(JsonGenerator.Feature.AUTO_CLOSE_TARGET, false);
        try {
            generator.writeStartArray();
            int remaining = maxSize;
            int datalakeEventsWritten = 0;
            try {
                remaining = writeDatalakeEvents(datalakeCrns, generator);
                datalakeEventsWritten = maxSize - remaining;
            } catch (Exception e) {
                LOGGER.error("Failed to stream datalake events, continuing with cloudbreak events only", e);
            }
            int cloudbreakEventsWritten = writeCloudbreakEvents(datalakes, generator, remaining);
            generator.writeEndArray();
            generator.flush();
            LOGGER.info("Finished streaming events for environment [{}]: {} datalake events, {} cloudbreak events",
                    environmentCrn, datalakeEventsWritten, cloudbreakEventsWritten);
        } finally {
            generator.close();
        }
    }

    /**
     * Streams datalake events from the local database using a JDBC cursor (no full result set in memory).
     * Requires a read-only transaction to keep the cursor alive during iteration.
     *
     * @return number of remaining event slots (maxSize - written)
     */
    private int writeDatalakeEvents(List<String> datalakeCrns, JsonGenerator generator) throws IOException {
        TransactionTemplate txTemplate = new TransactionTemplate(transactionManager);
        txTemplate.setReadOnly(true);

        AtomicInteger remaining = new AtomicInteger(maxSize);
        AtomicInteger written = new AtomicInteger(0);
        txTemplate.executeWithoutResult(status -> {
            try (Stream<CDPStructuredEvent> events = cdpStructuredEventDBService
                    .streamEventsOfResources(List.of(StructuredEventType.NOTIFICATION), datalakeCrns)) {
                Iterator<CDPStructuredEvent> it = events.iterator();
                while (it.hasNext() && remaining.get() > 0) {
                    generator.writeObject(it.next());
                    remaining.decrementAndGet();
                    flushIfNeeded(generator, written.incrementAndGet());
                }
                flushRemainder(generator, written.get());
            } catch (IOException e) {
                throw new RuntimeException("Failed to stream datalake events to zip", e);
            }
        });
        return remaining.get();
    }

    /**
     * Fetches cloudbreak (core service) events via the paged REST API.
     * Each datalake's corresponding cloudbreak stack is queried page by page.
     *
     * @return number of cloudbreak events written
     */
    private int writeCloudbreakEvents(List<SdxCluster> datalakes, JsonGenerator generator, int remaining) throws IOException {
        int written = 0;
        for (SdxCluster datalake : datalakes) {
            if (remaining <= 0) {
                break;
            }
            if (datalake.getDeleted() != null) {
                continue;
            }
            try {
                int eventsWrittenForCluster = writeEventsForSingleCluster(datalake, generator, remaining);
                remaining -= eventsWrittenForCluster;
                written += eventsWrittenForCluster;
            } catch (Exception e) {
                LOGGER.error("Failed to stream cloudbreak events for cluster [{}], skipping", datalake.getCrn(), e);
            }
        }
        return written;
    }

    private int writeEventsForSingleCluster(SdxCluster datalake, JsonGenerator generator, int remaining) throws IOException {
        String cloudbreakCrn = sdxEventsHelper.getCloudbreakCrn(datalake);
        int written = 0;
        int page = 0;
        boolean hasMore = true;

        while (remaining > 0 && hasMore) {
            List<CloudbreakEventV4Response> events = fetchCloudbreakEventsPage(cloudbreakCrn, page, CB_PAGE_SIZE);

            for (CloudbreakEventV4Response event : events) {
                if (remaining <= 0) {
                    break;
                }
                generator.writeObject(sdxEventsHelper.convert(event, datalake.getCrn()));
                remaining--;
                written++;
            }
            generator.flush();

            hasMore = events.size() == CB_PAGE_SIZE;
            page++;
        }
        return written;
    }

    private List<CloudbreakEventV4Response> fetchCloudbreakEventsPage(String cloudbreakCrn, int page, int pageSize) {
        return ThreadBasedUserCrnProvider.doAsInternalActor(
                () -> eventV4Endpoint.getPagedCloudbreakEventListByCrn(cloudbreakCrn, page, pageSize, false));
    }

    private void flushIfNeeded(JsonGenerator generator, int written) throws IOException {
        if (written % FLUSH_INTERVAL == 0) {
            generator.flush();
        }
    }

    private void flushRemainder(JsonGenerator generator, int written) throws IOException {
        if (written % FLUSH_INTERVAL != 0) {
            generator.flush();
        }
    }
}
