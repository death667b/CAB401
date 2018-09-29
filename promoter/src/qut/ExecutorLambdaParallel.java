package qut;

import edu.au.jacobi.pattern.Match;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

public class ExecutorLambdaParallel extends Sequential {
    private static ReentrantLock lock = new ReentrantLock(true);

    public static void run(String referenceFile, String dir, ExecutorService executorServiceLP) throws IOException {
        List<Gene> referenceGenes = ParseReferenceGenes(referenceFile);
        for (String filename : ListGenbankFiles(dir)) {
            GenbankRecord record = Parse(filename);
            for (Gene referenceGene : referenceGenes) {
                for (Gene gene : record.genes)
                    executorServiceLP.submit(() -> {
                        if (Homologous(gene.sequence, referenceGene.sequence)) {
                            NucleotideSequence upStreamRegion = GetUpstreamRegion(record.nucleotides, gene);

                            {
                                try {
                                    boolean isLockAcquired = lock.tryLock(1, TimeUnit.SECONDS);

                                    if (isLockAcquired) {
                                        try {
                                            Match prediction = PredictPromoter(upStreamRegion);
                                            if (prediction != null) {
                                                consensus.get(referenceGene.name).addMatch(prediction);
                                                consensus.get("all").addMatch(prediction);
                                            }

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

                    });
            }
        }

    }

    public static void main(String[] args) throws IOException, ExecutionException, InterruptedException {
        int numberOfThreads = Runtime.getRuntime().availableProcessors();
        ExecutorService executorServiceLP = Executors.newFixedThreadPool(numberOfThreads);

        long startTime = System.nanoTime();

        run("referenceGenes.list", "Ecoli", executorServiceLP);

        executorServiceLP.shutdown();
        try {
            executorServiceLP.awaitTermination(10, TimeUnit.MINUTES);
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