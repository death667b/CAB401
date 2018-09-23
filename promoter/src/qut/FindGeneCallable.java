package qut;

import edu.au.jacobi.pattern.Match;

import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

public class FindGeneCallable extends Sequential implements Callable<Void> {
    private Gene gene;
    private Gene referenceGene;
    private GenbankRecord record;
    private ReentrantLock lock;

    public FindGeneCallable(ReentrantLock lock, Gene gene, Gene referenceGene, GenbankRecord record) {
        this.lock = lock;
        this.gene = gene;
        this.referenceGene = referenceGene;
        this.record = record;
    }

    public static Match PredictPromoter(NucleotideSequence upStreamRegion) {
        return BioPatterns.getBestMatch(Sigma70Definition.getSeriesAll_Unanchored(0.7), upStreamRegion.toString());
    }

    @Override
    public Void call() {
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
    }
}
