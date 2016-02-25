#!/usr/bin/env bash

PR_LOCATION="$(cd `dirname "${BASH_SOURCE[0]}"` && pwd)"
CONF_LOCATION="$PR_LOCATION/../config/jsbeautifyrc"

find $PR_LOCATION -type f -name "*.js" | grep -v -e "/lib/" -e "/node_modules/" -e "/app/static/bower_components" | tr "\n" "\0" | xargs -0 -I FILE js-beautify FILE --config $CONF_LOCATION --replace --type "js"
find $PR_LOCATION -type f -name "*.css" | grep -v -e "/app/static/bower_components" | tr "\n" "\0" | xargs -0 -I FILE js-beautify FILE --config $CONF_LOCATION --replace --type "css"
find $PR_LOCATION -type f -name *.html | grep -v -e "/node_modules" -e "/app/static/bower_components"  | tr "\n" "\0" | xargs -0 -I FILE js-beautify FILE --config $CONF_LOCATION --replace --type "html"
find $PR_LOCATION -type f -name *.tag -print0 | xargs -0 -I FILE js-beautify FILE --config $CONF_LOCATION --replace --type "html"

