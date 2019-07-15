import com.hazelcast.jet.JetInstance;
import com.hazelcast.jet.aggregate.AggregateOperations;
import com.hazelcast.jet.config.JobConfig;
import com.hazelcast.jet.pipeline.*;

/**
 * Basic monitoring of the ingestion stream volume
 */
public class IngestionMonitor {

    public static void start(JetInstance jet) {
        jet.newJob(buildPipeline(), new JobConfig().setName("Ingestion monitor"));
    }


    private static Pipeline buildPipeline() {
        Pipeline p = Pipeline.create();
        p.drawFrom(Sources.<String, String>mapJournal(JetStarter.INPUT_MAP_NAME, JournalInitialPosition.START_FROM_CURRENT))
                .withoutTimestamps().setName("Stream from buffer")
                .map( e -> VehiclePosition.parse(e)).setName("Parse JSON")
                .addTimestamps(v -> v.timestamp, 0)
                .window(WindowDefinition.tumbling(1000))
                .aggregate(AggregateOperations.counting()).setName("Count datapoints")
                .drainTo(Sinks.logger(a -> "Datapoints last second: " + a.result())).setName("log");
        return p;
    }
}