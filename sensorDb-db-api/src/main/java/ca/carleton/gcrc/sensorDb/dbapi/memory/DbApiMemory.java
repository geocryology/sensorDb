package ca.carleton.gcrc.sensorDb.dbapi.memory;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

import ca.carleton.gcrc.sensorDb.dbapi.BulkObservationInsertResult;
import ca.carleton.gcrc.sensorDb.dbapi.DbAPI;
import ca.carleton.gcrc.sensorDb.dbapi.Device;
import ca.carleton.gcrc.sensorDb.dbapi.DeviceLocation;
import ca.carleton.gcrc.sensorDb.dbapi.DeviceSensor;
import ca.carleton.gcrc.sensorDb.dbapi.DeviceSensorProfile;
import ca.carleton.gcrc.sensorDb.dbapi.ImportRecord;
import ca.carleton.gcrc.sensorDb.dbapi.Location;
import ca.carleton.gcrc.sensorDb.dbapi.LogRecord;
import ca.carleton.gcrc.sensorDb.dbapi.Observation;
import ca.carleton.gcrc.sensorDb.dbapi.ObservationReader;
import ca.carleton.gcrc.sensorDb.dbapi.Sensor;

/**
 * Implements a DbAPI in memory. Should only be used for testing.
 *
 */
public class DbApiMemory implements DbAPI {
	
	static private int g_nextUuidInt = 1;

	static synchronized private String getNextUUID(){
		String uuid = String.format("%08x", g_nextUuidInt);
		++g_nextUuidInt;
		return uuid;
	}
	
	private Map<String,DeviceSensorProfile> deviceSensorProfilesById = new HashMap<String,DeviceSensorProfile>();
	private Map<String,Sensor> sensorsById = new HashMap<String,Sensor>();
	private Map<String,Device> devicesById = new HashMap<String,Device>();
	private Map<String,DeviceLocation> deviceLocationsById = new HashMap<String,DeviceLocation>();
	private Map<String,DeviceSensor> deviceSensorsById = new HashMap<String,DeviceSensor>();
	private Map<String,Location> locationsById = new HashMap<String,Location>();
	private Map<String,Observation> observationsById = new HashMap<String,Observation>();
	private Map<String,ImportRecord> importRecordsById = new HashMap<String,ImportRecord>();
	private Map<String,LogRecord> logRecordsById = new HashMap<String,LogRecord>();

	@Override
	public Collection<DeviceSensorProfile> getDeviceSensorProfilesFromManufacturerDeviceName(
			String manufacturerDeviceName
			) throws Exception {

		List<DeviceSensorProfile> profiles = new Vector<DeviceSensorProfile>();
		
		for(DeviceSensorProfile profile : deviceSensorProfilesById.values()){
			if( manufacturerDeviceName.equals(profile.getManufacturerDeviceName()) ){
				profiles.add(profile);
			}
		}
		
		return profiles;
	}

	@Override
	public Collection<DeviceSensorProfile> getDeviceSensorProfiles() throws Exception {
		List<DeviceSensorProfile> profiles = new Vector<DeviceSensorProfile>( deviceSensorProfilesById.values() );
		
		return profiles;
	}

	@Override
	public Sensor createSensor(Sensor sensor) throws Exception {
		Sensor dbSensor = new Sensor();
		
		dbSensor.setId( getNextUUID() );
		dbSensor.setAccuracy( sensor.getAccuracy() );
		dbSensor.setHeightInMetres( sensor.getHeightInMetres() );
		dbSensor.setLabel( sensor.getLabel() );
		dbSensor.setPrecision( sensor.getPrecision() );
		dbSensor.setSerialNumber( sensor.getSerialNumber() );
		dbSensor.setTypeOfMeasurement( sensor.getTypeOfMeasurement() );
		dbSensor.setUnitOfMeasurement( sensor.getUnitOfMeasurement() );
		
		sensorsById.put(dbSensor.getId(), dbSensor);
		
		return dbSensor;
	}

	@Override
	public Collection<Sensor> getSensors() throws Exception {
		List<Sensor> sensors = new Vector<Sensor>( sensorsById.values() );
		
		return sensors;
	}

	@Override
	public Sensor getSensorFromSensorId(String sensorId) throws Exception {
		return sensorsById.get(sensorId);
	}

	@Override
	public List<Sensor> getSensorsFromDeviceId(String device_id) throws Exception {
		Set<Sensor> uniqueSensors = new HashSet<Sensor>();
		List<Sensor> sensors = new Vector<Sensor>();
		List<DeviceSensor> deviceSensors = new Vector<DeviceSensor>();
		
		for (DeviceSensor deviceSensor : deviceSensorsById.values()){
			if ( device_id.equals( deviceSensor.getDeviceId() ) ){
				deviceSensors.add(deviceSensor);
			}
		}

		for (DeviceSensor deviceSensor : deviceSensors){ // TODO: make this better than O(n^2)
			for (Sensor sensor : sensorsById.values()){
				if ( deviceSensor.getSensorId().equals(sensor.getId() ) ) {
					uniqueSensors.add(sensor);
				}
			}
		}
		
		sensors.addAll(uniqueSensors);
		return sensors;
	}

	@Override
	public Device createDevice(Device device) throws Exception {
		Device dbDevice = new Device();
		
		dbDevice.setId( getNextUUID() );
		dbDevice.setAccessCode( device.getAccessCode() );
		dbDevice.setAcquiredOn( device.getAcquiredOn() );
		dbDevice.setDeviceType( device.getDeviceType() );
		dbDevice.setManufacturer( device.getManufacturer() );
		dbDevice.setManufacturerDeviceName( device.getManufacturerDeviceName() );
		dbDevice.setNotes( device.getNotes() );
		dbDevice.setSerialNumber( device.getSerialNumber() );
		
		devicesById.put(dbDevice.getId(), dbDevice);
		
		return dbDevice;
	}

	@Override
	public Collection<Device> getDevices() throws Exception {
		List<Device> devices = new Vector<Device>( devicesById.values() );
		
		return devices;
	}

	@Override
	public Device getDeviceFromId(String id) throws Exception {
		return devicesById.get(id);
	}

	@Override
	public Device getDeviceFromSerialNumber(String serialNumber) throws Exception {
		for(Device device : devicesById.values()){
			if( serialNumber.equals(device.getSerialNumber()) ){
				return device;
			}
		}
		
		return null;
	}

	@Override
	public DeviceLocation createDeviceLocation(DeviceLocation deviceLocation) throws Exception {
		DeviceLocation dbDeviceLocation = new DeviceLocation();
		
		dbDeviceLocation.setId( getNextUUID() );
		dbDeviceLocation.setDeviceId( deviceLocation.getDeviceId() );
		dbDeviceLocation.setLocationId( deviceLocation.getLocationId() );
		dbDeviceLocation.setNotes( deviceLocation.getNotes() );
		dbDeviceLocation.setTimestamp( deviceLocation.getTimestamp() );
		
		deviceLocationsById.put(dbDeviceLocation.getId(), dbDeviceLocation);
		
		return dbDeviceLocation;
	}

	@Override
	public List<DeviceLocation> getDeviceLocations() throws Exception {
		List<DeviceLocation> deviceLocations = new Vector<DeviceLocation>( deviceLocationsById.values() );
		
		return deviceLocations;
	}

	@Override
	public List<DeviceLocation> getDeviceLocationsFromDeviceId(String device_id) throws Exception {
		List<DeviceLocation> deviceLocations = new Vector<DeviceLocation>();
		
		for(DeviceLocation deviceLocation : deviceLocationsById.values()){
			if( device_id.equals(deviceLocation.getDeviceId()) ){
				deviceLocations.add(deviceLocation);
			}
		}
		
		return deviceLocations;
	}

	@Override
	public List<DeviceSensor> getDeviceSensors() throws Exception {
		List<DeviceSensor> deviceSensors = new Vector<DeviceSensor>( deviceSensorsById.values() );
		
		return deviceSensors;
	}

	@Override
	public List<DeviceSensor> getDeviceSensorsFromDeviceId(String device_id) throws Exception {
		List<DeviceSensor> deviceSensors = new Vector<DeviceSensor>();
		
		for(DeviceSensor deviceSensor : deviceSensorsById.values()){
			if( device_id.equals(deviceSensor.getDeviceId()) ){
				deviceSensors.add(deviceSensor);
			}
		}
		
		return deviceSensors;
	}

	@Override
	public List<Location> getLocationsFromDeviceLocations(List<DeviceLocation> deviceLocations) throws Exception {
		List<Location> locations = new Vector<Location>();
		
		// Accumulate all location ids
		Set<String> locationIds = new HashSet<String>();
		for(DeviceLocation deviceLocation : deviceLocations){
			String locationId = deviceLocation.getLocationId();
			if( null != locationId ){
				locationIds.add(locationId);
			}
		}
		
		for(String locationId : locationIds){
			Location location = getLocationFromLocationId(locationId);
			locations.add(location);
		}
		
		return locations;
	}

	@Override
	public List<Sensor> getSensorsFromDeviceSensors(List<DeviceSensor> deviceSensors) throws Exception {
		List<Sensor> sensors = new Vector<Sensor>();
		
		// Accumulate all sensor ids
		Set<String> sensorIds = new HashSet<String>();
		for(DeviceSensor deviceSensor : deviceSensors){
			String sensorId = deviceSensor.getSensorId();
			if( null != sensorId ){
				sensorIds.add(sensorId);
			}
		}
		
		for(String sensorId : sensorIds){
			Sensor sensor = getSensorFromSensorId(sensorId);
			sensors.add(sensor);
		}
		
		return sensors;
	}

	@Override
	public Location createLocation(Location location) throws Exception {
		Location dbLocation = new Location();
		
		dbLocation.setId( getNextUUID() );
		dbLocation.setAccuracy( location.getAccuracy() );
		dbLocation.setComment( location.getComment() );
		dbLocation.setElevation( location.getElevation() );
		dbLocation.setGeometry( location.getGeometry() );
		dbLocation.setName( location.getName() );
		dbLocation.setRecordingObservations( location.isRecordingObservations() );
		
		locationsById.put(dbLocation.getId(), dbLocation);
		
		return dbLocation;
	}

	@Override
	public Location getLocationFromLocationId(String locationId) throws Exception {
		return locationsById.get(locationId);
	}

	@Override
	public Collection<Location> getLocations() throws Exception {
		List<Location> locations = new Vector<Location>( locationsById.values() );
		
		return locations;
	}

	@Override
	public DeviceSensor createDeviceSensor(DeviceSensor deviceSensor) throws Exception {
		DeviceSensor dbDeviceSensor = new DeviceSensor();
		
		dbDeviceSensor.setId( getNextUUID() );
		dbDeviceSensor.setDeviceId( deviceSensor.getDeviceId() );
		dbDeviceSensor.setSensorId( deviceSensor.getSensorId() );
		dbDeviceSensor.setNotes( deviceSensor.getNotes() );
		dbDeviceSensor.setTimestamp( deviceSensor.getTimestamp() );
		
		deviceSensorsById.put(dbDeviceSensor.getId(), dbDeviceSensor);
		
		return dbDeviceSensor;
	}

	@Override
	public Observation createObservation(Observation observation) throws Exception {
		Observation dbObservation = new Observation();

		dbObservation.setId( getNextUUID() );
		dbObservation.setAccuracy( observation.getAccuracy() );
		dbObservation.setCorrectedTime( observation.getCorrectedTime() );
		dbObservation.setDeviceId( observation.getDeviceId() );
		dbObservation.setElevation( observation.getElevation() );
		dbObservation.setImportId( observation.getImportId() );
		dbObservation.setImportKey( observation.getImportKey() );
		dbObservation.setLocation( observation.getLocation() );
		dbObservation.setLoggedTime( observation.getLoggedTime() );
		dbObservation.setMaxHeight( observation.getMaxHeight() );
		dbObservation.setMinHeight( observation.getMinHeight() );
		dbObservation.setNumericValue( observation.getNumericValue() );
		dbObservation.setObservationType( observation.getObservationType() );
		dbObservation.setPrecision( observation.getPrecision() );
		dbObservation.setSensorId( observation.getSensorId() );
		dbObservation.setTextValue( observation.getTextValue() );
		dbObservation.setUnitOfMeasure( observation.getUnitOfMeasure() );
		
		observationsById.put(dbObservation.getId(), dbObservation);
		
		return dbObservation;
	}

	@Override
	public BulkObservationInsertResult createObservationsIfAbsent(List<Observation> observations) throws Exception {
		BulkObservationInsertResult result = new BulkObservationInsertResult();
		
		if( null == observations ){
			throw new Exception("Attempting to create a null list of observations");
		}
		
		for(Observation observation : observations){
			boolean collision = false;
			String importKey = observation.getImportKey();
			if( null != importKey ){
				Observation collidingObservation = getObservationFromImportKey(importKey);
				if( null != collidingObservation ){
					collision = true;
				}
			}
			
			if( collision ){
				result.addItemResult(observation, false, true);
			} else {
				Observation insertedObservation = createObservation(observation);
				result.addItemResult(insertedObservation, true, false);
			}
		}
		
		return result;
	}

	@Override
	public ObservationReader getObservationsFromImportId(String importId) throws Exception {
		List<Observation> observations = new Vector<Observation>();
		
		for(Observation observation : observationsById.values()){
			if( importId.equals(observation.getImportId()) ){
				observations.add(observation);
			}
		}
		
		ObservationReaderMemory obsReader = new ObservationReaderMemory(observations);
		
		return obsReader;
	}

	@Override
	public Observation getObservationFromImportKey(String importKey) throws Exception {
		for(Observation observation : observationsById.values()){
			if( importKey.equals(observation.getImportKey()) ){
				return observation;
			}
		}
		return null;
	}

	@Override
	public ImportRecord createImportRecord(ImportRecord importRecord) throws Exception {
		ImportRecord dbImportRecord = new ImportRecord();

		dbImportRecord.setId( getNextUUID() );
		dbImportRecord.setFileName( importRecord.getFileName() );
		dbImportRecord.setImportParameters( importRecord.getImportParameters() );
		dbImportRecord.setImportTime( importRecord.getImportTime() );
		
		importRecordsById.put(dbImportRecord.getId(), dbImportRecord);
		
		return dbImportRecord;
	}

	@Override
	public List<ImportRecord> getImportRecords() throws Exception {
		List<ImportRecord> importRecords = new Vector<ImportRecord>( importRecordsById.values() );
		
		return importRecords;
	}

	@Override
	public ImportRecord getImportRecordFromImportId(String importId) throws Exception {
		return importRecordsById.get(importId);
	}

	@Override
	public LogRecord createLogRecord(LogRecord logRecord) throws Exception {
		LogRecord dbLogRecord = new LogRecord();
		
		dbLogRecord.setId( getNextUUID() );
		dbLogRecord.setLog( logRecord.getLog() );
		dbLogRecord.setTimestamp( logRecord.getTimestamp() );
		
		logRecordsById.put(dbLogRecord.getId(), dbLogRecord);
		
		return dbLogRecord;
	}

	@Override
	public List<LogRecord> getLogRecords() throws Exception {
		List<LogRecord> logRecords = new Vector<LogRecord>( logRecordsById.values() );
		return logRecords;
	}

	@Override
	public LogRecord getLogRecordFromId(String logId) throws Exception {
		return logRecordsById.get(logId);
	}

}
