package servent.handler.snapshot;

import app.AppConfig;
import app.TreeNode;
import app.snapshot_bitcake.LaiYangBitcakeManager;
import app.snapshot_bitcake.SnapshotCollector;
import servent.handler.MessageHandler;
import servent.message.Message;
import servent.message.MessageType;
import servent.message.snapshot.RejectMarker;
import servent.message.snapshot.RoundMessage;

public class RoundMessageHandler implements MessageHandler {

    private Message clientMessage;
    private SnapshotCollector snapshotCollector;

    public RoundMessageHandler(Message clientMessage, SnapshotCollector snapshotCollector) {
        this.clientMessage = clientMessage;
        this.snapshotCollector = snapshotCollector;
    }

    @Override
    public void run() {
        if (clientMessage.getMessageType() == MessageType.ROUND_MESSAGE) {
            RoundMessage message = (RoundMessage) clientMessage;
            LaiYangBitcakeManager bitcakeManager = (LaiYangBitcakeManager) snapshotCollector.getBitcakeManager();
            AppConfig.timestampedStandardPrint("dobili smo poruku od " + message.getOriginalSenderInfo().getId());

            //treba li nam colorLock ovde?
            // ili mozda neki drugi lock
            //synchronized (AppConfig.colorLock)
            {
                snapshotCollector.addRecievedReply(message.getOriginalSenderInfo().getId(),message.getBitcakeAmountPerRegionId(),message.isBlank());

            }
        } else {
            AppConfig.timestampedErrorPrint("Tell amount handler got: " + clientMessage);
        }

    }
}