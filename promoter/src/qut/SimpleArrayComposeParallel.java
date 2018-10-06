package qut;

import edu.au.jacobi.pattern.Series;
import jaligner.BLOSUM62;
import jaligner.matrix.Matrix;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;


public class SimpleArrayComposeParallel extends Sequential{
    private static ReentrantLock lock = new ReentrantLock(true);

    private static final Matrix BLOSUM_62 = BLOSUM62.Load();
//    private static HashMap<String, Sigma70Consensus> consensus = new HashMap<String, Sigma70Consensus>();
    private static byte[] complement = new byte['z'];
    private static Series sigma70_pattern = Sigma70Definition.getSeriesAll_Unanchored(0.7);

    static {
        complement['C'] = 'G';
        complement['c'] = 'g';
        complement['G'] = 'C';
        complement['g'] = 'c';
        complement['T'] = 'A';
        complement['t'] = 'a';
        complement['A'] = 'T';
        complement['a'] = 't';
    }


    protected static List<Gene> ParseReferenceGenes(String referenceFile) throws FileNotFoundException, IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(referenceFile)));
        List<Gene> referenceGenes = new ArrayList<Gene>();
        while (true) {
            String name = reader.readLine();
            if (name == null)
                break;
            String sequence = reader.readLine();
            referenceGenes.add(new Gene(name, 0, 0, sequence));
            consensus.put(name, new Sigma70Consensus());
        }
        consensus.put("all", new Sigma70Consensus());
        reader.close();
        return referenceGenes;
    }

    protected static List<String> ListGenbankFiles(String dir) {
        List<String> list = new ArrayList<String>();
        ProcessDir(list, new File(dir));
        return list;
    }

    protected static void ProcessDir(List<String> list, File dir) {
        if (dir.exists())
            for (File file : dir.listFiles())
                if (file.isDirectory())
                    ProcessDir(list, file);
                else
                    list.add(file.getPath());
    }

    protected static GenbankRecord Parse(String file) throws IOException {
        GenbankRecord record = new GenbankRecord();
        BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(file)));
        record.Parse(reader);
        reader.close();
        return record;
    }

    public static void run(String referenceFile, String dir) throws IOException {
        List<Gene> referenceGenes = ParseReferenceGenes(referenceFile);
        List<GenbankRecord> records = new ArrayList<>();

        for (String filename : ListGenbankFiles(dir))
            records.add(Parse(filename));

        for (GenbankRecord record : records) {
            for (Gene referenceGene : referenceGenes) {
                for (Gene gene : record.genes)
                    new Thread(new FindGeneRunnable(lock, gene, referenceGene, record, consensus)).start();
            }
        }
    }

    public static void main(String[] args) throws IOException, InterruptedException {
        Object SYN_LOCK = new Object();
        long startTime = System.nanoTime();
        int synTimeOut = 2000;

        run("referenceGenes.list", "Ecoli");
        synchronized (SYN_LOCK) {
            SYN_LOCK.wait(synTimeOut);
        }
        long endTime = System.nanoTime();
        long timeElapsed = endTime - startTime - (synTimeOut * 1000000 );

        for (Map.Entry<String, Sigma70Consensus> entry : consensus.entrySet())
            System.out.println(entry.getKey() + " " + entry.getValue());

        System.out.println("Executing time in seconds: " + df2.format(timeElapsed/ 1000000000.0));
    }
}
//78