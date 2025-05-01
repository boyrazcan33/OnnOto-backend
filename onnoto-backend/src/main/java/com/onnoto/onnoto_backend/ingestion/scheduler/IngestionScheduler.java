package com.onnoto.onnoto_backend.ingestion.scheduler;

import com.onnoto.onnoto_backend.ingestion.service.DataIngestionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@EnableScheduling
@RequiredArgsConstructor
public class IngestionScheduler {

    private final DataIngestionService dataIngestionService;

    /**
     * Full station sync - run daily at 2 AM.
     */
    @Scheduled(cron = "0 0 2 * * ?")
    public void fullSync() {
        log.info("Starting scheduled full station sync");
        dataIngestionService.fetchAllStations();
    }

    /**
     * Status updates - run every 5 minutes.
     */
    @Scheduled(fixedRate = 300000)
    public void statusUpdates() {
        log.info("Starting scheduled status updates");
        dataIngestionService.fetchStatusUpdates();
    }

    /**
     * Initial data load on startup.
     */
    @Scheduled(initialDelay = 10000, fixedDelay = Long.MAX_VALUE)
    public void initialLoad() {
        log.info("Starting initial data load");
        dataIngestionService.fetchAllStations();
        dataIngestionService.fetchStatusUpdates();
    }
}