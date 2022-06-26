package app.snapshot_bitcake;

import app.Cancellable;

import java.util.List;
import java.util.Map;

/**
 * Describes a snapshot collector. Made not-so-flexibly for readability.
 * 
 * @author bmilojkovic
 *
 */
public interface SnapshotCollector extends Runnable, Cancellable {

	BitcakeManager getBitcakeManager();

	void addLYSnapshotInfo(int id, LYSnapshotResult lySnapshotResult);

	void startCollecting();

	List<LYSnapshotResult> getMyRegionResults();

	void addResultsToSnapshot(List<LYSnapshotResult> results, Map<Integer,LYSnapshotResult> borderResults);

	void addRecievedReply(Integer regionId, Map<Integer, Integer> bitcakeAmountPerRegionId, boolean blank);

	void addRecievedResponse(Integer regionId);

}