-- // CB-1247 Add redbeams status columns
-- Migration SQL that makes the change goes here.

CREATE TABLE dbstackstatus
(
    id BIGINT NOT NULL,
    status VARCHAR(255),
    statusReason TEXT,
    detaileddbstackstatus VARCHAR(255),
    created BIGINT,
    PRIMARY KEY (id)
);

-- The id column of dbstackstatus maps 1:1 to the id column of the corresponding dbstack
ALTER TABLE dbstackstatus ADD CONSTRAINT fk_dbstackstatus_id FOREIGN KEY (id) REFERENCES dbstack(id);

-- //@UNDO
-- SQL to undo the change goes here.

ALTER TABLE dbstackstatus DROP CONSTRAINT fk_dbstackstatus_id;

DROP TABLE dbstackstatus;
