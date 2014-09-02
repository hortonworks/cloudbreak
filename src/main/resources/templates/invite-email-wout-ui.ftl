<html>
<head>
  <title>Cloudbreak invitation</title>
</head>
<body>
    <h1>
        Hello,
    </h1>
    <p>
        You've been invited by ${user.firstName} to join their Cloudbreak account.
        To register please perform POST request to the resource: ${invite}
    </p>
    eg.:
    <pre>
    curl -X POST -d {"type" : "inviteConfirmation", "firstName" : "your-firstname", "lastName" : "your-lastname", "password" : "your-password"} ${invite}
    </pre>
    <p>Thank you,</p
    <p>The SequenceIQ Team</p>
</p>
</body>
</html>
