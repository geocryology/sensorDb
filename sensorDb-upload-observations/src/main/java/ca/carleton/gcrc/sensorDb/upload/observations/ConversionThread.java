package ca.carleton.gcrc.sensorDb.upload.observations;

import java.util.List;
import java.util.Map;
import java.util.Vector;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ca.carleton.gcrc.upload.LoadedFile;

public class ConversionThread extends Thread {

	final protected Logger logger = LoggerFactory.getLogger(this.getClass());

	private boolean isShuttingDown = false;
	private String threadName = null;
	private List<ConversionRequest> conversionRequests = new Vector<ConversionRequest>();
	private SensorFileImporter importer = null;
	
	public ConversionThread(SensorFileImporter importer){
		this.importer = importer;
	}
	
	public void shutdown() {
		
		logger.info("Shutting down thread "+this.getClass().getName());

		synchronized(this) {
			isShuttingDown = true;
			this.notifyAll();
		}
	}

	public void addFileToConvert(LoadedFile loadedFile, Map<String, List<String>> parameters) {
		synchronized(this){
			ConversionRequest request = new ConversionRequest();
			request.setFileToConvert(loadedFile.getFile());
			request.setOriginalFileName(loadedFile.getOriginalFileName());

			// Initial offset
			{
				request.setInitialOffset(0);
				List<String> offsetStrings = parameters.get("initial_offset");
				if( null != offsetStrings ){
					for(String offsetString : offsetStrings){
						int offset = Integer.parseInt(offsetString);
						request.setInitialOffset(offset);
					}
				}
			}

			// Final offset
			{
				request.setFinalOffset(0);
				List<String> offsetStrings = parameters.get("final_offset");
				if( null != offsetStrings ){
					for(String offsetString : offsetStrings){
						int offset = Integer.parseInt(offsetString);
						request.setFinalOffset(offset);
					}
				}
			}

			// Importer
			{
				List<String> params  = parameters.get("importer");
				if( null != params ){
					for(String param : params){
						request.setImporterName(param);
					}
				}
			}

			// Notes
			{
				List<String> params  = parameters.get("notes");
				if( null != params ){
					for(String param : params){
						request.setNotes(param);
					}
				}
			}

			conversionRequests.add(request);

			this.notify();
		}
	}
	
	@Override
	public void run() {
		
		long threadId = Thread.currentThread().getId();
		threadName = this.getClass().getSimpleName()+" ["+threadId+"]";
		
		logger.info("Start thread "+threadName);
		
		boolean done = false;
		do {
			synchronized(this) {
				done = isShuttingDown;
			}
			if( false == done ) {
				activity();
			}
		} while( false == done );

		logger.info("Thread exiting "+threadName);
	}
	
	private void activity() {
		// Get work
		ConversionRequest conversionRequest = null;
		synchronized(this){
			if( conversionRequests.size() > 0 ){
				conversionRequest = conversionRequests.remove(0);	
			} else {
				// Wait for work
				try {
					this.wait();
				} catch (Exception e) {
					logger.error("Waiting thread interrupted "+threadName,e);
				}
			}
		}
		
		// Check if we need to convert a file
		if( null != conversionRequest ){
			try {
				logger.info("Start file conversion "+ conversionRequest +" "+threadName);
				importer.importFile(conversionRequest);
				logger.info("End file conversion "+ conversionRequest +" "+threadName);
			} catch (Exception e) {
				logger.error("Work error on thread "+threadName,e);
			} catch (Throwable t) {
				logger.error("Unexpected error on thread "+threadName+", thread will continue processing",t);
			}
		}
	}

}
