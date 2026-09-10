package ca.carleton.gcrc.sensorDb.dbapi;

import java.util.Collection;
import java.util.List;

public interface DbAPI {

	Collection<DeviceSensorProfile> getDeviceSensorProfilesFromManufacturerDeviceName(String manufacturerDeviceName) throws Exception;

	Collection<DeviceSensorProfile> getDeviceSensorProfiles() throws Exception;

	Sensor createSensor(Sensor sensor) throws Exception;

	Collection<Sensor> getSensors() throws Exception;

	Sensor getSensorFromSensorId(String sensor_id) throws Exception;

	List<Sensor> getSensorsFromDeviceId(String device_id) throws Exception;

	Device createDevice(Device device) throws Exception;

	Collection<Device> getDevices() throws Exception;

	Device getDeviceFromId(String id) throws Exception;

	Device getDeviceFromSerialNumber(String serialNumber) throws Exception;

	DeviceLocation createDeviceLocation(DeviceLocation deviceLocation) throws Exception;
	
	List<DeviceLocation> getDeviceLocations() throws Exception;
	
	List<DeviceLocation> getDeviceLocationsFromDeviceId(String device_id) throws Exception;

	List<DeviceSensor> getDeviceSensors() throws Exception;
	
	List<DeviceSensor> getDeviceSensorsFromDeviceId(String device_id) throws Exception;

	List<Location> getLocationsFromDeviceLocations(List<DeviceLocation> deviceLocations) throws Exception;

	List<Sensor> getSensorsFromDeviceSensors(List<DeviceSensor> deviceSensors) throws Exception;

	Location createLocation(Location location) throws Exception;
	
	Location getLocationFromLocationId(String locationId) throws Exception;

	Collection<Location> getLocations() throws Exception;

	DeviceSensor createDeviceSensor(DeviceSensor deviceSensor) throws Exception;

	Observation createObservation(Observation observation) throws Exception;

	BulkObservationInsertResult createObservationsIfAbsent(List<Observation> observations) throws Exception;

	ObservationReader getObservationsFromImportId(String importId) throws Exception;

	Observation getObservationFromImportKey(String importKey) throws Exception;

	ImportRecord createImportRecord(ImportRecord importRecord) throws Exception;

	List<ImportRecord> getImportRecords() throws Exception;

	ImportRecord getImportRecordFromImportId(String importId) throws Exception;

	LogRecord createLogRecord(LogRecord logRecord) throws Exception;

	List<LogRecord> getLogRecords() throws Exception;

	LogRecord getLogRecordFromId(String logId) throws Exception;
}
