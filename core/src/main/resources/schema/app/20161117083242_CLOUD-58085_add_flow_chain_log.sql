-- // CLOUD-58085_add_flow_chain_log
-- Migration SQL that makes the change goes here.

CREATE SEQUENCE flowchainlog_id_seq START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

CREATE TABLE flowchainlog
(
   id BIGINT NOT NULL,
   created BIGINT NOT NULL DEFAULT (date_part('epoch'::text, now()) * (1000)::double precision),
   flowchainid VARCHAR (255) NOT NULL,
   parentflowchainid VARCHAR (255),
   chain TEXT NOT NULL,
   PRIMARY KEY (id)
);

ALTER TABLE flowchainlog ALTER COLUMN id SET DEFAULT nextval ('flowchainlog_id_seq');

-- //@UNDO
-- SQL to undo the change goes here.

DROP TABLE flowchainlog;

DROP SEQUENCE flowchainlog_id_seq;