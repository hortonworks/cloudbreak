## Account management

Cloudbreak defines three distinct roles:

1. DEPLOYER
2. ACCOUNT_ADMIN
3. ACCOUNT_USER

###Cloudbreak deployer
This is the `master` role - the user which is created during the deployment process will have this role.

###Account admin
We have introduced the notion of accounts - and with that comes an administrator role. Upon registration a user will become an account administrator.

The extra rights associated with the account admin role are:

* Invite users to join the account
* Share account wide resources (credential, blueprints, templates)
* See resources created by account users
* Monitor clusters started by account users
* Management and reporting tool available

###Account user
An account user is a user who has been invited to join Cloudbreak by an account administrator. Account users activity will show up in the management and reporting tool for account wide statistics - accessible by the account administrator. Apart from common account wide resources, the account users can manage their own private resources.
