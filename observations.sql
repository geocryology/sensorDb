-- CREATE DATABASE observations
-- 	ENCODING = 'UTF8'
-- 	TABLESPACE = pg_default
-- 	OWNER = postgres

 CREATE ROLE sensordb WITH 
 	INHERIT
 	LOGIN;

 CREATE ROLE observations_read WITH 
 	INHERIT;

CREATE ROLE observations_write WITH 
	INHERIT;

CREATE ROLE observations_admin WITH 
	INHERIT;

GRANT observations_write to sensordb;

CREATE EXTENSION postgis;
CREATE EXTENSION postgis_topology;
CREATE EXTENSION "uuid-ossp";

CREATE TABLE public.device_sensor_profiles(
	id uuid NOT NULL DEFAULT uuid_generate_v4(),
	device_type character varying NOT NULL,
    manufacturer character varying NOT NULL,
    manufacturer_device_name character varying NOT NULL,
    sensor_label character varying NOT NULL,
	sensor_type_of_measurement character varying NOT NULL,
	sensor_unit_of_measurement character varying,
	sensor_accuracy numeric,
	sensor_precision numeric,
	sensor_height_in_metres numeric,
	CONSTRAINT device_sensor_profiles_pk PRIMARY KEY (id)
);
ALTER TABLE public.device_sensor_profiles OWNER TO observations_admin;

CREATE TABLE public.devices(
	id uuid NOT NULL DEFAULT uuid_generate_v4(),
	serial_number character varying,
	access_code character varying,
	device_type character varying,
    manufacturer character varying,
    manufacturer_device_name character varying,
    acquired_on timestamp WITH TIME ZONE,
	notes text,
	CONSTRAINT devices_pk PRIMARY KEY (id),
	CONSTRAINT unique_serial_number UNIQUE (serial_number)

);
ALTER TABLE public.devices OWNER TO observations_admin;

CREATE TABLE public.sensors(
	id uuid NOT NULL DEFAULT uuid_generate_v4(),
	device_id uuid NOT NULL,
	label character varying NOT NULL,
	type_of_measurement character varying NOT NULL,
	unit_of_measurement character varying,
	accuracy numeric,
	precision numeric,
	height_in_metres numeric,
	serial_number character varying,
	CONSTRAINT sensors_pk PRIMARY KEY (id)

);
ALTER TABLE public.sensors OWNER TO observations_admin;

CREATE TABLE public.locations(
	id uuid NOT NULL DEFAULT uuid_generate_v4(),
	name character varying,
	coordinates geometry NOT NULL,
	elevation_in_metres numeric NOT NULL,
	comment text,
	record_observations boolean NOT NULL DEFAULT TRUE,
	accuracy_in_metres numeric,
	CONSTRAINT locations_pk PRIMARY KEY (id)

);
ALTER TABLE public.locations OWNER TO observations_admin;

CREATE INDEX locations_name ON locations USING btree (name);

CREATE INDEX locations_coordinates ON locations USING btree (coordinates);

CREATE TABLE public.devices_locations(
	id uuid NOT NULL DEFAULT uuid_generate_v4(),
	timestamp timestamp WITH TIME ZONE NOT NULL,
	device_id uuid NOT NULL,
	location_id uuid NOT NULL,
	notes text,
	CONSTRAINT device_location_pk PRIMARY KEY (id)

);
ALTER TABLE public.devices_locations OWNER TO observations_admin;

--- New imports table

CREATE TABLE public.imports (
    id uuid DEFAULT uuid_generate_v4() NOT NULL,
    import_time timestamp with time zone NOT NULL,
    filename varchar,
    import_parameters text
);

ALTER TABLE ONLY public.imports
    ADD CONSTRAINT imports_pk PRIMARY KEY (id);

ALTER TABLE public.imports OWNER TO observations_admin;

REVOKE ALL ON TABLE public.imports FROM PUBLIC;
REVOKE ALL ON TABLE public.imports FROM observations_admin;
GRANT ALL ON TABLE public.imports TO observations_admin;
GRANT SELECT ON TABLE public.imports TO observations_read;
GRANT SELECT ON TABLE public.imports TO observations_write;


--- Revised observations table

CREATE TABLE public.observations (
    id uuid DEFAULT uuid_generate_v4() NOT NULL,
    device_id uuid NOT NULL,
    sensor_id uuid NOT NULL,
    import_id uuid,
    import_key text,
    observation_type varchar,
    unit_of_measure varchar,
    accuracy numeric,
    "precision" numeric,
    numeric_value numeric,
    text_value text,
    logged_time timestamp with time zone,
    corrected_utc_time timestamp with time zone NOT NULL,
    location geometry(GEOMETRY,4326) NOT NULL,
	elevation_in_metres numeric NOT NULL,
    height_min_metres numeric,
    height_max_metres numeric
);

CREATE INDEX observations_import_key ON observations USING btree (import_key);

CREATE INDEX observations_corrected_utc_time ON observations USING btree (corrected_utc_time);

CREATE INDEX observations_unit_of_measure ON observations USING btree (unit_of_measure);

CREATE INDEX observations_location ON observations USING btree (location);

ALTER TABLE public.observations OWNER TO observations_admin;

ALTER TABLE ONLY public.observations
    ADD CONSTRAINT observations_pk PRIMARY KEY (id);

ALTER TABLE ONLY observations
    ADD CONSTRAINT observations_device_fk FOREIGN KEY (device_id) REFERENCES devices(id) MATCH FULL;

ALTER TABLE ONLY observations
    ADD CONSTRAINT observations_import_fk FOREIGN KEY (import_id) REFERENCES imports(id) MATCH FULL;

REVOKE ALL ON TABLE public.observations FROM PUBLIC;
REVOKE ALL ON TABLE public.observations FROM observations_admin;
GRANT ALL ON TABLE public.observations TO observations_admin;
GRANT SELECT ON TABLE public.observations TO observations_read;
GRANT SELECT,INSERT,DELETE,UPDATE ON TABLE public.observations TO observations_write;

--- end of observations table

CREATE TABLE public.dois(
	id uuid NOT NULL DEFAULT uuid_generate_v4(),
	doi character varying NOT NULL,
	notes text,
	CONSTRAINT dois_pk PRIMARY KEY (id)

);
ALTER TABLE public.dois OWNER TO observations_admin;

CREATE TABLE public.observations_dois(
	id uuid NOT NULL DEFAULT uuid_generate_v4(),
	observation_id uuid NOT NULL,
	doi_id uuid NOT NULL
);
ALTER TABLE public.observations_dois OWNER TO observations_admin;

--- end of dois tables

CREATE TABLE public.sets(
	id uuid NOT NULL DEFAULT uuid_generate_v4(),
	label character varying NOT NULL,
	doi character varying,
	notes text,
	CONSTRAINT sets_pk PRIMARY KEY (id)

);
ALTER TABLE public.sets OWNER TO observations_admin;

CREATE TABLE public.observations_sets(
	id uuid NOT NULL DEFAULT uuid_generate_v4(),
	observation_id uuid NOT NULL,
	set_id uuid NOT NULL
);
ALTER TABLE public.observations_sets OWNER TO observations_admin;

--- end of sets tables

CREATE TABLE public.logs(
	id uuid NOT NULL DEFAULT uuid_generate_v4(),
	timestamp timestamp WITH TIME ZONE NOT NULL,
	log text,
	CONSTRAINT logs_pk PRIMARY KEY (id)

);
ALTER TABLE public.logs OWNER TO observations_admin;


ALTER TABLE public.sensors ADD CONSTRAINT fk_device_id FOREIGN KEY (device_id)
REFERENCES public.devices (id) MATCH FULL
ON DELETE NO ACTION ON UPDATE NO ACTION;

ALTER TABLE public.devices_locations ADD CONSTRAINT device_location_fk_device FOREIGN KEY (device_id)
REFERENCES public.devices (id) MATCH FULL
ON DELETE NO ACTION ON UPDATE NO ACTION;

ALTER TABLE public.devices_locations ADD CONSTRAINT device_location_fk_location FOREIGN KEY (location_id)
REFERENCES public.locations (id) MATCH FULL
ON DELETE NO ACTION ON UPDATE NO ACTION;

ALTER TABLE public.observations ADD CONSTRAINT observations_sensor_fk FOREIGN KEY (sensor_id)
REFERENCES public.sensors (id) MATCH FULL
ON DELETE NO ACTION ON UPDATE NO ACTION;

ALTER TABLE public.observations_dois ADD CONSTRAINT observation_doi_observation_fk FOREIGN KEY (observation_id)
REFERENCES public.observations (id) MATCH FULL
ON DELETE NO ACTION ON UPDATE NO ACTION;

ALTER TABLE public.observations_dois ADD CONSTRAINT observation_doi_doi_fk FOREIGN KEY (doi_id)
REFERENCES public.dois (id) MATCH FULL
ON DELETE NO ACTION ON UPDATE NO ACTION;


GRANT SELECT on device_sensor_profiles to observations_read;
GRANT SELECT on devices to observations_read;
GRANT SELECT on devices_locations to observations_read;
GRANT SELECT on dois to observations_read;
GRANT SELECT on locations to observations_read;
GRANT SELECT on logs to observations_read;
GRANT SELECT on imports to observations_read;
GRANT SELECT on observations to observations_read;
GRANT SELECT on observations_dois to observations_read;
GRANT SELECT on sensors to observations_read;

GRANT SELECT, INSERT, UPDATE, DELETE on device_sensor_profiles to observations_write;
GRANT SELECT, INSERT, UPDATE, DELETE on devices to observations_write;
GRANT SELECT, INSERT, UPDATE, DELETE on devices_locations to observations_write;
GRANT SELECT, INSERT, UPDATE, DELETE on dois to observations_write;
GRANT SELECT, INSERT, UPDATE, DELETE on locations to observations_write;
GRANT SELECT, INSERT, UPDATE, DELETE on logs to observations_write;
GRANT SELECT, INSERT, UPDATE, DELETE on imports to observations_write;
GRANT SELECT, INSERT, UPDATE, DELETE on observations to observations_write;
GRANT SELECT, INSERT, UPDATE, DELETE on observations_dois to observations_write;
GRANT SELECT, INSERT, UPDATE, DELETE on sensors to observations_write;

