var express = require('express');
var app = express();

app.get('/', function(req, res){
  var message = process.env.POWERED_BY;
  if (typeof(message) == "undefined") {
  	message = "Deis"
  }
  res.send('Powered by ' + message);
});

/* Use PORT environment variable if it exists */
var port = process.env.SEQ_REG_PORT;
server = app.listen(port);

console.log('Server listening on port %d in %s mode', server.address().port, app.settings.env);
