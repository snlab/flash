package org.snlab.evaluation;

import org.snlab.evaluation.others.APVerifier;
import org.snlab.evaluation.others.AtomVerifier;
import org.snlab.evaluation.others.Checker;
import org.snlab.flash.ModelManager.Ports.ArrayPorts;
import org.snlab.flash.ModelManager.Ports.Ports;
import org.snlab.flash.ModelManager.Ports.PersistentPorts;
import org.snlab.flash.ModelManager.Changes;
import org.snlab.flash.ModelManager.InverseModel;
import org.snlab.network.Network;
import org.snlab.network.Port;
import org.snlab.network.Rule;
import org.snlab.networkLoader.Airtel1Network;
import org.snlab.networkLoader.I2Network;
import org.snlab.networkLoader.StanfordNetwork;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;

/**
 * 1. model construction time/memory;
 * 2. all pair reachability (time);
 * 3. batch size;
 * 4. update event.
 */

public class Table3 {
    private static final double byte2MB = 1024L * 1024L;
    private static final boolean testDeletion = true;
    private static final int warmup = 0, test = 1;

    private static double memoryBefore, ratio;

    public static void run() {
        evaluateOn(I2Network.getNetwork().setName("Internet2"));
        evaluateOn(StanfordNetwork.getNetwork().setName("Stanford"));
        evaluateOn(Airtel1Network.getNetwork().setName("Airtel1"));


        /*
        network = LNetNetwork.LNetNetwork.getLNET1().setName("LNet*");
        System.out.println("# Rules: " + network.getInitialRules().size() + " # Switches: " + network.getAllDevices().size());
        // network.filterIntoSubsapce(((1L << 8) + 1L) << 8, ((1L << 16) - 1) << 8); // for delta-net
        network.filterIntoSubsapce(1L << 24, ((1L << 8) - 1) << 24); // for delta-net
        evaluateOn(network);


        network = LNetNetwork.getNetworkSrcHackMore().setName("FB1");
        System.out.println("# Rules: " + network.getInitialRules().size() + " # Switches: " + network.getAllDevices().size());
        // network.filterIntoSubsapce(((1L << 8) + 1L) << 16, ((1L << 16) - 1) << 16); // SrcHackMore
        network.filterIntoSubsapce(1L << 24, ((1L << 8) - 1) << 24); // SrcHackMore
        evaluateOn(network);

        network = LNetNetwork.getNetwork().setName("FB");
        System.out.println("# Rules: " + network.getInitialRules().size() + " # Switches: " + network.getAllDevices().size());
        // network.filterIntoSubsapce(((1L << 8) + 1L) << 16, ((1L << 16) - 1) << 16); // SrcHackMore
        network.filterIntoSubsapce(1L << 24, ((1L << 8) - 1) << 24); // SrcHackMore
        evaluateOn(network);
         */

        /*
        network = null;
        System.gc();
        System.runFinalization();
        DeltanetEval.main(null);
         */
    }

    public static void evaluateOn(Network network) {
        System.gc();
        System.runFinalization();
        System.out.println("# Rules: " + network.getInitialRules().size() + " # Switches: " + network.getAllDevices().size());
        memoryBefore = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
        System.out.println("Memory usage (storing network): " + (memoryBefore / byte2MB) + " M");

        healthCheck(network);

        ratio = 1000L * network.getInitialRules().size() * (testDeletion ? 2 : 1) * test;
        overall(network);

        batchSize(network);
        // checkAllPair(network);
    }

    private static void checkAllPair(Network network) throws IOException {
        InverseModel ver1 = new InverseModel(network, new PersistentPorts());
        Changes changes = ver1.insertMiniBatch(network.getInitialRules());
        ver1.update(changes);

        PrintWriter printWriter = new PrintWriter(new FileWriter("all-pair.txt", true));
        printWriter.println();
        printWriter.println();

        double s;
        HashMap<Port, HashSet<Integer>> model = ver1.getPortToPredicate();

        s = 0;
        for (int i = 0; i < warmup; i ++) model = ver1.getPortToPredicate();
        s -= System.nanoTime();
        for (int i = 0; i < test; i ++) model = ver1.getPortToPredicate();
        s += System.nanoTime();
        printWriter.println(network.getName() + " convert Ports to PortToInteger: " + (s / ratio) + " us amoritized per-update.");
        printWriter.println(network.getName() + " convert Ports to PortToInteger: " + (s / test) + " ns total.");
        printWriter.close();

        printWriter = new PrintWriter(new FileWriter("all-pair.txt", true));
        for (int i = 0; i < warmup; i ++) Checker.allPair(network, model);
        s = 0;
        for (int i = 0; i < test; i ++) s += Checker.allPair(network, model);
        printWriter.println(network.getName() + " all-pair and loop: " + (s / ratio) + " us amoritized per-update.");
        printWriter.println(network.getName() + " all-pair and loop: " + (s / test) + " ns total.");
        printWriter.close();

        printWriter = new PrintWriter(new FileWriter("all-pair.txt", true));
        AtomVerifier ver2 = new AtomVerifier();
        for (Rule rule : network.getInitialRules()) ver2.insertRule(rule);
        s = 0;
        for (int i = 0; i < warmup; i ++) ver2.checkPECSize();
        s -= System.nanoTime();
        for (int i = 0; i < test; i ++) ver2.checkPECSize();
        s += System.nanoTime();
        printWriter.println(network.getName() + " atom to ECs: " + (s / ratio) + " us amoritized per-update.");
        printWriter.println(network.getName() + " atom to ECs: " + (s / test) + " ns total.");
        printWriter.close();

        printWriter.println(" ======  #Atoms: " + ver2.atomSize() + " #ECs: " + ver1.predSize() + " ====== ");
        model = ver2.getPortToPredicate();
        printWriter = new PrintWriter(new FileWriter("all-pair.txt", true));
        for (int i = 0; i < warmup; i ++) Checker.allPair(network, model);
        s = 0;
        for (int i = 0; i < test; i ++) s += Checker.allPair(network, model);
        printWriter.println(network.getName() + " all-pair on atom " + (s / ratio) + " us amoritized per-update.");
        printWriter.println(network.getName() + " all-pair on atom " + s + " us total.");
        printWriter.close();
    }

    private static void batchSize(Network network) {
        double s;

        int tot = network.getInitialRules().size(), b = tot + 1, cnt = 0;
        for (int size = 1; size <= tot; size ++) {
            s = 0;

            /*
            if ((tot / size) < b) {
                b = tot / size;
                cnt ++;
            } else {
                continue;
            }
             */

            if (size > 100 && size != tot) continue;

            System.out.println("==================== Size " + size + " ==================== ");
            for (int i = 0; i < warmup; i ++) testWithBatchSize(network, size);
            System.out.println("==================== Loaded ==================== ");
            for (int i = 0; i < test; i ++) {
                s += testWithBatchSize(network, size);
                System.gc();
            }
            System.out.println("==================== Ended ==================== ");

            FileWriter fileWriter = null;
            try {
                fileWriter = (size == 1) ? new FileWriter(network.getName() + "bPuUs.txt") :
                        new FileWriter(network.getName() + "bPuUs.txt", true);
            } catch (IOException e) {
                e.printStackTrace();
            }
            assert fileWriter != null;
            PrintWriter printWriter = new PrintWriter(fileWriter);
            printWriter.println(size + " " + (s / ratio));
            printWriter.close();
        }
    }

    private static double testWithBatchSize(Network network, int size) {
        System.gc();

        InverseModel verifier = new InverseModel (network, new PersistentPorts());
        ArrayList<Rule> rules = new ArrayList<>();

        int cnt = 0;
        for (Rule rule : network.getInitialRules()) {
            rules.add(rule);
            cnt ++;
            if (cnt % size == 0) {
                Changes changes = verifier.insertMiniBatch(rules);
                verifier.update(changes);
                rules.clear();
            }
        }
        if (rules.size() > 0) {
            Changes changes = verifier.insertMiniBatch(rules);
            verifier.update(changes);
            rules.clear();
        }
        if (testDeletion) {
            cnt = 0;
            System.out.println("Jiffy #EC: " + verifier.predSize());
            for (Rule rule : network.getInitialRules()) {
                rules.add(rule);
                cnt ++;
                if (cnt % size == 0) {
                    Changes changes = verifier.miniBatch(new ArrayList<>(), rules);
                    verifier.update(changes);
                    rules.clear();
                }
            }
            if (rules.size() > 0) {
                Changes changes = verifier.miniBatch(new ArrayList<>(), rules);
                verifier.update(changes);
                rules.clear();
            }
        }
        System.out.println("Jiffy #EC: " + verifier.predSize());
        printMemory();
        return verifier.printTime(network.getInitialRules().size());
    }

    private static double s1, s2, s3, s4, s5;
    private static double t1, t2, t3, t4, t5;

    private static void overall(Network network) {
        System.out.println("+++++++++++++++++++++ " + network.getName() + " +++++++++++++++++++++");
        s1 = s2 = s3 = s4 = s5 = 0;
        t1 = t2 = t3 = t4 = t5 = 0;

        for (int i = 0; i < warmup; i ++) seq(network, true);
        System.out.println("==================== Loaded ==================== ");
        for (int i = 0; i < test; i ++) s3 += seq(network, true);
        System.out.println("==================== Ended ==================== ");
        for (int i = 0; i < warmup; i ++) deltanet(network);
        System.out.println("==================== Loaded ==================== ");
        for (int i = 0; i < test; i ++) s1 += deltanet(network);
        System.out.println("==================== Ended ==================== ");
        for (int i = 0; i < warmup; i ++) seq(network, false);
        System.out.println("==================== Loaded ==================== ");
        for (int i = 0; i < test; i ++) s5 += seq(network, false);
        System.out.println("==================== Ended ==================== ");
        for (int i = 0; i < warmup; i ++) apkeep(network, new ArrayPorts());
        System.out.println("==================== Loaded ==================== ");
        for (int i = 0; i < test; i ++) s2 += apkeep(network, new ArrayPorts());
        System.out.println("==================== Ended ==================== ");

        FileWriter fileWriter = null;
        try {
            fileWriter = new FileWriter("overall.txt", true);
        } catch (IOException e) {
            e.printStackTrace();
        }
        assert fileWriter != null;
        PrintWriter printWriter = new PrintWriter(fileWriter);
        printWriter.println();
        printWriter.println();
        printWriter.println();
        printWriter.println();
        printWriter.println(network.getName() + " Amoritized time: (deltanet, apkeep, jiffy, jiffy w/o imt) = (" +
                (s1 / ratio) + ", " + (s2 / ratio) + ", " + (s3 / ratio) + ", " + (s5 /ratio) + " ) us.");
        printWriter.println(network.getName() + " Total time: (deltanet, apkeep, jiffy, jiffy w/o imt) = (" +
                (s1 / test) + ", " + (s2 / test) + ", " + (s3 / test) + ", " + (s5 / test) + " ) ns.");
        printWriter.println(network.getName() + " Operations: (deltanet, apkeep, jiffy, jiffy w/o imt) = (" +
                (t1 / (warmup + test)) + ", " + (t2 / (warmup + test)) + ", " + (t3 / (warmup + test)) + ", " + (t5 / (warmup + test)) + " ).");
        printWriter.close();

        System.out.println("+++++++++++++++++++++ END " + network.getName() + " END +++++++++++++++++++++");
    }

    private static void printMemory() {
        System.gc();
        double memoryAfter = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
        System.out.println("Memory usage (verification): " + ((memoryAfter - memoryBefore) / byte2MB) + " M");
    }

    private static void healthCheck(Network network) {
        System.gc();

        APVerifier APVerifier = new APVerifier(network, new ArrayPorts());
        InverseModel FIMT = new InverseModel(network, new PersistentPorts());

        int cnt = 0;
        for (Rule rule : network.getInitialRules()) {
            cnt ++;

            APVerifier.insertRule(new Rule(rule.getDevice(), rule.getMatch().longValue(), rule.getPrefix(), rule.getOutPort()));
            APVerifier.update();

            Changes changes = FIMT.insertMiniBatch(new ArrayList<>(Collections.singletonList(rule)));
            FIMT.update(changes);

            if (APVerifier.predSize() != FIMT.predSize()) {
                System.out.println("Error at " + cnt + " updates while #ECs of APVerifier, FIMT = (" + APVerifier.predSize() + ", " + FIMT.predSize() + ")");
            }
        }
        for (Rule rule : network.getInitialRules()) {
            cnt ++;

            APVerifier.removeRule(new Rule(rule.getDevice(), rule.getMatch().longValue(), rule.getPrefix(), rule.getOutPort()));
            APVerifier.update();

            Changes changes = FIMT.miniBatch(new ArrayList<>(), new ArrayList<>(Collections.singletonList(rule)));
            FIMT.update(changes);

            if (APVerifier.predSize() != FIMT.predSize()) {
                System.out.println("Error at " + cnt + " updates while #ECs of APVerifier, FIMT = (" + APVerifier.predSize() + ", " + FIMT.predSize() + ")");
            }
        }
    }

    private static double deltanet(Network network) {
        System.gc();

        AtomVerifier verifier = new AtomVerifier();
        for (Rule rule : network.getInitialRules()) {
            verifier.insertRule(rule);
        }
        System.out.println("#Atom: " + verifier.atomSize());
        // System.out.println("Delta-net #EC: " + verifier.checkPECSize());
        if (testDeletion) {
            for (Rule rule : network.getInitialRules()) {
                verifier.removeRule(rule);
            }
            System.out.println("#Atom: " + verifier.atomSize());
            // System.out.println("Delta-net #EC: " + verifier.checkPECSize());
        }
        printMemory();
        t1 += verifier.opCnt;
        return verifier.printTime(network.getInitialRules().size() * (testDeletion ? 2 : 1));
    }

    private static double apkeep(Network network, Ports base) {
        System.gc();
        APVerifier verifier = new APVerifier(network, base);
        for (Rule rule : network.getInitialRules()) {
            verifier.insertRule(rule);
            verifier.update(true);
        }
        System.out.println("APKeep #EC: " + verifier.predSize());
        if (testDeletion) {
            for (Rule rule : network.getInitialRules()) {
                verifier.removeRule(rule);
                verifier.update(true);
            }
            System.out.println("APKeep #EC: " + verifier.predSize());
        }
        printMemory();
        t2 += verifier.bddEngine.opCnt;
        return verifier.printTime(network.getInitialRules().size() * (testDeletion ? 2 : 1));
    }

    private static double seq(Network network, boolean useFFMT) {
        System.gc();
        // JiffyVerifier verifier = new JiffyVerifier(network, new ArrayPorts());
        InverseModel verifier = new InverseModel(network, new PersistentPorts());
        long s = System.nanoTime();
        if (useFFMT) {
            Changes changes = verifier.insertMiniBatch(network.getInitialRules());
            verifier.update(changes);
            // verifier.printTime(network.getInitialRules().size() * (testDeletion ? 2 : 1));
            if (testDeletion) {
                System.out.println("Jiffy #EC: " + verifier.predSize() + " with FFMT");
                changes = verifier.miniBatch(new ArrayList<>(), network.getInitialRules());
                verifier.update(changes);
            }
            t3 += verifier.bddEngine.opCnt;
        } else {
            for (Rule rule : network.getInitialRules()) {
                Changes changes = verifier.insertMiniBatch(new ArrayList<>(Collections.singletonList(rule)));
                verifier.update(changes);
            }
            if (testDeletion) {
                System.out.println("Jiffy #EC: " + verifier.predSize() + " w/o FFMT");
                for (Rule rule : network.getInitialRules()) {
                    Changes changes = verifier.miniBatch(new ArrayList<>(), new ArrayList<>(Collections.singletonList(rule)));
                    verifier.update(changes);
                }
            }
            t5 += verifier.bddEngine.opCnt;
        }
        System.out.println("Jiffy #EC: " + verifier.predSize() + (useFFMT ? " with FFMT" : " w/o FFMT"));
        printMemory();
        return verifier.printTime(network.getInitialRules().size() * (testDeletion ? 2 : 1));
        // Checker.allPair(network, new PPM(verifier.portsToPredicate).getPortToPreds());
    }
}
