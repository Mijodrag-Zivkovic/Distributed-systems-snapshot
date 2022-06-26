package servent.message.snapshot;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import app.ServentInfo;
import app.snapshot_bitcake.LYSnapshotResult;
import servent.message.BasicMessage;
import servent.message.Message;
import servent.message.MessageType;

public class LYTellMessage extends BasicMessage {

	private static final long serialVersionUID = 3116394054726162318L;

	private LYSnapshotResult lySnapshotResult;
	private List<LYSnapshotResult> results;
	private Map<Integer,LYSnapshotResult> borderResults = new ConcurrentHashMap<>();
	
	public LYTellMessage(ServentInfo sender, ServentInfo receiver, LYSnapshotResult lySnapshotResult) {
		super(MessageType.LY_TELL, sender, receiver);
		
		this.lySnapshotResult = lySnapshotResult;
	}

	public LYTellMessage(ServentInfo sender, ServentInfo receiver, List<LYSnapshotResult> results, Map<Integer,LYSnapshotResult> borderResults) {
		super(MessageType.LY_TELL, sender, receiver);
		this.borderResults = borderResults;
		this.results = results;
	}
	
	private LYTellMessage(MessageType messageType, ServentInfo sender, ServentInfo receiver, 
			boolean white, List<ServentInfo> routeList, String messageText, int messageId,
			LYSnapshotResult lySnapshotResult) {
		super(messageType, sender, receiver, white, routeList, messageText, messageId);
		this.lySnapshotResult = lySnapshotResult;
	}

	public LYSnapshotResult getLYSnapshotResult() {
		return lySnapshotResult;
	}

	public List<LYSnapshotResult> getResults() {
		return results;
	}

	public Map<Integer, LYSnapshotResult> getBorderResults() {
		return borderResults;
	}

	@Override
	public Message setRedColor() {
		Message toReturn = new LYTellMessage(getMessageType(), getOriginalSenderInfo(), getReceiverInfo(),
				false, getRoute(), getMessageText(), getMessageId(), getLYSnapshotResult());
		return toReturn;
	}
}
