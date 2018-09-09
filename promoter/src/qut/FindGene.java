package qut;

import edu.au.jacobi.pattern.Match;
import edu.au.jacobi.pattern.Series;
import jaligner.BLOSUM62;
import jaligner.Sequence;
import jaligner.SmithWatermanGotoh;
import jaligner.matrix.Matrix;

import java.util.HashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

public class FindGene implements Runnable {
    public Series pattern;
    public Gene referenceGene;
    public Gene gene;
    public GenbankRecord record;
    public HashMap<String, Sigma70Consensus> consensus;

    private ReentrantLock lock = new ReentrantLock(true);

    private static byte[] complement = new byte['z'];

    static
    {
        complement['C'] = 'G'; complement['c'] = 'g';
        complement['G'] = 'C'; complement['g'] = 'c';
        complement['T'] = 'A'; complement['t'] = 'a';
        complement['A'] = 'T'; complement['a'] = 't';
    }

    private static final Matrix BLOSUM_62 = BLOSUM62.Load();

    public FindGene(ReentrantLock lock, Gene gene, Gene referenceGene, GenbankRecord record, Series pattern, HashMap<String, Sigma70Consensus> consensus){
        this.lock = lock;
        this.gene = gene;
        this.record = record;
        this.referenceGene = referenceGene;
        this.pattern = pattern;
        this.consensus = consensus;
    }

    private Match PredictPromoter(Series pattern, NucleotideSequence upStreamRegion)
    {
        return BioPatterns.getBestMatch(pattern, upStreamRegion.toString());
    }

    public static boolean Homologous(PeptideSequence A, PeptideSequence B)
    {
        return SmithWatermanGotoh.align(new Sequence(A.toString()), new Sequence(B.toString()), BLOSUM_62, 10f, 0.5f).calculateScore() >= 60;
    }

    private static NucleotideSequence GetUpstreamRegion(NucleotideSequence dna, Gene gene)
    {
        int upStreamDistance = 250;
        if (gene.location < upStreamDistance)
            upStreamDistance = gene.location-1;

        if (gene.strand == 1)
            return new NucleotideSequence(java.util.Arrays.copyOfRange(dna.bytes, gene.location-upStreamDistance-1, gene.location-1));
        else
        {
            byte[] result = new byte[upStreamDistance];
            int reverseStart = dna.bytes.length - gene.location + upStreamDistance;
            for (int i=0; i<upStreamDistance; i++)
                result[i] = complement[dna.bytes[reverseStart-i]];
            return new NucleotideSequence(result);
        }
    }

//    @Override
//    public void run() {
//        if (Homologous(gene.sequence, referenceGene.sequence))
//        {
//            NucleotideSequence upStreamRegion = GetUpstreamRegion(record.nucleotides, gene);
//            Match prediction = PredictPromoter(pattern, upStreamRegion);
//            if (prediction != null) {
//
//                lock.lock();
//                try {
//                    consensus.get(referenceGene.name).addMatch(prediction);
//                    consensus.get("all").addMatch(prediction);
//                } finally {
//                    lock.unlock();
//                }
//            }
//        }
//    }


        @Override
    public void run() {
        if (Homologous(gene.sequence, referenceGene.sequence))
        {
            NucleotideSequence upStreamRegion = GetUpstreamRegion(record.nucleotides, gene);
            Match prediction = PredictPromoter(pattern, upStreamRegion);
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
    }
}