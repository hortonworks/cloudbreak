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

var log4javascript_stub = (function() {
	var log4javascript;

	function ff() {
		return function() {};
	}
	function copy(obj, props) {
		for (var i in props) {
			obj[i] = props[i];
		}
	}
	var f = ff();

	// Loggers
	var Logger = ff();
	copy(Logger.prototype, {
		addChild: f,
		getEffectiveAppenders: f,
		invalidateAppenderCache: f,
		getAdditivity: f,
		setAdditivity: f,
		addAppender: f,
		removeAppender: f,
		removeAllAppenders: f,
		log: f,
		setLevel: f,
		getLevel: f,
		getEffectiveLevel: f,
		trace: f,
		debug: f,
		info: f,
		warn: f,
		error: f,
		fatal: f,
		isEnabledFor: f,
		isTraceEnabled: f,
		isDebugEnabled: f,
		isInfoEnabled: f,
		isWarnEnabled: f,
		isErrorEnabled: f,
		isFatalEnabled: f,
		callAppenders: f,
		group: f,
		groupEnd: f,
		time: f,
		timeEnd: f,
		assert: f,
		parent: new Logger()
	});

	var getLogger = function() {
		return new Logger();
	};

	function EventSupport() {}

	copy(EventSupport.prototype, {
		setEventTypes: f,
		addEventListener: f,
		removeEventListener: f,
		dispatchEvent: f,
		eventTypes: [],
		eventListeners: {}
	});

	function Log4JavaScript() {}
	Log4JavaScript.prototype = new EventSupport();
	log4javascript = new Log4JavaScript();

	log4javascript = {
		isStub: true,
		version: "1.4.6",
		edition: "log4javascript",
        setDocumentReady: f,
		setEventTypes: f,
		addEventListener: f,
		removeEventListener: f,
		dispatchEvent: f,
		eventTypes: [],
		eventListeners: {},
		logLog: {
			setQuietMode: f,
			setAlertAllErrors: f,
			debug: f,
			displayDebug: f,
			warn: f,
			error: f
		},
		handleError: f,
		setEnabled: f,
		isEnabled: f,
		setTimeStampsInMilliseconds: f,
		isTimeStampsInMilliseconds: f,
		evalInScope: f,
		setShowStackTraces: f,
		getLogger: getLogger,
		getDefaultLogger: getLogger,
		getNullLogger: getLogger,
		getRootLogger: getLogger,
		resetConfiguration: f,
		Level: ff(),
		LoggingEvent: ff(),
		Layout: ff(),
		Appender: ff()
	};

	// LoggingEvents
	log4javascript.LoggingEvent.prototype = {
		getThrowableStrRep: f,
		getCombinedMessages: f
	};

	// Levels
	log4javascript.Level.prototype = {
		toString: f,
		equals: f,
		isGreaterOrEqual: f
	};
	var level = new log4javascript.Level();
	copy(log4javascript.Level, {
		ALL: level,
		TRACE: level,
		DEBUG: level,
		INFO: level,
		WARN: level,
		ERROR: level,
		FATAL: level,
		OFF: level
	});

	// Layouts
	log4javascript.Layout.prototype = {
		defaults: {},
		format: f,
		ignoresThrowable: f,
		getContentType: f,
		allowBatching: f,
		getDataValues: f,
		setKeys: f,
		setCustomField: f,
		hasCustomFields: f,
		setTimeStampsInMilliseconds: f,
		isTimeStampsInMilliseconds: f,
		getTimeStampValue: f,
		toString: f
	};

	// PatternLayout related
	log4javascript.SimpleDateFormat = ff();
	log4javascript.SimpleDateFormat.prototype = {
		setMinimalDaysInFirstWeek: f,
		getMinimalDaysInFirstWeek: f,
		format: f
	};

	// PatternLayout
	log4javascript.PatternLayout = ff();
	log4javascript.PatternLayout.prototype = new log4javascript.Layout();

	// Appenders
	log4javascript.Appender = ff();
	log4javascript.Appender.prototype = new EventSupport();

	copy(log4javascript.Appender.prototype, {
		layout: new log4javascript.PatternLayout(),
		threshold: log4javascript.Level.ALL,
		loggers: [],
		doAppend: f,
		append: f,
		setLayout: f,
		getLayout: f,
		setThreshold: f,
		getThreshold: f,
		setAddedToLogger: f,
		setRemovedFromLogger: f,
		group: f,
		groupEnd: f,
		toString: f
	});
	// SimpleLayout
	log4javascript.SimpleLayout = ff();
	log4javascript.SimpleLayout.prototype = new log4javascript.Layout();
	// NullLayout
	log4javascript.NullLayout = ff();
	log4javascript.NullLayout.prototype = new log4javascript.Layout();
	// ZmlLayout
	log4javascript.XmlLayout = ff();
	log4javascript.XmlLayout.prototype = new log4javascript.Layout();
	copy(log4javascript.XmlLayout.prototype, {
		escapeCdata: f,
		isCombinedMessages: f
	});
	// JsonLayout
	log4javascript.JsonLayout = ff();
	log4javascript.JsonLayout.prototype = new log4javascript.Layout();
	copy(log4javascript.JsonLayout.prototype, {
		isReadable: f,
		isCombinedMessages: f
	});
	// HttpPostDataLayout 
	log4javascript.HttpPostDataLayout = ff();
	log4javascript.HttpPostDataLayout.prototype = new log4javascript.Layout();
	// PatternLayout
	log4javascript.PatternLayout = ff();
	log4javascript.PatternLayout.prototype = new log4javascript.Layout();
	// AlertAppender
	log4javascript.AlertAppender = ff();
	log4javascript.AlertAppender.prototype = new log4javascript.Appender();
	// BrowserConsoleAppender
	log4javascript.BrowserConsoleAppender = ff();
	log4javascript.BrowserConsoleAppender.prototype = new log4javascript.Appender();
	// AjaxAppender
	log4javascript.AjaxAppender = ff();
	log4javascript.AjaxAppender.prototype = new log4javascript.Appender();
	copy(log4javascript.AjaxAppender.prototype, {
		getSessionId: f,
		setSessionId: f,
		isTimed: f,
		setTimed: f,
		getTimerInterval: f,
		setTimerInterval: f,
		isWaitForResponse: f,
		setWaitForResponse: f,
		getBatchSize: f,
		setBatchSize: f,
		isSendAllOnUnload: f,
		setSendAllOnUnload: f,
		setRequestSuccessCallback: f,
		setFailCallback: f,
		getPostVarName: f,
		setPostVarName: f,
		sendAll: f,
		sendAllRemaining: f,
		defaults: {
			requestSuccessCallback: null,
			failCallback: null
		}
	});
	// ConsoleAppender
	function ConsoleAppender() {}
	ConsoleAppender.prototype = new log4javascript.Appender();
	copy(ConsoleAppender.prototype, {
		create: f,
		isNewestMessageAtTop: f,
		setNewestMessageAtTop: f,
		isScrollToLatestMessage: f,
		setScrollToLatestMessage: f,
		getWidth: f,
		setWidth: f,
		getHeight: f,
		setHeight: f,
		getMaxMessages: f,
		setMaxMessages: f,
		isShowCommandLine: f,
		setShowCommandLine: f,
		isShowHideButton: f,
		setShowHideButton: f,
		isShowCloseButton: f,
		setShowCloseButton: f,
		getCommandLineObjectExpansionDepth: f,
		setCommandLineObjectExpansionDepth: f,
		isInitiallyMinimized: f,
		setInitiallyMinimized: f,
		isUseDocumentWrite: f,
		setUseDocumentWrite: f,
		group: f,
		groupEnd: f,
		clear: f,
		focus: f,
		focusCommandLine: f,
		focusSearch: f,
		getCommandWindow: f,
		setCommandWindow: f,
		executeLastCommand: f,
		getCommandLayout: f,
		setCommandLayout: f,
		evalCommandAndAppend: f,
		addCommandLineFunction: f,
		storeCommandHistory: f,
		unload: f
	});

	ConsoleAppender.addGlobalCommandLineFunction = f;

	// InPageAppender
	log4javascript.InPageAppender = ff();
	log4javascript.InPageAppender.prototype = new ConsoleAppender();
	copy(log4javascript.InPageAppender.prototype, {
		addCssProperty: f,
		hide: f,
		show: f,
		isVisible: f,
		close: f,
		defaults: {
			layout: new log4javascript.PatternLayout(),
			maxMessages: null
		}
	});
	log4javascript.InlineAppender = log4javascript.InPageAppender;

	// PopUpAppender
	log4javascript.PopUpAppender = ff();
	log4javascript.PopUpAppender.prototype = new ConsoleAppender();
	copy(log4javascript.PopUpAppender.prototype, {
		isUseOldPopUp: f,
		setUseOldPopUp: f,
		isComplainAboutPopUpBlocking: f,
		setComplainAboutPopUpBlocking: f,
		isFocusPopUp: f,
		setFocusPopUp: f,
		isReopenWhenClosed: f,
		setReopenWhenClosed: f,
		close: f,
		hide: f,
		show: f,
		defaults: {
			layout: new log4javascript.PatternLayout(),
			maxMessages: null
		}
	});
	return log4javascript;
})();
if (typeof window.log4javascript == "undefined") {
	var log4javascript = log4javascript_stub;
}
