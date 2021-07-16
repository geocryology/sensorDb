;(function($,$n2,$s){

function GetConfiguration(opts_){
	var opts = $n2.extend({
		rootPath: './'
	},opts_);
	
	var config = {};
	
	config.dbService = new $s.db.Database({
		dbUrl: opts.rootPath + 'db/'
	});
	
	config.htmlService = new $s.html.HtmlFixer({
		dbService: config.dbService
	});
	
	return config;
};

// ============================================
// Export
$s.config = {
	GetConfiguration: GetConfiguration
};

})(jQuery,nunaliit2,sensorDb);