package ca.carleton.gcrc.sensorDb.dbapi;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.TimeZone;

import org.json.JSONObject;

public class ImportReportMemory implements ImportReport {

	private String importId;
	private int expectedObservationCount = 0;
	private int insertedObservations = 0;
	private int skippedObservations = 0;
	private int inTransitObservations = 0;
	private int collisionObservations = 0;
	private DateFormat dateFormatter;
	private Map<String,Integer> observedTextFields = new HashMap<String,Integer>();
	private Throwable reportedError = null;
	
	public ImportReportMemory() {
		dateFormatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss z");
	    dateFormatter.setTimeZone(TimeZone.getTimeZone("UTC"));	
	}
	
	public String getImportId() {
		return importId;
	}
	
	@Override
	public void setImportId(String importId){
		this.importId = importId;
	}

	@Override
	public void setExpectedObservationCount(int expectedObservationCount) {
		this.expectedObservationCount = expectedObservationCount;
	}

	@Override
	public void insertedObservation(Observation observation) {
		++insertedObservations;
		
		if( null != observation.getTextValue() ){
			// Accumulate the text fields and count them
			String text = observation.getTextValue();
			if( false == observedTextFields.containsKey(text) ){
				observedTextFields.put(text,0);
			}
			
			int count = observedTextFields.get(text);
			++count;
			observedTextFields.put(text,count);
		}
	}

	@Override
	public void skippedObservation(Observation observation) {
		++skippedObservations;
	}

	@Override
	public void inTransitObservation(Observation observation) {
		++inTransitObservations;
	}

	@Override
	public void collisionObservation(Observation observation) {
		++collisionObservations;
	}

	@Override
	public void setError(Throwable err) {
		this.reportedError = err;
	}

	@Override
	public JSONObject produceReport() throws Exception {
		JSONObject jsonReport = new JSONObject();
		
		jsonReport.put("type", "import");
		jsonReport.put("importId", importId);
		jsonReport.put("expectedCount", expectedObservationCount);
		jsonReport.put("insertedCount", insertedObservations);
		jsonReport.put("skippedCount", skippedObservations);
		jsonReport.put("inTransitCount", inTransitObservations);
		jsonReport.put("collisionCount", collisionObservations);

		int accountedCount = insertedObservations + skippedObservations;
		int lostCount = expectedObservationCount - accountedCount;
		jsonReport.put("accountedCount", accountedCount);
		jsonReport.put("lostCount", lostCount);
		
		JSONObject jsonProblems = new JSONObject();
		int problemCount = 0;
		for(String errorText : observedTextFields.keySet()){
			int count = observedTextFields.get(errorText);
			problemCount += count;
			jsonProblems.put(errorText, count);
		}
		if( problemCount > 0 ){
			jsonReport.put("problemCount", problemCount);
			jsonReport.put("problems", jsonProblems);
		}
		
		if( null != reportedError ){
			JSONObject jsonErr = errorToJSON(reportedError);
			jsonReport.put("error", jsonErr);
		}
		
		return jsonReport;
	}

	public int getInsertedObservations() {
		return insertedObservations;
	}

	public int getSkippedObservations() {
		return skippedObservations;
	}
	
	public int getInTransitObservationCount() {
		return inTransitObservations;
	}
	
	public int getCollisionObservationCount() {
		return collisionObservations;
	}

	public int getExpectedObservationCount() {
		return expectedObservationCount;
	}

	public int getLostObservationCount() {
		return expectedObservationCount - (insertedObservations + skippedObservations);
	}
	
	private JSONObject errorToJSON(Throwable t){
		JSONObject errorObj = new JSONObject();
		errorObj.put("error", t.getMessage());
		
		int limit = 15;
		Throwable cause = t;
		JSONObject causeObj = errorObj;
		while( null != cause && limit > 0 ){
			--limit;
			cause = cause.getCause();
			
			if( null != cause ){
				JSONObject causeErr = new JSONObject();
				causeErr.put("error", cause.getMessage());
				causeObj.put("cause", causeErr);
				
				causeObj = causeErr;
			}
		}
		
		return errorObj;
	}
}
