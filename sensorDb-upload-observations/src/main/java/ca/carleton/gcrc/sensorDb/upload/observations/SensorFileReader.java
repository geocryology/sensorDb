package ca.carleton.gcrc.sensorDb.upload.observations;

import java.io.BufferedReader;
import java.io.Reader;
import java.util.Date;
import java.util.List;
import java.util.Stack;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/*

Logger: #E5096D 'PT1000TEMP' - USP_EXP2 - (CGI) Expander for GP5W - (V2.60, Mai 27 2014)
No,Time,#1:oC,#HK-Bat:V
1,22.04.2015 18:00:01,22.4735,3.510
2,22.04.2015 19:00:01,21.962

The serial number for the device is: E5096D

Two sensors:
- Label: '1'  Units: 'oC'  (temperature)
- Label: 'HK-Bat' Units: 'V' (voltage)

Each line has a date and a number of variables readings. 

Sometimes, there is a Delta Time line. This value should be used to override the final
offset.

Logger: #E50BB3 'PT1000TEMP' - USP_EXP2 - (CGI) Expander for GP5W - (V2.7, Jan 12 2016)
Delta Time: 1341 secs
No,Time,#1:oC,#HK-Bat:V,#HK-Temp:oC
1,28.06.2016 16:01:17,23.6905,3.604,22.20
2,28.06.2016 16:02:17,22.7814
3,28.06.2016 16:03:17,22.4845,3.590,22.63

 */

public class SensorFileReader {

	final protected Logger logger = LoggerFactory.getLogger(this.getClass());

	static private Pattern patternFirstLine = Pattern.compile("^Logger:\\s*#([^']*)'.*$");
	static private Pattern patternDeltaTimeLine = Pattern.compile("^Delta\\s+Time:\\s*(-?[0-9]+)\\s+secs[,\\s]*$");
	static private Pattern patternIgnoreLine = Pattern.compile("^\\(.*\\)\\s*$");
	static private Pattern patternTextNumber = Pattern.compile("^\\s*-?[0-9]+(\\.[0-9]+)?([eE][+-]?[0-9]+)?\\s*$");
	static private Pattern patternIgnoreValue = Pattern.compile("^\\s*\\(.*\\)\\s*$");
	static private Pattern patternIgnoreValue2 = Pattern.compile("^\\s*$");

	private BufferedReader bufReader;
	private String deviceSerialNumber = null;
	private Integer deltaTimeInSecs = null;
	private List<SampleColumn> columns = new Vector<SampleColumn>();
	private int timeColumnIndex;
	private boolean reachedEnd = false;
	private List<Sample> cachedObservations = new Vector<Sample>();
	private int lineNumber = 0;
	private Stack<String> bufferedLines = new Stack<String>();
	
	public SensorFileReader(Reader reader) throws Exception {
		CarriageReturnFilterReader crfr = new CarriageReturnFilterReader(reader);
		bufReader = new BufferedReader(crfr);
		
		readPreamble();
	}

	public String getDeviceSerialNumber() {
		return deviceSerialNumber;
	}
	
	public Integer getDeltaTimeInSecs() {
		return deltaTimeInSecs;
	}

	public List<SampleColumn> getColumns() {
		return columns;
	}
	
	public Sample read() throws Exception {
		// If end is reached, keep returning null
		if( reachedEnd ){
			return null;
		}
		
		// Picked what we have already parsed
		if( cachedObservations.size() > 0 ){
			Sample obs = cachedObservations.remove(0);
			return obs;
		}
		
		// Get new line
		int columnCount = columns.size();
		String line = readLine();
		if( null == line ){
			reachedEnd = true;
			return null;
		}
		
		// If line starts with "(Parameter", then it should be ignored
		Matcher matcherIgnoreLine = patternIgnoreLine.matcher(line);
		if( matcherIgnoreLine.matches() ){
			// Ignore this line. Re-enter
			return read();
		}

		// Parse line
		String[] fields = line.split(",");
		if( fields.length > columnCount ){
			throw new Exception("More fields than columns on line "+lineNumber);
		}
		
		// Time
		if( timeColumnIndex >= fields.length ){
			throw new Exception("Time not included on line "+lineNumber);
		}
		Date time = null;
		try {
			time = DateUtils.parseUtcString(fields[timeColumnIndex]);
		} catch(Exception e) {
			throw new Exception("Problem parsing date on line "+lineNumber, e);
		}
		
		// Create an observation for each value
		for(int index=0; index<fields.length; ++index){
			String fieldStr = fields[index];
			SampleColumn column = columns.get(index);
			
			if( column.isValue() ){
				Sample obs = null;
				
				boolean ignoreThisValue = false;
				Matcher matcherIgnoreValue = patternIgnoreValue.matcher(fieldStr);
				if( matcherIgnoreValue.matches() ){
					ignoreThisValue = true;
				};
				Matcher matcherIgnoreValue2 = patternIgnoreValue2.matcher(fieldStr);
				if( matcherIgnoreValue2.matches() ){
					ignoreThisValue = true;
				};

				if( !ignoreThisValue ){
					Matcher matcherTextNumber = patternTextNumber.matcher(fieldStr);
					if( matcherTextNumber.matches() ){
						double value = Double.parseDouble(fieldStr.trim());
						obs = new Sample(time, column, value);
					} else {
						obs = new Sample(time, column, fieldStr.trim());
					}
					
					if( null != obs ){
						obs.setLine(line);
						obs.setLineNumber(lineNumber);
						obs.setDeviceSerialNumber(deviceSerialNumber);
						cachedObservations.add(obs);
					}
				}
			}
		}
		
		// Re-enter
		return read();
	}

	private void readPreamble() throws Exception {
		// Read top line
		{
			String firstLine = readLine();
			Matcher matcherFirstLine = patternFirstLine.matcher(firstLine);
			if( matcherFirstLine.matches() ){
				String serialNumber = matcherFirstLine.group(1).trim();
				this.deviceSerialNumber = serialNumber;
				
			} else {
				logger.error("First line: "+firstLine);
				throw new Exception("Error while analyzing first line");
			}
		}
		
		// Read Delta Time line
		{
			String line = readLine();
			Matcher matcherDeltaTimeLine = patternDeltaTimeLine.matcher(line);
			if( matcherDeltaTimeLine.matches() ){
				int delta = Integer.parseInt( matcherDeltaTimeLine.group(1) );
				deltaTimeInSecs = new Integer(delta);
			} else {
				// If it is not the optional Delta Time line, push back
				pushBackLine(line);
			}
		}
		
		// Read columns
		{
			String line = readLine();
			String[] columnStrings = line.split(",");
			for(String columnString : columnStrings){
				SampleColumn column = SampleColumn.parseColumnString(columnString);
				this.columns.add(column);
			}
		}
		
		// Check that a date is provided in columns
		{
			boolean dateProvided = false;
			int index = 0;
			for(SampleColumn column : columns){
				if( column.isTime() ){
					if( dateProvided ){
						throw new Exception("Multiple time columns reported");
					}
					
					timeColumnIndex = index;
					dateProvided = true;
				}
				++index;
			}
			
			if( !dateProvided ){
				throw new Exception("No column reporting time");
			}
		}
	}
	
	private String readLine() throws Exception {
		String line = null;
		if( bufferedLines.size() > 0 ){
			line = bufferedLines.pop();
		} else {
			line = bufReader.readLine();
		}

		++lineNumber;
		
		return line;
	}
	
	private void pushBackLine(String line) {
		bufferedLines.push(line);
		--lineNumber;
	}
}
