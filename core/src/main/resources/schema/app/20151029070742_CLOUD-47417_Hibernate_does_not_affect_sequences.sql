-- // CLOUD-47417 Hibernate does not affect sequences
-- Migration SQL that makes the change goes here.


-- // regenerate sequence for table: recipe --------------

CREATE SEQUENCE recipe_id_seq START WITH 1
                                          INCREMENT BY 1
                                          NO MINVALUE
                                          NO MAXVALUE
                                          CACHE 1;

ALTER TABLE recipe
   ALTER COLUMN id SET DEFAULT nextval ('recipe_id_seq');


SELECT setval ('recipe_id_seq',
               (SELECT max (id) + 1
                  FROM recipe),
               FALSE);


SELECT last_value FROM recipe_id_seq;


-- // regenerate sequence for table: securitygroup --------------

CREATE SEQUENCE securitygroup_id_seq START WITH 1
                                          INCREMENT BY 1
                                          NO MINVALUE
                                          NO MAXVALUE
                                          CACHE 1;

ALTER TABLE securitygroup
   ALTER COLUMN id SET DEFAULT nextval ('securitygroup_id_seq');

DROP SEQUENCE IF EXISTS security_group_seq;

SELECT setval ('securitygroup_id_seq',
               (SELECT max (id) + 1
                  FROM securitygroup),
               FALSE);


SELECT last_value FROM securitygroup_id_seq;


-- // regenerate sequence for table: cluster --------------

CREATE SEQUENCE cluster_id_seq START WITH 1
                                          INCREMENT BY 1
                                          NO MINVALUE
                                          NO MAXVALUE
                                          CACHE 1;

ALTER TABLE cluster
   ALTER COLUMN id SET DEFAULT nextval ('cluster_id_seq');

DROP SEQUENCE IF EXISTS cluster_seq;

SELECT setval ('cluster_id_seq',
               (SELECT max (id) + 1
                  FROM cluster),
               FALSE);


SELECT last_value FROM cluster_id_seq;


-- // regenerate sequence for table: securityrule --------------

CREATE SEQUENCE securityrule_id_seq START WITH 1
                                          INCREMENT BY 1
                                          NO MINVALUE
                                          NO MAXVALUE
                                          CACHE 1;

ALTER TABLE securityrule
   ALTER COLUMN id SET DEFAULT nextval ('securityrule_id_seq');

DROP SEQUENCE IF EXISTS security_rule_seq;

SELECT setval ('securityrule_id_seq',
               (SELECT max (id) + 1
                  FROM securityrule),
               FALSE);


SELECT last_value FROM securityrule_id_seq;


-- // regenerate sequence for table: securityconfig --------------

CREATE SEQUENCE securityconfig_id_seq START WITH 1
                                          INCREMENT BY 1
                                          NO MINVALUE
                                          NO MAXVALUE
                                          CACHE 1;

ALTER TABLE securityconfig
   ALTER COLUMN id SET DEFAULT nextval ('securityconfig_id_seq');

DROP SEQUENCE IF EXISTS securityconfig_table;

SELECT setval ('securityconfig_id_seq',
               (SELECT max (id) + 1
                  FROM securityconfig),
               FALSE);


SELECT last_value FROM securityconfig_id_seq;


-- // regenerate sequence for table: instancemetadata --------------

CREATE SEQUENCE instancemetadata_id_seq START WITH 1
                                          INCREMENT BY 1
                                          NO MINVALUE
                                          NO MAXVALUE
                                          CACHE 1;

ALTER TABLE instancemetadata
   ALTER COLUMN id SET DEFAULT nextval ('instancemetadata_id_seq');


SELECT setval ('instancemetadata_id_seq',
               (SELECT max (id) + 1
                  FROM instancemetadata),
               FALSE);


SELECT last_value FROM instancemetadata_id_seq;


-- // regenerate sequence for table: network --------------

CREATE SEQUENCE network_id_seq START WITH 1
                                          INCREMENT BY 1
                                          NO MINVALUE
                                          NO MAXVALUE
                                          CACHE 1;

ALTER TABLE network
   ALTER COLUMN id SET DEFAULT nextval ('network_id_seq');

DROP SEQUENCE IF EXISTS network_table;

SELECT setval ('network_id_seq',
               (SELECT max (id) + 1
                  FROM network),
               FALSE);


SELECT last_value FROM network_id_seq;


-- // regenerate sequence for table: ambaristackdetails --------------

CREATE SEQUENCE ambaristackdetails_id_seq START WITH 1
                                          INCREMENT BY 1
                                          NO MINVALUE
                                          NO MAXVALUE
                                          CACHE 1;

ALTER TABLE ambaristackdetails
   ALTER COLUMN id SET DEFAULT nextval ('ambaristackdetails_id_seq');

DROP SEQUENCE IF EXISTS amb_stack_table;

SELECT setval ('ambaristackdetails_id_seq',
               (SELECT max (id) + 1
                  FROM ambaristackdetails),
               FALSE);


SELECT last_value FROM ambaristackdetails_id_seq;


-- // regenerate sequence for table: instancegroup --------------

CREATE SEQUENCE instancegroup_id_seq START WITH 1
                                          INCREMENT BY 1
                                          NO MINVALUE
                                          NO MAXVALUE
                                          CACHE 1;

ALTER TABLE instancegroup
   ALTER COLUMN id SET DEFAULT nextval ('instancegroup_id_seq');


SELECT setval ('instancegroup_id_seq',
               (SELECT max (id) + 1
                  FROM instancegroup),
               FALSE);


SELECT last_value FROM instancegroup_id_seq;


-- // regenerate sequence for table: failurepolicy --------------

CREATE SEQUENCE failurepolicy_id_seq START WITH 1
                                          INCREMENT BY 1
                                          NO MINVALUE
                                          NO MAXVALUE
                                          CACHE 1;

ALTER TABLE failurepolicy
   ALTER COLUMN id SET DEFAULT nextval ('failurepolicy_id_seq');

DROP SEQUENCE IF EXISTS sequence_table;

SELECT setval ('failurepolicy_id_seq',
               (SELECT max (id) + 1
                  FROM failurepolicy),
               FALSE);


SELECT last_value FROM failurepolicy_id_seq;


-- // regenerate sequence for table: template --------------

CREATE SEQUENCE template_id_seq START WITH 1
                                          INCREMENT BY 1
                                          NO MINVALUE
                                          NO MAXVALUE
                                          CACHE 1;

ALTER TABLE template
   ALTER COLUMN id SET DEFAULT nextval ('template_id_seq');


SELECT setval ('template_id_seq',
               (SELECT max (id) + 1
                  FROM template),
               FALSE);


SELECT last_value FROM template_id_seq;


-- // regenerate sequence for table: cloudbreakevent --------------

CREATE SEQUENCE cloudbreakevent_id_seq START WITH 1
                                          INCREMENT BY 1
                                          NO MINVALUE
                                          NO MAXVALUE
                                          CACHE 1;

ALTER TABLE cloudbreakevent
   ALTER COLUMN id SET DEFAULT nextval ('cloudbreakevent_id_seq');

DROP SEQUENCE IF EXISTS cloudbreakevent_seq;

SELECT setval ('cloudbreakevent_id_seq',
               (SELECT max (id) + 1
                  FROM cloudbreakevent),
               FALSE);


SELECT last_value FROM cloudbreakevent_id_seq;


-- // regenerate sequence for table: blueprint --------------

CREATE SEQUENCE blueprint_id_seq START WITH 1
                                          INCREMENT BY 1
                                          NO MINVALUE
                                          NO MAXVALUE
                                          CACHE 1;

ALTER TABLE blueprint
   ALTER COLUMN id SET DEFAULT nextval ('blueprint_id_seq');

DROP SEQUENCE IF EXISTS blueprint_table;

SELECT setval ('blueprint_id_seq',
               (SELECT max (id) + 1
                  FROM blueprint),
               FALSE);


SELECT last_value FROM blueprint_id_seq;


-- // regenerate sequence for table: credential --------------

CREATE SEQUENCE credential_id_seq START WITH 1
                                          INCREMENT BY 1
                                          NO MINVALUE
                                          NO MAXVALUE
                                          CACHE 1;

ALTER TABLE credential
   ALTER COLUMN id SET DEFAULT nextval ('credential_id_seq');

DROP SEQUENCE IF EXISTS credential_table;

SELECT setval ('credential_id_seq',
               (SELECT max (id) + 1
                  FROM credential),
               FALSE);


SELECT last_value FROM credential_id_seq;


-- // regenerate sequence for table: component --------------

CREATE SEQUENCE component_id_seq START WITH 1
                                          INCREMENT BY 1
                                          NO MINVALUE
                                          NO MAXVALUE
                                          CACHE 1;

ALTER TABLE component
   ALTER COLUMN id SET DEFAULT nextval ('component_id_seq');

DROP SEQUENCE IF EXISTS component_table;

SELECT setval ('component_id_seq',
               (SELECT max (id) + 1
                  FROM component),
               FALSE);


SELECT last_value FROM component_id_seq;


-- // regenerate sequence for table: hostmetadata --------------

CREATE SEQUENCE hostmetadata_id_seq START WITH 1
                                          INCREMENT BY 1
                                          NO MINVALUE
                                          NO MAXVALUE
                                          CACHE 1;

ALTER TABLE hostmetadata
   ALTER COLUMN id SET DEFAULT nextval ('hostmetadata_id_seq');


SELECT setval ('hostmetadata_id_seq',
               (SELECT max (id) + 1
                  FROM hostmetadata),
               FALSE);


SELECT last_value FROM hostmetadata_id_seq;


-- // regenerate sequence for table: stack --------------

CREATE SEQUENCE stack_id_seq START WITH 1
                                          INCREMENT BY 1
                                          NO MINVALUE
                                          NO MAXVALUE
                                          CACHE 1;

ALTER TABLE stack
   ALTER COLUMN id SET DEFAULT nextval ('stack_id_seq');

DROP SEQUENCE IF EXISTS stack_table;

SELECT setval ('stack_id_seq',
               (SELECT max (id) + 1
                  FROM stack),
               FALSE);


SELECT last_value FROM stack_id_seq;


-- // regenerate sequence for table: subscription --------------

CREATE SEQUENCE subscription_id_seq START WITH 1
                                          INCREMENT BY 1
                                          NO MINVALUE
                                          NO MAXVALUE
                                          CACHE 1;

ALTER TABLE subscription
   ALTER COLUMN id SET DEFAULT nextval ('subscription_id_seq');


SELECT setval ('subscription_id_seq',
               (SELECT max (id) + 1
                  FROM subscription),
               FALSE);


SELECT last_value FROM subscription_id_seq;


-- // regenerate sequence for table: resource --------------

CREATE SEQUENCE resource_id_seq START WITH 1
                                          INCREMENT BY 1
                                          NO MINVALUE
                                          NO MAXVALUE
                                          CACHE 1;

ALTER TABLE resource
   ALTER COLUMN id SET DEFAULT nextval ('resource_id_seq');


SELECT setval ('resource_id_seq',
               (SELECT max (id) + 1
                  FROM resource),
               FALSE);


SELECT last_value FROM resource_id_seq;


-- // regenerate sequence for table: cloudbreakusage --------------

CREATE SEQUENCE cloudbreakusage_id_seq START WITH 1
                                          INCREMENT BY 1
                                          NO MINVALUE
                                          NO MAXVALUE
                                          CACHE 1;

ALTER TABLE cloudbreakusage
   ALTER COLUMN id SET DEFAULT nextval ('cloudbreakusage_id_seq');

DROP SEQUENCE IF EXISTS cloudbreakusage_seq;

SELECT setval ('cloudbreakusage_id_seq',
               (SELECT max (id) + 1
                  FROM cloudbreakusage),
               FALSE);


SELECT last_value FROM cloudbreakusage_id_seq;


-- // regenerate sequence for table: hostgroup --------------

CREATE SEQUENCE hostgroup_id_seq START WITH 1
                                          INCREMENT BY 1
                                          NO MINVALUE
                                          NO MAXVALUE
                                          CACHE 1;

ALTER TABLE hostgroup
   ALTER COLUMN id SET DEFAULT nextval ('hostgroup_id_seq');


SELECT setval ('hostgroup_id_seq',
               (SELECT max (id) + 1
                  FROM hostgroup),
               FALSE);


SELECT last_value FROM hostgroup_id_seq;


-- // regenerate sequence for table: filesystem --------------

CREATE SEQUENCE filesystem_id_seq START WITH 1
                                          INCREMENT BY 1
                                          NO MINVALUE
                                          NO MAXVALUE
                                          CACHE 1;

ALTER TABLE filesystem
   ALTER COLUMN id SET DEFAULT nextval ('filesystem_id_seq');

DROP SEQUENCE IF EXISTS filesystem_table;

SELECT setval ('filesystem_id_seq',
               (SELECT max (id) + 1
                  FROM filesystem),
               FALSE);


SELECT last_value FROM filesystem_id_seq;




DROP SEQUENCE IF EXISTS hibernate_sequence;




-- //@UNDO
-- SQL to undo the change goes here.


-- // no way back the former version was totally wrong