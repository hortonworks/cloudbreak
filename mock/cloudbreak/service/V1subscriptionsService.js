'use strict';


/**
 * retrive subscribe identifier
 * Accepting client subscriptions to notification events.
 *
 * body SubscriptionRequest  (optional)
 * returns Id
 **/
exports.subscribeSubscription = function(body) {
  return new Promise(function(resolve, reject) {
    var examples = {};
    examples['application/json'] = {
  "id" : 0
};
    if (Object.keys(examples).length > 0) {
      resolve(examples[Object.keys(examples)[0]]);
    } else {
      resolve();
    }
  });
}

