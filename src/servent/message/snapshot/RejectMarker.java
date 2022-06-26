package servent.message.snapshot;

import app.ServentInfo;
import servent.message.BasicMessage;
import servent.message.MessageType;

import java.io.Serial;


public class RejectMarker extends BasicMessage {


    @Serial
    private static final long serialVersionUID = 4166695054092924269L;
    private final boolean sameRegion;
    
    
    public RejectMarker( ServentInfo originalSenderInfo, ServentInfo receiverInfo,boolean sameRegion) {
        super(MessageType.REJECT_MARKER, originalSenderInfo, receiverInfo);
        this.sameRegion = sameRegion;
    }

    public boolean isSameRegion() {
        return sameRegion;
    }
}
