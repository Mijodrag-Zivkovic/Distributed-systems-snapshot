package servent.message.snapshot;

import app.ServentInfo;
import servent.message.BasicMessage;
import servent.message.MessageType;

public class RoundResponseMessage extends BasicMessage {
    public RoundResponseMessage(ServentInfo originalSenderInfo, ServentInfo receiverInfo) {
        super(MessageType.ROUND_RESPONSE_MESSAGE, originalSenderInfo, receiverInfo);
    }
}
