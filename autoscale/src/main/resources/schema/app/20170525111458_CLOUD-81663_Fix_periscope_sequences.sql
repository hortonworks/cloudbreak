-- // CLOUD-81663 Fix periscope sequences
-- Migration SQL that makes the change goes here.

-- // regenerate sequence for table: ambari --------------

CREATE SEQUENCE ambari_id_seq START WITH 1
                                          INCREMENT BY 1
                                          NO MINVALUE
                                          NO MAXVALUE
                                          CACHE 1;

ALTER TABLE ambari
   ALTER COLUMN id SET DEFAULT nextval ('ambari_id_seq');

SELECT setval ('ambari_id_seq',
               (SELECT max (id) + 1 FROM (
                  SELECT id FROM ambari
                  UNION ALL
                  SELECT 0) as subQuery),
               FALSE);

-- // regenerate sequence for tables: timealert, prometheusalert, metricalert --------------

CREATE SEQUENCE alert_id_seq START WITH 1
                                          INCREMENT BY 1
                                          NO MINVALUE
                                          NO MAXVALUE
                                          CACHE 1;

ALTER TABLE timealert
   ALTER COLUMN id SET DEFAULT nextval ('alert_id_seq');
ALTER TABLE metricalert
   ALTER COLUMN id SET DEFAULT nextval ('alert_id_seq');
ALTER TABLE prometheusalert
   ALTER COLUMN id SET DEFAULT nextval ('alert_id_seq');

SELECT setval ('alert_id_seq',
               (SELECT MAX(Id)+1 FROM (
                    SELECT id FROM timealert
                    UNION ALL
                    SELECT id FROM prometheusalert
                    UNION ALL
                    SELECT id FROM metricalert
                    UNION ALL
                    SELECT 0) as subQuery),
               FALSE);

-- // regenerate sequence for table: cluster --------------

CREATE SEQUENCE cluster_id_seq START WITH 1
                                          INCREMENT BY 1
                                          NO MINVALUE
                                          NO MAXVALUE
                                          CACHE 1;

ALTER TABLE cluster
   ALTER COLUMN id SET DEFAULT nextval ('cluster_id_seq');

SELECT setval ('cluster_id_seq',
               (SELECT max (id) + 1 FROM (
                  SELECT id FROM cluster
                  UNION ALL
                  SELECT 0) as subQuery),
               FALSE);

-- // regenerate sequence for table: cluster --------------

CREATE SEQUENCE history_id_seq START WITH 1
                                          INCREMENT BY 1
                                          NO MINVALUE
                                          NO MAXVALUE
                                          CACHE 1;

ALTER TABLE history
   ALTER COLUMN id SET DEFAULT nextval ('history_id_seq');

SELECT setval ('history_id_seq',
               (SELECT max (id) + 1 FROM (
                  SELECT id FROM history
                  UNION ALL
                  SELECT 0) as subQuery),
               FALSE);

-- // regenerate sequence for table: notification --------------

CREATE SEQUENCE notification_id_seq START WITH 1
                                          INCREMENT BY 1
                                          NO MINVALUE
                                          NO MAXVALUE
                                          CACHE 1;

ALTER TABLE notification
   ALTER COLUMN id SET DEFAULT nextval ('notification_id_seq');

SELECT setval ('notification_id_seq',
               (SELECT max (id) + 1 FROM (
                  SELECT id FROM notification
                  UNION ALL
                  SELECT 0) as subQuery),
               FALSE);

-- // regenerate sequence for table: scalingpolicy --------------

CREATE SEQUENCE scalingpolicy_id_seq START WITH 1
                                          INCREMENT BY 1
                                          NO MINVALUE
                                          NO MAXVALUE
                                          CACHE 1;

ALTER TABLE scalingpolicy
   ALTER COLUMN id SET DEFAULT nextval ('scalingpolicy_id_seq');

SELECT setval ('scalingpolicy_id_seq',
               (SELECT max (id) + 1 FROM (
                  SELECT id FROM scalingpolicy
                  UNION ALL
                  SELECT 0) as subQuery),
               FALSE);

-- // regenerate sequence for table: securityconfig --------------

CREATE SEQUENCE securityconfig_id_seq START WITH 1
                                          INCREMENT BY 1
                                          NO MINVALUE
                                          NO MAXVALUE
                                          CACHE 1;

ALTER TABLE securityconfig
   ALTER COLUMN id SET DEFAULT nextval ('securityconfig_id_seq');

SELECT setval ('securityconfig_id_seq',
               (SELECT max (id) + 1 FROM (
                  SELECT id FROM securityconfig
                  UNION ALL
                  SELECT 0) as subQuery),
               FALSE);

DROP SEQUENCE IF EXISTS securityconfig_table;
DROP SEQUENCE IF EXISTS sequence_table;

-- //@UNDO
-- SQL to undo the change goes here.

-- // no way back the former version was totally wrong
