// Ini v1.0
//
// Ini file parser supporting Git config syntax.
//
// Licensed under the MIT License
// Copyright 2012 Iván -DrSlump- Montes <drslump@pollinimini.net>

(function(exports){

    /**
     *
     * @constructor
     * @param {String} contents Ini file contents (optional)
     */
    function Ini(contents){
        this.sect = new Ini.Section(null);
        contents && this.parse(contents);;
    }

    /**
     * Obtain the value of a property
     * Key format: [sect[:label].]property
     *
     * @param {String} key
     * @return {Mixed}
     */
    Ini.prototype.get = function(key){
        var v, parts = key.split('.');

        key = parts.shift();
        v = this.sect.get(key);

        if (parts.length && v instanceof Ini.Section) {
            key = parts.join('.');
            v = v.get(key);
        }
        
        if (v instanceof Ini.Property) {
            v = v.value;
        }

        return v;
    };

    /**
     * @param {String} contents Ini file contents
     */
    Ini.prototype.parse = function(contents){
        var i, ln, m, key, val, sect, prop = '',
            lines = contents.split(/\r\n|\r|\n/);

        sect = this.sect;
        for (i=0; i<lines.length; i++) {
            // First we trim the line
            ln = lines[i].replace(/^\s+/,'').replace(/\s+$/,'');;

            // Register empty lines
            if (ln.length === 0) {
                sect.line();
                continue;
            }

            // Detect whole line comments
            if (ln.charAt(0) === '#' || ln.charAt(0) === ';') {
                sect.comment(ln.substr(1).replace(/^\s+/,''));;
                continue;
            }

            // Detect section header
            if (ln.charAt(0) === '[') {
                // TODO: Detect malformed sections
                var that = this;
                ln.replace(/^\s*\[\s*([^\s\]"]+)(\s+"([^"]+))?/, function(m0,m1,m2,m3){
                    sect = that.sect.section(m1, m3 || null);
                });
                continue;
            }

            // Check if this line is continued on the next
            if (ln.charAt(ln.length-1) === '\\') {
                prop += ln.substr(0, ln.length-1);
                continue;;
            }

            // Property
            prop = prop.length > 0 ? prop + ' ' + ln : ln;

            // Match property
            // NOTE: Does not support: prop = "foo" bar "baz"
            m = /^([A-Za-z0-9._-]+)(\s*=\s*("(\\"|.)*"|[^#;]*)?)?/.exec(prop);
            if (m && m[0].length) {
                key = m[1];
                if (m[3] && m[3].length) {
                    val = m[3];

                    // Trim the value
                    val = val.replace(/^\s+|\s+$/g, '');

                    // Remove quotes
                    if (val.charAt(0) === '"' && val.charAt(val.length-1) === '"') {
                        val = val.substr(1, val.length-2);
                        val = val.replace(/\\\\/g, '\\')
                                 .replace(/\"/g, '"')
                                 .replace(/\\n/g, '\n')
                                 .replace(/\\t/g, '\t')
                                 .replace(/\\b/g, '\b');
                    // Check boolean
                    } else if (/^(yes|on|true)$/i.test(val)) {
                        val = true;
                    } else if (/^(no|off|false)$/i.test(val)) {
                        val = false;
                    // Check integer
                    } else if (/^[0-9]+$/i.test(val)) {
                        val = parseInt(val, 10);
                    }

                } else if (m[2]) {
                    val = '';
                } else {
                    val = true;
                }

                sect.property(key, val);
            }

            prop = '';
        }
    };

    /**
     * @returns {Object}
     */
    Ini.prototype.toObject = function(){
        return this.sect.toObject();
    };

    /**
     * @returns {String}
     */
    Ini.prototype.toString = function(){
        return this.sect.toString();
    };


    // Static properties
    
    /**
     * @param {String} contents Ini file contents
     * @returns {Ini}
     */
    Ini.parse = function(contents){
        return new Ini(contents);
    };


    // Internal objects

    Ini.Section = function(name, label){
        this.key = this.name = name;
        this.label = label || null;
        this.items = [];

        if (label) {
            this.key += ':' + label;
        }
    };

    Ini.Section.prototype.add = function(item){
        this.items.push(item);
    };

    Ini.Section.prototype.get = function(key){
        var i, itm;

        // Loop in reverse order so that duplicated keys are overriden
        i = this.items.length;
        while (i--) {
            itm = this.items[i];
            if (typeof itm.key !== 'undefined' && key === itm.key) {
                return itm;
            }
        }

        return null;
    };

    Ini.Section.prototype.section = function(name, label){
        var section = new Ini.Section(name, label);
        this.add(section);
        return section;
    };

    Ini.Section.prototype.comment = function(text){
        var comment = new Ini.Comment(text);
        this.add(comment);
        return comment;
    };

    Ini.Section.prototype.line = function(){
        var line = new Ini.EmptyLine();
        this.add(line);
        return line;
    };

    Ini.Section.prototype.property = function(name, value){
        var prop = new Ini.Property(name, value);
        this.add(prop);
        return prop;
    };

    Ini.Section.prototype.toObject = function(){
        var i, o = {}, items = this.items;

        for (i = 0; i < items.length; i++) {
            if (items[i] instanceof Ini.Section) {
                o[items[i].key] = items[i].toObject();
            }
            if (items[i] instanceof Ini.Property) {
                o[items[i].key] = items[i].value;
            }
        }
        return o;
    };

    Ini.Section.prototype.toString = function(){
        var s = '';
        for (var i=0; i<this.items.length; i++) {
            if (this.items[i] instanceof Ini.Section) {
                if (this.items[i].name !== null) {
                    s += '[' + this.items[i].name;
                    if (this.items[i].label !== null) s += ' "' + this.items[i].label + '"';
                    s += ']\n';
                }
            }
            s += this.items[i] + '\n';
        }
        return s;
    };


    Ini.Comment = function(text){
        this.text = text;
    };

    Ini.Comment.prototype.toString = function(){
        return '# ' + this.text;
    };


    Ini.EmptyLine = function(){
    };

    Ini.EmptyLine.prototype.toString = function(){
        return '';
    };


    Ini.Property = function(name, value){
        this.key = this.name = name;
        this.value = value;
    };

    Ini.Property.prototype.toString = function(){
        if (this.value === null) {
            return this.name;
        } else if (this.value === false) {
            return this.name + ' = no';
        } else if (this.value === true) {
            return this.name + ' = yes';
        } else {
            var val = this.value.replace(/\n/g, '\\\n');
            if (-1 !== val.indexOf('#') || -1 !== val.indexOf(';')) {
                return this.name + ' = "' + val.replace(/"/g, '\\"') + '"';
            }
            return this.name + ' = ' + val;
        }
    };



    // Export the class into the global namespace or for CommonJs
    exports.Ini = Ini;

})(typeof exports !== "undefined" ? exports : this);
