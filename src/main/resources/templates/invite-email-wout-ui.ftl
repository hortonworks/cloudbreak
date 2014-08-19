<html>
<head>
  <title>Cloudbreak invitation</title>
</head>
<body>
  <h1>
    Hello,
  </h1>

</br>
    You've been invited by ${user.firstName} to join his/her Cloudbreak account.
    To register - POST ${invite} with the password.
<p>
  Command line example: curl -X POST -H "Content-Type: application/json" -d '{"password":"&#60;new password&#62;"}' ${invite} | jq .
<p>
  Thank you,

  </br>
  </br>
  The SequenceIQ Team
</p>
</body>
</html>
