-- // CLOUD-713 create periscope baseline schema
-- Migration SQL that makes the change goes here.

--
-- PostgreSQL database dump
--

SET statement_timeout = 0;
SET lock_timeout = 0;
SET client_encoding = 'UTF8';
SET standard_conforming_strings = on;
SET check_function_bodies = false;
SET client_min_messages = warning;

--
-- Name: postgres; Type: COMMENT; Schema: -; Owner: postgres
--

COMMENT ON DATABASE postgres IS 'default administrative connection database';


--
-- Name: plpgsql; Type: EXTENSION; Schema: -; Owner:
--

CREATE EXTENSION IF NOT EXISTS plpgsql WITH SCHEMA pg_catalog;


--
-- Name: EXTENSION plpgsql; Type: COMMENT; Schema: -; Owner:
--

COMMENT ON EXTENSION plpgsql IS 'PL/pgSQL procedural language';


SET search_path = public, pg_catalog;

SET default_tablespace = '';

SET default_with_oids = false;

--
-- Name: ambari; Type: TABLE; Schema: public; Owner: postgres; Tablespace:
--

CREATE TABLE ambari (
    id bigint NOT NULL,
    ambari_host character varying(255),
    ambari_pass character varying(255),
    ambari_port character varying(255),
    ambari_user character varying(255)
);


ALTER TABLE ambari OWNER TO postgres;

--
-- Name: cluster; Type: TABLE; Schema: public; Owner: postgres; Tablespace:
--

CREATE TABLE cluster (
    id bigint NOT NULL,
    cooldown integer,
    last_scaling_activity bigint,
    max_size integer,
    min_size integer,
    cb_stack_id bigint,
    state character varying(255),
    ambari_id bigint,
    user_id character varying(255)
);


ALTER TABLE cluster OWNER TO postgres;

--
-- Name: history; Type: TABLE; Schema: public; Owner: postgres; Tablespace:
--

CREATE TABLE history (
    id bigint NOT NULL,
    adjustment integer NOT NULL,
    adjustment_type character varying(255),
    alert_type character varying(255),
    cb_stack_id bigint,
    cluster_id bigint,
    host_group character varying(255),
    original_node_count integer,
    status character varying(255),
    status_reason character varying(255),
    "timestamp" bigint NOT NULL,
    user_id character varying(255)
);


ALTER TABLE history OWNER TO postgres;

--
-- Name: history_properties; Type: TABLE; Schema: public; Owner: postgres; Tablespace:
--

CREATE TABLE history_properties (
    history_id bigint NOT NULL,
    value text,
    key character varying(255) NOT NULL
);


ALTER TABLE history_properties OWNER TO postgres;

--
-- Name: metricalert; Type: TABLE; Schema: public; Owner: postgres; Tablespace:
--

CREATE TABLE metricalert (
    id bigint NOT NULL,
    description character varying(255),
    name character varying(255),
    scalingpolicy_id bigint,
    alert_state character varying(255),
    definition_name character varying(255),
    period integer NOT NULL,
    cluster_id bigint
);


ALTER TABLE metricalert OWNER TO postgres;

--
-- Name: notification; Type: TABLE; Schema: public; Owner: postgres; Tablespace:
--

CREATE TABLE notification (
    id bigint NOT NULL,
    target bytea,
    type character varying(255)
);


ALTER TABLE notification OWNER TO postgres;

--
-- Name: periscope_user; Type: TABLE; Schema: public; Owner: postgres; Tablespace:
--

CREATE TABLE periscope_user (
    id character varying(255) NOT NULL,
    account character varying(255),
    email character varying(255)
);


ALTER TABLE periscope_user OWNER TO postgres;

--
-- Name: scalingpolicy; Type: TABLE; Schema: public; Owner: postgres; Tablespace:
--

CREATE TABLE scalingpolicy (
    id bigint NOT NULL,
    adjustment_type character varying(255),
    host_group character varying(255),
    name character varying(255),
    scaling_adjustment integer
);


ALTER TABLE scalingpolicy OWNER TO postgres;

--
-- Name: sequence_table; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE sequence_table
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE sequence_table OWNER TO postgres;

--
-- Name: timealert; Type: TABLE; Schema: public; Owner: postgres; Tablespace:
--

CREATE TABLE timealert (
    id bigint NOT NULL,
    description character varying(255),
    name character varying(255),
    scalingpolicy_id bigint,
    cron character varying(255),
    time_zone character varying(255),
    cluster_id bigint
);


ALTER TABLE timealert OWNER TO postgres;

--
-- Name: ambari_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres; Tablespace:
--

ALTER TABLE ONLY ambari
    ADD CONSTRAINT ambari_pkey PRIMARY KEY (id);


--
-- Name: cluster_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres; Tablespace:
--

ALTER TABLE ONLY cluster
    ADD CONSTRAINT cluster_pkey PRIMARY KEY (id);


--
-- Name: history_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres; Tablespace:
--

ALTER TABLE ONLY history
    ADD CONSTRAINT history_pkey PRIMARY KEY (id);


--
-- Name: history_properties_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres; Tablespace:
--

ALTER TABLE ONLY history_properties
    ADD CONSTRAINT history_properties_pkey PRIMARY KEY (history_id, key);


--
-- Name: metricalert_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres; Tablespace:
--

ALTER TABLE ONLY metricalert
    ADD CONSTRAINT metricalert_pkey PRIMARY KEY (id);


--
-- Name: notification_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres; Tablespace:
--

ALTER TABLE ONLY notification
    ADD CONSTRAINT notification_pkey PRIMARY KEY (id);


--
-- Name: periscope_user_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres; Tablespace:
--

ALTER TABLE ONLY periscope_user
    ADD CONSTRAINT periscope_user_pkey PRIMARY KEY (id);


--
-- Name: scalingpolicy_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres; Tablespace:
--

ALTER TABLE ONLY scalingpolicy
    ADD CONSTRAINT scalingpolicy_pkey PRIMARY KEY (id);


--
-- Name: timealert_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres; Tablespace:
--

ALTER TABLE ONLY timealert
    ADD CONSTRAINT timealert_pkey PRIMARY KEY (id);


--
-- Name: fk_3v03tedcm5hbjtcxo1ri6ecbr; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY timealert
    ADD CONSTRAINT fk_3v03tedcm5hbjtcxo1ri6ecbr FOREIGN KEY (cluster_id) REFERENCES cluster(id);


--
-- Name: fk_c22ixbmoqm2xeoqisctiwtdhb; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY timealert
    ADD CONSTRAINT fk_c22ixbmoqm2xeoqisctiwtdhb FOREIGN KEY (scalingpolicy_id) REFERENCES scalingpolicy(id);


--
-- Name: fk_fbrkapmhprkr0lcscre4ym2ap; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY metricalert
    ADD CONSTRAINT fk_fbrkapmhprkr0lcscre4ym2ap FOREIGN KEY (scalingpolicy_id) REFERENCES scalingpolicy(id);


--
-- Name: fk_ng61bjx78yaahvwkhdco7yxgi; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY cluster
    ADD CONSTRAINT fk_ng61bjx78yaahvwkhdco7yxgi FOREIGN KEY (user_id) REFERENCES periscope_user(id);


--
-- Name: fk_sbni0w74jud722u3m5p3djspl; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY history_properties
    ADD CONSTRAINT fk_sbni0w74jud722u3m5p3djspl FOREIGN KEY (history_id) REFERENCES history(id);


--
-- Name: fk_t36ceslxpwdtcag3y1lq7wf6b; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY metricalert
    ADD CONSTRAINT fk_t36ceslxpwdtcag3y1lq7wf6b FOREIGN KEY (cluster_id) REFERENCES cluster(id);


--
-- Name: fk_tq6aoyelc0pf02o5vy6d5qi5p; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY cluster
    ADD CONSTRAINT fk_tq6aoyelc0pf02o5vy6d5qi5p FOREIGN KEY (ambari_id) REFERENCES ambari(id);


--
-- Name: public; Type: ACL; Schema: -; Owner: postgres
--

REVOKE ALL ON SCHEMA public FROM PUBLIC;
REVOKE ALL ON SCHEMA public FROM postgres;
GRANT ALL ON SCHEMA public TO postgres;
GRANT ALL ON SCHEMA public TO PUBLIC;


--
-- PostgreSQL database dump complete
--

-- //@UNDO
-- SQL to undo the change goes here.


