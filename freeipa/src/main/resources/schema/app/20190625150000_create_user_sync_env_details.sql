-- // create usersyncenvdetails table

-- This change is to create table usersyncenvdetails.


CREATE SEQUENCE usersyncenvdetails_id_seq START WITH 1
  INCREMENT BY 1
  NO MINVALUE
  NO MAXVALUE
  CACHE 1;



CREATE TABLE IF NOT EXISTS usersyncenvdetails
(
    id BIGINT DEFAULT nextval('usersyncenvdetails_id_seq'::regclass) NOT NULL
        CONSTRAINT CONSTRAINT usersyncenvdetails_pkey
            PRIMARY KEY,
    usersyncsummaryid  bigint not null
      constraint fk_usersyncsummary_id
        references usersyncsummary,
    envcrn VARCHAR(255),
    umspulltime int8 NOT NULL DEFAULT '-1'::integer,
    endtime int8 NOT NULL DEFAULT '-1'::integer,
    status VARCHAR(255),
    detail_status_obj  VARCHAR(255)
);

-- //@UNDO

DROP TABLE IF EXISTS usersyncenvdetails;

DROP SEQUENCE IF EXISTS usersyncenvdetails_id_seq;
--------------------------------


CREATE INDEX usersed_id_usersyncsummaryid_idx
    ON usersyncenvdetails (id, usersyncsummaryid);

DROP INDEX usersed_id_usersyncsummaryid_idx;

