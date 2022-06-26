package app.snapshot_bitcake;

import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

import app.AppConfig;
import app.TreeNode;
import servent.message.snapshot.RoundMessage;
import servent.message.snapshot.RoundResponseMessage;
import servent.message.util.MessageUtil;

/**
 * Main snapshot collector class. Has support for Naive, Chandy-Lamport
 * and Lai-Yang snapshot algorithms.
 * 
 * @author bmilojkovic
 *
 */
public class SnapshotCollectorWorker implements SnapshotCollector {

	private volatile boolean working = true;
	private volatile boolean resultsReady = false;
	
	private AtomicBoolean collecting = new AtomicBoolean(false);
	private AtomicBoolean roundPhase = new AtomicBoolean(false);
	
	private Map<Integer, LYSnapshotResult> collectedLYValues;
	
	private BitcakeManager bitcakeManager;

	//

	private List<LYSnapshotResult> myRegionResults;
	private Map<Integer,LYSnapshotResult> borderResults = new ConcurrentHashMap<>();

	private Object roundsLock = new Object();
	private Set<Integer> otherRegionsIds = new HashSet<Integer>();
	private Map<Integer, Boolean> recievedReplies = new ConcurrentHashMap<>();
	private Map<Integer, Boolean> recievedResponses = new ConcurrentHashMap<>();
	private Map<Integer,Boolean> blanksRecieved = new ConcurrentHashMap<>();
	private Map<Integer,Boolean> blanksSent = new ConcurrentHashMap<>();
	private Map<Integer, Integer> bitcakeAmountPerRegionId = new ConcurrentHashMap<>();
	private Map<Integer,Integer> newBitcakeAmountsPerId = new ConcurrentHashMap<>();
	private boolean previousRoundChange;


	public SnapshotCollectorWorker() {
		bitcakeManager = new LaiYangBitcakeManager();
	}

	@Override
	public BitcakeManager getBitcakeManager() {
		return bitcakeManager;
	}
	
	@Override
	public void run() {
		while(working) {
			
			/*
			 * Not collecting yet - just sleep until we start actual work, or finish
			 */
			while (collecting.get() == false) {
				try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				
				if (working == false) {
					return;
				}
			}
			AppConfig.timestampedStandardPrint("izasao iz collecting whilea");
			/*
			 * Collecting is done in three stages:
			 * 1. Send messages asking for values
			 * 2. Wait for all the responses
			 * 3. Print result
			 */
			
			//1 send asks
			//((LaiYangBitcakeManager)bitcakeManager).markerEvent(AppConfig.myServentInfo.getId(), this);
			((LaiYangBitcakeManager)bitcakeManager).LiSkMarkerEvent(AppConfig.myServentInfo.getId(), this,null,AppConfig.snapshotSerialNumbers.get(AppConfig.myServentInfo.getId()));
			AppConfig.timestampedStandardPrint("poslao zahteve");
			//2 wait for responses or finish
			while (!resultsReady) {

				try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				
				if (working == false) {
					return;
				}
			}
			AppConfig.timestampedStandardPrint("rezultati stigli");
			//calculation and print phase
			int sum;
			sum = 0;
			for (LYSnapshotResult result : myRegionResults)
			{
				sum+=result.getRecordedAmount();
				AppConfig.timestampedStandardPrint(
						"Recorded bitcake amount for " + result.getServentId() + " = " + result.getRecordedAmount());
			}
			//todo logika za proveru kanala
			//
			for (LYSnapshotResult result1 : this.myRegionResults)
			{
				int resId1 = result1.getServentId();
				for (LYSnapshotResult result2 : this.myRegionResults)
				{
					int resId2 = result2.getServentId();
					if(resId1!=resId2)
					{
						if(AppConfig.getInfoById(resId1).getNeighbors().contains(resId2) &&
								AppConfig.getInfoById(resId2).getNeighbors().contains(resId1))
						{
							int res1Give = result1.getGiveHistory().get(resId2);
							int res2Get = result2.getGetHistory().get(resId1);
							String outputString = String.format(
									"Unreceived bitcake amount: %d from servent %d to servent %d",
									res1Give - res2Get, resId1, resId2);
							AppConfig.timestampedStandardPrint(outputString);
							sum += res1Give - res2Get;

						}
					}
				}
			}
			
			AppConfig.timestampedStandardPrint("My region bitcake count: " + sum);



			//razmena i runde
			if(!borderResults.isEmpty())
			{
				AppConfig.timestampedStandardPrint("Calculating border unrecieved bitcakes-----------------------------");
				for (Entry<Integer,LYSnapshotResult> entry : borderResults.entrySet())
				{
					AppConfig.timestampedStandardPrint("Border id " + entry.getKey());
				}


				//kanalne poruke koje nismo dobili iz drugog regiona
				for (LYSnapshotResult myResult : myRegionResults)
				{
					Integer myResultId = myResult.getServentId();
					for (Entry<Integer,LYSnapshotResult> entry : borderResults.entrySet())
					{
						otherRegionsIds.add(entry.getValue().getRootId());
						//AppConfig.timestampedStandardPrint("Border id " + entry.getKey());
						if (AppConfig.getInfoById(myResultId).getNeighbors().contains(entry.getKey()))
						{
							int given = entry.getValue().getGiveHistory().get(myResultId);
							int taken = myResult.getGetHistory().get(entry.getKey());
							String outputString = String.format(
									"Unreceived bitcake amount: %d from servent %d to servent %d",
									given - taken, entry.getKey(), myResultId);
							AppConfig.timestampedStandardPrint(outputString);
							sum+=given-taken;
						}
					}
				}
				AppConfig.timestampedStandardPrint("Suma uz granicne kanalne poruke: " + sum);
				bitcakeAmountPerRegionId.put(AppConfig.myServentInfo.getId(), sum);
				AppConfig.timestampedStandardPrint("Beggining round phase-----------------------------");
				AppConfig.timestampedStandardPrint("Other regions ids:");
				for (Integer regionId : otherRegionsIds)
					AppConfig.timestampedStandardPrint("other region " + regionId);
				previousRoundChange=true;
				startRounds();
			}

			//prodji kroz mapu i saberi sve
			//
			int finalSum=0;
			for (Entry<Integer,Integer> entry : bitcakeAmountPerRegionId.entrySet())
			{
				finalSum+=entry.getValue();
			}
			AppConfig.timestampedStandardPrint("Final sum: " + finalSum + " !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
			// na kraju
			((LaiYangBitcakeManager) bitcakeManager).resetHistory(TreeNode.rootId);
			//collectedLYValues.clear(); //reset for next invocation
			myRegionResults.clear();
			borderResults.clear();
			otherRegionsIds.clear();
			//todo sve clear
			collecting.set(false);
			recievedReplies.clear();
			bitcakeAmountPerRegionId.clear();
			blanksRecieved.clear();
			blanksSent.clear();
			newBitcakeAmountsPerId.clear();
			previousRoundChange=true;
		}

	}

	private void startRounds()
	{
		//synchronized (roundsLock)
		{
			roundPhase.set(true);
			int roundCounter = 1;

			while(roundPhase.get())
			{

				synchronized (roundsLock)
				{

					AppConfig.timestampedStandardPrint("Round " + roundCounter + " -----------------------------------------");
					roundCounter++;
					for(Integer regionId : otherRegionsIds)
					{
						AppConfig.timestampedStandardPrint("Saljem svoje rezultate regionu " + regionId);
						if(!previousRoundChange)
						{
							AppConfig.timestampedStandardPrint("saljem blank");
							blanksSent.put(regionId,true);
						}
						RoundMessage message = new RoundMessage(AppConfig.myServentInfo,AppConfig.getInfoById(regionId),bitcakeAmountPerRegionId,!previousRoundChange);
						MessageUtil.sendMessage(message);
					}
				}
				while (!recievedAllReplies() ) {

					try {
						Thread.sleep(500);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}

					if (working == false) {
						return;
					}
				}
				AppConfig.timestampedStandardPrint("Dobio sam poruke drugih regiona");
				synchronized (roundsLock)
				{

					if (recievedAllBlanks() && sentAllBlanks())
					{
						AppConfig.timestampedStandardPrint("kraj runde");
						roundPhase.set(false);

					}
					else if(newBitcakeAmountsPerId.isEmpty())
					{
						AppConfig.timestampedStandardPrint("nema novih rezultata");
						previousRoundChange=false;
					}
					else
					{
						AppConfig.timestampedStandardPrint("Nije kraj runde");
						previousRoundChange=true;
						for (Entry<Integer,Integer> entry : newBitcakeAmountsPerId.entrySet())
						{
							bitcakeAmountPerRegionId.put(entry.getKey(), entry.getValue());
						}

					}
					newBitcakeAmountsPerId.clear();
					recievedReplies.clear();
					recievedResponses.clear();
					blanksRecieved.clear();
					blanksSent.clear();
				}

			}
		}
	}

	public void addRecievedReply(Integer regionId, Map<Integer, Integer> recievedBitcakeAmountPerRegion, boolean blank)
	{
		//AppConfig.timestampedStandardPrint("Van locka AddRecievedReply za id " + regionId);
		synchronized (roundsLock)
		{
			//AppConfig.timestampedStandardPrint("Unutar locka AddRecievedReply za id " + regionId);
			recievedReplies.put(regionId,true);
			if (blank)
			{
				AppConfig.timestampedStandardPrint("dobio blank od " +regionId);
				blanksRecieved.put(regionId,true);
			}
			else
			{
				for (Entry<Integer,Integer> entry : recievedBitcakeAmountPerRegion.entrySet())
				{
					if (bitcakeAmountPerRegionId.get(entry.getKey())==null)
					{
						AppConfig.timestampedStandardPrint("Dodajem novi rezultat za id " + entry.getKey());
						newBitcakeAmountsPerId.put(entry.getKey(), entry.getValue());
						previousRoundChange=true;
					}
				}
			}
//			if (recievedAllReplies())
//			{
//				for (Integer id : otherRegionsIds)
//				{
//					MessageUtil.sendMessage(new RoundResponseMessage(AppConfig.myServentInfo,AppConfig.getInfoById(id)));
//				}
//			}

			AppConfig.timestampedStandardPrint("Izlazim iz addReply-a");
		}
	}

	@Override
	public void addRecievedResponse(Integer regionId) {
		//synchronized (roundsLock)
		{
			recievedResponses.put(regionId,true);
		}
	}

	private boolean recievedAllResponses()
	{
		for(Integer regionId : otherRegionsIds)
		{
			if (recievedResponses.get(regionId)==null)
			{
				return false;
			}
		}
		return true;
	}

	private boolean recievedAllReplies()
	{
		for(Integer regionId : otherRegionsIds)
		{
			if (recievedReplies.get(regionId)==null)
			{
				//AppConfig.timestampedStandardPrint("recievedAllReplies vraca false");
				return false;
			}
		}
		//AppConfig.timestampedStandardPrint("recievedAllReplies vraca true");
		return true;
	}

	private boolean recievedAllBlanks()
	{
		for(Integer regionId : otherRegionsIds)
		{
			if (blanksRecieved.get(regionId)==null)
				return false;
		}
		return true;
	}

	private boolean sentAllBlanks()
	{
		for(Integer regionId : otherRegionsIds)
		{
			if (blanksSent.get(regionId)==null)
				return false;
		}
		return true;
	}

	public List<LYSnapshotResult> getMyRegionResults() {
		return myRegionResults;
	}

	@Override
	public void addResultsToSnapshot(List<LYSnapshotResult> results,Map<Integer,LYSnapshotResult> borderResults ) {
		this.myRegionResults = new ArrayList<>(results);
		this.borderResults = new ConcurrentHashMap(borderResults);
		this.resultsReady=true;
	}

	@Override
	public void addLYSnapshotInfo(int id, LYSnapshotResult lySnapshotResult) {
		collectedLYValues.put(id, lySnapshotResult);
	}
	
	@Override
	public void startCollecting() {
		boolean oldValue = this.collecting.getAndSet(true);
		
		if (oldValue == true) {
			AppConfig.timestampedErrorPrint("Tried to start collecting before finished with previous.");
		}
	}
	
	@Override
	public void stop() {
		working = false;
	}

	public AtomicBoolean getCollecting() {
		return collecting;
	}
}
