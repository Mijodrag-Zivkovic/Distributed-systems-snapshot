package servent.message.snapshot;

import app.ServentInfo;
import servent.message.BasicMessage;
import servent.message.MessageType;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class RoundMessage extends BasicMessage {

    private final Map<Integer, Integer> bitcakeAmountPerRegionId;
    private final boolean blank;

    public RoundMessage(ServentInfo originalSenderInfo, ServentInfo receiverInfo, Map<Integer, Integer> bitcakeAmountPerRegionId, boolean blank) {
        super(MessageType.ROUND_MESSAGE, originalSenderInfo, receiverInfo);
        this.bitcakeAmountPerRegionId = bitcakeAmountPerRegionId;
        this.blank = blank;
    }

    public Map<Integer, Integer> getBitcakeAmountPerRegionId() {
        return bitcakeAmountPerRegionId;
    }

    public boolean isBlank() {
        return blank;
    }
}
