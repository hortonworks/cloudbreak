-- // create flowlog
-- Migration SQL that makes the change goes here.

CREATE SEQUENCE flowlog_id_seq START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;
    
CREATE TABLE flowlog (
    id BIGINT NOT NULL,
    created BIGINT NOT NULL DEFAULT (date_part('epoch'::text, now()) * (1000)::double precision),
    flowid CHARACTER VARYING(255) NOT NULL,
    nextevent CHARACTER VARYING(255),
    payloadtype CHARACTER VARYING(255),
    flowtype CHARACTER VARYING(255),
    currentstate CHARACTER VARYING(255) NOT NULL,
    payload TEXT
);

ALTER TABLE flowlog
    ADD CONSTRAINT PK_flowlog PRIMARY KEY (id),
    ALTER COLUMN id SET DEFAULT nextval ('flowlog_id_seq');

-- //@UNDO
-- SQL to undo the change goes here.

DROP TABLE flowlog;

DROP SEQUENCE flowlog_id_seq;