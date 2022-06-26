package app.snapshot_bitcake;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiFunction;

import app.AppConfig;
import app.ServentInfo;
import app.TreeNode;
import servent.message.Message;
import servent.message.snapshot.LYMarkerMessage;
import servent.message.snapshot.LYTellMessage;
import servent.message.snapshot.RejectCollector;
import servent.message.snapshot.RejectMarker;
import servent.message.util.MessageUtil;

public class LaiYangBitcakeManager implements BitcakeManager {

	private final AtomicInteger currentAmount = new AtomicInteger(1000);
	//private Map<Integer, Integer> giveHistory = new ConcurrentHashMap<>();
	//private Map<Integer, Integer> getHistory = new ConcurrentHashMap<>();

	private Map<Integer, Map<Integer,Integer>> giveHistory = new ConcurrentHashMap<>();
	private Map<Integer, Map<Integer,Integer>> getHistory= new ConcurrentHashMap<>();

	private Map<Integer, Boolean> neighboursReplies = new ConcurrentHashMap<>();
	private List<LYSnapshotResult> collectedResults = new ArrayList<>();
	//private List<LYSnapshotResult> borderResults = new ArrayList<>();
	private Map<Integer,LYSnapshotResult> borderResults = new ConcurrentHashMap<>();
	Set<Integer> currentSnapshotInits = new HashSet<Integer>();

	private Map<Integer,LYSnapshotResult> resultsForOtherRegions = new ConcurrentHashMap<>();

	private boolean waitingForReset = false;

	/*
	 * This value is protected by AppConfig.colorLock.
	 * Access it only if you have the blessing.
	 */
	public int recordedAmount = 0;
	
	public LaiYangBitcakeManager() {
		initHistory();
	}

	public void LiSkMarkerEvent(int collectorId, SnapshotCollector snapshotCollector, ServentInfo sender, int snapshotId ) {
		synchronized (AppConfig.colorLock) {
			//provera jesam li ja init
				//ako jesam, probaj da napravis od mene root (vidi jesam li vec deo drugog stabla)
			AppConfig.timestampedStandardPrint("pozvan marker event");
			if(collectorId == AppConfig.myServentInfo.getId())
			{
				if (waitingForReset)
				{
					//initHistory();
					resetHistory(TreeNode.rootId);
				}

				if(TreeNode.rootId != -1)
				{
					//TODO stigao mi je marker event iz mog regiona
					RejectMarker rejectMessage = new RejectMarker(AppConfig.myServentInfo,sender,true);
					MessageUtil.sendMessage(rejectMessage);
					AppConfig.timestampedStandardPrint("odbio sam potencijalnog parenta " + sender.getId());

				}
				else
				{
					TreeNode.setNode(collectorId,-1);
					currentSnapshotInits.add(collectorId);
					//povecavam snapshotNo za sebe
					AppConfig.snapshotSerialNumbers.put(collectorId, AppConfig.snapshotCounter.incrementAndGet());
					AppConfig.timestampedStandardPrint("snapshot_init: " + collectorId + " snapshot_no: " + AppConfig.snapshotSerialNumbers.get(collectorId));
					doSnapShot();
					AppConfig.timestampedStandardPrint("postavljam se za root sa id-jem " + collectorId);
				}
			}
			else
			{
				AppConfig.timestampedStandardPrint("stigao marker od " + sender.getId() + " a root je " + collectorId);
				if (AppConfig.snapshotSerialNumbers.get(collectorId) < snapshotId)
				{
					AppConfig.snapshotSerialNumbers.put(collectorId, AppConfig.snapshotSerialNumbers.get(collectorId) + 1);
					AppConfig.timestampedStandardPrint("povecavam snapshot no za collector " + collectorId);
					AppConfig.timestampedStandardPrint("snapshot_init: " + collectorId + " snapshot_no: " + AppConfig.snapshotSerialNumbers.get(collectorId));
					if (waitingForReset)
					{
						//initHistory();
						resetHistory(TreeNode.rootId);
					}
				}

				//dodaj me u region
				if (TreeNode.rootId==-1)
				{
					TreeNode.setNode(collectorId,sender.getId());
					//obelezi reply

					currentSnapshotInits.add(collectorId);
					doSnapShot();
					if (recievedAllReplies())
					{
						sendResultsToParent();
					}
					AppConfig.timestampedStandardPrint("dodao sam se u region gde je root " + collectorId + " a parent je " + sender.getId());
					addReply(sender.getId());
					if (recievedAllReplies())
						sendResultsToParent();
				}
				else
				{
					//odbij collectora
					if(TreeNode.rootId!=collectorId)
					{
						AppConfig.timestampedStandardPrint("odbijam " + sender.getId() + " iz regiona sa root-om " + collectorId);

						RejectCollector rejectCollector = new RejectCollector(AppConfig.myServentInfo,sender,resultsForOtherRegions.get(collectorId));
						MessageUtil.sendMessage(rejectCollector);
						currentSnapshotInits.add(collectorId);
						//posalji poruku odbijanja i podatke
						// rezultat koji stigne ce da obelezi odgovor od ovog cvora
					}
					//odbij parenta
					else
					{
						//OBELEZI PORUKU OD PARENTA
						//addReply(sender.getId());
//						if (recievedAllReplies())
//							sendResultsToParent();
						RejectMarker rejectMessage = new RejectMarker(AppConfig.myServentInfo,sender,true);
						MessageUtil.sendMessage(rejectMessage);
						AppConfig.timestampedStandardPrint("odbio sam potencijalnog parenta " + sender.getId());
					}
					return;
				}
			}

			AppConfig.timestampedStandardPrint("saljem poruke drugima");
			//proveri sve slucajeve
			for (Integer neighbor : AppConfig.myServentInfo.getNeighbors()) {
				//|| neighbor==TreeNode.rootId
				if (TreeNode.parentId==neighbor ) continue;
				Message clMarker = new LYMarkerMessage(AppConfig.myServentInfo, AppConfig.getInfoById(neighbor), collectorId);
				AppConfig.timestampedStandardPrint("saljem poruku komsiji " + neighbor);
				MessageUtil.sendMessage(clMarker);
				try {
					/*
					 * This sleep is here to artificially produce some white node -> red node messages.
					 * Not actually recommended, as we are sleeping while we have colorLock.
					 */
					Thread.sleep(100);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}

			//probaj da posaljes rezultate (mozda smo list)
		}
	}

	public void initHistory()
	{
		for(Integer neighbor : AppConfig.myServentInfo.getNeighbors()) {
			neighboursReplies.put(neighbor,false);
		}
		for (Integer initiatorId : AppConfig.initiators)
		{
			giveHistory.put(initiatorId,new ConcurrentHashMap<>());
			getHistory.put(initiatorId,new ConcurrentHashMap<>());
			for(Integer neighbor : AppConfig.myServentInfo.getNeighbors()) {
				giveHistory.get(initiatorId).put(neighbor, 0);
				getHistory.get(initiatorId).put(neighbor, 0);
			}
		}
		borderResults.clear();
		//todo maybe for tree also
		TreeNode.resetNode();
		waitingForReset=false;

	}

	public void resetHistory(Integer initiatorId2)
	{
		for(Integer neighbor : AppConfig.myServentInfo.getNeighbors()) {
			neighboursReplies.put(neighbor,false);
		}
		for(Integer initiatorId : currentSnapshotInits)
		{
			giveHistory.put(initiatorId,new ConcurrentHashMap<>());
			getHistory.put(initiatorId,new ConcurrentHashMap<>());
			for(Integer neighbor : AppConfig.myServentInfo.getNeighbors()) {
				giveHistory.get(initiatorId).put(neighbor, 0);
				getHistory.get(initiatorId).put(neighbor, 0);
			}
		}
		borderResults.clear();
		//todo maybe for tree also
		TreeNode.resetNode();
		waitingForReset=false;
		collectedResults.clear();
		currentSnapshotInits.clear();
		resultsForOtherRegions.clear();
	}

	private void doSnapShot()
	{
		recordedAmount = currentAmount.get();
		for (Integer initiatorId : AppConfig.initiators)
		{
			LYSnapshotResult result = new LYSnapshotResult(AppConfig.myServentInfo.getId(),recordedAmount,
					giveHistory.get(initiatorId),getHistory.get(initiatorId),TreeNode.rootId);
			resultsForOtherRegions.put(initiatorId,result);
		}

		collectedResults.add(resultsForOtherRegions.get(TreeNode.rootId));
		// todo printaj res
	}

	public void sendResultsToParent()
	{
		LYTellMessage msg = new LYTellMessage(AppConfig.myServentInfo,AppConfig.getInfoById(TreeNode.parentId),collectedResults, borderResults);
		MessageUtil.sendMessage(msg);
		AppConfig.timestampedStandardPrint("sending results to parent -------------------------------\n");
		//todo reset history
		//initHistory();
		waitingForReset=true;
	}

	public void addReply(Integer neighbourId)
	{
		AppConfig.timestampedStandardPrint("obelezavam odgovor od " + neighbourId);
		neighboursReplies.put(neighbourId,true);

	}

	public boolean recievedAllReplies()
	{
		for (Integer neighbourId : AppConfig.myServentInfo.getNeighbors())
		{
			if(neighboursReplies.get(neighbourId)==false)
				return false;
		}
		return true;
	}

	public List<LYSnapshotResult> getCollectedResults() {
		return collectedResults;
	}

	public Map<Integer, LYSnapshotResult> getBorderResults() {
		return borderResults;
	}

	//Banetov kod
	private class MapValueUpdater implements BiFunction<Integer, Integer, Integer> {
		
		private int valueToAdd;
		
		public MapValueUpdater(int valueToAdd) {
			this.valueToAdd = valueToAdd;
		}
		
		@Override
		public Integer apply(Integer key, Integer oldValue) {
			return oldValue + valueToAdd;
		}
	}
	
	public void recordGiveTransaction(int neighbor, int amount) {
		for (Map.Entry<Integer, Map<Integer, Integer>> entry: giveHistory.entrySet()) {
			entry.getValue().compute(neighbor, new MapValueUpdater(amount));
		}
	}
	
	public void recordGetTransaction(int neighbor, int amount) {
		for (Map.Entry<Integer, Map<Integer, Integer>> entry: getHistory.entrySet()) {
			entry.getValue().compute(neighbor, new MapValueUpdater(amount));
		}
	}

	public void takeSomeBitcakes(int amount) {
		currentAmount.getAndAdd(-amount);
	}

	public void addSomeBitcakes(int amount) {
		currentAmount.getAndAdd(amount);
	}

	public int getCurrentBitcakeAmount() {
		return currentAmount.get();
	}
}
