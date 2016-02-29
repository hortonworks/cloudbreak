#!/usr/bin/env bash

PR_LOCATION="$(cd `dirname "${BASH_SOURCE[0]}"` && pwd)"
CONF_LOCATION="$PR_LOCATION/../config/jsbeautifyrc"

find $PR_LOCATION -type f -name "*.js" | grep -v -e "/lib/" -e "/node_modules/" | tr "\n" "\0" | xargs -0 -I FILE js-beautify FILE --config $CONF_LOCATION --replace --type "js"
find $PR_LOCATION -type f -name "*.css" | tr "\n" "\0" | xargs -0 -I FILE js-beautify FILE --config $CONF_LOCATION --replace --type "css"
find $PR_LOCATION -type f -name *.html | grep -v -e "/node_modules" | tr "\n" "\0" | xargs -0 -I FILE js-beautify FILE --config $CONF_LOCATION --replace --type "html"

