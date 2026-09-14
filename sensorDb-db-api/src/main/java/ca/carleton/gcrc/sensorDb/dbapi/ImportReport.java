package ca.carleton.gcrc.sensorDb.dbapi;

import org.json.JSONObject;

public interface ImportReport {
	
	void setImportId(String importId);

	void setExpectedObservationCount(int expectedObservationCount);

	void insertedObservation(Observation observation);

	void skippedObservation(Observation observation);

	void inTransitObservation(Observation observation);

	void collisionObservation(Observation observation);
	
	void setError(Throwable err);
	
	JSONObject produceReport() throws Exception;
}
