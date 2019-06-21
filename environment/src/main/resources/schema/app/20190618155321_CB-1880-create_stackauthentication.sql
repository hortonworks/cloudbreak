-- // CB-1880 adding SSH key data linked to database

CREATE SEQUENCE environment_authentication_id_seq START WITH 1
  INCREMENT BY 1
  NO MINVALUE
  NO MAXVALUE
  CACHE 1;

CREATE TABLE environment_authentication
(
  id bigint NOT NULL
    CONSTRAINT environment_authentication_pkey
      PRIMARY KEY,
  publickey TEXT,
  publickeyid VARCHAR(255),
  loginusername VARCHAR(255) NOT NULL
);

ALTER TABLE environment ADD COLUMN environment_authentication_id BIGINT;

DO $$
DECLARE
  envcursor CURSOR FOR SELECT id from environment;
  auth_id BIGINT;
BEGIN
  FOR row IN envcursor LOOP
    INSERT INTO environment_authentication
    (
      id,
      publickey,
      publickeyid,
      loginusername
    )
    VALUES
    (
      nextval('environment_authentication_id_seq'),
      'ssh-rsa AAAAB3NzaC1yc2EAAAADAQABAAABAQC0Rfl2G2vDs6yc19RxCqReunFgpYj+ucyLobpTCBtfDwzIbJot2Fmife6M42mBtiTmAK6x8kcUEeab6CB4MUzsqF7vGTFUjwWirG/XU5pYXFUBhi8xzey+KS9KVrQ+UuKJh/AN9iSQeMV+rgT1yF5+etVH+bK1/37QCKp3+mCqjFzPyQOrvkGZv4sYyRwX7BKBLleQmIVWpofpjT7BfcCxH877RzC5YMIi65aBc82Dl6tH6OEiP7mzByU52yvH6JFuwZ/9fWj1vXCWJzxx2w0F1OU8Zwg8gNNzL+SVb9+xfBE7xBHMpYFg72hBWPh862Ce36F4NZd3MpWMSjMmpDPh centos',
      NULL,
      'cloudbreak'
    )
    RETURNING id INTO auth_id;

    UPDATE environment SET environment_authentication_id = auth_id WHERE CURRENT OF envcursor;
  END LOOP;
END;
$$ LANGUAGE 'plpgsql';

ALTER TABLE ONLY environment ADD CONSTRAINT fk_environment_environment_authentication_id
  FOREIGN KEY (environment_authentication_id) REFERENCES environment_authentication(id);


-- //@UNDO

ALTER TABLE environment DROP CONSTRAINT IF EXISTS fk_environment_environment_authentication_id;

ALTER TABLE environment DROP COLUMN IF EXISTS environment_authentication_id;

DROP TABLE environment_authentication;

DROP SEQUENCE environment_authentication_id_seq;
