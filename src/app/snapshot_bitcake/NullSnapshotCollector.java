package app.snapshot_bitcake;

import java.util.List;
import java.util.Map;

/**
 * This class is used if the user hasn't specified a snapshot type in config.
 * 
 * @author bmilojkovic
 *
 */
public class NullSnapshotCollector implements SnapshotCollector {

	@Override
	public void run() {}

	@Override
	public void stop() {}

	@Override
	public BitcakeManager getBitcakeManager() {
		return null;
	}

	@Override
	public void addLYSnapshotInfo(int id, LYSnapshotResult lySnapshotResult) {}

	@Override
	public void startCollecting() {}

	@Override
	public List<LYSnapshotResult> getMyRegionResults() {
		return null;
	}

	@Override
	public void addResultsToSnapshot(List<LYSnapshotResult> results, Map<Integer,LYSnapshotResult> borderResults) {

	}

	@Override
	public void addRecievedReply(Integer regionId, Map<Integer, Integer> bitcakeAmountPerRegionId, boolean blank) {

	}

	@Override
	public void addRecievedResponse(Integer regionId) {

	}

}
