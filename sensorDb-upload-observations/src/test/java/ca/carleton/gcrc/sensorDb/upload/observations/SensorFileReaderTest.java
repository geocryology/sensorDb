package ca.carleton.gcrc.sensorDb.upload.observations;

import java.io.StringReader;

import junit.framework.TestCase;

public class SensorFileReaderTest extends TestCase {

	public void testCrCrLf() throws Exception {
		String input = "Logger: #E50AA2 'T2NODE_STR' - USP_EXP2 - (CGI) Expander for GP5W - (V2.7, Jan 12 2016)\r\r\n"
				+"No,Time,#1:oC,#2:oC,#3:oC,#4:oC,#HK-Bat:V\r\r\n"
				+"1,06.05.2015 10:18:45,21.6816,21.1393,21.6108,21.1236,3.662\r\r\n";

		StringReader sr = new StringReader(input);
		SensorFileReader sensorReader = new SensorFileReader(sr);

		String sn = sensorReader.getDeviceSerialNumber();
		if( false == "E50AA2".equals(sn) ){
			fail("Unexpected serial number");
		}

		Sample sample1 = sensorReader.read();
		if( null == sample1 ){
			fail("Expected sample1");
		}
		Sample sample2 = sensorReader.read();
		if( null == sample2 ){
			fail("Expected sample2");
		}
		Sample sample3 = sensorReader.read();
		if( null == sample3 ){
			fail("Expected sample3");
		}
		Sample sample4 = sensorReader.read();
		if( null == sample4 ){
			fail("Expected sample4");
		}
		Sample sample5 = sensorReader.read();
		if( null == sample5 ){
			fail("Expected sample5");
		}

		Sample sample6 = sensorReader.read();
		if( null != sample6 ){
			fail("sample6 should be null");
		}
	}

	public void testNoFLoating() throws Exception {
		String input = "Logger: #E509EC 'PT1000TEMP' - USP_EXP2 - (CGI) Expander for GP5W - (V2.7, Jan 12 2016)\n"
				+"No,Time,#1:oC,#HK-Bat:V\n"
				+"1,28.01.2016 15:40:00,-5\n"
				+"2,28.01.2016 15:40:00,-5.2\n"
				;

		StringReader sr = new StringReader(input);
		SensorFileReader sensorReader = new SensorFileReader(sr);

		String sn = sensorReader.getDeviceSerialNumber();
		if( false == "E509EC".equals(sn) ){
			fail("Unexpected serial number");
		}

		// First sample should have a numeric value
		{
			Sample sample = sensorReader.read();
			Double value = sample.getValue();
			if( null == value ){
				fail("A numeric value should be reported");
			}
		}
	}

	public void testFloatingPointNotation() throws Exception {
		String input = "Logger: #E509EC 'PT1000TEMP' - USP_EXP2 - (CGI) Expander for GP5W - (V2.7, Jan 12 2016)\n"
				+"No,Time,#1:oC,#HK-Bat:V\n"
				+"1,28.01.2016 15:40:00,-7.24792e-05\n"
				+"2,28.01.2016 15:50:00,-7.24792e+05\n"
				+"3,28.01.2016 16:00:00,-7.24792e05\n"
				+"4,28.01.2016 16:10:00,-7.24792E05\n"
				+"5,28.01.2016 16:20:00,-7E-05\n"
				+"6,28.01.2016 16:30:00,-7\n"
				+"7,28.01.2016 16:40:00,7\n"
				+"8,28.01.2016 16:50:00,7.0\n"
				;

		StringReader sr = new StringReader(input);
		SensorFileReader sensorReader = new SensorFileReader(sr);

		// Check that samples have numeric values
		int index = 1;
		Sample sample = sensorReader.read();
		while( null != sample ){
			Double value = sample.getValue();
			if( null == value ){
				fail("A numeric value should be reported at: "+index);
			}
			
			++index;
			sample = sensorReader.read();
		}
	}

	public void testParameterChangedLine() throws Exception {
		String input = "Logger: #E509EC 'PT1000TEMP' - USP_EXP2 - (CGI) Expander for GP5W - (V2.7, Jan 12 2016)\n"
				+"No,Time,#1:oC,#HK-Bat:V\n"
				+"1,28.01.2016 15:40:00,1\n"
				+"2,28.01.2016 15:50:00,1.1\n"
				+"3,28.01.2016 16:00:00,1\n"
				+"4,28.01.2016 16:10:00,1\n"
				+"(Parameter changed ('E509A7_20150527171243'))\n"
				+"5,28.01.2016 16:20:00,1.1\n"
				+"6,28.01.2016 16:30:00,1.2\n"
				+"7,28.01.2016 16:40:00,1.1\n"
				+"8,28.01.2016 16:50:00,1\n"
				;

		StringReader sr = new StringReader(input);
		SensorFileReader sensorReader = new SensorFileReader(sr);

		// Check that 8 samples are returned
		int count = 0;
		Sample sample = sensorReader.read();
		while( null != sample ){
			++count;
			sample = sensorReader.read();
		}
		if( 8 != count ){
			fail("Unexpected observation count: "+count);
		}
	}

	public void testFirmwareResetLine() throws Exception {
		String input = "Logger: #E509EC 'PT1000TEMP' - USP_EXP2 - (CGI) Expander for GP5W - (V2.7, Jan 12 2016)\n"
				+"No,Time,#1:oC,#HK-Bat:V\n"
				+"1,28.01.2016 15:40:00,1\n"
				+"2,28.01.2016 15:50:00,1.1\n"
				+"3,28.01.2016 16:00:00,1\n"
				+"4,28.01.2016 16:10:00,1\n"
				+"(Firmware Reset (28.05.2015 23:19:01 (THU)))\n"
				+"5,28.01.2016 16:20:00,1.1\n"
				+"6,28.01.2016 16:30:00,1.2\n"
				+"7,28.01.2016 16:40:00,1.1\n"
				+"8,28.01.2016 16:50:00,1\n"
				;

		StringReader sr = new StringReader(input);
		SensorFileReader sensorReader = new SensorFileReader(sr);

		// Check that 8 samples are returned
		int count = 0;
		Sample sample = sensorReader.read();
		while( null != sample ){
			++count;
			sample = sensorReader.read();
		}
		if( 8 != count ){
			fail("Unexpected observation count: "+count);
		}
	}

	public void testSkippedSensors() throws Exception {
		String input = "Logger: #E509EC 'PT1000TEMP' - USP_EXP2 - (CGI) Expander for GP5W - (V2.7, Jan 12 2016)\n"
				+"No,Time,#1:oC,#HK-Bat:V,#HK-Temp:C\n"
				+"1,28.01.2016 15:40:00,1,5,1.1\n"
				+"2,28.01.2016 15:41:00,1\n"
				+"3,28.01.2016 15:42:00,1,,1.2\n"
				;

		StringReader sr = new StringReader(input);
		SensorFileReader sensorReader = new SensorFileReader(sr);

		// Check that 8 samples are returned
		Double[] expectedValues = new Double[]{
			1.0 ,5.0 ,1.1
			,1.0
			,1.0 ,1.2	
		};
		String[] expectedLabels = new String[]{
			"#1:oC","#HK-Bat:V","#HK-Temp:C"
			,"#1:oC"
			,"#1:oC","#HK-Temp:C"
		};
		int count = 0;
		Sample sample = sensorReader.read();
		while( null != sample ){
			Number expectedValue = expectedValues[count];
			if( false == expectedValue.equals(sample.getValue()) ){
				fail("Expected value: "+expectedValue+" Reported by sample: "+sample.getValue());
			}
			
			String expectedLabel = expectedLabels[count];
			if( false == expectedLabel.equals(sample.getColumn().getName()) ){
				fail("Expected label: "+expectedLabel+" Reported by sample: "+sample.getColumn().getName());
			}
			
			sample = sensorReader.read();
			++count;
		}
		if( 6 != count ){
			fail("Unexpected observation count: "+count);
		}
	}

	public void testErrorValues() throws Exception {
		String input = "Logger: #E509EC 'PT1000TEMP' - USP_EXP2 - (CGI) Expander for GP5W - (V2.7, Jan 12 2016)\n"
				+"No,Time,#1:oC,#HK-Bat:V,#HK-Temp:C\n"
				+"1,28.01.2016 15:40:00,1,5,1.1\n"
				+"2,28.01.2016 15:41:00,1,(NO REPLY),(ERROR 255)\n"
				+"3,28.01.2016 15:42:00,1,(DATA CORRUPT),1.2\n"
				;

		StringReader sr = new StringReader(input);
		SensorFileReader sensorReader = new SensorFileReader(sr);

		// Check that 8 samples are returned
		Double[] expectedValues = new Double[]{
			1.0 ,5.0 ,1.1
			,1.0
			,1.0 ,1.2	
		};
		String[] expectedLabels = new String[]{
			"#1:oC","#HK-Bat:V","#HK-Temp:C"
			,"#1:oC"
			,"#1:oC","#HK-Temp:C"
		};
		int count = 0;
		Sample sample = sensorReader.read();
		while( null != sample ){
			Number expectedValue = expectedValues[count];
			if( false == expectedValue.equals(sample.getValue()) ){
				fail("Expected value: "+expectedValue+" Reported by sample: "+sample.getValue());
			}
			
			String expectedLabel = expectedLabels[count];
			if( false == expectedLabel.equals(sample.getColumn().getName()) ){
				fail("Expected label: "+expectedLabel+" Reported by sample: "+sample.getColumn().getName());
			}
			
			sample = sensorReader.read();
			++count;
		}
		if( 6 != count ){
			fail("Unexpected observation count: "+count);
		}
	}

	public void testDeltaTime() throws Exception {
		String input = "Logger: #E509EC 'PT1000TEMP' - USP_EXP2 - (CGI) Expander for GP5W - (V2.7, Jan 12 2016)\n"
				+"Delta Time: 1322 secs\n"
				+"No,Time,#1:oC,#HK-Bat:V\n"
				+"1,28.01.2016 15:40:00,1\n"
				+"2,28.01.2016 15:50:00,1.1\n"
				;

		StringReader sr = new StringReader(input);
		SensorFileReader sensorReader = new SensorFileReader(sr);

		Integer deltaTime = sensorReader.getDeltaTimeInSecs();
		
		if( null == deltaTime ){
			fail("Delta Time line not detected");
		} else if( 1322 != deltaTime.intValue() ) {
			fail("Unexpected delta time returned: "+deltaTime.intValue());
		}
	}

	public void testCommasInDeltaTime() throws Exception {
		String input = "Logger: #E509EC 'PT1000TEMP' - USP_EXP2 - (CGI) Expander for GP5W - (V2.7, Jan 12 2016)\n"
				+"Delta Time: 1322 secs,,\n"
				+"No,Time,#1:oC,#HK-Bat:V\n"
				+"1,28.01.2016 15:40:00,1\n"
				+"2,28.01.2016 15:50:00,1.1\n"
				;

		StringReader sr = new StringReader(input);
		SensorFileReader sensorReader = new SensorFileReader(sr);

		Integer deltaTime = sensorReader.getDeltaTimeInSecs();
		
		if( null == deltaTime ){
			fail("Delta Time line not detected");
		} else if( 1322 != deltaTime.intValue() ) {
			fail("Unexpected delta time returned: "+deltaTime.intValue());
		}
	}

	public void testNegativeDeltaTime() throws Exception {
		String input = "Logger: #E509EC 'PT1000TEMP' - USP_EXP2 - (CGI) Expander for GP5W - (V2.7, Jan 12 2016)\n"
				+"Delta Time: -122 secs\n"
				+"No,Time,#1:oC,#HK-Bat:V\n"
				+"1,28.01.2016 15:40:00,1\n"
				+"2,28.01.2016 15:50:00,1.1\n"
				;

		StringReader sr = new StringReader(input);
		SensorFileReader sensorReader = new SensorFileReader(sr);

		Integer deltaTime = sensorReader.getDeltaTimeInSecs();
		
		if( null == deltaTime ){
			fail("Delta Time line not detected");
		} else if( -122 != deltaTime.intValue() ) {
			fail("Unexpected delta time returned: "+deltaTime.intValue());
		}
	}

	public void testNoDeltaTime() throws Exception {
		String input = "Logger: #E509EC 'PT1000TEMP' - USP_EXP2 - (CGI) Expander for GP5W - (V2.7, Jan 12 2016)\n"
				+"No,Time,#1:oC,#HK-Bat:V\n"
				+"1,28.01.2016 15:40:00,1\n"
				+"2,28.01.2016 15:50:00,1.1\n"
				;

		StringReader sr = new StringReader(input);
		SensorFileReader sensorReader = new SensorFileReader(sr);

		Integer deltaTime = sensorReader.getDeltaTimeInSecs();
		
		if( null != deltaTime ){
			fail("Delta Time should not be detected");
		}
	}
}
