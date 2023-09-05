package in.nic.npi.utilities;

public class CounterClass {

    public CounterClass() {

        BATCH_ID_INDEX = 0;
        RECORD_ID_INDEX = 0;
        RECORD_COUNTER = 0;
        TOTAL_RECORD_KAFKA_COUNTER = 0;
    }

    public int BATCH_ID_INDEX;
    public int RECORD_ID_INDEX;
    public int RECORD_COUNTER;
    public int TOTAL_RECORD_KAFKA_COUNTER;

    public void increamentBatchId() {
        BATCH_ID_INDEX = BATCH_ID_INDEX + 1;

    }

    public void increamentRecordId() {
        RECORD_ID_INDEX = RECORD_ID_INDEX + 1;

    }

    public void increamentRecordCounter() {
        RECORD_COUNTER = RECORD_COUNTER + 1;

    }

    public void increamentRecordKafkaCounter() {
        TOTAL_RECORD_KAFKA_COUNTER = TOTAL_RECORD_KAFKA_COUNTER + 1;

    }

    public void resetBatchId() {
        BATCH_ID_INDEX = 0;

    }

    public void resetRecordIndexId() {
        RECORD_ID_INDEX = 0;

    }

    public void resetRecordCounter() {
        RECORD_COUNTER = 0;

    }

    public void resetRecordKafkaCounter() {
        TOTAL_RECORD_KAFKA_COUNTER = 0;

    }

}
