-- // CLOUD-872 db migration for security group and rule resources
-- Migration SQL that makes the change goes here.

DROP TABLE IF EXISTS subnet;
DROP SEQUENCE  IF EXISTS  subnet_seq;

CREATE SEQUENCE security_group_seq
START WITH 1
INCREMENT BY 1
NO MINVALUE
NO MAXVALUE
CACHE 1;

CREATE TABLE securitygroup
(
   id                  bigint NOT NULL DEFAULT nextval('security_group_seq'),
   name                CHARACTER VARYING (255) NOT NULL,
   description         text,
   account             CHARACTER VARYING (255),
   owner               CHARACTER VARYING (255),
   publicinaccount     boolean NOT NULL,
   status              CHARACTER VARYING (255)
);

ALTER TABLE ONLY securitygroup
ADD CONSTRAINT securitygroup_pkey PRIMARY KEY (id);

ALTER TABLE ONLY securitygroup
ADD CONSTRAINT uk_securitygroupnameinaccount UNIQUE (account, name);

ALTER TABLE stack ADD COLUMN securitygroup_id bigint;

ALTER TABLE ONLY stack
ADD CONSTRAINT fk_securitygroupidinstack FOREIGN KEY (securitygroup_id) REFERENCES securitygroup(id);

CREATE SEQUENCE security_rule_seq
START WITH 1
INCREMENT BY 1
NO MINVALUE
NO MAXVALUE
CACHE 1;

CREATE TABLE securityrule
(
   id                  bigint NOT NULL DEFAULT nextval('security_rule_seq'),
   securitygroup_id    bigint NOT NULL,
   cidr                CHARACTER VARYING (255),
   ports               text,
   protocol            CHARACTER VARYING (255),
   modifiable          boolean NOT NULL
);

ALTER TABLE ONLY securityrule
ADD CONSTRAINT securityrule_pkey PRIMARY KEY (id);

ALTER TABLE ONLY securityrule
ADD CONSTRAINT fk_securitygroupidinsecurityrule FOREIGN KEY (securitygroup_id) REFERENCES securitygroup(id);

-- //@UNDO
-- SQL to undo the change goes here.
CREATE SEQUENCE subnet_seq
START WITH 1
INCREMENT BY 1
NO MINVALUE
NO MAXVALUE
CACHE 1;

CREATE TABLE subnet
(
    id bigint NOT NULL DEFAULT nextval('subnet_seq'),
    stack_id bigint,
    cidr text,
    modifiable boolean default true,
    PRIMARY KEY (id),
    FOREIGN KEY (stack_id) REFERENCES Stack(id)
);

INSERT INTO subnet (stack_id, cidr, modifiable)
select s.id as stack_id, sr.cidr as cidr, sr.modifiable as modifiable from stack s, securitygroup sg, securityrule sr where s.securitygroup_id = sg.id and sr.securitygroup_id = sg.id;

ALTER TABLE ONLY stack
DROP CONSTRAINT IF EXISTS fk_securitygroupidinstack;

ALTER TABLE stack DROP COLUMN IF EXISTS securitygroup_id;

ALTER TABLE ONLY securityrule
DROP CONSTRAINT IF EXISTS fk_securitygroupidinsecurityrule;

DROP TABLE IF EXISTS securityrule;

DROP SEQUENCE  IF EXISTS  security_rule_seq;

DROP TABLE IF EXISTS securitygroup;

DROP SEQUENCE  IF EXISTS  security_group_seq;
