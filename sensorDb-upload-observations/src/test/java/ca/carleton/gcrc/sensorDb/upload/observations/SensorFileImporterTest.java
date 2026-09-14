package ca.carleton.gcrc.sensorDb.upload.observations;

import java.io.File;
import java.util.Date;
import java.util.List;
import java.util.ArrayList;

import org.json.JSONObject;

import ca.carleton.gcrc.sensorDb.dbapi.Device;
import ca.carleton.gcrc.sensorDb.dbapi.DeviceLocation;
import ca.carleton.gcrc.sensorDb.dbapi.DeviceSensor;
import ca.carleton.gcrc.sensorDb.dbapi.ImportRecord;
import ca.carleton.gcrc.sensorDb.dbapi.Location;
import ca.carleton.gcrc.sensorDb.dbapi.LogRecord;
import ca.carleton.gcrc.sensorDb.dbapi.Observation;
import ca.carleton.gcrc.sensorDb.dbapi.ObservationReader;
import ca.carleton.gcrc.sensorDb.dbapi.Sensor;
import ca.carleton.gcrc.sensorDb.dbapi.memory.DbApiMemory;
import junit.framework.TestCase;

public class SensorFileImporterTest extends TestCase {

	public void testDeltaTimeLine() throws Exception {
		File testFile = TestSupport.findResourceFile("sensor.delta.txt");
		
		ConversionRequest conversionRequest = new ConversionRequest();
		conversionRequest.setFileToConvert(testFile);
		conversionRequest.setOriginalFileName(testFile.getName());
		conversionRequest.setImporterName("Me");
		conversionRequest.setInitialOffset(0);
		conversionRequest.setFinalOffset(100);
		
		DbApiMemory dbApi = new DbApiMemory();
		
		// Populate with appropriate devices and sensors
		Device device = null;
		Location location = null;
		List<Sensor> sensors = new ArrayList<Sensor>(2);
		{
			device = new Device();
			device.setSerialNumber("E509EC");
			device = dbApi.createDevice(device);
		}
		{
			Sensor sensor = new Sensor();
			sensor.setLabel("#1:oC");
			sensor = dbApi.createSensor(sensor);
			sensors.add(sensor);
		}
		{
			Sensor sensor = new Sensor();
			sensor.setLabel("#HK-Bat:V");
			sensor = dbApi.createSensor(sensor);
			sensors.add(sensor);
		}
		{
			location = new Location();
			location.setGeometry("POINT(0 0)");
			location.setRecordingObservations(true);
			location = dbApi.createLocation(location);
		}
		{
			DeviceLocation deviceLocation = new DeviceLocation();
			deviceLocation.setDeviceId( device.getId() );
			deviceLocation.setLocationId( location.getId() );
			deviceLocation.setTimestamp( DateUtils.parseUtcString("01.01.2016 01:00:00") );
			dbApi.createDeviceLocation(deviceLocation);
		}
		{
			DeviceSensor deviceSensor = new DeviceSensor();
			deviceSensor.setDeviceId(device.getId());
			deviceSensor.setSensorId(sensors.get(0).getId());
			deviceSensor.setTimestamp(DateUtils.parseUtcString("01.01.2000 01:00:00"));
			dbApi.createDeviceSensor(deviceSensor);
		}
		{
			DeviceSensor deviceSensor = new DeviceSensor();
			deviceSensor.setDeviceId(device.getId());
			deviceSensor.setSensorId(sensors.get(1).getId());
			deviceSensor.setTimestamp(DateUtils.parseUtcString("01.01.2000 01:00:00"));
			dbApi.createDeviceSensor(deviceSensor);
		}
		
		SensorFileImporter importer = new SensorFileImporter(dbApi);
		ImportRecord importRecord = importer.importFile(conversionRequest);
		
		// Now check that the delta time in the file took precedence over
		// the set offset. To do that, loop through all the observations
		// and capture the maximum time. It should be close to the last obervation
		// time plus the delta time
		ObservationReader obsReader = dbApi.getObservationsFromImportId( importRecord.getId() );
		Date maxTime = null;
		Observation observation = obsReader.read();
		while( null != observation ){
			if( null == maxTime ){
				maxTime = observation.getCorrectedTime();
			} else if( observation.getCorrectedTime().getTime() > maxTime.getTime() ){
				maxTime = observation.getCorrectedTime();
			}
			
			observation = obsReader.read();
		}
		obsReader.close();
		Date compareTime = DateUtils.parseUtcString("28.01.2016 16:45:00");
		if( compareTime.getTime() > maxTime.getTime() ){
			fail("Delta time specified in file was not respected");
		}
	}
	
	public void testRepeatedImportReportsCollisionAndSkipped() throws Exception {
		File testFile = TestSupport.findResourceFile("sensor.delta.txt");
		ConversionRequest conversionRequest = createConversionRequest(testFile);
		
		DbApiMemory dbApi = createConfiguredDbApi(true);
		SensorFileImporter importer = new SensorFileImporter(dbApi);
		
		ImportRecord firstImportRecord = importer.importFile(conversionRequest);
		int firstInsertedCount = countObservationsForImport(dbApi, firstImportRecord.getId());
		assertTrue(firstInsertedCount > 0);
		
		JSONObject firstReport = getImportReport(dbApi, firstImportRecord.getId());
		assertEquals(firstInsertedCount, firstReport.getInt("insertedCount"));
		assertEquals(0, firstReport.getInt("skippedCount"));
		assertEquals(0, firstReport.getInt("collisionCount"));
		assertEquals(0, firstReport.getInt("inTransitCount"));
		assertEquals(firstInsertedCount, firstReport.getInt("expectedCount"));
		assertEquals(0, firstReport.getInt("lostCount"));
		
		ImportRecord secondImportRecord = importer.importFile(conversionRequest);
		int secondInsertedCount = countObservationsForImport(dbApi, secondImportRecord.getId());
		assertEquals(0, secondInsertedCount);
		
		JSONObject secondReport = getImportReport(dbApi, secondImportRecord.getId());
		assertEquals(0, secondReport.getInt("insertedCount"));
		assertEquals(firstInsertedCount, secondReport.getInt("skippedCount"));
		assertEquals(firstInsertedCount, secondReport.getInt("collisionCount"));
		assertEquals(0, secondReport.getInt("inTransitCount"));
		assertEquals(firstInsertedCount, secondReport.getInt("expectedCount"));
		assertEquals(0, secondReport.getInt("lostCount"));
	}
	
	public void testInTransitObservationsRemainSkipped() throws Exception {
		File testFile = TestSupport.findResourceFile("sensor.delta.txt");
		ConversionRequest conversionRequest = createConversionRequest(testFile);
		
		DbApiMemory dbApi = createConfiguredDbApi(false);
		SensorFileImporter importer = new SensorFileImporter(dbApi);
		
		ImportRecord importRecord = importer.importFile(conversionRequest);
		int insertedCount = countObservationsForImport(dbApi, importRecord.getId());
		assertEquals(0, insertedCount);
		
		JSONObject report = getImportReport(dbApi, importRecord.getId());
		assertEquals(0, report.getInt("insertedCount"));
		assertEquals(0, report.getInt("collisionCount"));
		assertTrue(report.getInt("inTransitCount") > 0);
		assertEquals(report.getInt("inTransitCount"), report.getInt("skippedCount"));
		assertEquals(report.getInt("skippedCount"), report.getInt("expectedCount"));
		assertEquals(0, report.getInt("lostCount"));
	}
	
	private ConversionRequest createConversionRequest(File testFile) {
		ConversionRequest conversionRequest = new ConversionRequest();
		conversionRequest.setFileToConvert(testFile);
		conversionRequest.setOriginalFileName(testFile.getName());
		conversionRequest.setImporterName("Me");
		conversionRequest.setInitialOffset(0);
		conversionRequest.setFinalOffset(100);
		return conversionRequest;
	}
	
	private DbApiMemory createConfiguredDbApi(boolean recordingObservations) throws Exception {
		DbApiMemory dbApi = new DbApiMemory();
		
		// Populate with appropriate devices and sensors
		Device device = null;
		Location location = null;
		List<Sensor> sensors = new ArrayList<Sensor>(2);
		{
			device = new Device();
			device.setSerialNumber("E509EC");
			device = dbApi.createDevice(device);
		}
		{
			Sensor sensor = new Sensor();
			sensor.setLabel("#1:oC");
			sensor = dbApi.createSensor(sensor);
			sensors.add(sensor);
		}
		{
			Sensor sensor = new Sensor();
			sensor.setLabel("#HK-Bat:V");
			sensor = dbApi.createSensor(sensor);
			sensors.add(sensor);
		}
		{
			location = new Location();
			location.setGeometry("POINT(0 0)");
			location.setRecordingObservations(recordingObservations);
			location = dbApi.createLocation(location);
		}
		{
			DeviceLocation deviceLocation = new DeviceLocation();
			deviceLocation.setDeviceId( device.getId() );
			deviceLocation.setLocationId( location.getId() );
			deviceLocation.setTimestamp( DateUtils.parseUtcString("01.01.2016 01:00:00") );
			dbApi.createDeviceLocation(deviceLocation);
		}
		{
			DeviceSensor deviceSensor = new DeviceSensor();
			deviceSensor.setDeviceId(device.getId());
			deviceSensor.setSensorId(sensors.get(0).getId());
			deviceSensor.setTimestamp(DateUtils.parseUtcString("01.01.2000 01:00:00"));
			dbApi.createDeviceSensor(deviceSensor);
		}
		{
			DeviceSensor deviceSensor = new DeviceSensor();
			deviceSensor.setDeviceId(device.getId());
			deviceSensor.setSensorId(sensors.get(1).getId());
			deviceSensor.setTimestamp(DateUtils.parseUtcString("01.01.2000 01:00:00"));
			dbApi.createDeviceSensor(deviceSensor);
		}
		
		return dbApi;
	}
	
	private int countObservationsForImport(DbApiMemory dbApi, String importId) throws Exception {
		int count = 0;
		ObservationReader obsReader = dbApi.getObservationsFromImportId(importId);
		Observation observation = obsReader.read();
		while( null != observation ){
			++count;
			observation = obsReader.read();
		}
		obsReader.close();
		return count;
	}
	
	private JSONObject getImportReport(DbApiMemory dbApi, String importId) throws Exception {
		List<LogRecord> logRecords = dbApi.getLogRecords();
		for(LogRecord logRecord : logRecords){
			JSONObject log = logRecord.getLog();
			if( importId.equals(log.optString("importId")) ){
				return log;
			}
		}
		
		throw new Exception("Could not find import report for import id: "+importId);
	}
	
}
