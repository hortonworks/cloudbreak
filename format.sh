#!/usr/bin/env bash

js-beautify server.js --config .jsbeautifyrc --replace --type "js"
js-beautify app/static/js/controllers/* --config .jsbeautifyrc --replace --type "js"
js-beautify app/static/js/app.js --config .jsbeautifyrc --replace --type "js"
js-beautify app/static/js/console.UI.js --config .jsbeautifyrc --replace --type "js"
js-beautify app/static/js/controllers.js --config .jsbeautifyrc --replace --type "js"
js-beautify app/static/js/services.js --config .jsbeautifyrc --replace --type "js"

js-beautify app/static/partials/dashboard.html --config .jsbeautifyrc --replace --type "html"

js-beautify app/static/tags/accountpanel.tag --config .jsbeautifyrc --replace --type "html"
js-beautify app/static/tags/blueprintpanel.tag --config .jsbeautifyrc --replace --type "html"
js-beautify app/static/tags/credentialpanel.tag --config .jsbeautifyrc --replace --type "html"
js-beautify app/static/tags/eventpanel.tag --config .jsbeautifyrc --replace --type "html"
js-beautify app/static/tags/networkpanel.tag --config .jsbeautifyrc --replace --type "html"
js-beautify app/static/tags/recipepanel.tag --config .jsbeautifyrc --replace --type "html"
js-beautify app/static/tags/securitygrouppanel.tag --config .jsbeautifyrc --replace --type "html"
js-beautify app/static/tags/templatepanel.tag --config .jsbeautifyrc --replace --type "html"
js-beautify app/static/tags/usagepanel.tag --config .jsbeautifyrc --replace --type "html"
js-beautify app/static/tags/userspanel.tag --config .jsbeautifyrc --replace --type "html"

js-beautify app/static/tags/blueprint/* --config .jsbeautifyrc --replace --type "html"
js-beautify app/static/tags/credential/* --config .jsbeautifyrc --replace --type "html"
js-beautify app/static/tags/network/* --config .jsbeautifyrc --replace --type "html"
js-beautify app/static/tags/periscope/* --config .jsbeautifyrc --replace --type "html"
js-beautify app/static/tags/recipe/* --config .jsbeautifyrc --replace --type "html"
js-beautify app/static/tags/securitygroup/* --config .jsbeautifyrc --replace --type "html"
js-beautify app/static/tags/stack/* --config .jsbeautifyrc --replace --type "html"
js-beautify app/static/tags/template/* --config .jsbeautifyrc --replace --type "html"

js-beautify app/index.html --config .jsbeautifyrc --replace --type "html"

