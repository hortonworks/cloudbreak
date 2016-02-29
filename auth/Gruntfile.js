module.exports = function(grunt) {

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
                mode: "VERIFY_ONLY"
            }
        }
    });

    grunt.loadNpmTasks('grunt-jsbeautifier');

    grunt.registerTask('default', ['jsbeautifier']);

};