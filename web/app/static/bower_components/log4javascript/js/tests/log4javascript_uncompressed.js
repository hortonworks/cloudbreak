/**
 * Copyright 2013 Tim Down.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

function array_contains(arr, val) {
	for (var i = 0; i < arr.length; i++) {
		if (arr[i] == val) {
			return true;
		}
	}
	return false;
}

// Recursively checks that obj2's interface contains all of obj1's
// interface (functions and objects only)
function compareObjectInterface(obj1, obj1_name, obj2, obj2_name, namePrefix) {
	if (!namePrefix) {
		namePrefix = "";
	}
	var obj1PropertyNames = new Array();
	for (var i in obj1) {
		if (i != "prototype" && i != "arguments") {
			obj1PropertyNames.push(i);
		}
	}
	if (obj1 && obj1.prototype && !array_contains(obj1PropertyNames, "prototype")) {
		//obj1PropertyNames.push("prototype");
	}
	for (var j = 0; j < obj1PropertyNames.length; j++) {
		var propertyName = obj1PropertyNames[j];
		if ((typeof obj1[propertyName] == "function" || typeof obj1[propertyName] == "object") && !(obj1[propertyName] instanceof Array)) {
			var propertyFullyQualifiedName = (namePrefix == "") ? propertyName : namePrefix + "." + propertyName;
			try {
				if (typeof obj2[propertyName] == "undefined") {
					throw new Error(obj2_name + " does not contain " + propertyFullyQualifiedName + " in " + obj1_name);
				} else if (typeof obj2[propertyName] != typeof obj1[propertyName]){
					throw new Error(obj2_name + "'s " + propertyFullyQualifiedName + " is of the wrong type: " + typeof obj2[propertyName] + " when it is type " + typeof obj1[propertyName] + " in " + obj1_name);
				} else if (obj1[propertyName] != Function.prototype.apply) {
					if (!compareObjectInterface(obj1[propertyName], obj1_name, obj2[propertyName], obj2_name, propertyFullyQualifiedName)) {
						throw new Error("Interfaces don't match");
					}
				}
			} catch(ex) {
				throw new Error("Exception while checking property name " + propertyFullyQualifiedName + " in " + obj2_name + ": " + ex.message);
			}
		}
	}
	return true;
};

// Simply tests a layout for exceptions when formatting
var testLayoutWithVariables = function(layout, t) {
	var emptyObject = {};
	var emptyArray = [];
	var emptyString = "";
	var localUndefined = emptyArray[0];
	var oneLevelObject = {
		"name": "One-level object"
	};
	var twoLevelObject = {
		"name": "Two-level object",
		"data": oneLevelObject
	};
	var threeLevelObject = {
		"name": "Three-level object",
		"data": twoLevelObject
	};
	var anArray = [
		3,
		"stuff",
		true,
		false,
		0,
		null,
		localUndefined,
		3.14,
		function(p) { return "I'm a function"; },
		[1, "things"]
	];
	var arrayOfTestItems = [emptyObject, emptyString, emptyString, localUndefined, oneLevelObject,
			twoLevelObject, threeLevelObject, anArray];

	t.log("Testing layout " + layout)
	for (var i = 0; i < arrayOfTestItems.length; i++) {
		var ex = new Error("Test error");
		var loggingEvent = new log4javascript.LoggingEvent(t.logger, new Date(), log4javascript.Level.INFO,
				[arrayOfTestItems[i]], null);
		t.log("Formatting", arrayOfTestItems[i], result);
		var result = layout.format(loggingEvent);
		// Now try with an exception
		loggingEvent.exception = ex;
		t.log("Formatting with exception", arrayOfTestItems[i], result);
		result = layout.format(loggingEvent);
	}
};

xn.test.enableTestDebug = true;
xn.test.enable_log4javascript = false;

xn.test.suite("log4javascript tests", function(s) {
	log4javascript.logLog.setQuietMode(true);
	var ArrayAppender = function(layout) {
		if (layout) {
			this.setLayout(layout);
		}
		this.logMessages = [];
	};

	ArrayAppender.prototype = new log4javascript.Appender();

	ArrayAppender.prototype.layout = new log4javascript.NullLayout();

	ArrayAppender.prototype.append = function(loggingEvent) {
		var formattedMessage = this.getLayout().format(loggingEvent);
		if (this.getLayout().ignoresThrowable()) {
			formattedMessage += loggingEvent.getThrowableStrRep();
		}
		this.logMessages.push(formattedMessage);
	};

	ArrayAppender.prototype.toString = function() {
		return "[ArrayAppender]";
	};

    s.setUp = function(t) {
        t.logger = log4javascript.getLogger("test");
		t.logger.removeAllAppenders();
		t.appender = new ArrayAppender();
        t.logger.addAppender(t.appender);
    };

    s.tearDown = function(t) {
        t.logger.removeAppender(t.appender);
		log4javascript.resetConfiguration();
	};

    s.test("Stub script interface test", function(t) {
        try {
            compareObjectInterface(log4javascript, "log4javascript", log4javascript_stub, "log4javascript_stub");
        } catch (ex) {
            t.fail(ex);
        }
    });

	s.test("Disable log4javascript test", function(t) {
		log4javascript.setEnabled(false);
		t.logger.debug("TEST");
		t.assertEquals(t.appender.logMessages.length, 0);
		log4javascript.setEnabled(true);
	});

    s.test("Array.splice test 1", function(t) {
        var a = ["Marlon", "Ashley", "Darius", "Lloyd"];
        var deletedItems = a.splice(1, 2);
        t.assertEquals(a.join(","), "Marlon,Lloyd");
        t.assertEquals(deletedItems.join(","), "Ashley,Darius");
    });

    s.test("Array.splice test 2", function(t) {
        var a = ["Marlon", "Ashley", "Darius", "Lloyd"];
        var deletedItems = a.splice(1, 1, "Malky", "Jay");
        t.assertEquals(a.join(","), "Marlon,Malky,Jay,Darius,Lloyd");
        t.assertEquals(deletedItems.join(","), "Ashley");
    });

    s.test("array_remove test", function(t) {
        var array_remove = log4javascript.evalInScope("array_remove");
        var a = ["Marlon", "Ashley", "Darius"];
        array_remove(a, "Darius");
        t.assertEquals(a.join(","), "Marlon,Ashley");
    });

	s.test("array_remove with empty array test", function(t) {
		var array_remove = log4javascript.evalInScope("array_remove");
		var a = [];
		array_remove(a, "Darius");
		t.assertEquals(a.join(","), "");
	});

    s.test("Logger logging test", function(t) {
        // Should log since the default level for loggers is DEBUG and
        // the default threshold for appenders is ALL
        t.logger.debug("TEST");
        t.assertEquals(t.appender.logMessages.length, 1);
    });

    s.test("Logger levels test", function(t) {
        var originalLevel = t.logger.getEffectiveLevel();
        t.logger.setLevel(log4javascript.Level.INFO);
        t.logger.debug("TEST");
		t.logger.setLevel(originalLevel);
        t.assertEquals(t.appender.logMessages.length, 0);
    });

	s.test("Logger getEffectiveLevel inheritance test 1", function(t) {
		var parentLogger = log4javascript.getLogger("test1");
		var childLogger = log4javascript.getLogger("test1.test2");
		parentLogger.setLevel(log4javascript.Level.ERROR);
		t.assertEquals(childLogger.getEffectiveLevel(), log4javascript.Level.ERROR);
	});

	s.test("Logger getEffectiveLevel inheritance test 2", function(t) {
		var grandParentLogger = log4javascript.getLogger("test1");
		var childLogger = log4javascript.getLogger("test1.test2.test3");
		grandParentLogger.setLevel(log4javascript.Level.ERROR);
		t.assertEquals(childLogger.getEffectiveLevel(), log4javascript.Level.ERROR);
	});

	s.test("Logger getEffectiveLevel inheritance test 3", function(t) {
		var parentLogger = log4javascript.getLogger("test1");
		var childLogger = log4javascript.getLogger("test1.test2");
		parentLogger.setLevel(log4javascript.Level.ERROR);
		childLogger.setLevel(log4javascript.Level.INFO);
		t.assertEquals(childLogger.getEffectiveLevel(), log4javascript.Level.INFO);
	});

	s.test("Logger getEffectiveLevel root inheritance test", function(t) {
		var rootLogger = log4javascript.getRootLogger();
		var childLogger = log4javascript.getLogger("test1.test2.test3");
		rootLogger.setLevel(log4javascript.Level.WARN);
		t.assertEquals(childLogger.getEffectiveLevel(), log4javascript.Level.WARN);
	});

	s.test("Logger null level test", function(t) {
		t.logger.setLevel(null);
		// Should default to root logger level, which is DEBUG
		t.assertEquals(t.logger.getEffectiveLevel(), log4javascript.Level.DEBUG);
	});

	s.test("Logger appender additivity test 1", function(t) {
		var parentLogger = log4javascript.getLogger("test1");
		var childLogger = log4javascript.getLogger("test1.test2");
		var parentLoggerAppender = new ArrayAppender();
		var childLoggerAppender = new ArrayAppender();

		parentLogger.addAppender(parentLoggerAppender);
		childLogger.addAppender(childLoggerAppender);

		parentLogger.info("Parent logger test message");
		childLogger.info("Child logger test message");

		t.assertEquals(parentLoggerAppender.logMessages.length, 2);
		t.assertEquals(childLoggerAppender.logMessages.length, 1);
	});

	s.test("Logger appender additivity test 2", function(t) {
		var parentLogger = log4javascript.getLogger("test1");
		var childLogger = log4javascript.getLogger("test1.test2");
		var parentLoggerAppender = new ArrayAppender();
		var childLoggerAppender = new ArrayAppender();

		parentLogger.addAppender(parentLoggerAppender);
		childLogger.addAppender(childLoggerAppender);

		childLogger.setAdditivity(false);

		parentLogger.info("Parent logger test message");
		childLogger.info("Child logger test message");

		t.assertEquals(parentLoggerAppender.logMessages.length, 1);
		t.assertEquals(childLoggerAppender.logMessages.length, 1);
	});

	s.test("Logger appender additivity test 3", function(t) {
		var parentLogger = log4javascript.getLogger("test1");
		var childLogger = log4javascript.getLogger("test1.test2");
		var parentLoggerAppender = new ArrayAppender();
		var childLoggerAppender = new ArrayAppender();

		parentLogger.addAppender(parentLoggerAppender);
		childLogger.addAppender(childLoggerAppender);

		childLogger.setAdditivity(false);

		parentLogger.info("Parent logger test message");
		childLogger.info("Child logger test message");

		childLogger.setAdditivity(true);

		childLogger.info("Child logger test message 2");

		t.assertEquals(parentLoggerAppender.logMessages.length, 2);
		t.assertEquals(childLoggerAppender.logMessages.length, 2);
	});

	s.test("Appender threshold test", function(t) {
        t.appender.setThreshold(log4javascript.Level.INFO);
        t.logger.debug("TEST");
        t.assertEquals(t.appender.logMessages.length, 0);
    });

    s.test("Basic appender / layout test", function(t) {
        t.logger.debug("TEST");
		t.assertEquals(t.appender.logMessages[0], "TEST");
    });

	s.test("Appender uniqueness within logger test", function(t) {
		// Add the same appender to the logger for a second time
		t.logger.addAppender(t.appender);
        t.logger.debug("TEST");
        t.assertEquals(t.appender.logMessages.length, 1);
    });

	s.test("Logger remove appender test", function(t) {
		t.logger.debug("TEST");
		t.logger.removeAppender(t.appender);
		t.logger.debug("TEST AGAIN");
		t.assertEquals(t.appender.logMessages.length, 1);
	});

	s.test("", function(t) {
		t.logger.debug("TEST");
		t.logger.removeAppender(t.appender);
		t.logger.debug("TEST AGAIN");
		t.assertEquals(t.appender.logMessages.length, 1);
	});
	s.test("SimpleLayout format test", function(t) {
		var layout = new log4javascript.SimpleLayout();
		testLayoutWithVariables(layout, t);
	});

    s.test("SimpleLayout test", function(t) {
        t.appender.setLayout(new log4javascript.SimpleLayout());
        t.logger.debug("TEST");
        t.assertEquals(t.appender.logMessages[0], "DEBUG - TEST");
    });
	s.test("NullLayout format test", function(t) {
		var layout = new log4javascript.NullLayout();
		testLayoutWithVariables(layout, t);
	});

    s.test("NullLayout test", function(t) {
        t.appender.setLayout(new log4javascript.NullLayout());
        t.logger.debug("TEST");
        t.assertEquals(t.appender.logMessages[0], "TEST");
    });
	s.test("XmlLayout format test", function(t) {
		var layout = new log4javascript.XmlLayout();
		testLayoutWithVariables(layout, t);
	});

    s.test("XmlLayout test", function(t) {
        t.appender.setLayout(new log4javascript.XmlLayout());
        t.logger.debug("TEST");
        t.assertRegexMatches(/^<log4javascript:event logger="test" timestamp="\d+" level="DEBUG">\s*<log4javascript:message><!\[CDATA\[TEST\]\]><\/log4javascript:message>\s*<\/log4javascript:event>\s*$/, t.appender.logMessages[0]);
    });

    s.test("XmlLayout with exception test", function(t) {
        t.appender.setLayout(new log4javascript.XmlLayout());
        t.logger.debug("TEST", new Error("Test error"));
        t.assertRegexMatches(/^<log4javascript:event logger="test" timestamp="\d+" level="DEBUG">\s*<log4javascript:message><!\[CDATA\[TEST\]\]><\/log4javascript:message>\s*<log4javascript:exception>\s*<!\[CDATA\[.*\]\]><\/log4javascript:exception>\s*<\/log4javascript:event>\s*$/, t.appender.logMessages[0]);
    });

	var setUpXmlLayoutMillisecondsTest = function(t) {
		t.date = new Date();
		t.timeInMilliseconds = t.date.getTime();
		t.timeInSeconds = Math.floor(t.timeInMilliseconds / 1000);
		t.milliseconds = t.date.getMilliseconds();
		
		t.loggingEvent = new log4javascript.LoggingEvent(t.logger, t.date, log4javascript.Level.DEBUG, ["TEST"], null);
		t.layout = new log4javascript.XmlLayout();
	}

	s.test("XmlLayout seconds/milliseconds test 1", function(t) {
		setUpXmlLayoutMillisecondsTest(t);

		// Test default (i.e. timestamps in milliseconds) first
        var regex = new RegExp('^<log4javascript:event logger="test" timestamp="' + t.timeInMilliseconds + '" level="DEBUG">\\s*<log4javascript:message><!\\[CDATA\\[TEST\\]\\]></log4javascript:message>\\s*</log4javascript:event>\\s*$');
        t.assertRegexMatches(regex, t.layout.format(t.loggingEvent));
	});
	
	s.test("XmlLayout seconds/milliseconds test 2", function(t) {
		setUpXmlLayoutMillisecondsTest(t);

        // Change the global setting
        log4javascript.setTimeStampsInMilliseconds(false);
        var formatted = t.layout.format(t.loggingEvent);
        log4javascript.setTimeStampsInMilliseconds(true);
        var regex = new RegExp('^<log4javascript:event logger="test" timestamp="' + t.timeInSeconds + '" milliseconds="' + t.milliseconds + '" level="DEBUG">\\s*<log4javascript:message><!\\[CDATA\\[TEST\\]\\]></log4javascript:message>\\s*</log4javascript:event>\\s*$');
        t.assertRegexMatches(regex, formatted);
	});

	s.test("XmlLayout seconds/milliseconds test 3", function(t) {
		setUpXmlLayoutMillisecondsTest(t);

        // Change the layout setting
        t.layout.setTimeStampsInMilliseconds(false);
        var formatted = t.layout.format(t.loggingEvent);
        var regex = new RegExp('^<log4javascript:event logger="test" timestamp="' + t.timeInSeconds + '" milliseconds="' + t.milliseconds + '" level="DEBUG">\\s*<log4javascript:message><!\\[CDATA\\[TEST\\]\\]></log4javascript:message>\\s*</log4javascript:event>\\s*$');
        t.assertRegexMatches(regex, formatted);
	});
	s.test("escapeNewLines test", function(t) {
		var escapeNewLines = log4javascript.evalInScope("escapeNewLines");
		var str = "1\r2\n3\n4\r\n5\r6\r\n7";
		t.assertEquals(escapeNewLines(str), "1\\r\\n2\\r\\n3\\r\\n4\\r\\n5\\r\\n6\\r\\n7");
	});

	s.test("JsonLayout format test", function(t) {
		var layout = new log4javascript.JsonLayout();
		testLayoutWithVariables(layout, t);
	});

    s.test("JsonLayout test", function(t) {
        t.appender.setLayout(new log4javascript.JsonLayout());
        t.logger.debug("TEST");
        t.assertRegexMatches(/^{"logger":"test","timestamp":\d+,"level":"DEBUG","url":".*","message":"TEST"}$/, t.appender.logMessages[0]);
    });

    s.test("JsonLayout JSON validity test", function(t) {
        t.appender.setLayout(new log4javascript.JsonLayout());
        t.logger.debug("TEST");
        eval("var o = " + t.appender.logMessages[0]);
        t.assertEquals(o.message, "TEST");
    });

    s.test("JsonLayout with number type message test", function(t) {
        t.appender.setLayout(new log4javascript.JsonLayout());
        t.logger.debug(15);
        t.assertRegexMatches(/^{"logger":"test","timestamp":\d+,"level":"DEBUG","url":".*","message":15}$/, t.appender.logMessages[0]);
    });

    s.test("JsonLayout with object type message test", function(t) {
        t.appender.setLayout(new log4javascript.JsonLayout());
        t.logger.debug({});
        t.assertRegexMatches(/^{"logger":"test","timestamp":\d+,"level":"DEBUG","url":".*","message":"\[object Object\]"}$/, t.appender.logMessages[0]);
    });

    s.test("JsonLayout with boolean type message test", function(t) {
        t.appender.setLayout(new log4javascript.JsonLayout());
        t.logger.debug(false);
        t.assertRegexMatches(/^{"logger":"test","timestamp":\d+,"level":"DEBUG","url":".*","message":false}$/, t.appender.logMessages[0]);
    });

    s.test("JsonLayout with quote test", function(t) {
        t.appender.setLayout(new log4javascript.JsonLayout());
        t.logger.debug("TE\"S\"T");
        t.assertRegexMatches(/^{"logger":"test","timestamp":\d+,"level":"DEBUG","url":".*","message":"TE\\"S\\"T"}$/, t.appender.logMessages[0]);
    });

    s.test("JsonLayout with exception test", function(t) {
        t.appender.setLayout(new log4javascript.JsonLayout());
        t.logger.debug("TEST", new Error("Test error"));
        t.assertRegexMatches(/^{"logger":"test","timestamp":\d+,"level":"DEBUG","url":".*","message":"TEST","exception":.*}$/, t.appender.logMessages[0]);
    });

	var setUpJsonLayoutMillisecondsTest = function(t) {
		t.date = new Date();
		t.timeInMilliseconds = t.date.getTime();
		t.timeInSeconds = Math.floor(t.timeInMilliseconds / 1000);
		t.milliseconds = t.date.getMilliseconds();
		
		t.loggingEvent = new log4javascript.LoggingEvent(t.logger, t.date, log4javascript.Level.DEBUG, ["TEST"], null);
		t.layout = new log4javascript.JsonLayout();
	};

	s.test("JsonLayout seconds/milliseconds test 1", function(t) {
		setUpJsonLayoutMillisecondsTest(t);

		// Test default (i.e. timestamps in milliseconds) first
        var regex = new RegExp('^{"logger":"test","timestamp":' + t.timeInMilliseconds + ',"level":"DEBUG","url":".*","message":"TEST"}$');
        t.assertRegexMatches(regex, t.layout.format(t.loggingEvent));
	});
	
	s.test("JsonLayout seconds/milliseconds test 2", function(t) {
		setUpJsonLayoutMillisecondsTest(t);

        // Change the global setting
        log4javascript.setTimeStampsInMilliseconds(false);
        var formatted = t.layout.format(t.loggingEvent);
        log4javascript.setTimeStampsInMilliseconds(true);
        var regex = new RegExp('^{"logger":"test","timestamp":' + t.timeInSeconds + ',"level":"DEBUG","url":".*","message":"TEST","milliseconds":' + t.milliseconds + '}$');
        t.assertRegexMatches(regex, formatted);
	});

	s.test("JsonLayout seconds/milliseconds test 3", function(t) {
		setUpJsonLayoutMillisecondsTest(t);

        // Change the layout setting
        t.layout.setTimeStampsInMilliseconds(false);
        var formatted = t.layout.format(t.loggingEvent);
        var regex = new RegExp('^{"logger":"test","timestamp":' + t.timeInSeconds + ',"level":"DEBUG","url":".*","message":"TEST","milliseconds":' + t.milliseconds + '}$');
        t.assertRegexMatches(regex, formatted);
	});
	s.test("HttpPostDataLayout format test", function(t) {
		var layout = new log4javascript.HttpPostDataLayout();
		testLayoutWithVariables(layout, t);
	});

    s.test("HttpPostDataLayout test", function(t) {
        t.appender.setLayout(new log4javascript.HttpPostDataLayout());
        t.logger.debug("TEST");
        t.assertRegexMatches(/^logger=test&timestamp=\d+&level=DEBUG&url=.*&message=TEST$/, t.appender.logMessages[0]);
    });

    s.test("HttpPostDataLayout URL encoding test", function(t) {
        t.appender.setLayout(new log4javascript.HttpPostDataLayout());
        t.logger.debug("TEST +\"1\"");
        t.assertRegexMatches(/^logger=test&timestamp=\d+&level=DEBUG&url=.*&message=TEST%20%2B%221%22$/, t.appender.logMessages[0]);
    });

    s.test("HttpPostDataLayout with exception test", function(t) {
        t.appender.setLayout(new log4javascript.HttpPostDataLayout());
        t.logger.debug("TEST", new Error("Test error"));
        t.assertRegexMatches(/^logger=test&timestamp=\d+&level=DEBUG&url=.*&message=TEST&exception=.*$/, t.appender.logMessages[0]);
    });

	(function() {
		var formatObjectExpansion = log4javascript.evalInScope("formatObjectExpansion");
		var newLine = log4javascript.evalInScope("newLine");
		var arr = [
			null,
			undefined,
			1.2,
			"A string",
			[1, "test"],
			{
				a: {
					b: 1
				}
			}
		];

		s.test("Basic formatObjectExpansion array test (depth: 1)", function(t) {
			t.assertEquals(formatObjectExpansion(arr, 1),
				"[" + newLine +
				"  null," + newLine +
				"  undefined," + newLine +
				"  1.2," + newLine +
				"  A string," + newLine +
				"  1,test," + newLine +
				"  [object Object]" + newLine +
				"]"
			);
		});

		s.test("Basic formatObjectExpansion array test (depth: 2)", function(t) {
			t.assertEquals(formatObjectExpansion(arr, 2),
				"[" + newLine +
				"  null," + newLine +
				"  undefined," + newLine +
				"  1.2," + newLine +
				"  A string," + newLine +
				"  [" + newLine +
				"    1," + newLine +
				"    test" + newLine +
				"  ]," + newLine +
				"  {" + newLine +
				"    a: [object Object]" + newLine +
				"  }" + newLine +
				"]"
			);
		});

		s.test("formatObjectExpansion simple object test", function(t) {
			var obj = {
				STRING: "A string"
			};
			t.assertEquals(formatObjectExpansion(obj, 1), 
				"{" + newLine +
				"  STRING: A string" + newLine +
				"}"
			);
		});

		s.test("formatObjectExpansion simple circular object test", function(t) {
			var obj = {};
			obj.a = obj;
			
			t.assertEquals(formatObjectExpansion(obj, 2), 
				"{" + newLine +
				"  a: [object Object] [already expanded]" + newLine +
				"}"
			);
		});
	})();    /* ---------------------------------------------------------- */

    var getSampleDate = function() {
        var date = new Date();
        date.setFullYear(2006);
        date.setMonth(7);
        date.setDate(30);
        date.setHours(15);
        date.setMinutes(38);
        date.setSeconds(45);
        return date;
    };

    /* ---------------------------------------------------------- */

    s.test("String.replace test", function(t) {
        t.assertEquals("Hello world".replace(/o/g, "Z"), "HellZ wZrld");
    });

	s.test("PatternLayout format test", function(t) {
		var layout = new log4javascript.PatternLayout();
		testLayoutWithVariables(layout, t);
	});

    s.test("PatternLayout dates test", function(t) {
        var layout = new log4javascript.PatternLayout("%d %d{DATE} %d{HH:ss}");
        t.appender.setLayout(layout);
        t.logger.debug("TEST");
        t.assertRegexMatches(/^\d{4}-\d{2}-\d{2} \d{2}:\d{2}:\d{2},\d{3} \d{2} [A-Z][a-z]{2} \d{4} \d{2}:\d{2}:\d{2},\d{3} \d{2}:\d{2}$/, t.appender.logMessages[0]);
    });

    s.test("PatternLayout modifiers test", function(t) {
        var layout = new log4javascript.PatternLayout("%m|%3m|%-3m|%6m|%-6m|%.2m|%1.2m|%6.8m|%-1.2m|%-6.8m|");
        t.appender.setLayout(layout);
        t.logger.debug("TEST");
        t.assertEquals(t.appender.logMessages[0], "TEST|TEST|TEST|  TEST|TEST  |ST|ST|  TEST|ST|TEST  |");
    });

    s.test("PatternLayout conversion characters test", function(t) {
        var layout = new log4javascript.PatternLayout("%c %n %p %r literal %%");
        t.appender.setLayout(layout);
        t.logger.debug("TEST");
        t.assertRegexMatches(/^test \s+ DEBUG \d+ literal %$/, t.appender.logMessages[0]);
    });

    s.test("PatternLayout message test", function(t) {
        var layout = new log4javascript.PatternLayout("%m{1} %m{2}");
        t.appender.setLayout(layout);
        var testObj = {
            strikers: {
                quick: "Marlon"
            }
        };
        t.logger.debug(testObj);
        t.assertEquals("{\r\n  strikers: [object Object]\r\n} {\r\n\  strikers: {\r\n    quick: Marlon\r\n  }\r\n}", t.appender.logMessages[0]);
    });
	// Tests for exceptions when logging
	s.test("Logging/grouping test", function(t) {
		var browserConsoleAppender = new log4javascript.BrowserConsoleAppender();
		t.logger.addAppender(browserConsoleAppender);

		// Test each level
		t.logger.trace("TEST TRACE");
		t.logger.debug("TEST DEBUG");
		t.logger.info("TEST INFO");
		t.logger.warn("TEST WARN");
		t.logger.error("TEST ERROR");
		t.logger.fatal("TEST FATAL");
		
		// Test with exception
		t.logger.fatal("TEST FATAL", new Error("Fake error"));
		
		// Test multiple messages
		t.logger.info("TEST INFO", "Second message", ["a", "b", "c"]);
		
		// Test groups
		t.logger.group("TEST GROUP");
		t.logger.info("TEST INFO");
		t.logger.groupEnd("TEST GROUP");
		t.logger.info("TEST INFO");
		
		t.logger.removeAppender(browserConsoleAppender);
	});

/*
	s.test("AjaxAppender JsonLayout single message test", function(t) {
		t.async(10000);
		// Create and add an Ajax appender
		var ajaxAppender = new log4javascript.AjaxAppender("../log4javascript.do");
		ajaxAppender.setLayout(new log4javascript.JsonLayout());
		ajaxAppender.setRequestSuccessCallback(
			function(xmlHttp) {
				// Response comes back as JSON array of messages logged
				var jsonResponse = xmlHttp.responseText;
				var arr = eval(jsonResponse);
				t.assertEquals(arr.length, 1);
				t.assertEquals(arr[0], "TEST");
				t.succeed();
			}
		);
		ajaxAppender.setFailCallback(
			function(msg) {
				t.fail(msg);
				ajaxErrorMessage = msg;
			}
		);
		t.logger.addAppender(ajaxAppender);
		t.logger.debug("TEST");
	});

	s.test("AjaxAppender JsonLayout batched messages test", function(t) {
		t.async(10000);
		var message1 = "TEST 1";
		var message2 = "String with \"lots of 'quotes'\" + plusses in";
		var message3 = "A non-threatening string";
		// Create and add an Ajax appender
		var ajaxAppender = new log4javascript.AjaxAppender("../log4javascript.do");
		ajaxAppender.setLayout(new log4javascript.JsonLayout());
		ajaxAppender.setBatchSize(3);
		ajaxAppender.setRequestSuccessCallback(
			function(xmlHttp) {
				// Response comes back as JSON array of messages logged
				var jsonResponse = xmlHttp.responseText;
				var arr = eval(jsonResponse);
				t.assertEquals(arr.length, 3);
				t.assertEquals(arr[0], message1);
				t.assertEquals(arr[1], message2);
				t.assertEquals(arr[2], message3);
				t.succeed();
			}
		);
		ajaxAppender.setFailCallback(
			function(msg) {
				t.fail(msg);
				ajaxErrorMessage = msg;
			}
		);
		t.logger.addAppender(ajaxAppender);
		t.logger.debug(message1);
		t.logger.info(message2);
		t.logger.warn(message3);
	});

	s.test("AjaxAppender HttpPostDataLayout single message test", function(t) {
		t.async(10000);
		// Create and add an Ajax appender
		var ajaxAppender = new log4javascript.AjaxAppender("../log4javascript.do");
		var testMessage = "TEST +\"1\"";
		ajaxAppender.setLayout(new log4javascript.HttpPostDataLayout());
		ajaxAppender.setRequestSuccessCallback(
			function(xmlHttp) {
				// Response comes back as JSON array of messages logged
				var jsonResponse = xmlHttp.responseText;
				var arr = eval(jsonResponse);
				t.assertEquals(arr.length, 1);
				t.assertEquals(arr[0], testMessage);
				t.succeed();
			}
		);
		ajaxAppender.setFailCallback(
			function(msg) {
				t.fail(msg);
				ajaxErrorMessage = msg;
			}
		);
		t.logger.addAppender(ajaxAppender);
		t.logger.debug(testMessage);
	});
*/
	var testConsoleAppender = function(t, appender) {
		var timeoutCallback = function() {
			//alert("Failed. Debug messages follow.");
			//log4javascript.logLog.displayDebug();
			return (windowLoaded ? "Timed out while waiting for messages to appear" :
				   "Timed out while waiting for window to load") + ". Debug messages: " +
				   log4javascript.logLog.debugMessages.join("\r\n");
		}

		t.async(60000, timeoutCallback);

		var windowLoaded = false;
		var domChecked = false;

		// Set a timeout to allow the pop-up to appear
		var onLoadHandler = function() {
			log4javascript.logLog.debug("onLoadHandler");
			windowLoaded = true;
			var win = appender.getConsoleWindow();

			if (win && win.loaded) {
				// Check that the log container element contains the log message. Since
				// the console window waits 100 milliseconds before actually rendering the
				// message as a DOM element, we need to use a timer
				var checkDom = function() {
					log4javascript.logLog.debug("checkDom");
					domChecked = true;
					var logContainer = win.logMainContainer;
					if (logContainer.hasChildNodes()) {
						if (logContainer.innerHTML.indexOf("TEST MESSAGE") == -1) {
							appender.close();
							t.fail("Log message not correctly logged (log container innerHTML: " + logContainer.innerHTML + ")");
						} else {
							t.assert(appender.isVisible());
							appender.close();
							t.assert(!appender.isVisible());
							t.succeed();
						}
					} else {
						appender.close();
						t.fail("Console has no log messages");
					}
				}
				window.setTimeout(checkDom, 300);
			} else {
				appender.close();
				t.fail("Console mistakenly raised load event");
			}
		}

		appender.addEventListener("load", onLoadHandler);
		t.logger.addAppender(appender);
		t.logger.debug("TEST MESSAGE");
	};

	s.test("InlineAppender test", function(t) {
		var inlineAppender = new log4javascript.InlineAppender();
		inlineAppender.setInitiallyMinimized(false);
		inlineAppender.setNewestMessageAtTop(false);
		inlineAppender.setScrollToLatestMessage(true);
		inlineAppender.setWidth(600);
		inlineAppender.setHeight(200);

		testConsoleAppender(t, inlineAppender);
	});

	s.test("InPageAppender with separate console HTML file test", function(t) {
		var inPageAppender = new log4javascript.InPageAppender();
		inPageAppender.setInitiallyMinimized(false);
		inPageAppender.setNewestMessageAtTop(false);
		inPageAppender.setScrollToLatestMessage(true);
		inPageAppender.setUseDocumentWrite(false);
		inPageAppender.setWidth(600);
		inPageAppender.setHeight(200);

		testConsoleAppender(t, inPageAppender);
	});

	s.test("PopUpAppender test", function(t) {
		var popUpAppender = new log4javascript.PopUpAppender();
		popUpAppender.setFocusPopUp(true);
		popUpAppender.setUseOldPopUp(false);
		popUpAppender.setNewestMessageAtTop(false);
		popUpAppender.setScrollToLatestMessage(true);
		popUpAppender.setComplainAboutPopUpBlocking(false);
		popUpAppender.setWidth(600);
		popUpAppender.setHeight(200);

		testConsoleAppender(t, popUpAppender);
		
		
	});

	s.test("PopUpAppender with separate console HTML file test", function(t) {
		var popUpAppender = new log4javascript.PopUpAppender();
		popUpAppender.setFocusPopUp(true);
		popUpAppender.setUseOldPopUp(false);
		popUpAppender.setNewestMessageAtTop(false);
		popUpAppender.setScrollToLatestMessage(true);
		popUpAppender.setComplainAboutPopUpBlocking(false);
		popUpAppender.setUseDocumentWrite(false);
		popUpAppender.setWidth(600);
		popUpAppender.setHeight(200);

		testConsoleAppender(t, popUpAppender);
	});
});