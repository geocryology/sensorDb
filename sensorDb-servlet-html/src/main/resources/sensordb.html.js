;(function($,$n2,$s){

var HtmlFixer = $n2.Class({
	
	dbService: null,
	
	initialize: function(opts_){
		var opts = $n2.extend({
			dbService: null
		},opts_);
		
		this.dbService = opts.dbService;
	},
	
	fixHtml: function($elem){
		var _this = this;
		
		if( !$elem ){
			$elem = $('body');
		};
		
		var $set = $elem.find('*').addBack();
		
		// Locations
		$set.filter('select.sdb_getLocations').each(function(){
			var $select = $(this);
			_this._getLocationOptions($select);
		});
		
		// Device Types
		$set.filter('select.sdb_getDeviceTypes').each(function(){
			var $select = $(this);
			_this._getDeviceTypeOptions($select);
		});
		
		// Devices
		$set.filter('select.sdb_getDevices').each(function(){
			var $select = $(this);
			_this._getDeviceOptions($select);
		});
		
		// List log entries
		$set.filter('.sdb_listLogEntries').each(function(){
			var $elem = $(this);
			_this._listLogEntries($elem);
		});
		
		// Import Records
		$set.filter('.sdb_insertImportRecords').each(function(){
			var $elem = $(this);
			_this._insertImportRecords($elem);
		});
		
		$set.filter('fieldset.sdb_createLocation_select').each(function(){
			var $elem = $(this);
			_this._fixCreateLocationHtml($elem);
		});
	},
	
	_getLocationOptions: function($select){
		this.dbService.getLocations({
			onSuccess: function(locations){
				$select.empty();
				
				locations.sort(function(o1,o2){
					if(o1.name < o2.name) return -1;
					if(o1.name > o2.name) return 1;
					return 0;
				});
				
				for(var i=0,e=locations.length; i<e; ++i){
					var location = locations[i];
					var name = location.name;
					var id = location.id;
					
					$('<option>')
						.text(name)
						.attr('value',id)
						.appendTo($select);
				};
			}
		});
	},
	
	_getDeviceTypeOptions: function($select){
		this.dbService.getDeviceTypes({
			onSuccess: function(deviceTypes){
				$select.empty();
				
				deviceTypes.sort(function(o1,o2){
					if(o1.device_type < o2.device_type) return -1;
					if(o1.device_type > o2.device_type) return 1;
					return 0;
				});
				
				for(var i=0,e=deviceTypes.length; i<e; ++i){
					var deviceType = deviceTypes[i];
					var manufacturer_device_name = deviceType.manufacturer_device_name;
					
					$('<option>')
						.text(manufacturer_device_name)
						.attr('value',manufacturer_device_name)
						.appendTo($select);
				};
			}
		});
	},
	
	_getDeviceOptions: function($select){
		this.dbService.getDevices({
			onSuccess: function(devices){
				$select.empty();
				
				devices.sort(function(o1,o2){
					if(o1.serial_number < o2.serial_number) return -1;
					if(o1.serial_number > o2.serial_number) return 1;
					return 0;
				});
				
				for(var i=0,e=devices.length; i<e; ++i){
					var device = devices[i];
					var serialNumber = device.serial_number;
					var id = device.id;
					
					$('<option>')
						.text(serialNumber)
						.attr('value',id)
						.appendTo($select);
				};
			}
		});
	},
	
	_listLogEntries: function($elem){
		var _this = this;
		
		$elem.empty();
		
		var elemId = $n2.getUniqueId();
		
		$('<button>')
			.text('Get Logs')
			.appendTo($elem)
			.click(function(){
				_this.dbService.getListOfLogEntries({
					onSuccess: function(logEntries){
						var $div = $('#'+elemId).empty();

						logEntries.sort(function(o1,o2){
							if(o1.timestamp > o2.timestamp) return -1;
							if(o1.timestamp < o2.timestamp) return 1;
							return 0;
						});

						for(var i=0,e=logEntries.length; i<e; ++i){
							var logEntry = logEntries[i];
							var ts = logEntry.timestamp;
							var d = new Date(ts);
							var tsText = '' + d;
							var id = logEntry.id;
							
							var $line = $('<div>')
								.appendTo($div);
							
							var $a = $('<a>')
								.attr('href','#')
								.attr('sdb_id',id)
								.text(tsText)
								.appendTo($div)
								.click(showLog);
						};
					}
				});
			});
		
		$('<div>')
			.attr('id',elemId)
			.appendTo($elem);
		
		function showLog(){
			var $a = $(this);
			var id = $a.attr('sdb_id');

			_this.dbService.getLog({
				id: id
				,onSuccess: function(log){
					var $div = $('#'+elemId)
						.empty();
					
					if( typeof log.log === 'string' ){
						$('<pre>')
							.text(log.log)
							.appendTo($div);
					} else if( typeof log.log === 'object' ) {
						var txt = JSON.stringify(log.log, null, 3);
						$('<pre>')
							.text(txt)
							.appendTo($div);
					};
				}
			});

			return false;
		};
	},
	
	_insertImportRecords: function($elem){
		var _this = this;
		
		$elem.empty()
			.addClass('sdb_importRecords');
		
		var elemId = $n2.utils.getElementIdentifier($elem);
		
		this.dbService.getImportRecords({
			onSuccess: function(importRecords){
				var $div = $('#'+elemId).empty();
				
				importRecords.sort(function(o1,o2){
					if(o1.importTime < o2.importTime) return -1;
					if(o1.importTime > o2.importTime) return 1;
					return 0;
				});
				
				for(var i=0,e=importRecords.length; i<e; ++i){
					var importRecord = importRecords[i];
					var ts = importRecord.importTime;
					var d = new Date(ts);
					var tsText = '' + d;
					var id = importRecord.id;
					var fileName = importRecord.fileName;
					var importParameters = importRecord.importParameters;
					
					var importerName = undefined;
					var initialOffset = undefined;
					var finalOffset = undefined;
					var originalFileName = undefined;
					var notes = undefined;
					
					if( importParameters ){
						importerName = importParameters.importerName;
						initialOffset = importParameters.initialOffset;
						finalOffset = importParameters.finalOffset;
						originalFileName = importParameters.originalFileName;
						notes = importParameters.notes;
					};
					
					var $line = $('<div>')
						.addClass('sdb_importRecord')
						.appendTo($div);

					var $importId = $('<div>')
						.addClass('sdb_importRecord_id')
						.appendTo($line);

					$('<a>')
						.attr('href',_this.dbService.getObservationsByImportIdUrl(id)+'/'+id+'.csv')
						.text(id)
						.appendTo($importId);

					$('<div>')
						.addClass('sdb_importRecord_date')
						.text(tsText)
						.appendTo($line);

					$('<div>')
						.addClass('sdb_importRecord_fileName')
						.text(fileName)
						.appendTo($line);

					$('<div>')
						.addClass('sdb_importRecord_importerName')
						.text(importerName)
						.appendTo($line);

					$('<div>')
						.addClass('sdb_importRecord_initialOffset')
						.text(initialOffset)
						.appendTo($line);

					$('<div>')
						.addClass('sdb_importRecord_finalOffset')
						.text(finalOffset)
						.appendTo($line);

					var $originalDiv = $('<div>')
						.addClass('sdb_importRecord_originalFileName')
						.appendTo($line);
					
					$('<a>')
						.attr('href',_this.dbService.getImportFileUrl(id)+'/'+originalFileName)
						.text(originalFileName)
						.appendTo($originalDiv);

					$('<div>')
						.addClass('sdb_importRecord_notes')
						.text(notes)
						.appendTo($line);
				};
			}
		});
	},
	
	_fixCreateLocationHtml: function($elem){
		
		adjustClasses($elem);
		
		$elem.find('input[name="geomType"]')
			.change(function(){
				var $fields = $('.sdb_createLocation_select');
				$fields.each(function(){
					var $field = $(this);
					adjustClasses($field);
				});
			});
		
		function adjustClasses($elem){
			var valueMap = {};
			
			var $radioButtons = $elem.find('input[name="geomType"]');
			$radioButtons.each(function(){
				var $radioButton = $(this);
				var value = $radioButton.val();
				var isChecked = $radioButton.is(':checked');
				valueMap[value] = isChecked;
			});
			
			for(var value in valueMap){
				// Adjust class at the top element
				var isChecked = valueMap[value];
				var className = 'sdb_createLocation_select_'+value;
				if( isChecked ) {
					$elem.addClass(className);
				} else {
					$elem.removeClass(className);
				};
				
				// Adjust required attributes
				var requiredSelector = '.sdb_createLocation_required_'+value;
				$(requiredSelector).each(function(){
					if( isChecked ){
						$(this).attr('required','required');
					} else {
						$(this).removeAttr('required');
					};
				});
			};

			$n2.log('valueMap',valueMap);
		};
	}
});	

// ============================================
// Export
$s.html = {
	HtmlFixer: HtmlFixer
};

})(jQuery,nunaliit2,sensorDb);