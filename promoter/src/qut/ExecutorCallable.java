package qut;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.ReentrantLock;

public class ExecutorCallable extends Sequential {
    private static ReentrantLock lock = new ReentrantLock(true);

    public static void run(String referenceFile, String dir, ExecutorService executorServiceC) throws IOException {
        List<Callable<Void>> executorList = new ArrayList<>();

        List<Gene> referenceGenes = ParseReferenceGenes(referenceFile);
        for (String filename : ListGenbankFiles(dir)) {
            GenbankRecord record = Parse(filename);
            for (Gene referenceGene : referenceGenes) {
                for (Gene gene : record.genes)
                    executorList.add(new FindGeneCallable(lock, gene, referenceGene, record));
            }
        }

        try {
            executorServiceC.invokeAll(executorList);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

    }

    public static void main(String[] args) throws IOException {
        int numberOfThreads = Runtime.getRuntime().availableProcessors();
        ExecutorService executorServiceC = Executors.newFixedThreadPool(numberOfThreads);

        long startTime = System.nanoTime();

        run("referenceGenes.list", "Ecoli", executorServiceC);

        long endTime = System.nanoTime();
        long timeElapsed = endTime - startTime;

        for (Map.Entry<String, Sigma70Consensus> entry : consensus.entrySet())
            System.out.println(entry.getKey() + " " + entry.getValue());

        System.out.println("Executing time in seconds: " + timeElapsed / 1000000000);
        executorServiceC.shutdown();
    }
}