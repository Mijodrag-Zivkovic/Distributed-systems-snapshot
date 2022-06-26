package servent.handler.snapshot;

import app.AppConfig;
import app.TreeNode;
import app.snapshot_bitcake.LaiYangBitcakeManager;
import app.snapshot_bitcake.SnapshotCollector;
import servent.handler.MessageHandler;
import servent.message.Message;
import servent.message.MessageType;
import servent.message.snapshot.RejectCollector;

public class RejectCollectorHandler implements MessageHandler {

    private Message clientMessage;
    private SnapshotCollector snapshotCollector;

    public RejectCollectorHandler(Message clientMessage, SnapshotCollector snapshotCollector) {
        this.clientMessage = clientMessage;
        this.snapshotCollector = snapshotCollector;
    }

    @Override
    public void run() {
        if (clientMessage.getMessageType() == MessageType.REJECT_COLLECTOR) {
            RejectCollector message = (RejectCollector) clientMessage;
            LaiYangBitcakeManager bitcakeManager = (LaiYangBitcakeManager) snapshotCollector.getBitcakeManager();
            AppConfig.timestampedStandardPrint("dobili smo reject od " + message.getOriginalSenderInfo().getId() + " iz drugog regiona");

            synchronized (AppConfig.colorLock)
            {
                //bitcakeManager.addReply(message.getOriginalSenderInfo().getId());
                //isto za neigbours
                //dodaj rezultate u border bitcake managera
                //daj senderu response, pa tek onda sender upisuje da je dobio odgovor
                bitcakeManager.getBorderResults().put(message.getOriginalSenderInfo().getId(),message.getBorderResult());
                bitcakeManager.addReply(message.getOriginalSenderInfo().getId());
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
        } else {
            AppConfig.timestampedErrorPrint("Tell amount handler got: " + clientMessage);
        }

    }
}