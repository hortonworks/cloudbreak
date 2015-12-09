-- // CLOUD-48917 generic credentials
-- Migration SQL that makes the change goes here.

ALTER TABLE credential ADD COLUMN attributes TEXT;
ALTER TABLE credential ADD COLUMN cloudplatform VARCHAR(255);

UPDATE credential SET attributes = json_build_object('selector', 'cb-keystone-v2', 'userName', username, 'password', password, 'tenantName', tenantname, 'endpoint', endpoint, 'userDomain', userdomain, 'keystoneVersion', keystoneversion, 'keystoneAuthScope', keystoneauthscope, 'projectName', projectname, 'projectDomainName', projectdomainname, 'domainName', domainname) WHERE dtype = 'OpenStackCredential';
UPDATE credential SET attributes = json_build_object('serviceAccountId', serviceaccountid, 'serviceAccountPrivateKey', serviceaccountprivatekey, 'projectId', projectid) WHERE dtype = 'GcpCredential';
UPDATE credential SET attributes = json_build_object('subscriptionId', subscriptionid, 'tenantId', tenantid, 'accessKey', acceskey, 'secretKey', secretkey) WHERE dtype = 'AzureRmCredential';
UPDATE credential SET attributes = json_build_object('roleArn', rolearn, 'keyPairName', keypairname) WHERE dtype = 'AwsCredential';

UPDATE credential SET cloudplatform = 'AWS' WHERE dtype = 'AwsCredential';
UPDATE credential SET cloudplatform = 'AZURE_RM' WHERE dtype = 'AzureRmCredential';
UPDATE credential SET cloudplatform = 'GCP' WHERE dtype = 'GcpCredential';
UPDATE credential SET cloudplatform = 'OPENSTACK' WHERE dtype = 'OpenStackCredential';

ALTER TABLE credential ALTER COLUMN cloudplatform SET NOT NULL;

ALTER TABLE credential DROP COLUMN dtype;
ALTER TABLE credential DROP COLUMN userName;
ALTER TABLE credential DROP COLUMN password;
ALTER TABLE credential DROP COLUMN tenantName;
ALTER TABLE credential DROP COLUMN endpoint;
ALTER TABLE credential DROP COLUMN userdomain;
ALTER TABLE credential DROP COLUMN keystoneversion;
ALTER TABLE credential DROP COLUMN keystoneauthscope;
ALTER TABLE credential DROP COLUMN projectname;
ALTER TABLE credential DROP COLUMN projectdomainname;
ALTER TABLE credential DROP COLUMN domainname;
ALTER TABLE credential DROP COLUMN subscriptionid;
ALTER TABLE credential DROP COLUMN cerfile;
ALTER TABLE credential DROP COLUMN jksfile;
ALTER TABLE credential DROP COLUMN sshcerfile;
ALTER TABLE credential DROP COLUMN jks;
ALTER TABLE credential DROP COLUMN postfix;
ALTER TABLE credential DROP COLUMN serviceaccountid;
ALTER TABLE credential DROP COLUMN serviceaccountprivatekey;
ALTER TABLE credential DROP COLUMN projectid;
ALTER TABLE credential DROP COLUMN tenantid;
ALTER TABLE credential DROP COLUMN acceskey;
ALTER TABLE credential DROP COLUMN secretkey;
ALTER TABLE credential DROP COLUMN rolearn;
ALTER TABLE credential DROP COLUMN keypairname;

-- //@UNDO
-- SQL to undo the change goes here.

ALTER TABLE credential ADD COLUMN dtype VARCHAR(50);
ALTER TABLE credential ADD COLUMN userName VARCHAR(255);
ALTER TABLE credential ADD COLUMN password VARCHAR(255);
ALTER TABLE credential ADD COLUMN tenantName VARCHAR(255);
ALTER TABLE credential ADD COLUMN endpoint VARCHAR(255);
ALTER TABLE credential ADD COLUMN userdomain VARCHAR(255);
ALTER TABLE credential ADD COLUMN keystoneversion VARCHAR(255);
ALTER TABLE credential ADD COLUMN keystoneauthscope VARCHAR(255);
ALTER TABLE credential ADD COLUMN projectname VARCHAR(255);
ALTER TABLE credential ADD COLUMN projectdomainname VARCHAR(255);
ALTER TABLE credential ADD COLUMN domainname VARCHAR(255);
ALTER TABLE credential ADD COLUMN subscriptionid VARCHAR(255);
ALTER TABLE credential ADD COLUMN cerfile TEXT;
ALTER TABLE credential ADD COLUMN jksfile TEXT;
ALTER TABLE credential ADD COLUMN sshcerfile TEXT;
ALTER TABLE credential ADD COLUMN jks VARCHAR(255);
ALTER TABLE credential ADD COLUMN postfix VARCHAR(255);
ALTER TABLE credential ADD COLUMN serviceaccountid VARCHAR(255);
ALTER TABLE credential ADD COLUMN serviceaccountprivatekey TEXT;
ALTER TABLE credential ADD COLUMN projectid VARCHAR(255);
ALTER TABLE credential ADD COLUMN tenantid VARCHAR(255);
ALTER TABLE credential ADD COLUMN acceskey VARCHAR(255);
ALTER TABLE credential ADD COLUMN secretkey VARCHAR(255);
ALTER TABLE credential ADD COLUMN rolearn VARCHAR(255);
ALTER TABLE credential ADD COLUMN keypairname VARCHAR(255);

UPDATE credential SET dtype = 'AwsCredential' WHERE cloudplatform = 'AWS';
UPDATE credential SET dtype = 'AzureRmCredential' WHERE cloudplatform = 'AZURE_RM';
UPDATE credential SET dtype = 'GcpCredential' WHERE cloudplatform = 'GCP';
UPDATE credential SET dtype = 'OpenStackCredential' WHERE cloudplatform = 'OPENSTACK';

ALTER TABLE credential ALTER COLUMN attributes SET DATA TYPE jsonb USING attributes::jsonb;

UPDATE credential SET username = attributes->> 'userName', password = attributes->> 'password', tenantname = attributes->> 'tenantName', endpoint = attributes->> 'endpoint', userdomain = attributes->> 'userDomain', keystoneversion = attributes->> 'keystoneVersion', keystoneauthscope = attributes->> 'keystoneAuthScope', projectname = attributes->> 'projectName', projectdomainname = attributes->> 'projectDomainName', domainname = attributes->> 'domainName' WHERE cloudplatform = 'OPENSTACK';
UPDATE credential SET serviceaccountid = attributes->> 'serviceAccountId', serviceaccountprivatekey = attributes->> 'serviceAccountPrivateKey', projectid = attributes->> 'projectId' WHERE cloudplatform = 'GCP';
UPDATE credential SET subscriptionid = attributes->> 'subscriptionId', tenantid = attributes->> 'tenantId', acceskey = attributes->> 'accessKey', secretkey = attributes->> 'secretKey' WHERE cloudplatform = 'AZURE_RM';
UPDATE credential SET rolearn = attributes->> 'roleArn', keypairname = attributes->> 'keyPairName' WHERE cloudplatform = 'AWS';

ALTER TABLE credential DROP COLUMN attributes;
ALTER TABLE credential DROP COLUMN cloudplatform;
