;(function($,$n2,$s){
"use strict";

// Localization
var _loc = function(str,args){ return $n2.loc(str,'sensordb',args); };

function httpJsonError(XMLHttpRequest, defaultStr) {
	// Need JSON
	if( !JSON || typeof(JSON.parse) !== 'function' ) {
		return defaultStr;
	};
	
	// Need a response text
	var text = XMLHttpRequest.responseText;
	if( !text ) return defaultStr;
	
	// Parse
	var error = JSON.parse(text);
	if( !error ) return defaultStr;
	if( !error.reason ) return defaultStr;
	
	return error.reason;
};

//============================================
var Database = $n2.Class({
	
	dbUrl: null,
	
	initialize: function(opts_){
		var opts = $n2.extend({
			dbUrl: null
		},opts_);
		
		this.dbUrl = opts.dbUrl;
	},
	
	getLocations: function(opts_){
		var opts = $n2.extend({
			onSuccess: function(locations){}
			,onError: function(err){}
		},opts_);
		
		$.ajax({
			url: this.dbUrl + 'getLocations'
			,type: 'get'
			,async: true
			,dataType: 'json'
			,success: function(res) {
				if( res.ok ) {
					opts.onSuccess(res.locations);
				} else {
					opts.onError('Malformed response');
				};
			}
			,error: function(XMLHttpRequest, textStatus, errorThrown) {
				var errStr = httpJsonError(XMLHttpRequest, textStatus);
				opts.onError('Error obtaining locations: '+errStr);
			}
		});
	},
	
	getDeviceTypes: function(opts_){
		var opts = $n2.extend({
			onSuccess: function(deviceTypes){}
			,onError: function(err){}
		},opts_);
		
		$.ajax({
			url: this.dbUrl + 'getDeviceTypes'
			,type: 'get'
			,async: true
			,dataType: 'json'
			,success: function(res) {
				if( res.ok ) {
					opts.onSuccess(res.deviceTypes);
				} else {
					opts.onError('Malformed response');
				};
			}
			,error: function(XMLHttpRequest, textStatus, errorThrown) {
				var errStr = httpJsonError(XMLHttpRequest, textStatus);
				opts.onError('Error obtaining device types: '+errStr);
			}
		});
	},
	
	getDevices: function(opts_){
		var opts = $n2.extend({
			onSuccess: function(devices){}
			,onError: function(err){}
		},opts_);
		
		$.ajax({
			url: this.dbUrl + 'getDevices'
			,type: 'get'
			,async: true
			,dataType: 'json'
			,success: function(res) {
				if( res.ok ) {
					opts.onSuccess(res.devices);
				} else {
					opts.onError('Malformed response');
				};
			}
			,error: function(XMLHttpRequest, textStatus, errorThrown) {
				var errStr = httpJsonError(XMLHttpRequest, textStatus);
				opts.onError('Error obtaining devices: '+errStr);
			}
		});
	},
	
	getListOfLogEntries: function(opts_){
		var opts = $n2.extend({
			onSuccess: function(logEntries){}
			,onError: function(err){}
		},opts_);
		
		$.ajax({
			url: this.dbUrl + 'getListOfLogEntries'
			,type: 'get'
			,async: true
			,dataType: 'json'
			,success: function(res) {
				if( res.ok ) {
					opts.onSuccess(res.logEntries);
				} else {
					opts.onError('Malformed response');
				};
			}
			,error: function(XMLHttpRequest, textStatus, errorThrown) {
				var errStr = httpJsonError(XMLHttpRequest, textStatus);
				opts.onError('Error obtaining list of log entries: '+errStr);
			}
		});
	},
	
	getLog: function(opts_){
		var opts = $n2.extend({
			id: null
			,onSuccess: function(log){}
			,onError: function(err){}
		},opts_);
		
		$.ajax({
			url: this.dbUrl + 'getLog'
			,type: 'get'
			,async: true
			,dataType: 'json'
			,data: {
				id: opts.id
			}
			,success: function(res) {
				if( res.ok && res.logs && res.logs.length === 1 ) {
					opts.onSuccess(res.logs[0]);
				} else {
					opts.onError('Malformed response');
				};
			}
			,error: function(XMLHttpRequest, textStatus, errorThrown) {
				var errStr = httpJsonError(XMLHttpRequest, textStatus);
				opts.onError('Error obtaining log '+opts.id+': '+errStr);
			}
		});
	},
	
	getImportRecords: function(opts_){
		var opts = $n2.extend({
			id: null
			,onSuccess: function(importRecords){}
			,onError: function(err){}
		},opts_);
		
		$.ajax({
			url: this.dbUrl + 'getImportRecords'
			,type: 'get'
			,async: true
			,dataType: 'json'
			,data: {
				id: opts.id
			}
			,success: function(res) {
				if( res.ok ) {
					var importRecords = [];
				
					if( res.importRecords ){
						for(var i=0,e=res.importRecords.length; i<e; ++i){
							var importRecord = res.importRecords[i];
							importRecords.push(importRecord);
						};
					};
					
					opts.onSuccess(importRecords);

				} else {
					opts.onError('Malformed response');
				};
			}
			,error: function(XMLHttpRequest, textStatus, errorThrown) {
				var errStr = httpJsonError(XMLHttpRequest, textStatus);
				opts.onError('Error obtaining import records: '+errStr);
			}
		});
	},
	
	getImportFileUrl: function(id){
		return this.dbUrl + 'getImportFile/' + id;
	},
	
	getObservationsByImportIdUrl: function(id){
		return this.dbUrl + 'getObservationsByImportId/' + id;
	}
});	

// ============================================
// Export
$s.db = {
	Database: Database
};

})(jQuery,nunaliit2,sensorDb);