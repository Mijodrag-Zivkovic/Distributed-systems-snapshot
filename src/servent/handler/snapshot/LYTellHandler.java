package servent.handler.snapshot;

import app.AppConfig;
import app.TreeNode;
import app.snapshot_bitcake.LaiYangBitcakeManager;
import app.snapshot_bitcake.SnapshotCollector;
import servent.handler.MessageHandler;
import servent.message.Message;
import servent.message.MessageType;
import servent.message.snapshot.LYTellMessage;

public class LYTellHandler implements MessageHandler {

	private Message clientMessage;
	private SnapshotCollector snapshotCollector;
	
	public LYTellHandler(Message clientMessage, SnapshotCollector snapshotCollector) {
		this.clientMessage = clientMessage;
		this.snapshotCollector = snapshotCollector;
	}

	@Override
	public void run() {
		if (clientMessage.getMessageType() == MessageType.LY_TELL) {
			LYTellMessage lyTellMessage = (LYTellMessage)clientMessage;
			LaiYangBitcakeManager bitcakeManager = (LaiYangBitcakeManager) snapshotCollector.getBitcakeManager();

			synchronized (AppConfig.colorLock)
			{
				bitcakeManager.addReply(lyTellMessage.getOriginalSenderInfo().getId());
				TreeNode.childrenIds.add(lyTellMessage.getOriginalSenderInfo().getId());
				bitcakeManager.getCollectedResults().addAll(lyTellMessage.getResults());
				//bitcakeManager.getBorderResults().addAll(lyTellMessage.getOtherRegionsResults());
				bitcakeManager.getBorderResults().putAll(lyTellMessage.getBorderResults());
				//isto za neigbours

				if (AppConfig.myServentInfo.getId() == TreeNode.rootId)
				{
					if (bitcakeManager.recievedAllReplies())
					{
						snapshotCollector.addResultsToSnapshot(bitcakeManager.getCollectedResults(),bitcakeManager.getBorderResults());
					}
				}
				else
				{
					if (bitcakeManager.recievedAllReplies())
					{
						bitcakeManager.sendResultsToParent();
					}
				}
			}


//			snapshotCollector.addLYSnapshotInfo(
//					lyTellMessage.getOriginalSenderInfo().getId(),
//					lyTellMessage.getLYSnapshotResult());
		} else {
			AppConfig.timestampedErrorPrint("Tell amount handler got: " + clientMessage);
		}

	}

}
