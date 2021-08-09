-- Migration SQL that makes the change goes here.

CREATE TABLE IF NOT EXISTS cdp_structured_event (
                                                    id                      bigserial NOT NULL,
                                                    eventtype               VARCHAR (255) NOT NULL,
                                                    resourcetype            VARCHAR (255) NOT NULL,
                                                    resourcecrn             VARCHAR (255) NOT NULL,
                                                    accountid               VARCHAR (255) NOT NULL,
                                                    timestamp               BIGINT NOT NULL DEFAULT (date_part('epoch'::text, now()) * 1000::double precision),
                                                    structuredeventjson     TEXT NOT NULL,

                                                    CONSTRAINT              pk_cdp_structured_event_id              PRIMARY KEY (id)
);

CREATE INDEX idx_cdp_structured_event_resourcecrn_eventtype ON cdp_structured_event (resourcecrn, eventtype);
CREATE INDEX idx_cdp_structured_event_resourcecrn_eventtype_timestamp ON cdp_structured_event (resourcecrn, eventtype, timestamp);


-- //@UNDO
-- SQL to undo the change goes here.

DROP INDEX IF EXISTS idx_cdp_structured_event_resourcecrn_eventtype;
DROP INDEX IF EXISTS idx_cdp_structured_event_resourcecrn_eventtype_timestamp;
DROP TABLE IF EXISTS cdp_structured_event;