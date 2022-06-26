package servent.handler.snapshot;

import app.AppConfig;
import app.snapshot_bitcake.LaiYangBitcakeManager;
import app.snapshot_bitcake.SnapshotCollector;
import servent.handler.MessageHandler;
import servent.message.Message;
import servent.message.MessageType;
import servent.message.snapshot.RoundMessage;
import servent.message.snapshot.RoundResponseMessage;

public class RoundResponseHandler implements MessageHandler {

    private Message clientMessage;
    private SnapshotCollector snapshotCollector;

    public RoundResponseHandler(Message clientMessage, SnapshotCollector snapshotCollector) {
        this.clientMessage = clientMessage;
        this.snapshotCollector = snapshotCollector;
    }

    @Override
    public void run() {
        if (clientMessage.getMessageType() == MessageType.ROUND_RESPONSE_MESSAGE) {
            RoundResponseMessage message = (RoundResponseMessage) clientMessage;
            LaiYangBitcakeManager bitcakeManager = (LaiYangBitcakeManager) snapshotCollector.getBitcakeManager();
            AppConfig.timestampedStandardPrint("dobili smo response od " + message.getOriginalSenderInfo().getId());

            //treba li nam colorLock ovde?
            // ili mozda neki drugi lock
            //synchronized (AppConfig.colorLock)
            {
                //snapshotCollector.addRecievedReply(message.getOriginalSenderInfo().getId(),message.getBitcakeAmountPerRegionId(),message.isBlank());
                snapshotCollector.addRecievedResponse(message.getOriginalSenderInfo().getId());
            }
        } else {
            AppConfig.timestampedErrorPrint("Tell amount handler got: " + clientMessage);
        }

    }
}