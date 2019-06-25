-- // create usersyncsummary table

-- This change is to create table usersyncsummary.


CREATE SEQUENCE usersyncsummary_id_seq START WITH 1
  INCREMENT BY 1
  NO MINVALUE
  NO MAXVALUE
  CACHE 1;



CREATE TABLE IF NOT EXISTS usersyncsummary
(
    id BIGINT DEFAULT nextval('usersyncsummary_id_seq'::regclass) NOT NULL
        CONSTRAINT usersyncsummary_pkey
            PRIMARY KEY,
    actorcrn VARCHAR(255),
    requestid VARCHAR(255),
    starttime int8 NOT NULL DEFAULT '-1'::integer,
    endtime int8 NOT NULL DEFAULT '-1'::integer,
    status VARCHAR(255),
    accountid VARCHAR(255),
    errorcause VARCHAR(255)
);

-- //@UNDO

DROP TABLE IF EXISTS usersyncsummary;

DROP SEQUENCE IF EXISTS usersyncsummary_id_seq;
--------------------------------


CREATE INDEX uss_id_accountid_idx
    ON usersyncsummary (id, accountid);

DROP INDEX uss_id_accountid_idx;

