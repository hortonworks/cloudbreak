-- // CLOUD-61497 Duplicated HDP repo impementation
-- Migration SQL that makes the change goes here.
ALTER TABLE cluster DROP COLUMN ambaristackdetails_id;
DROP SEQUENCE IF EXISTS amb_stack_table;
DROP TABLE ambaristackdetails;

-- //@UNDO
-- SQL to undo the change goes here.
CREATE TABLE ambaristackdetails
(
   id             BIGINT NOT NULL,
   os             VARCHAR (255),
   stack          VARCHAR (255),
   stackbaseurl   VARCHAR (255),
   stackrepoid    VARCHAR (255),
   utilsbaseurl   VARCHAR (255),
   utilsrepoid    VARCHAR (255),
   verify         BOOLEAN DEFAULT TRUE NOT NULL,
   version        VARCHAR (255),
   PRIMARY KEY (id)
);

ALTER TABLE cluster ADD COLUMN ambaristackdetails_id BIGINT REFERENCES ambaristackdetails(id);
