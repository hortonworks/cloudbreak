module.exports = function(grunt) {

    var jsbeautifierReplace = grunt.option('jsbeautifier.replace') || false;

    grunt.initConfig({
        jsbeautifier: {
            files: [
                "server.js",
                "app/static/js/**/*.js",
                "app/static/css/**/*.css",
                "app/static/tags/**/*.tag",

                "!app/static/js/lib/**/*.js"
            ],
            options: {
                config: "../config/jsbeautifyrc",
                mode: jsbeautifierReplace ? "VERIFY_AND_WRITE" : "VERIFY_ONLY"
            }
        }
    });

    grunt.loadNpmTasks('grunt-jsbeautifier');

    grunt.registerTask('default', ['jsbeautifier']);

};