package qut;

import edu.au.jacobi.pattern.Match;

import java.util.HashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

public class FindGeneRunnable extends Sequential implements Runnable {
    private Gene referenceGene;
    private Gene gene;
    private GenbankRecord record;
    private HashMap<String, Sigma70Consensus> consensus;
    private ReentrantLock lock;

    public FindGeneRunnable(ReentrantLock lock, Gene gene, Gene referenceGene, GenbankRecord record, HashMap<String, Sigma70Consensus> consensus) {
        this.lock = lock;
        this.gene = gene;
        this.record = record;
        this.referenceGene = referenceGene;
        this.consensus = consensus;
    }

    public static Match PredictPromoter(NucleotideSequence upStreamRegion) {
        return BioPatterns.getBestMatch(Sigma70Definition.getSeriesAll_Unanchored(0.7), upStreamRegion.toString());
    }

    @Override
    public void run() {
        if (Homologous(gene.sequence, referenceGene.sequence)) {
            NucleotideSequence upStreamRegion = GetUpstreamRegion(record.nucleotides, gene);
            Match prediction = this.PredictPromoter(upStreamRegion);
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
                        System.out.println("Lock Failed!!!");
                        System.out.println("**DO NOT TRUST THIS DATA**");
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}