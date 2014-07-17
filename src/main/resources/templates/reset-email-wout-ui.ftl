<html>
<head>
  <title>Password reset</title>
</head>
<body>
  <h1>
    Hello ${user.firstName},
  </h1>

</br>
  To generate a new password - POST ${confirm} with the password.
<p>
  Command line example: curl -X POST -H "Content-Type: application/json" -d '{"password":"&#60;new password&#62;"}' ${confirm} | jq .
<p>
  Thank you,

  </br>
  </br>
  The SequenceIQ Team
</p>
</body>
</html>
