package qut;

import java.io.IOException;
import java.text.DecimalFormat;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class TestRunner extends Sequential {
    public static int timesToRun = 20;
    public static int [] threadArray = {1, 2, 3, 4};


    public static void main(String[] args) throws IOException, ExecutionException, InterruptedException {
        double startTime, endTime, timeElapsed,  avgTime = 0;
        DecimalFormat df2 = new DecimalFormat(".##");

        // This is the main test - this is everything
        String testingOutput = "all Consensus: -35: T T G A C A gap: 17.6 -10: T A T A A T  (5430 matches)" +
                "fixB Consensus: -35: T T G A C A gap: 17.7 -10: T A T A A T  (965 matches)" +
                "carA Consensus: -35: T T G A C A gap: 17.7 -10: T A T A A T  (1079 matches)" +
                "fixA Consensus: -35: T T G A C A gap: 17.6 -10: T A T A A T  (896 matches)" +
                "caiF Consensus: -35: T T C A A A gap: 18.0 -10: T A T A A T  (11 matches)" +
                "caiD Consensus: -35: T T G A C A gap: 17.6 -10: T A T A A T  (550 matches)" +
                "yaaY Consensus: -35: T T G T C G gap: 18.0 -10: T A T A C T  (4 matches)" +
                "nhaA Consensus: -35: T T G A C A gap: 17.6 -10: T A T A A T  (1879 matches)" +
                "folA Consensus: -35: T T G A C A gap: 17.5 -10: T A T A A T  (46 matches)";

        // Just using this to test the output while building the testRunner
        // In the Ecoli folder - only have 'Escherichia_coli_BW2952_uid59391' folder
        // In the referenceGenes.list only have yaaY and nhaA data
        String xtestingOutput = "all Consensus: -35: T T G A C A gap: 17.5 -10: T A T A A T  (467 matches)" +
                "yaaY Consensus: -35: T T G T C G gap: 18.0 -10: T A T A C T  (1 matches)" +
                "nhaA Consensus: -35: T T G A C A gap: 17.5 -10: T A T A A T  (466 matches)";

        String runOutput = "";

        // Get Average Sequential time
        System.out.println("    --:Sequential(Original, 1 thread only):--");
        avgTime = 0.0;
        for (int runNumber = 0; runNumber < timesToRun; runNumber++) {
            startTime = System.nanoTime();

            Sequential.run("referenceGenes.list", "Ecoli");

            endTime = System.nanoTime();
            timeElapsed = endTime - startTime;
            avgTime += timeElapsed;

            System.out.print("Test #" + (runNumber+1) + "/" + timesToRun  + " " + df2.format(timeElapsed/ 1000000000) + " seconds");

            for (Map.Entry<String, Sigma70Consensus> entry : consensus.entrySet())
                runOutput += entry.getKey() + entry.getValue();

            if (testingOutput.equals(runOutput))
                System.out.println(" (Correct output)");
            else
                System.out.println(" *** OUTPUT IS FALSE ***");

            consensus.clear();
            runOutput = "";
        }
        avgTime /= timesToRun;
        System.out.println("Average runtime: "  + df2.format(avgTime / 1000000000) + "\n");
        //////////////////////////



        // Get Average Simple Parallel time
        int numberOfSimpleThreads = Runtime.getRuntime().availableProcessors();
        System.out.println("    --:Simple Parallel(Runs at max threads(" + numberOfSimpleThreads + ") ):--");
        avgTime = 0.0;
        for (int runNumber = 0; runNumber < timesToRun; runNumber++) {
            startTime = System.nanoTime();

            SimpleParallel.run("referenceGenes.list", "Ecoli");

            endTime = System.nanoTime();
            timeElapsed = endTime - startTime;
            avgTime += timeElapsed;

            System.out.print("Test #" + (runNumber+1) + "/" + timesToRun  + " " + df2.format(timeElapsed/ 1000000000) + " seconds");

            for (Map.Entry<String, Sigma70Consensus> entry : consensus.entrySet())
                runOutput += entry.getKey() + entry.getValue();

            if (testingOutput.equals(runOutput))
                System.out.println(" (Correct output)");
            else
                System.out.println(" *** OUTPUT IS FALSE ***");

            consensus.clear();
            runOutput = "";
        }
        avgTime /= timesToRun;
        System.out.println("Average runtime: "  + df2.format(avgTime / 1000000000) + "\n");
        //////////////////////////



        for(int numberOfThreads : threadArray) {
            System.out.println("Running on " + numberOfThreads + " threads.");
            System.out.println("Running " + timesToRun + " tests on each\n");


            // Get Average Executor Parallel time
            System.out.println("    --:ExecutorParallel (Running on " + numberOfThreads + " threads):--");
            avgTime = 0.0;
            for (int runNumber = 0; runNumber < timesToRun; runNumber++) {
                ExecutorService executorServiceP = Executors.newFixedThreadPool(numberOfThreads);
                startTime = System.nanoTime();

                ExecutorParallel.run("referenceGenes.list", "Ecoli", executorServiceP);

                executorServiceP.shutdown();
                try {
                    executorServiceP.awaitTermination(10, TimeUnit.MINUTES);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                executorServiceP = null;

                endTime = System.nanoTime();
                timeElapsed = endTime - startTime;
                avgTime += timeElapsed;

                System.out.print("Test #" + (runNumber + 1) + "/" + timesToRun + " " + df2.format(timeElapsed / 1000000000) + " seconds");

                for (Map.Entry<String, Sigma70Consensus> entry : consensus.entrySet())
                    runOutput += entry.getKey() + entry.getValue();

                if (testingOutput.equals(runOutput))
                    System.out.println(" (Correct output)");
                else
                    System.out.println(" *** OUTPUT IS FALSE ***");

                consensus.clear();
                runOutput = "";
            }
            avgTime /= timesToRun;
            System.out.println("Average runtime: " + df2.format(avgTime / 1000000000) + "\n");
            //////////////////////////


            // Get Average Executor Lambda Parallel time
            System.out.println("    --:ExecutorLambdaParallel (Running on " + numberOfThreads + " threads):--");
            avgTime = 0.0;

            for (int runNumber = 0; runNumber < timesToRun; runNumber++) {
                ExecutorService executorServiceLP = Executors.newFixedThreadPool(numberOfThreads);
                startTime = System.nanoTime();

                ExecutorLambdaParallel.run("referenceGenes.list", "Ecoli", executorServiceLP);

                executorServiceLP.shutdown();
                try {
                    executorServiceLP.awaitTermination(10, TimeUnit.MINUTES);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                executorServiceLP = null;

                endTime = System.nanoTime();
                timeElapsed = endTime - startTime;
                avgTime += timeElapsed;

                System.out.print("Test #" + (runNumber + 1) + "/" + timesToRun + " " + df2.format(timeElapsed / 1000000000) + " seconds");

                for (Map.Entry<String, Sigma70Consensus> entry : consensus.entrySet())
                    runOutput += entry.getKey() + entry.getValue();

                if (testingOutput.equals(runOutput))
                    System.out.println(" (Correct output)");
                else
                    System.out.println(" *** OUTPUT IS FALSE ***");

                consensus.clear();
                runOutput = "";
            }

            avgTime /= timesToRun;
            System.out.println("Average runtime: " + df2.format(avgTime / 1000000000) + "\n");
            //////////////////////////


            // Get Average Executor Lambda Callable time
            System.out.println("    --:ExecutorLambdaCallable (Running on " + numberOfThreads + " threads):--");
            avgTime = 0.0;
            ExecutorService executorServiceLC = Executors.newFixedThreadPool(numberOfThreads);
            for (int runNumber = 0; runNumber < timesToRun; runNumber++) {
                startTime = System.nanoTime();

                ExecutorLambdaCallable.run("referenceGenes.list", "Ecoli", executorServiceLC);

                endTime = System.nanoTime();
                timeElapsed = endTime - startTime;
                avgTime += timeElapsed;

                System.out.print("Test #" + (runNumber + 1) + "/" + timesToRun + " " + df2.format(timeElapsed / 1000000000) + " seconds");

                for (Map.Entry<String, Sigma70Consensus> entry : consensus.entrySet())
                    runOutput += entry.getKey() + entry.getValue();

                if (testingOutput.equals(runOutput))
                    System.out.println(" (Correct output)");
                else
                    System.out.println(" *** OUTPUT IS FALSE ***");

                consensus.clear();
                runOutput = "";
            }
            executorServiceLC.shutdown();
            avgTime /= timesToRun;
            System.out.println("Average runtime: " + df2.format(avgTime / 1000000000) + "\n");
            //////////////////////////


            // Get Average Executor Callable time
            System.out.println("    --:ExecutorCallable (Running on " + numberOfThreads + " threads):--");
            avgTime = 0.0;
            ExecutorService executorServiceC = Executors.newFixedThreadPool(numberOfThreads);
            for (int runNumber = 0; runNumber < timesToRun; runNumber++) {
                startTime = System.nanoTime();

                ExecutorLambdaCallable.run("referenceGenes.list", "Ecoli", executorServiceC);

                endTime = System.nanoTime();
                timeElapsed = endTime - startTime;
                avgTime += timeElapsed;

                System.out.print("Test #" + (runNumber + 1) + "/" + timesToRun + " " + df2.format(timeElapsed / 1000000000) + " seconds");

                for (Map.Entry<String, Sigma70Consensus> entry : consensus.entrySet())
                    runOutput += entry.getKey() + entry.getValue();

                if (testingOutput.equals(runOutput))
                    System.out.println(" (Correct output)");
                else
                    System.out.println(" *** OUTPUT IS FALSE ***");

                consensus.clear();
                runOutput = "";
            }
            executorServiceC.shutdown();
            avgTime /= timesToRun;
            System.out.println("Average runtime: " + df2.format(avgTime / 1000000000));
            //////////////////////////
        }
    }
}
