package ca.carleton.gcrc.sensorDb.upload.observations;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.HashSet;
import java.util.Map;
import java.util.Vector;

import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ca.carleton.gcrc.sensorDb.dbapi.BulkObservationInsertResult;
import ca.carleton.gcrc.sensorDb.dbapi.DbAPI;
import ca.carleton.gcrc.sensorDb.dbapi.Device;
import ca.carleton.gcrc.sensorDb.dbapi.DeviceLocation;
import ca.carleton.gcrc.sensorDb.dbapi.DeviceSensor;
import ca.carleton.gcrc.sensorDb.dbapi.ImportRecord;
import ca.carleton.gcrc.sensorDb.dbapi.ImportReport;
import ca.carleton.gcrc.sensorDb.dbapi.ImportReportMemory;
import ca.carleton.gcrc.sensorDb.dbapi.Location;
import ca.carleton.gcrc.sensorDb.dbapi.LogRecord;
import ca.carleton.gcrc.sensorDb.dbapi.Observation;
import ca.carleton.gcrc.sensorDb.dbapi.Sensor;
import ca.carleton.gcrc.sensorDb.jdbc.DbConnection;

public class SensorFileImporter {

	final protected Logger logger = LoggerFactory.getLogger(this.getClass());
	private static final int OBSERVATION_INSERT_CHUNK_SIZE = 1000;

	//private DbConnection dbConn;
	private DbAPI dbAPI;
	
	public SensorFileImporter(DbConnection dbConn) throws Exception{
		//this.dbConn = dbConn;
		this.dbAPI = dbConn.getAPI();
		
		if( null == this.dbAPI ){
			throw new Exception("Unable to get dbAPI from database connection");
		}
	}
	
	public SensorFileImporter(DbAPI dbApi) throws Exception{
		this.dbAPI = dbApi;
		
		if( null == this.dbAPI ){
			throw new Exception("dbAPI must be provided");
		}
	}
	
	public ImportRecord importFile(ConversionRequest conversionRequest) throws Exception {
		if( null == conversionRequest ){
			throw new Exception("A conversion request must be provided");
		}

		File file = conversionRequest.getFileToConvert();
		String fileName = null;
		if( null != file ){
			fileName = file.getAbsolutePath();
		}
		
		JSONObject jsonParams = new JSONObject();
		jsonParams.put("initial_offset", conversionRequest.getInitialOffset());
		jsonParams.put("final_offset", conversionRequest.getFinalOffset());
		jsonParams.put("originalFileName", conversionRequest.getOriginalFileName());
		jsonParams.put("importerName", conversionRequest.getImporterName());
		jsonParams.put("notes", conversionRequest.getNotes());

		// Record this file into the database
		ImportRecord importRecord = new ImportRecord();
		importRecord.setImportTime(new Date()); // now
		importRecord.setFileName(file.getName());
		importRecord.setImportParameters(jsonParams);
		
		importRecord = dbAPI.createImportRecord(importRecord);
		
		logger.info("Import UUID: "+importRecord.getId());
		
		// Import the file
		FileInputStream fis = null;
		InputStreamReader isr = null;
		try {
			fis = new FileInputStream(file);
			isr = new InputStreamReader(fis,"UTF-8");
			
			importFile(
					isr
					,importRecord.getId()
					,conversionRequest.getInitialOffset()
					,conversionRequest.getFinalOffset()
					);

		} catch (Exception e) {
			throw new Exception("Error while importing observation file "+fileName,e);
		
		} finally {
			if( null != isr ){
				try {
					isr.close();
				} catch(Exception e) {
					// Ignore
				}
			}
			if( null != fis ){
				try {
					fis.close();
				} catch(Exception e) {
					// Ignore
				}
			}
		}
		
		return importRecord;
	}

	public void importFile(
			Reader reader
			,String importUUID
			,int initialOffset
			,int finalOffset
			) throws Exception {

		ImportReport report = new ImportReportMemory();

		Device device = null;
		try {
			SensorFileReader obsReader = new SensorFileReader(reader);
			
			String deviceSerialNumber = obsReader.getDeviceSerialNumber();
			
			device = dbAPI.getDeviceFromSerialNumber(deviceSerialNumber);
			String device_id = device.getId();
			List<Sensor> sensors = dbAPI.getSensorsFromDeviceId(device_id);
			
			// Make a list of sensors based on label
			Set<String> sensorLabelSet = new HashSet<String>();
			for(Sensor sensor : sensors){
				sensorLabelSet.add(sensor.getLabel());
			}

			// Check that sensors were found for all parsed columns
			for(SampleColumn column : obsReader.getColumns()){
				if( column.isValue() ){
					if( !(sensorLabelSet.contains( column.getName() )) ){
						throw new Exception("Sensor with label ("+column.getName()+") not found for device: "+device);
					}
				}
			}

			report.setImportId(importUUID);
			
			// Get all observations that should be saved
			List<Sample> samples = new Vector<Sample>();
			Date firstTime = null;
			Date lastTime = null;
			{
				Sample sample = obsReader.read();
				while( null != sample ){
					samples.add(sample);
					
					Date currentTime = sample.getTime();
					if( null == firstTime ){
						firstTime = currentTime;
					} else if( currentTime.getTime() < firstTime.getTime() ){
						firstTime = currentTime;
					}
					if( null == lastTime ){
						lastTime = currentTime;
					} else if( currentTime.getTime() > lastTime.getTime() ){
						lastTime = currentTime;
					}

					sample = obsReader.read();
				}
			}
			
			// Compute a time corrector
			TimeCorrector timeCorrector = new TimeCorrector();
			timeCorrector.setStartTime(firstTime);
			timeCorrector.setEndTime(lastTime);
			timeCorrector.setInitialOffsetInSec(initialOffset);
			timeCorrector.setFinalOffsetInSec(finalOffset);
			
			// If a delta time was provided in the file, then this should
			// override the final offset
			Integer deltaTimeInSecs = obsReader.getDeltaTimeInSecs();
			if( null != deltaTimeInSecs ){
				timeCorrector.setFinalOffsetInSec(deltaTimeInSecs.intValue());
			}
			
			// Get all the device locations for this device
			List<DeviceLocation> deviceLocations = dbAPI.getDeviceLocationsFromDeviceId(device_id);
			List<Location> locations = dbAPI.getLocationsFromDeviceLocations(deviceLocations);
			DeviceLocator deviceLocator = new DeviceLocator(deviceLocations, locations);

			// Get all sensors for this device
			List<DeviceSensor> deviceSensors = dbAPI.getDeviceSensorsFromDeviceId(device_id);
			List<Sensor> allSensors = dbAPI.getSensorsFromDeviceSensors(deviceSensors);
			DeviceSensorHistory deviceSensorHistory = new DeviceSensorHistory(deviceSensors, allSensors);

			// Start saving observations
			List<Observation> observationChunk = new Vector<Observation>();
			for( Sample sample : samples ){
				String sensor_label = sample.getColumn().getName();
				
				try {
					ObservationAndLocation observationAndLocation = createObservation(
						importUUID, 
						device_id, 
						deviceSensorHistory, 
						sensor_label,
						sample, 
						timeCorrector, 
						deviceLocator
					);
					
					if( observationAndLocation.isRecordingObservation() ){
						observationChunk.add(observationAndLocation.getObservation());
						
						if( observationChunk.size() >= OBSERVATION_INSERT_CHUNK_SIZE ){
							flushObservationChunk(observationChunk, report);
						}
						
					} else {
						Observation observation = observationAndLocation.getObservation();
						report.inTransitObservation(observation);
						report.skippedObservation(observation);
					}
					
				} catch (Exception e) {
					logger.error("Error on sample: "+sample);
					logger.error("Sample found on line: "+sample.getLineNumber());
					logger.error("Sample line: "+sample.getLine());
					throw new Exception("Error inserting sample: "+sample,e);
				}
			}
			
			flushObservationChunk(observationChunk, report);

		} catch (Exception e) {
			
			report.setError(e);
			throw new Exception("Error during import process for device: "+device,e);

		} finally {
			try {
				saveImportReport(report);
			} catch(Exception e2) {
				// Ignore
				logger.error("Unable to save log",e2);
			}
		}
	}

	private ObservationAndLocation createObservation(
		String importUUID,
		String device_id,
		DeviceSensorHistory deviceSensorHistory,
		String sensor_label,
		Sample sample, 
		TimeCorrector timeCorrector,
		DeviceLocator deviceLocator
		) throws Exception {
	
	Sensor sensor = null;
	// insert into observations (device_id,sensor_id,location) values ('123','456',ST_GeomFromEWKT('srid=4326;POINT(0 0)'));
	try {
		Date loggerTime = sample.getTime();
		Date correctedTime = timeCorrector.correctTime(loggerTime);
		
		Location location = deviceLocator.getLocationFromTimestamp(correctedTime);
		if( null == location ){
			throw new Exception("Can not find location of device (id="+device_id+") for time "+correctedTime.toString());
		}

		sensor = deviceSensorHistory.getSensorAtTimestamp(sensor_label, correctedTime);
		if ( null == sensor ){
			throw new Exception("Can not find sensor with label '" + sensor_label +
						        "' corresponding to device (id="+device_id+") at time "+correctedTime.toString());
		}

		String geometry = location.getGeometry();
		
		Observation observation = new Observation();
		observation.setDeviceId( device_id );
		observation.setSensorId( sensor.getId() );
		observation.setImportId( importUUID );
		observation.setImportKey( sample.computeImportKey() );
		observation.setObservationType( sensor.getTypeOfMeasurement() );
		observation.setUnitOfMeasure( sensor.getUnitOfMeasurement() );
		observation.setAccuracy( sensor.getAccuracy() );
		observation.setPrecision( sensor.getPrecision() );
		observation.setNumericValue( sample.getValue() );
		observation.setTextValue( sample.getText() );
		observation.setLoggedTime( loggerTime );
		observation.setCorrectedTime( correctedTime );
		observation.setLocation( geometry );
		observation.setElevation( location.getElevation() );
		observation.setMinHeight( sensor.getHeightInMetres() );
		observation.setMaxHeight( sensor.getHeightInMetres() );
		
		ObservationAndLocation observationAndLocation = new ObservationAndLocation();
		observationAndLocation.setObservation(observation);
		observationAndLocation.setRecordingObservation(location.isRecordingObservations());
		return observationAndLocation;

	} catch (Exception e) {
		if (null == sensor){
			throw new Exception("Error inserting observation for sensor (label="+sensor_label+") to database", e);
		} else{
			throw new Exception("Error inserting observation for sensor (id="+sensor.getId()+") to database", e);
		}
		
	}
}

	private void flushObservationChunk(List<Observation> observationChunk, ImportReport report) throws Exception {
		if( observationChunk.size() < 1 ){
			return;
		}
		
		BulkObservationInsertResult insertResult = dbAPI.createObservationsIfAbsent(observationChunk);
		List<BulkObservationInsertResult.ItemResult> itemResults = insertResult.getItemResults();
		for(BulkObservationInsertResult.ItemResult itemResult : itemResults){
			Observation observation = itemResult.getObservation();
			if( itemResult.isInserted() ){
				report.insertedObservation(observation);
			}
			if( itemResult.isCollision() ){
				report.collisionObservation(observation);
				report.skippedObservation(observation);
			}
		}
		
		observationChunk.clear();
	}

	private static class ObservationAndLocation {
		private Observation observation;
		private boolean recordingObservation;
		
		public Observation getObservation() {
			return observation;
		}
		public void setObservation(Observation observation) {
			this.observation = observation;
		}
		
		public boolean isRecordingObservation() {
			return recordingObservation;
		}
		public void setRecordingObservation(boolean recordingObservation) {
			this.recordingObservation = recordingObservation;
		}
	}

	private void saveImportReport(ImportReport report) throws Exception {
		try {
			JSONObject jsonLog = report.produceReport();
			
			LogRecord logRecord = new LogRecord();
			logRecord.setTimestamp( new Date() ); // now
			logRecord.setLog(jsonLog);
			
			dbAPI.createLogRecord(logRecord);
			
		} catch (Exception e) {
			throw new Exception("Error inserting log to database", e);
		}
	}
}
