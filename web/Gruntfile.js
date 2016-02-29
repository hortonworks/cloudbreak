module.exports = function(grunt) {

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
                config: ".jsbeautifyrc",
                mode: "VERIFY_ONLY"
            }
        }
    });

    grunt.loadNpmTasks('grunt-jsbeautifier');

    grunt.registerTask('default', ['jsbeautifier']);

};