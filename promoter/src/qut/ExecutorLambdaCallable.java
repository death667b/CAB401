package qut;


import edu.au.jacobi.pattern.Match;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

public class ExecutorLambdaCallable extends Sequential {
    private static ReentrantLock lock = new ReentrantLock(true);

    private static int nThreads = 4;
    private static ExecutorService executorService = Executors.newFixedThreadPool(nThreads);

    public static Match PredictPromoter(NucleotideSequence upStreamRegion) {
        return BioPatterns.getBestMatch(Sigma70Definition.getSeriesAll_Unanchored(0.7), upStreamRegion.toString());
    }

    public static void run(String referenceFile, String dir) throws IOException {
        List<Callable<Void>> executorList = new ArrayList<>();

        List<Gene> referenceGenes = ParseReferenceGenes(referenceFile);
        for (String filename : ListGenbankFiles(dir)) {
            System.out.println(filename);
            GenbankRecord record = Parse(filename);
            for (Gene referenceGene : referenceGenes) {
                System.out.println(referenceGene.name);
                for (Gene gene : record.genes) {
                    executorList.add(() -> {
                        if (Homologous(gene.sequence, referenceGene.sequence)) {
                            NucleotideSequence upStreamRegion = GetUpstreamRegion(record.nucleotides, gene);
                            Match prediction = PredictPromoter(upStreamRegion);

                            if (prediction != null) {
                                try {
                                    boolean isLockAcquired = lock.tryLock(1, TimeUnit.SECONDS);

                                    if (isLockAcquired) {
                                        try {
                                            consensus.get(referenceGene.name).addMatch(prediction);
                                            consensus.get("all").addMatch(prediction);
                                        } finally {
                                            lock.unlock();
                                        }
                                    } else {
                                        System.out.println("lock failed");
                                    }
                                } catch (InterruptedException e) {
                                    e.printStackTrace();
                                }
                            }
                        }

                        return null;
                    });
                }
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