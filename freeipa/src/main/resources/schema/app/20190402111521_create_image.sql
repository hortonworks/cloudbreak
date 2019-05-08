-- // Create Changelog

-- Default DDL for changelog table that will keep
-- a record of the migrations that have been run.

-- You can modify this to suit your database before
-- running your first migration.

-- Be sure that ID and DESCRIPTION fields exist in
-- BigInteger and String compatible fields respectively.

CREATE SEQUENCE image_id_seq START WITH 1
  INCREMENT BY 1
  NO MINVALUE
  NO MAXVALUE
  CACHE 1;

create table if not exists image
(
  id bigint default nextval('image_id_seq'::regclass) not null
    constraint image_pkey
      primary key,
  stack_id bigint not null
    constraint fk_instancegroup_stack_id
      references stack,
  imagename varchar(255),
  os varchar(255),
  ostype varchar(255),
  imagecatalogurl text,
  imageid varchar(255),
  imagecatalogname varchar(255),
  userdata text
);

create unique index if not exists image_id_idx
  on image (id);

create unique index if not exists image_stackid_idx
  on image (stack_id);

-- //@UNDO

DROP TABLE image;

DROP SEQUENCE image_id_seq;