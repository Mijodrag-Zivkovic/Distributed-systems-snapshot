package servent.message.snapshot;

import app.ServentInfo;
import app.snapshot_bitcake.LYSnapshotResult;
import servent.message.BasicMessage;
import servent.message.MessageType;

import java.io.Serial;

public class RejectCollector extends BasicMessage {


    @Serial
    private static final long serialVersionUID = 8203570216964699066L;
    private final LYSnapshotResult borderResult;

    public RejectCollector(ServentInfo originalSenderInfo, ServentInfo receiverInfo, LYSnapshotResult borderResult) {
        super(MessageType.REJECT_COLLECTOR, originalSenderInfo, receiverInfo);
        this.borderResult=borderResult;
    }

    public LYSnapshotResult getBorderResult() {
        return borderResult;
    }
}
