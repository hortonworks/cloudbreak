#Provisioning prerequisites

## Generate a new SSH key
If you don't have a SSH key you have to generate one. With Terminal still open, copy and paste the text below.
```
ssh-keygen -t rsa -b 4096 -C "your_email@example.com"
# Creates a new ssh key, using the provided email as a label
# Generating public/private rsa key pair.
```

```
# Enter file in which to save the key (/Users/you/.ssh/id_rsa): [Press enter]
You'll be asked to enter a passphrase.

# Enter passphrase (empty for no passphrase): [Type a passphrase]
# Enter same passphrase again: [Type passphrase again]
```

After you enter a passphrase, you'll be given the fingerprint, or id, of your SSH key. It will look something like this:
```
# Your identification has been saved in /Users/you/.ssh/id_rsa.
# Your public key has been saved in /Users/you/.ssh/id_rsa.pub.
# The key fingerprint is:
# 01:0f:f4:3b:ca:85:sd:17:sd:7d:sd:68:9d:sd:a2:sd your_email@example.com
```

Once your prerequisites created you can use the [Cloudbreak UI](openstack_cb_ui.md) or use the [Cloudbreak shell](openstack_cb_shell.md).
