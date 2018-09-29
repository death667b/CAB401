package qut;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;


public class SimpleParallel extends Sequential {
    private static ReentrantLock lock = new ReentrantLock(true);

    public static void run(String referenceFile, String dir) throws IOException {
        List<Gene> referenceGenes = ParseReferenceGenes(referenceFile);
        for (String filename : ListGenbankFiles(dir)) {
            GenbankRecord record = Parse(filename);
            for (Gene referenceGene : referenceGenes) {
                for (Gene gene : record.genes)
                    new Thread(new FindGeneRunnable(lock, gene, referenceGene, record, consensus)).start();
            }
        }
    }

    public static void main(String[] args) throws IOException {
        long startTime = System.nanoTime();

        run("referenceGenes.list", "Ecoli");

        long endTime = System.nanoTime();
        long timeElapsed = endTime - startTime;

        for (Map.Entry<String, Sigma70Consensus> entry : consensus.entrySet())
            System.out.println(entry.getKey() + " " + entry.getValue());

        System.out.println("Executing time in seconds: " + timeElapsed / 1000000000);
    }
}
