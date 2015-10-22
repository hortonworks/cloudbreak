# Provision prerequisites

Cloudbreak works by connecting your AWS account through so called *Credentials*, and then uses these credentials to create resources on your behalf.
It is important that a Cloudbreak deployment uses two different AWS accounts for two different purposes:

- The account belonging to the Cloudbreak webapp itself that acts as a *third party* that creates resources on the account of the *end-user*. This account is configured at server-deployment time.
- The account belonging to the *end user* who uses the UI or the Shell to create clusters. This account is configured when setting up credentials.

These two accounts are usually *the same* when the end user is the same who deployed the Cloudbreak server, but it allows Cloudbreak to act as a SaaS project as well if needed.

Credentials use [IAM Roles](http://docs.aws.amazon.com/IAM/latest/UserGuide/id_roles.html) to give access to the third party to act on behalf of the end user without giving full access to your resources.
This IAM Role will be *assumed* later by the deployment account.
This section is about how to setup the IAM role used to create Cloudbreak credentials - to see the other part about how to setup the deployment account with cbd check out [this description](aws.md).

To connect your (end user) AWS account with a credential in Cloudbreak you'll have to create an IAM role on your AWS account that is configured to allow the third-party account to access and create resources on your behalf.
The easiest way to do this is with cbd commands but it can also be done manually from the AWS Console:

```
cbd aws generate-role  - Generates an AWS IAM role for Cloudbreak provisioning on AWS
cbd aws show-role      - Show assumers and policies for an AWS role
cbd aws delete-role    - Deletes an AWS IAM role, removes all inline policies
```

The `generate-role` command creates a role that is assumable by the Cloudbreak deployer's AWS account and has a broad policy setup.

You can check the generated role on your AWS console, under IAM roles:

![](https://raw.githubusercontent.com/sequenceiq/cloudbreak-deployer/docsupdate/docs/images/aws-iam-role.png)

After you're IAM role is configured you can move on to create clusters on the [UI](aws_cb_ui.md) or with the [Shell](aws_cb_shell.md).