package ca.carleton.gcrc.sensorDb.jdbc;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.Vector;

import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ca.carleton.gcrc.sensorDb.dbapi.DbAPI;
import ca.carleton.gcrc.sensorDb.dbapi.Device;
import ca.carleton.gcrc.sensorDb.dbapi.DeviceLocation;
import ca.carleton.gcrc.sensorDb.dbapi.DeviceSensorProfile;
import ca.carleton.gcrc.sensorDb.dbapi.ImportRecord;
import ca.carleton.gcrc.sensorDb.dbapi.Location;
import ca.carleton.gcrc.sensorDb.dbapi.LogRecord;
import ca.carleton.gcrc.sensorDb.dbapi.Observation;
import ca.carleton.gcrc.sensorDb.dbapi.ObservationReader;
import ca.carleton.gcrc.sensorDb.dbapi.Sensor;

public class DbApiJdbc implements DbAPI {
	final protected Logger logger = LoggerFactory.getLogger(this.getClass());

	private DbConnection dbConn;
	
	public DbApiJdbc(DbConnection connection){
		this.dbConn = connection;
	}

	@Override
	public Collection<DeviceSensorProfile> getDeviceSensorProfilesFromManufacturerDeviceName(String manufacturerDeviceName) throws Exception {
		List<DeviceSensorProfile> profiles = new Vector<DeviceSensorProfile>();

		try {
			PreparedStatement pstmt = dbConn.getConnection().prepareStatement(
				"SELECT id,device_type,manufacturer,manufacturer_device_name,sensor_label,"
				+ "sensor_type_of_measurement,sensor_unit_of_measurement,sensor_accuracy,"
				+ "sensor_precision,sensor_height_in_metres"
				+ " FROM device_sensor_profiles"
				+ " WHERE manufacturer_device_name=?"
			);
			
			pstmt.setString(1, manufacturerDeviceName);
			
			ResultSet resultSet = pstmt.executeQuery();
			
			while( resultSet.next() ){
				DeviceSensorProfile sensorProfile = new DeviceSensorProfile();
				
				sensorProfile.setId( resultSet.getString(1) );
				sensorProfile.setDeviceType( resultSet.getString(2) );
				sensorProfile.setManufacturer( resultSet.getString(3) );
				sensorProfile.setManufacturerDeviceName( resultSet.getString(4) );
				sensorProfile.setSensorLabel( resultSet.getString(5) );
				sensorProfile.setSensorTypeOfMeasurement( resultSet.getString(6) );
				sensorProfile.setSensorUnitOfMeasurement( resultSet.getString(7) );
				sensorProfile.setSensorAccuracy( resultSet.getDouble(8) );
				sensorProfile.setSensorPrecision( resultSet.getDouble(9) );
				sensorProfile.setSensorHeightInMetres( resultSet.getDouble(10) );

				profiles.add(sensorProfile);
			}

			resultSet.close();
			
		} catch (Exception e) {
			throw new Exception("Error retrieving device sensor profiles ("+manufacturerDeviceName+") from database", e);
		}
		
		return profiles;
	}

	@Override
	public Collection<DeviceSensorProfile> getDeviceSensorProfiles() throws Exception {
		List<DeviceSensorProfile> profiles = new Vector<DeviceSensorProfile>();

		try {
			PreparedStatement pstmt = dbConn.getConnection().prepareStatement(
				"SELECT id,device_type,manufacturer,manufacturer_device_name,sensor_label,"
				+ "sensor_type_of_measurement,sensor_unit_of_measurement,sensor_accuracy,"
				+ "sensor_precision,sensor_height_in_metres"
				+ " FROM device_sensor_profiles"
			);
			
			ResultSet resultSet = pstmt.executeQuery();
			
			while( resultSet.next() ){
				DeviceSensorProfile sensorProfile = new DeviceSensorProfile();
				
				sensorProfile.setId( resultSet.getString(1) );
				sensorProfile.setDeviceType( resultSet.getString(2) );
				sensorProfile.setManufacturer( resultSet.getString(3) );
				sensorProfile.setManufacturerDeviceName( resultSet.getString(4) );
				sensorProfile.setSensorLabel( resultSet.getString(5) );
				sensorProfile.setSensorTypeOfMeasurement( resultSet.getString(6) );
				sensorProfile.setSensorUnitOfMeasurement( resultSet.getString(7) );
				sensorProfile.setSensorAccuracy( resultSet.getDouble(8) );
				sensorProfile.setSensorPrecision( resultSet.getDouble(9) );
				sensorProfile.setSensorHeightInMetres( resultSet.getDouble(10) );
					
				profiles.add(sensorProfile);
			}

			resultSet.close();
			
		} catch (Exception e) {
			throw new Exception("Error retrieving all device sensor profiles from database", e);
		}

		return profiles;
	}

	@Override
	public Sensor createSensor(Sensor sensor) throws Exception {

		Sensor result = null;
		
		if( null != sensor.getId() ){
			throw new Exception("Id should not be set when creating a sensor");
		}
		
		try {
			PreparedStatement pstmt = dbConn.getConnection().prepareStatement(
					"INSERT INTO sensors"
					+" (device_id,label,type_of_measurement,unit_of_measurement,accuracy,"
					+ "precision,height_in_metres)"
					+" VALUES (?,?,?,?,?,?,?)"
					+" RETURNING id,device_id,label,type_of_measurement,unit_of_measurement,accuracy,"
					+ "precision,height_in_metres"
				);
				
			pstmt.setObject(1, UUID.fromString(sensor.getDeviceId()));
			pstmt.setString(2, sensor.getLabel());
			pstmt.setString(3, sensor.getTypeOfMeasurement());
			pstmt.setString(4, sensor.getUnitOfMeasurement());
			pstmt.setDouble(5, sensor.getAccuracy());
			pstmt.setDouble(6, sensor.getPrecision());
			pstmt.setDouble(7, sensor.getHeightInMetres());

			ResultSet resultSet = pstmt.executeQuery();
			
			resultSet.next();
			
			result = new Sensor();
			result.setId( resultSet.getString(1) );
			result.setDeviceId( resultSet.getString(2) );
			result.setLabel( resultSet.getString(3) );
			result.setTypeOfMeasurement( resultSet.getString(4) );
			result.setUnitOfMeasurement( resultSet.getString(5) );
			result.setAccuracy( resultSet.getDouble(6) );
			result.setPrecision( resultSet.getDouble(7) );
			result.setHeightInMetres( resultSet.getDouble(8) );

			resultSet.close();
				
		} catch(Exception e) {
			throw new Exception("Error while creating sensor ("+sensor.getLabel()+") for device ("+sensor.getDeviceId()+")",e);
		}
		
		return result;
	}

	@Override
	public Collection<Sensor> getSensors() throws Exception {
		List<Sensor> sensors = new Vector<Sensor>();
		
		try {
			PreparedStatement pstmt = dbConn.getConnection().prepareStatement(
				"SELECT id,device_id,label,type_of_measurement,unit_of_measurement,"
				+ "accuracy,precision,height_in_metres,serial_number"
				+ " FROM sensors"
			);
			
			ResultSet resultSet = pstmt.executeQuery();
			
			while( resultSet.next() ){
				String id = resultSet.getString(1);
				String deviceId = resultSet.getString(2);
				String label = resultSet.getString(3);
				String typeOfMeasurement = resultSet.getString(4);
				String unitOfMeasurement = resultSet.getString(5);
				double accuracy = resultSet.getDouble(6);
				double precision = resultSet.getDouble(7);
				double heightInMetres = resultSet.getDouble(8);
				String serialNumber = resultSet.getString(9);
				
				Sensor sensor = new Sensor();
				sensor.setId(id);
				sensor.setDeviceId(deviceId);
				sensor.setLabel(label);
				sensor.setTypeOfMeasurement(typeOfMeasurement);
				sensor.setUnitOfMeasurement(unitOfMeasurement);
				sensor.setAccuracy(accuracy);
				sensor.setPrecision(precision);
				sensor.setHeightInMetres(heightInMetres);
				sensor.setSerialNumber(serialNumber);
				
				sensors.add(sensor);
			}
			
			resultSet.close();
			
		} catch (Exception e) {
			throw new Exception("Error retrieving sensors from database", e);
		}

		return sensors;
	}

	@Override
	public List<Sensor> getSensorsFromDeviceId(String device_id) throws Exception {
		List<Sensor> sensors = new Vector<Sensor>();
		
		try {
			PreparedStatement pstmt = dbConn.getConnection().prepareStatement(
				"SELECT id,device_id,label,type_of_measurement,unit_of_measurement,"
				+ "accuracy,precision,height_in_metres,serial_number"
				+ " FROM sensors"
				+ " WHERE device_id=?"
			);
			
			pstmt.setObject(1, UUID.fromString(device_id));
			
			ResultSet resultSet = pstmt.executeQuery();
			
			while( resultSet.next() ){
				String id = resultSet.getString(1);
				String deviceId = resultSet.getString(2);
				String label = resultSet.getString(3);
				String typeOfMeasurement = resultSet.getString(4);
				String unitOfMeasurement = resultSet.getString(5);
				double accuracy = resultSet.getDouble(6);
				double precision = resultSet.getDouble(7);
				double heightInMetres = resultSet.getDouble(8);
				String serialNumber = resultSet.getString(9);
				
				Sensor sensor = new Sensor();
				sensor.setId(id);
				sensor.setDeviceId(deviceId);
				sensor.setLabel(label);
				sensor.setTypeOfMeasurement(typeOfMeasurement);
				sensor.setUnitOfMeasurement(unitOfMeasurement);
				sensor.setAccuracy(accuracy);
				sensor.setPrecision(precision);
				sensor.setHeightInMetres(heightInMetres);
				sensor.setSerialNumber(serialNumber);
				
				sensors.add(sensor);
			}
			
			resultSet.close();
			
		} catch (Exception e) {
			throw new Exception("Error retrieving sensors for device (id="+device_id+") from database", e);
		}

		return sensors;
	}

	@Override
	public Device createDevice(Device device) throws Exception {

		Device result = null;
		
		if( null != device.getId() ){
			throw new Exception("Id should not be set when creating a device");
		}
		
		try {
			Collection<DeviceSensorProfile> deviceSensorProfiles = 
				getDeviceSensorProfilesFromManufacturerDeviceName(
					device.getManufacturerDeviceName()
				);
			if( deviceSensorProfiles.size() < 1 ){
				throw new Exception("Can not find device sensor profiles for manufacturer name: "+device.getManufacturerDeviceName());
			}
			DeviceSensorProfile firstSensorProfile = null;
			for(DeviceSensorProfile sensorProfile : deviceSensorProfiles){
				firstSensorProfile = sensorProfile;
				break;
			}
			
			PreparedStatement pstmt = dbConn.getConnection().prepareStatement(
				"INSERT INTO devices"
				+" (serial_number,access_code,device_type,manufacturer,manufacturer_device_name,acquired_on,notes)"
				+" VALUES (?,?,?,?,?,?,?)"
				+" RETURNING id,serial_number,access_code,device_type,manufacturer,manufacturer_device_name,acquired_on,notes"
			);
			
			pstmt.setString(1, device.getSerialNumber());
            pstmt.setString(2, device.getAccessCode());
			pstmt.setString(3, firstSensorProfile.getDeviceType());
			pstmt.setString(4, firstSensorProfile.getManufacturer());
			pstmt.setString(5, firstSensorProfile.getManufacturerDeviceName());
			pstmt.setTimestamp(6, new Timestamp(device.getAcquiredOn().getTime()));
			pstmt.setString(7, device.getNotes());

			ResultSet resultSet = pstmt.executeQuery();
			
			resultSet.next();
			
			result = new Device();
			result.setId( resultSet.getString(1) );
			result.setSerialNumber( resultSet.getString(2) );
			result.setAccessCode( resultSet.getString(3) );
			result.setDeviceType( resultSet.getString(4) );
			result.setManufacturer( resultSet.getString(5) );
			result.setManufacturerDeviceName( resultSet.getString(6) );
			result.setAcquiredOn( resultSet.getTimestamp(7) );
			result.setNotes( resultSet.getString(8) );

			resultSet.close();
				
			// Create sensors for this device...
			for(DeviceSensorProfile sensorProfile : deviceSensorProfiles){
				Sensor sensor = new Sensor();
				sensor.setDeviceId(result.getId());
				sensor.setLabel( sensorProfile.getSensorLabel() );
				sensor.setAccuracy( sensorProfile.getSensorAccuracy() );
				sensor.setHeightInMetres( sensorProfile.getSensorHeightInMetres() );
				sensor.setPrecision( sensorProfile.getSensorPrecision() );
				sensor.setTypeOfMeasurement( sensorProfile.getSensorTypeOfMeasurement() );
				sensor.setUnitOfMeasurement( sensorProfile.getSensorUnitOfMeasurement() );

				createSensor(sensor);
			}
			
		} catch (Exception e) {
			throw new Exception("Error inserting device ("+device.getSerialNumber()+") into database", e);
		}
		
		return result;
	}

	@Override
	public Collection<Device> getDevices() throws Exception {
		List<Device> devices = new Vector<Device>();
		
		try {
			PreparedStatement pstmt = dbConn.getConnection().prepareStatement(
				"SELECT id,serial_number,access_code,device_type,manufacturer,"
				+ "manufacturer_device_name,acquired_on,notes"
				+ " FROM devices"
			);
			
			ResultSet resultSet = pstmt.executeQuery();
			
			while( resultSet.next() ){
				Device device = new Device();
				device.setId( resultSet.getString(1) );
				device.setSerialNumber( resultSet.getString(2) );
				device.setAccessCode( resultSet.getString(3) );
				device.setDeviceType( resultSet.getString(4) );
				device.setManufacturer( resultSet.getString(5) );
				device.setManufacturerDeviceName( resultSet.getString(6) );
				device.setAcquiredOn( resultSet.getTimestamp(7) );
				device.setNotes( resultSet.getString(8) );
				
				devices.add(device);
			}
			
			resultSet.close();
			
		} catch (Exception e) {
			throw new Exception("Error retrieving devices from database", e);
		}

		return devices;
	}

	@Override
	public Device getDeviceFromId(String id) throws Exception {
		Device device = null;
		
		try {
			PreparedStatement pstmt = dbConn.getConnection().prepareStatement(
				"SELECT id,serial_number,access_code,device_type,manufacturer,"
				+ "manufacturer_device_name,acquired_on,notes"
				+ " FROM devices"
				+ " WHERE id=?"
			);
			
			pstmt.setObject(1, UUID.fromString(id));
			
			ResultSet resultSet = pstmt.executeQuery();
			
			while( resultSet.next() ){
				if( null != device ){
					resultSet.close();
					throw new Exception("More than one device with id: "+id);
				}
				
				device = new Device();
				device.setId( resultSet.getString(1) );
				device.setSerialNumber( resultSet.getString(2) );
				device.setAccessCode( resultSet.getString(3) );
				device.setDeviceType( resultSet.getString(4) );
				device.setManufacturer( resultSet.getString(5) );
				device.setManufacturerDeviceName( resultSet.getString(6) );
				device.setAcquiredOn( resultSet.getTimestamp(7) );
				device.setNotes( resultSet.getString(8) );
			}
			
			resultSet.close();
			
			if( null == device ){
				throw new Exception("Can not find device with id: "+id);
			}
			
		} catch (Exception e) {
			throw new Exception("Error retrieving device (id="+id+") from database", e);
		}

		return device;
	}

	@Override
	public Device getDeviceFromSerialNumber(String serialNumber) throws Exception {
		Device device = null;
		
		try {
			PreparedStatement pstmt = dbConn.getConnection().prepareStatement(
				"SELECT id,serial_number,access_code,device_type,manufacturer,"
				+ "manufacturer_device_name,acquired_on,notes"
				+ " FROM devices"
				+ " WHERE serial_number=?"
			);
			
			pstmt.setString(1, serialNumber);
			
			ResultSet resultSet = pstmt.executeQuery();
			
			while( resultSet.next() ){
				if( null != device ){
					resultSet.close();
					throw new Exception("More than one device with serial number: "+serialNumber);
				}
				
				device = new Device();
				device.setId( resultSet.getString(1) );
				device.setSerialNumber( resultSet.getString(2) );
				device.setAccessCode( resultSet.getString(3) );
				device.setDeviceType( resultSet.getString(4) );
				device.setManufacturer( resultSet.getString(5) );
				device.setManufacturerDeviceName( resultSet.getString(6) );
				device.setAcquiredOn( resultSet.getTimestamp(7) );
				device.setNotes( resultSet.getString(8) );
			}
			
			resultSet.close();
			
			if( null == device ){
				throw new Exception("Can not find device with serial number: "+serialNumber);
			}
			
		} catch (Exception e) {
			throw new Exception("Error retrieving device (sn="+serialNumber+") from database", e);
		}

		return device;
	}

	@Override
	public DeviceLocation createDeviceLocation(DeviceLocation deviceLocation) throws Exception {

		DeviceLocation result = null;
		
		if( null != deviceLocation.getId() ){
			throw new Exception("Id should not be set when creating a device location");
		}
		
		try {
			// Check if device_id is valid
			try {
				Device device = getDeviceFromId(deviceLocation.getDeviceId());
				if( null == device ){
					throw new Exception("Device not found");
				}
				
			} catch (Exception e) {
				throw new Exception("Error finding device ("+deviceLocation.getDeviceId()+")",e);
			}
			
			// Check if location_id is valid
			try {
				Location location = getLocationFromLocationId(deviceLocation.getLocationId());
				if( null == location ){
					throw new Exception("Location not found");
				}
				
			} catch (Exception e) {
				throw new Exception("Error finding location ("+deviceLocation.getLocationId()+")",e);
			}
			
			// Get Sql Time
			Timestamp dbTime = new Timestamp( deviceLocation.getTimestamp().getTime() );
			
			PreparedStatement pstmt = dbConn.getConnection().prepareStatement(
				"INSERT INTO devices_locations"
				+" (timestamp,device_id,location_id,notes)"
				+" VALUES (?,?,?,?)"
				+" RETURNING id,timestamp,device_id,location_id,notes"
			);
			
			pstmt.setTimestamp(1, dbTime);
			pstmt.setObject(2, UUID.fromString(deviceLocation.getDeviceId()) );
			pstmt.setObject(3, UUID.fromString(deviceLocation.getLocationId()) );
			pstmt.setString(4, deviceLocation.getNotes());

			ResultSet resultSet = pstmt.executeQuery();
			
			resultSet.next();
			
			result = new DeviceLocation();
			result.setId( resultSet.getString(1) );
			result.setTimestamp( resultSet.getTimestamp(2) );
			result.setDeviceId( resultSet.getString(3) );
			result.setLocationId( resultSet.getString(4) );
			result.setNotes( resultSet.getString(5) );

			resultSet.close();
				
		} catch (Exception e) {
			throw new Exception("Error inserting deviceLocation into database", e);
		}
		
		return result;
	}

	@Override
	public List<DeviceLocation> getDeviceLocations() throws Exception {
		List<DeviceLocation> deviceLocations = new Vector<DeviceLocation>();

		try {
			PreparedStatement pstmt = dbConn.getConnection().prepareStatement(
				"SELECT id,device_id,location_id,timestamp,notes"
				+ " FROM devices_locations"
			);
			
			ResultSet resultSet = pstmt.executeQuery();
			
			while( resultSet.next() ){
				String id = resultSet.getString(1);
				String deviceId = resultSet.getString(2);
				String locationId = resultSet.getString(3);
				Date timestamp = resultSet.getTimestamp(4);
				String notes = resultSet.getString(5);

				DeviceLocation deviceLocation = new DeviceLocation();
				deviceLocation.setId(id);
				deviceLocation.setDeviceId(deviceId);
				deviceLocation.setLocationId(locationId);
				deviceLocation.setTimestamp(timestamp);
				deviceLocation.setNotes(notes);
				
				deviceLocations.add(deviceLocation);
			}
			
			resultSet.close();
			
		} catch (Exception e) {
			throw new Exception("Error retrieving device locations from database", e);
		}

		return deviceLocations;
	}
	
	@Override
	public List<DeviceLocation> getDeviceLocationsFromDeviceId(String device_id) throws Exception {
		List<DeviceLocation> deviceLocations = new Vector<DeviceLocation>();

		try {
			PreparedStatement pstmt = dbConn.getConnection().prepareStatement(
				"SELECT id,device_id,location_id,timestamp,notes FROM devices_locations WHERE device_id=?"
			);
			
			pstmt.setObject(1, UUID.fromString(device_id));
			
			ResultSet resultSet = pstmt.executeQuery();
			
			while( resultSet.next() ){
				String id = resultSet.getString(1);
				String deviceId = resultSet.getString(2);
				String locationId = resultSet.getString(3);
				Date timestamp = resultSet.getTimestamp(4);
				String notes = resultSet.getString(5);

				DeviceLocation deviceLocation = new DeviceLocation();
				deviceLocation.setId(id);
				deviceLocation.setDeviceId(deviceId);
				deviceLocation.setLocationId(locationId);
				deviceLocation.setTimestamp(timestamp);
				deviceLocation.setNotes(notes);
				
				deviceLocations.add(deviceLocation);
			}
			
			resultSet.close();
			
		} catch (Exception e) {
			throw new Exception("Error retrieving device locations for device (id="+device_id+") from database", e);
		}

		return deviceLocations;
	}
	
	@Override
	public List<Location> getLocationsFromDeviceLocations(List<DeviceLocation> deviceLocations) throws Exception {
		List<Location> locations = new Vector<Location>();
		
		try {
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
			
		} catch (Exception e) {
			throw new Exception("Error retrieving locations for device locations from database", e);
		}

		return locations;
	}
	
	@Override
	public Location createLocation(Location location) throws Exception {
		
		Location result = null;
		
		if( null != location.getId() ){
			throw new Exception("Id should not be set when creating a location");
		}
		
		try {
			PreparedStatement pstmt = dbConn.getConnection().prepareStatement(
				"INSERT INTO locations (name,coordinates,elevation_in_metres,comment,record_observations,accuracy_in_metres)"
				+" VALUES (?,ST_GeomFromEWKT(?),?,?,?,?)"
				+" RETURNING id,name,ST_AsEWKT(coordinates),elevation_in_metres,comment,record_observations,accuracy_in_metres"
			);
			
			pstmt.setString(1, location.getName());
			pstmt.setString(2, location.getGeometry());
			pstmt.setDouble(3, location.getElevation());
			pstmt.setString(4, location.getComment());
			pstmt.setBoolean(5, location.isRecordingObservations());
			if( null == location.getAccuracy() ){
				pstmt.setNull(6, java.sql.Types.NUMERIC);
			} else {
				pstmt.setDouble(6, location.getAccuracy());
			}
			
			ResultSet resultSet = pstmt.executeQuery();
			
			resultSet.next();
			
			result = new Location();
			result.setId( resultSet.getString(1) );
			result.setName( resultSet.getString(2) );
			result.setGeometry( resultSet.getString(3) );
			result.setElevation( resultSet.getDouble(4) );
			result.setComment( resultSet.getString(5) );
			result.setRecordingObservations( resultSet.getBoolean(6) );
			result.setAccuracy( resultSet.getDouble(7) );

			resultSet.close();
				
		} catch (Exception e) {
			throw new Exception("Error inserting location ("+location.getName()+") into database", e);
		}

		return result;
	}

	@Override
	public Location getLocationFromLocationId(String locationId) throws Exception {
		Location location = null;
		try {
			PreparedStatement pstmt = dbConn.getConnection().prepareStatement(
				"SELECT id,name,ST_AsEWKT(coordinates),elevation_in_metres,comment,record_observations,accuracy_in_metres"
				+ " FROM locations"
				+ " WHERE id=?"
			);
			
			pstmt.setObject(1, UUID.fromString(locationId));
			
			ResultSet resultSet = pstmt.executeQuery();
			
			while( resultSet.next() ){
				String location_id = resultSet.getString(1);
				String name = resultSet.getString(2);
				String geometry = resultSet.getString(3);
				double elevation = resultSet.getDouble(4);
				String comment = resultSet.getString(5);
				boolean recordingObservations = resultSet.getBoolean(6);
				Double accuracy = resultSet.getDouble(7);
				
				location = new Location();
				location.setId(location_id);
				location.setName(name);
				location.setGeometry(geometry);
				location.setElevation(elevation);
				location.setComment(comment);
				location.setRecordingObservations(recordingObservations);
				location.setAccuracy(accuracy);
			}
			
			resultSet.close();
			
		} catch (Exception e) {
			throw new Exception("Error retrieving location (id="+locationId+") from database", e);
		}

		return location;
	}

	@Override
	public Collection<Location> getLocations() throws Exception {
		List<Location> locations = new Vector<Location>();

		try {
			PreparedStatement pstmt = dbConn.getConnection().prepareStatement(
				"SELECT id,name,ST_AsEWKT(coordinates),elevation_in_metres,comment,record_observations,accuracy_in_metres"
				+ " FROM locations"
			);
			
			ResultSet resultSet = pstmt.executeQuery();
			
			while( resultSet.next() ){
				Location location = new Location();
				location.setId( resultSet.getString(1) );
				location.setName( resultSet.getString(2) );
				location.setGeometry( resultSet.getString(3) );
				location.setElevation( resultSet.getDouble(4) );
				location.setComment( resultSet.getString(5) );
				location.setRecordingObservations( resultSet.getBoolean(6) );
				location.setAccuracy( resultSet.getDouble(7) );
				
				locations.add(location);
			}
			
			resultSet.close();
			
		} catch (Exception e) {
			throw new Exception("Error retrieving locations from database", e);
		}

		return locations;
	}

	@Override
	public Observation createObservation(Observation observation) throws Exception {
		
		Observation result = null;
		
		if( null == observation ){
			throw new Exception("Attempting to create a null observation");
		}
		if( null != observation.getId() ){
			throw new Exception("Id should not be set when creating an observation");
		}
		
		try {
			PreparedStatement pstmt = dbConn.getConnection().prepareStatement(
				"INSERT INTO observations"
				+" (device_id,sensor_id,import_id,import_key,observation_type,"
				+" unit_of_measure,accuracy,precision,numeric_value,text_value,"
				+" logged_time,corrected_utc_time,location,height_min_metres,"
				+" height_max_metres,elevation_in_metres)"
				+" VALUES (?,?,?,?,?,?,?,?,?,?,?,?,ST_GeomFromEWKT(?),?,?,?)"
				+" RETURNING id,device_id,sensor_id,import_id,import_key,observation_type,"
				+" unit_of_measure,accuracy,precision,numeric_value,text_value,"
				+" logged_time,corrected_utc_time,ST_AsEWKT(location),height_min_metres,"
				+" height_max_metres,elevation_in_metres"
			);
			
			pstmt.setObject(1, UUID.fromString(observation.getDeviceId()));
			pstmt.setObject(2, UUID.fromString(observation.getSensorId()));
			pstmt.setObject(3, UUID.fromString(observation.getImportId()));
			pstmt.setString(4, observation.getImportKey());
			pstmt.setString(5, observation.getObservationType());
			pstmt.setString(6, observation.getUnitOfMeasure());
			pstmt.setDouble(7, observation.getAccuracy());
			pstmt.setDouble(8, observation.getPrecision());
			pstmt.setDouble(9, observation.getNumericValue());
			pstmt.setString(10, observation.getTextValue());
			pstmt.setTimestamp(11, new Timestamp(observation.getLoggedTime().getTime()));
			pstmt.setTimestamp(12, new Timestamp(observation.getCorrectedTime().getTime()));
			pstmt.setString(13, observation.getLocation());
			pstmt.setDouble(14, observation.getMinHeight());
			pstmt.setDouble(15, observation.getMaxHeight());
			pstmt.setDouble(16, observation.getElevation());
			
			ResultSet resultSet = pstmt.executeQuery();
			
			resultSet.next();
			
			result = new Observation();
			result.setId( resultSet.getString(1) );
			result.setDeviceId( resultSet.getString(2) );
			result.setSensorId( resultSet.getString(3) );
			result.setImportId( resultSet.getString(4) );
			result.setImportKey( resultSet.getString(5) );
			result.setObservationType( resultSet.getString(6) );
			result.setUnitOfMeasure( resultSet.getString(7) );
			result.setAccuracy( resultSet.getDouble(8) );
			result.setPrecision( resultSet.getDouble(9) );
			result.setNumericValue( resultSet.getDouble(10) );
			result.setTextValue( resultSet.getString(11) );
			result.setLoggedTime( resultSet.getTimestamp(12) );
			result.setCorrectedTime( resultSet.getTimestamp(13) );
			result.setLocation( resultSet.getString(14) );
			result.setMinHeight( resultSet.getDouble(15) );
			result.setMaxHeight( resultSet.getDouble(16) );
			result.setElevation( resultSet.getDouble(17) );

			resultSet.close();
				
		} catch (Exception e) {
			logger.error("Error inserting observation: "+observation);
			throw new Exception("Error inserting observation into database", e);
		}

		return result;
	}

	@Override
	public ObservationReader getObservationsFromImportId(String importId) throws Exception {
		ObservationReader observationReader = null;

		try {
			PreparedStatement pstmt = dbConn.getConnection().prepareStatement(
				"SELECT "
				+ ObservationReaderJdbc.getFields()
				+" FROM observations"
				+" WHERE import_id=?"
			);
			
			pstmt.setObject(1, UUID.fromString(importId));
			
			ResultSet resultSet = pstmt.executeQuery();
			
			observationReader = new ObservationReaderJdbc(resultSet);
			
		} catch (Exception e) {
			throw new Exception("Error while looking for an observation with import id: "+importId, e);
		}
		
		return observationReader;
	}

	@Override
	public Observation getObservationFromImportKey(String importKey) throws Exception {
		Observation observation = null;

		try {
			PreparedStatement pstmt = dbConn.getConnection().prepareStatement(
				"SELECT id,device_id,sensor_id,import_id,import_key,observation_type,"
				+" unit_of_measure,accuracy,precision,numeric_value,text_value,"
				+" logged_time,corrected_utc_time,ST_AsEWKT(location),height_min_metres,"
				+" height_max_metres,elevation_in_metres"
				+" FROM observations"
				+" WHERE import_key=?"
			);
			
			pstmt.setString(1, importKey);
			
			ResultSet resultSet = pstmt.executeQuery();
			
			while( resultSet.next() ){
				if( null != observation ){
					throw new Exception("Multiple observations reported for import key: "+importKey);
				}

				observation = new Observation();
				observation.setId( resultSet.getString(1) );
				observation.setDeviceId( resultSet.getString(2) );
				observation.setSensorId( resultSet.getString(3) );
				observation.setImportId( resultSet.getString(4) );
				observation.setImportKey( resultSet.getString(5) );
				observation.setObservationType( resultSet.getString(6) );
				observation.setUnitOfMeasure( resultSet.getString(7) );
				observation.setAccuracy( resultSet.getDouble(8) );
				observation.setPrecision( resultSet.getDouble(9) );
				observation.setNumericValue( resultSet.getDouble(10) );
				observation.setTextValue( resultSet.getString(11) );
				observation.setLoggedTime( resultSet.getTimestamp(12) );
				observation.setCorrectedTime( resultSet.getTimestamp(13) );
				observation.setLocation( resultSet.getString(14) );
				observation.setMinHeight( resultSet.getDouble(15) );
				observation.setMaxHeight( resultSet.getDouble(16) );
				observation.setElevation( resultSet.getDouble(17) );
			}

			resultSet.close();
			
		} catch (Exception e) {
			throw new Exception("Error while looking for an observation with import key: "+importKey, e);
		}
		
		return observation;
	}

	@Override
	public ImportRecord createImportRecord(ImportRecord importRecord) throws Exception {

		ImportRecord result = null;

		if( null != importRecord.getId() ){
			throw new Exception("Id should not be set when creating an import record");
		}
		
		try {
			PreparedStatement pstmt = dbConn.getConnection().prepareStatement(
				"INSERT INTO imports"
				+" (import_time,filename,import_parameters)"
				+" VALUES (?,?,?)"
				+" RETURNING id,import_time,filename,import_parameters"
			);

			pstmt.setTimestamp(1, new Timestamp(importRecord.getImportTime().getTime())); // now
			pstmt.setString(2, importRecord.getFileName());
			
			String importParamStr = null;
			if( null != importRecord.getImportParameters() ){
				importParamStr = importRecord.getImportParameters().toString();
			}
			pstmt.setString(3, importParamStr);
			
			ResultSet resultSet = pstmt.executeQuery();
			
			resultSet.next();
			
			result = new ImportRecord();
			result.setId( resultSet.getString(1) );
			result.setImportTime( resultSet.getTimestamp(2) );
			result.setFileName( resultSet.getString(3) );

			JSONObject jsonImportParams = null;
			importParamStr = resultSet.getString(4);
			if( null != importParamStr ){
				jsonImportParams = new JSONObject(importParamStr);
			}
			result.setImportParameters( jsonImportParams );
			
			resultSet.close();

		} catch (Exception e) {
			throw new Exception("Error while inserting import record in database",e);
		}
		
		return result;
	}

	@Override
	public List<ImportRecord> getImportRecords() throws Exception {
		List<ImportRecord> importRecords = new Vector<ImportRecord>();

		try {
			PreparedStatement pstmt = dbConn.getConnection().prepareStatement(
				"SELECT id,import_time,filename,import_parameters FROM imports ORDER BY import_time DESC LIMIT 10"
			);
			
			ResultSet resultSet = pstmt.executeQuery();
			
			while( resultSet.next() ){
				String id = resultSet.getString(1);
				Date importTime = resultSet.getTimestamp(2);
				String fileName = resultSet.getString(3);
				String importParametersStr = resultSet.getString(4);
				
				JSONObject importParameters = new JSONObject(importParametersStr);

				ImportRecord importRecord = new ImportRecord();
				importRecord.setId(id);
				importRecord.setImportTime(importTime);
				importRecord.setFileName(fileName);
				importRecord.setImportParameters(importParameters);
				
				importRecords.add(importRecord);
			}
			
			resultSet.close();
			
		} catch (Exception e) {
			throw new Exception("Error retrieving import records from database", e);
		}

		return importRecords;
	}

	@Override
	public ImportRecord getImportRecordFromImportId(String importId) throws Exception {
		ImportRecord importRecord = null;

		try {
			PreparedStatement pstmt = dbConn.getConnection().prepareStatement(
				"SELECT id,import_time,filename,import_parameters"
				+ " FROM imports"
				+ " WHERE id=?"
			);
			
			pstmt.setObject(1, UUID.fromString(importId));
			
			ResultSet resultSet = pstmt.executeQuery();
			
			while( resultSet.next() ){
				String id = resultSet.getString(1);
				Date importTime = resultSet.getTimestamp(2);
				String fileName = resultSet.getString(3);
				String importParametersStr = resultSet.getString(4);
				
				JSONObject importParameters = new JSONObject(importParametersStr);

				importRecord = new ImportRecord();
				importRecord.setId(id);
				importRecord.setImportTime(importTime);
				importRecord.setFileName(fileName);
				importRecord.setImportParameters(importParameters);
			}
			
			resultSet.close();
			
		} catch (Exception e) {
			throw new Exception("Error retrieving import record (id="+importId+") from database", e);
		}

		return importRecord;
	}

	@Override
	public LogRecord createLogRecord(LogRecord logRecord) throws Exception {

		if( null != logRecord.getId() ){
			throw new Exception("Id should not be set when creating an log record");
		}
		
		LogRecord result = null;

		Date time = new Date(); // now
		
		try {
			PreparedStatement pstmt = dbConn.getConnection().prepareStatement(
				"INSERT INTO logs"
				+" (timestamp,log)"
				+" VALUES (?,?)"
				+" RETURNING id,timestamp,log"
			);
			
			String logStr = null;
			JSONObject jsonLog = logRecord.getLog();
			if( null != jsonLog ){
				logStr = jsonLog.toString();
			};
			
			pstmt.setTimestamp(1, new Timestamp(time.getTime()));
			pstmt.setString(2, logStr);
			
			ResultSet resultSet = pstmt.executeQuery();

			resultSet.next();
			
			result = new LogRecord();
			result.setId( resultSet.getString(1) );
			result.setTimestamp( resultSet.getTimestamp(2) );
			
			logStr = resultSet.getString(3);
			if( null != logStr ){
				jsonLog = new JSONObject(logStr);
				result.setLog(jsonLog);
			}
			
			resultSet.close();
			
		} catch (Exception e) {
			throw new Exception("Error inserting log record to database", e);
		}
		
		return result;
	}

	@Override
	public List<LogRecord> getLogRecords() throws Exception {
		List<LogRecord> logRecords = new Vector<LogRecord>();

		try {
			PreparedStatement pstmt = dbConn.getConnection().prepareStatement(
				"SELECT id,timestamp,log FROM logs"
			);
			
			ResultSet resultSet = pstmt.executeQuery();
			
			while( resultSet.next() ){
				String id = resultSet.getString(1);
				Date timestamp = resultSet.getTimestamp(2);
				String logText = resultSet.getString(3);
				
				JSONObject logObj = new JSONObject(logText);

				LogRecord logRecord = new LogRecord();
				logRecord.setId(id);
				logRecord.setTimestamp(timestamp);
				logRecord.setLog(logObj);
				
				logRecords.add(logRecord);
			}
			
			resultSet.close();
			
		} catch (Exception e) {
			throw new Exception("Error retrieving log records from database", e);
		}

		return logRecords;
	}

	@Override
	public LogRecord getLogRecordFromId(String logId) throws Exception {
		LogRecord logRecord = null;

		try {
			PreparedStatement pstmt = dbConn.getConnection().prepareStatement(
				"SELECT id,timestamp,log FROM logs WHERE id=?"
			);
			
			pstmt.setObject(1, UUID.fromString(logId));
			
			ResultSet resultSet = pstmt.executeQuery();
			
			while( resultSet.next() ){
				String id = resultSet.getString(1);
				Date timestamp = resultSet.getTimestamp(2);
				String logText = resultSet.getString(3);
				
				JSONObject logObj = new JSONObject(logText);

				logRecord = new LogRecord();
				logRecord.setId(id);
				logRecord.setTimestamp(timestamp);
				logRecord.setLog(logObj);
			}
			
			resultSet.close();
			
		} catch (Exception e) {
			throw new Exception("Error retrieving log record (id="+logId+") from database", e);
		}

		return logRecord;
	}
}
