# Provision prerequisites

## IAM role setup

Cloudbreak works by connecting your AWS account through so called *Credentials*, and then uses these credentials to create resources on your behalf.

**Important!** Cloudbreak deployment uses two different AWS accounts for two different purposes:

- The account belonging to the *Cloudbreak webapp* itself that acts as a *third party* that creates resources on the account of the *end-user*. This account is configured at server-deployment time.
- The account belonging to the *end user* who uses the UI or the Shell to create clusters. This account is configured when setting up credentials.

These two accounts are usually *the same* when the end user is the same who deployed the Cloudbreak server, but it allows Cloudbreak to act as a SaaS project as well if needed.

Credentials use [IAM Roles](http://docs.aws.amazon.com/IAM/latest/UserGuide/id_roles.html) to give access to the third party to act on behalf of the end user without giving full access to your resources.
This IAM Role will be *assumed* later by the deployment account.
This section is about how to setup the IAM role, to see how to setup the *deployment* account check out [this description](aws.md).

To connect your (*end user*) AWS account with a credential in Cloudbreak you'll have to create an IAM role on your AWS account that is configured to allow the third-party account to access and create resources on your behalf.
The easiest way to do this is with cbd commands (but it can also be done manually from the [AWS Console](https://console.aws.amazon.com)):

```
cbd aws generate-role  - Generates an AWS IAM role for Cloudbreak provisioning on AWS
cbd aws show-role      - Show assumers and policies for an AWS role
cbd aws delete-role    - Deletes an AWS IAM role, removes all inline policies
```

The `generate-role` command creates a role that is assumable by the Cloudbreak Deployer AWS account and has a broad policy setup.
By default the `generate-role` command creates a role with the name `cbreak-deployer`.
If you'd like to create the role with a different name or if you'd like to create multiple roles then the role's name can be changed by adding this line to your `Profile`:

```
export AWS_ROLE_NAME=my-cloudbreak-role
```

You can check the generated role on your AWS console, under IAM roles:

![](https://raw.githubusercontent.com/sequenceiq/cloudbreak-deployer/1.1.0/docs/images/aws-iam-role.png)

## Generate a new SSH key

All the instances created by Cloudbreak are configured to allow key-based SSH,
so you'll need to provide an SSH public key that can be used later to SSH onto the instances in the clusters you'll create with Cloudbreak.
You can use one of your existing keys or you can generate a new one.

To generate a new SSH keypair:

```
ssh-keygen -t rsa -b 4096 -C "your_email@example.com"
# Creates a new ssh key, using the provided email as a label
# Generating public/private rsa key pair.
```

```
# Enter file in which to save the key (/Users/you/.ssh/id_rsa): [Press enter]
You'll be asked to enter a passphrase, but you can leave it empty.

# Enter passphrase (empty for no passphrase): [Type a passphrase]
# Enter same passphrase again: [Type passphrase again]
```

After you enter a passphrase the keypair is generated. The output should look something like below.
```
# Your identification has been saved in /Users/you/.ssh/id_rsa.
# Your public key has been saved in /Users/you/.ssh/id_rsa.pub.
# The key fingerprint is:
# 01:0f:f4:3b:ca:85:sd:17:sd:7d:sd:68:9d:sd:a2:sd your_email@example.com
```

Later you'll need to pass the `.pub` file's contents to Cloudbreak and use the private part to SSH to the instances.

## Next steps

After your IAM role is configured and you have an SSH key you can move on to create clusters on the [UI](aws_cb_ui.md) or with the [Shell](aws_cb_shell.md).