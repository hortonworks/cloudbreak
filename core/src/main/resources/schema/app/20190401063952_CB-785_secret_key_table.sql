-- // CB-785 secret key table
-- Migration SQL that makes the change goes here.

CREATE TABLE IF NOT EXISTS secretkey (
    id bigint NOT NULL,
    value varchar (255)
);

-- //@UNDO
-- SQL to undo the change goes here.


