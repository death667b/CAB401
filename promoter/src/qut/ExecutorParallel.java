package qut;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

public class ExecutorParallel extends Sequential {
    private static ReentrantLock lock = new ReentrantLock(true);

    public static void run(String referenceFile, String dir, ExecutorService executorServiceP) throws IOException {
        List<Gene> referenceGenes = ParseReferenceGenes(referenceFile);
        for (String filename : ListGenbankFiles(dir)) {
            GenbankRecord record = Parse(filename);
            for (Gene referenceGene : referenceGenes) {
                for (Gene gene : record.genes)
                    executorServiceP.submit(new FindGeneRunnable(lock, gene, referenceGene, record, consensus));

            }
        }
    }

    public static void main(String[] args) throws IOException {
        int numberOfThreads = Runtime.getRuntime().availableProcessors();
        ExecutorService executorServiceP = Executors.newFixedThreadPool(numberOfThreads);

        long startTime = System.nanoTime();

        run("referenceGenes.list", "Ecoli", executorServiceP);

        executorServiceP.shutdown();
        try {
            executorServiceP.awaitTermination(10, TimeUnit.MINUTES);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        long endTime = System.nanoTime();
        long timeElapsed = endTime - startTime;

        for (Map.Entry<String, Sigma70Consensus> entry : consensus.entrySet())
            System.out.println(entry.getKey() + " " + entry.getValue());

        System.out.println("Executing time in seconds: " + timeElapsed / 1000000000);
    }
}