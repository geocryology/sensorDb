package ca.carleton.gcrc.sensorDb.dbapi;

import java.util.List;
import java.util.Vector;

public class BulkObservationInsertResult {

	private List<ItemResult> itemResults = new Vector<ItemResult>();

	public List<ItemResult> getItemResults() {
		return itemResults;
	}

	public void addItemResult(Observation observation, boolean inserted, boolean collision) {
		ItemResult itemResult = new ItemResult();
		itemResult.setObservation(observation);
		itemResult.setInserted(inserted);
		itemResult.setCollision(collision);
		itemResults.add(itemResult);
	}

	public static class ItemResult {
		private Observation observation;
		private boolean inserted;
		private boolean collision;

		public Observation getObservation() {
			return observation;
		}
		public void setObservation(Observation observation) {
			this.observation = observation;
		}

		public boolean isInserted() {
			return inserted;
		}
		public void setInserted(boolean inserted) {
			this.inserted = inserted;
		}

		public boolean isCollision() {
			return collision;
		}
		public void setCollision(boolean collision) {
			this.collision = collision;
		}
	}
}
