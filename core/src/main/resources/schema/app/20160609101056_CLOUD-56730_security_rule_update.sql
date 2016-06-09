-- // CLOUD-56730 security rule update
-- Migration SQL that makes the change goes here.

UPDATE securitygroup SET description = REPLACE(description, '2022 (SSH) ', '') WHERE name='all-services-port' AND status='DEFAULT';

UPDATE securityrule SET ports = REPLACE(ports, ',2022', '')
	WHERE securitygroup_id IN
		(SELECT id FROM securitygroup WHERE name='all-services-port' AND status='DEFAULT');

UPDATE securitygroup SET description = REPLACE(description, '2022 (SSH) ', '443 (HTTPS) ') WHERE name='only-ssh-and-ssl' AND status='DEFAULT';

UPDATE securityrule SET ports = REPLACE(ports, ',2022', ',443')
	WHERE securitygroup_id IN
		(SELECT id FROM securitygroup WHERE name='only-ssh-and-ssl' AND status='DEFAULT');

-- //@UNDO
-- SQL to undo the change goes here.

UPDATE securitygroup SET description = REPLACE(description, '22 (SSH) 443 (HTTPS)', '22 (SSH) 2022 (SSH) 443 (HTTPS)')
	WHERE name='all-services-port' AND status='DEFAULT';

UPDATE securityrule SET ports = REPLACE(ports, '22,443', '22,2022,443')
	WHERE securitygroup_id IN
		(SELECT id FROM securitygroup WHERE name='all-services-port' AND status='DEFAULT');

UPDATE securitygroup SET description = REPLACE(description, '443 (HTTPS) ', '2022 (SSH) ') WHERE name='only-ssh-and-ssl' AND status='DEFAULT';

UPDATE securityrule SET ports = REPLACE(ports, ',443', ',2022')
	WHERE securitygroup_id IN
		(SELECT id FROM securitygroup WHERE name='only-ssh-and-ssl' AND status='DEFAULT');