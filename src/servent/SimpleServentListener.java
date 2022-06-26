package servent;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import app.AppConfig;
import app.Cancellable;
import app.ServentInfo;
import app.snapshot_bitcake.LaiYangBitcakeManager;
import app.snapshot_bitcake.SnapshotCollector;
import servent.handler.MessageHandler;
import servent.handler.NullHandler;
import servent.handler.TransactionHandler;
import servent.handler.snapshot.*;
import servent.message.Message;
import servent.message.MessageType;
import servent.message.util.MessageUtil;

public class SimpleServentListener implements Runnable, Cancellable {

	private volatile boolean working = true;
	
	private SnapshotCollector snapshotCollector;

	private Map<Integer, Map<Integer, List<Message>>> queue = new HashMap<>();

	public SimpleServentListener(SnapshotCollector snapshotCollector) {
		this.snapshotCollector = snapshotCollector;
		for (Integer initiatorId: AppConfig.initiators) {
			queue.put(initiatorId, new HashMap<>());
		}
	}

	/*
	 * Thread pool for executing the handlers. Each client will get it's own handler thread.
	 */
	private final ExecutorService threadPool = Executors.newWorkStealingPool();
	
	private List<Message> queue2 = new ArrayList<>();
	
	@Override
	public void run() {
		ServerSocket listenerSocket = null;
		try {
			listenerSocket = new ServerSocket(AppConfig.myServentInfo.getListenerPort(), 100);
			/*
			 * If there is no connection after 1s, wake up and see if we should terminate.
			 */
			listenerSocket.setSoTimeout(1000);
		} catch (IOException e) {
			AppConfig.timestampedErrorPrint("Couldn't open listener socket on: " + AppConfig.myServentInfo.getListenerPort());
			System.exit(0);
		}
		
		
		while (working) {
			try {
				Message clientMessage=null;
				
				/*
				 * Lai-Yang stuff. Process any red messages we got before we got the marker.
				 * The marker contains the collector id, so we need to process that as our first
				 * red message. 
				 */

				//todo logika za vadjenje iz queue-a
				for(Message message : queue2)
				{
					boolean messageCanBeTakenOut = true;
					Map<Integer,Integer> msgSnapshotSerialNumbers = message.getSnapshotSerialNumbers();
					for (Map.Entry<Integer,Integer> serventEntry : AppConfig.snapshotSerialNumbers.entrySet())
					{
						if (msgSnapshotSerialNumbers.get(serventEntry.getKey()) > serventEntry.getValue())
						{
							messageCanBeTakenOut = false;
							break;
						}
					}
					if (messageCanBeTakenOut){
						clientMessage=message;
						AppConfig.timestampedStandardPrint("Izvlacim poruku iz queue-a -------------");
						queue2.remove(message);
						break;
					}
				}
				if (clientMessage==null)
				{
					/*
					 * This blocks for up to 1s, after which SocketTimeoutException is thrown.
					 */
					Socket clientSocket = listenerSocket.accept();
					
					//GOT A MESSAGE! <3
					clientMessage = MessageUtil.readMessage(clientSocket);
				}
				//todo logika za queue
				synchronized (AppConfig.colorLock) {
					//if (clientMessage.isWhite() == false && AppConfig.isWhite.get())
					{
						/*
						 * If the message is red, we are white, and the message isn't a marker,
						 * then store it. We will get the marker soon, and then we will process
						 * this message. The point is, we need the marker to know who to send
						 * our info to, so this is the simplest way to work around that.
						 */
//						if (clientMessage.getMessageType() != MessageType.LY_MARKER) {
//							//radi fino i bez red messages
//							//redMessages.add(clientMessage);
//							continue;
						//}
						boolean msgPutInQueue = false;
						if(clientMessage.getMessageType() == MessageType.TRANSACTION)
						{
							Map<Integer,Integer> msgSnapshotSerialNumbers = clientMessage.getSnapshotSerialNumbers();
							for (Map.Entry<Integer,Integer> serventEntry : AppConfig.snapshotSerialNumbers.entrySet())
							{
								if (msgSnapshotSerialNumbers.get(serventEntry.getKey()) > serventEntry.getValue())
								{
//									Map<Integer,List<Message>> listPerSnapshot = queue.get(serventEntry.getKey());
//
//									List<Message> list = listPerSnapshot.get(serventEntry.getValue());
//									if (list==null)
//									{
//										list = new ArrayList<>();
//										list.add(clientMessage);
//										listPerSnapshot.put(serventEntry.getValue(),list);
//
//									}
//									else
//									{
//										list.add(clientMessage);
//									}
									queue2.add(clientMessage);
									msgPutInQueue = true;
									break;
								}
							}
							if (msgPutInQueue)
								continue;

						}
						else if (clientMessage.getMessageType() == MessageType.LY_MARKER)
						{
							LaiYangBitcakeManager lyFinancialManager = (LaiYangBitcakeManager)snapshotCollector.getBitcakeManager();
	//							lyFinancialManager.markerEvent(
	//									Integer.parseInt(clientMessage.getMessageText()), snapshotCollector);
							int collectorId = Integer.parseInt(clientMessage.getMessageText());
							int snapshotId = clientMessage.getSnapshotSerialNumbers().get(collectorId);
							//AppConfig.timestampedStandardPrint("snapshot_init: " + collectorId + " snapshot_no: " + snapshotId);
							lyFinancialManager.LiSkMarkerEvent(collectorId, snapshotCollector,
									clientMessage.getOriginalSenderInfo(),clientMessage.getSnapshotSerialNumbers().get(collectorId));
						}
					}
				}
				
				MessageHandler messageHandler = new NullHandler(clientMessage);
				
				/*
				 * Each message type has it's own handler.
				 * If we can get away with stateless handlers, we will,
				 * because that way is much simpler and less error prone.
				 */
				switch (clientMessage.getMessageType())
				{
					case TRANSACTION:
						messageHandler = new TransactionHandler(clientMessage, snapshotCollector.getBitcakeManager());
						break;
					case LY_MARKER:
						messageHandler = new LYMarkerHandler();
						break;
					case LY_TELL:
						messageHandler = new LYTellHandler(clientMessage, snapshotCollector);
						break;
					case REJECT_MARKER:
						messageHandler = new RejectMarkerHandler(clientMessage,snapshotCollector);
						break;
					case REJECT_COLLECTOR:
						messageHandler = new RejectCollectorHandler(clientMessage,snapshotCollector);
						break;
					case ROUND_MESSAGE:
						messageHandler = new RoundMessageHandler(clientMessage,snapshotCollector);
						break;
					case ROUND_RESPONSE_MESSAGE:
						messageHandler = new RoundResponseHandler(clientMessage,snapshotCollector);
						break;
				}
				
				threadPool.submit(messageHandler);
			} catch (SocketTimeoutException timeoutEx) {
				//Uncomment the next line to see that we are waking up every second.
//				AppConfig.timedStandardPrint("Waiting...");
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	@Override
	public void stop() {
		this.working = false;
	}

}
