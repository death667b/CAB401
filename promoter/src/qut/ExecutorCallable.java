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

    private static int nThreads = 4;
    private static ExecutorService executorService = Executors.newFixedThreadPool(nThreads);

    public static void run(String referenceFile, String dir) throws IOException {
        List<Callable<Void>> executorList = new ArrayList<>();

        List<Gene> referenceGenes = ParseReferenceGenes(referenceFile);
        for (String filename : ListGenbankFiles(dir)) {
            System.out.println(filename);
            GenbankRecord record = Parse(filename);
            for (Gene referenceGene : referenceGenes) {
                System.out.println(referenceGene.name);
                for (Gene gene : record.genes)
                    executorList.add(new FindGeneCallable(lock, gene, referenceGene, record));
            }
        }

        try {
            executorService.invokeAll(executorList);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        executorService.shutdown();
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