package cli.command;

import app.AppConfig;
import app.snapshot_bitcake.SnapshotCollector;
import app.snapshot_bitcake.SnapshotCollectorWorker;

public class BitcakeInfoCommand implements CLICommand {

	private SnapshotCollector collector;
	
	public BitcakeInfoCommand(SnapshotCollector collector) {
		this.collector = collector;
	}
	
	@Override
	public String commandName() {
		return "bitcake_info";
	}

	@Override
	public void execute(String args) {

		if(AppConfig.myServentInfo.isInit())
		{
			//SnapshotCollectorWorker snapshotCollectorWorker = (SnapshotCollectorWorker) collector;
			if (!((SnapshotCollectorWorker) collector).getCollecting().get())
				collector.startCollecting();
			else
				AppConfig.timestampedErrorPrint("Servent "+AppConfig.myServentInfo.getId()+ " already started snapshot that hasn't finished!");

		}
		else
			AppConfig.timestampedErrorPrint("Servent "+AppConfig.myServentInfo.getId()+ " can't initiate snapshot!");

	}

}
