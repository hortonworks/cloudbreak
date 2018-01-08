'use strict';

exports.subscribeSubscription = function(args, res, next) {
  /**
   * retrive subscribe identifier
   * Accepting client subscriptions to notification events.
   *
   * body SubscriptionRequest  (optional)
   * returns Id
   **/
  var examples = {};
  examples['application/json'] = {
  "id" : 0
};
  if (Object.keys(examples).length > 0) {
    res.setHeader('Content-Type', 'application/json');
    res.end(JSON.stringify(examples[Object.keys(examples)[0]] || {}, null, 2));
  } else {
    res.end();
  }
}

