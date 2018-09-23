package qut;

import java.io.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.locks.ReentrantLock;

public class ExecutorParallel extends Sequential
{
    private static int nThreads = 4;
    private static ExecutorService executor = Executors.newFixedThreadPool(nThreads);

    private static ReentrantLock lock = new ReentrantLock(true);

    public static void run(String referenceFile, String dir) throws IOException
    {
        List<Gene> referenceGenes = ParseReferenceGenes(referenceFile);
        for (String filename : ListGenbankFiles(dir))
        {
            System.out.println(filename);
            GenbankRecord record = Parse(filename);
            for (Gene referenceGene : referenceGenes)
            {
                System.out.println(referenceGene.name);
                for (Gene gene : record.genes)
                    executor.submit(new FindGeneRunnable(lock, gene, referenceGene, record, consensus));

            }
        }

        executor.shutdown();
        try {
            executor.awaitTermination(10, TimeUnit.MINUTES);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) throws IOException
    {
        long startTime = System.nanoTime();

        run("referenceGenes.list", "Ecoli");

        long endTime = System.nanoTime();
        long timeElapsed = endTime - startTime;

        for (Map.Entry<String, Sigma70Consensus> entry : consensus.entrySet())
            System.out.println(entry.getKey() + " " + entry.getValue());

        System.out.println("Executing time in seconds: "+ timeElapsed / 1000000000);
    }
}