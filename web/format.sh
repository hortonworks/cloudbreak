#!/usr/bin/env bash

find . -type f -name "*.js" | grep -v -e "/lib/" -e "/node_modules/" -e "/app/static/bower_components" | tr "\n" "\0" | xargs -0 -I FILE js-beautify FILE --config .jsbeautifyrc --replace --type "js"
find . -type f -name *.html | grep -v -e "/node_modules" -e "/app/static/bower_components"  | tr "\n" "\0" | xargs -0 -I FILE js-beautify FILE --config .jsbeautifyrc --replace --type "html"
find . -type f -name *.tag -print0 | xargs -0 -I FILE js-beautify FILE --config .jsbeautifyrc --replace --type "html"

