-- // CB-13156 Add flow tracking table for update_load_balancers API
-- Migration SQL that makes the change goes here.

CREATE TABLE IF NOT EXISTS lbupdate_flowlog (
    id                  bigserial NOT NULL,
    environmentcrn      character varying(255),
    parentflowid        character varying(255),
    childflowid         character varying(255),
    childresourcename   character varying(255),
    childresourcecrn    character varying(255),
    PRIMARY KEY (id)
);

-- //@UNDO
-- SQL to undo the change goes here.

DROP TABLE IF EXISTS lbupdate_flowlog;
