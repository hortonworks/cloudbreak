module.exports = function(grunt) {

    var jsbeautifierReplace = grunt.option('jsbeautifier.replace') || false;

    grunt.initConfig({
        jsbeautifier: {
            files: [
                "*.js",
                "public/js/**/*.js",
                "public/css/**/*.css",
                "app/*.html",

                "!public/js/lib/**/*.js"
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