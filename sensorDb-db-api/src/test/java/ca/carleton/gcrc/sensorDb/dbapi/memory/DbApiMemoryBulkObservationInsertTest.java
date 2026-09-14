package ca.carleton.gcrc.sensorDb.dbapi.memory;

import java.util.List;
import java.util.Vector;

import ca.carleton.gcrc.sensorDb.dbapi.BulkObservationInsertResult;
import ca.carleton.gcrc.sensorDb.dbapi.Observation;
import ca.carleton.gcrc.sensorDb.dbapi.ObservationReader;
import junit.framework.TestCase;

public class DbApiMemoryBulkObservationInsertTest extends TestCase {

	public void testCreateObservationsIfAbsentMarksLaterDuplicateAsCollision() throws Exception {
		DbApiMemory dbApi = new DbApiMemory();
		
		List<Observation> observations = new Vector<Observation>();
		observations.add( createObservation("import-1", "key-1") );
		observations.add( createObservation("import-1", "key-1") );
		
		BulkObservationInsertResult result = dbApi.createObservationsIfAbsent(observations);
		List<BulkObservationInsertResult.ItemResult> itemResults = result.getItemResults();
		
		assertEquals(2, itemResults.size());
		assertTrue(itemResults.get(0).isInserted());
		assertFalse(itemResults.get(0).isCollision());
		assertFalse(itemResults.get(1).isInserted());
		assertTrue(itemResults.get(1).isCollision());
		
		int insertedCount = 0;
		ObservationReader observationReader = dbApi.getObservationsFromImportId("import-1");
		try {
			Observation observation = observationReader.read();
			while( null != observation ){
				++insertedCount;
				observation = observationReader.read();
			}
		} finally {
			observationReader.close();
		}
		assertEquals(1, insertedCount);
	}

	private Observation createObservation(String importId, String importKey) {
		Observation observation = new Observation();
		observation.setImportId(importId);
		observation.setImportKey(importKey);
		return observation;
	}
}
