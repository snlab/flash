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
import org.snlab.networkLoader.I2Network;
import org.snlab.networkLoader.LNetNetwork;
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
    private static final int warmupRepeat = 0, testRepeat = 1;

    private static double memoryBefore, ratio;

    public static void run() {
        evaluateOn(I2Network.getNetwork().setName("Internet2"));
        evaluateOn(StanfordNetwork.getNetwork().setName("Stanford"));
        // evaluateOn(Airtel1Network.getNetwork().setName("Airtel1")); FIXME see deltanet eval

        Network network = LNetNetwork.getLNET().setName("LNet0");
        network.filterIntoSubsapce(1L << 24, ((1L << 8) - 1) << 24);
        evaluateOn(network);

        network = LNetNetwork.getLNET1().setName("LNet1");
        network.filterIntoSubsapce(1L << 24, ((1L << 8) - 1) << 24); // add src match
        evaluateOn(network);

        network = LNetNetwork.getLNET1().setName("LNet1");
        network.filterIntoSubsapce(1L << 24, ((1L << 8) - 1) << 24); // add src match
        evaluateOn(network);

        network = null;
        System.gc();
        System.runFinalization();
    }

    public static void evaluateOn(Network network) {
        System.gc();
        System.runFinalization();
        System.out.println("# Rules: " + network.getInitialRules().size() + " # Switches: " + network.getAllDevices().size());
        memoryBefore = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
        System.out.println("Memory usage (storing network): " + (memoryBefore / byte2MB) + " M");

        overall(network);
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
        for (int i = 0; i < warmupRepeat; i ++) model = ver1.getPortToPredicate();
        s -= System.nanoTime();
        for (int i = 0; i < testRepeat; i ++) model = ver1.getPortToPredicate();
        s += System.nanoTime();
        printWriter.println(network.getName() + " convert Ports to PortToInteger: " + (s / ratio) + " us amoritized per-update.");
        printWriter.println(network.getName() + " convert Ports to PortToInteger: " + (s / testRepeat) + " ns total.");
        printWriter.close();

        printWriter = new PrintWriter(new FileWriter("all-pair.txt", true));
        for (int i = 0; i < warmupRepeat; i ++) Checker.allPair(network, model);
        s = 0;
        for (int i = 0; i < testRepeat; i ++) s += Checker.allPair(network, model);
        printWriter.println(network.getName() + " all-pair and loop: " + (s / ratio) + " us amoritized per-update.");
        printWriter.println(network.getName() + " all-pair and loop: " + (s / testRepeat) + " ns total.");
        printWriter.close();

        printWriter = new PrintWriter(new FileWriter("all-pair.txt", true));
        AtomVerifier ver2 = new AtomVerifier();
        for (Rule rule : network.getInitialRules()) ver2.insertRule(rule);
        s = 0;
        for (int i = 0; i < warmupRepeat; i ++) ver2.checkPECSize();
        s -= System.nanoTime();
        for (int i = 0; i < testRepeat; i ++) ver2.checkPECSize();
        s += System.nanoTime();
        printWriter.println(network.getName() + " atom to ECs: " + (s / ratio) + " us amoritized per-update.");
        printWriter.println(network.getName() + " atom to ECs: " + (s / testRepeat) + " ns total.");
        printWriter.close();

        printWriter.println(" ======  #Atoms: " + ver2.atomSize() + " #ECs: " + ver1.predSize() + " ====== ");
        model = ver2.getPortToPredicate();
        printWriter = new PrintWriter(new FileWriter("all-pair.txt", true));
        for (int i = 0; i < warmupRepeat; i ++) Checker.allPair(network, model);
        s = 0;
        for (int i = 0; i < testRepeat; i ++) s += Checker.allPair(network, model);
        printWriter.println(network.getName() + " all-pair on atom " + (s / ratio) + " us amoritized per-update.");
        printWriter.println(network.getName() + " all-pair on atom " + s + " us total.");
        printWriter.close();
    }

    private static double s1, s2, s3, s4;
    private static double t1, t2, t3, t4;
    private static double m1, m2, m3, m4;

    private static void overall(Network network) { // Table 3
        s1 = s2 = s3 = s4 = 0;
        t1 = t2 = t3 = t4 = 0;
        System.out.println("+++++++++++++++++++++ " + network.getName() + " +++++++++++++++++++++");
        for (int i = 0; i < warmupRepeat; i ++) deltanet(network);
        System.out.println("==================== Loaded ==================== ");
        for (int i = 0; i < testRepeat; i ++) s1 += deltanet(network);
        System.out.println("==================== Ended ==================== ");
        for (int i = 0; i < warmupRepeat; i ++) apkeep(network, new ArrayPorts());
        System.out.println("==================== Loaded ==================== ");
        for (int i = 0; i < testRepeat; i ++) s2 += apkeep(network, new ArrayPorts());
        System.out.println("==================== Ended ==================== ");
        for (int i = 0; i < warmupRepeat; i ++) seq(network, true);
        System.out.println("==================== Loaded ==================== ");
        for (int i = 0; i < testRepeat; i ++) s3 += seq(network, true);
        System.out.println("==================== Ended ==================== ");
        for (int i = 0; i < warmupRepeat; i ++) seq(network, false);
        System.out.println("==================== Loaded ==================== ");
        for (int i = 0; i < testRepeat; i ++) s4 += seq(network, false);
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
        printWriter.println(network.getName() + " # Rules: " + network.getInitialRules().size() + " # Switches: " + network.getAllDevices().size());
        printWriter.println("deltanet, apkeep, jiffy, jiffy w/o imt");
        ratio = 1e9  * (testDeletion ? 2 : 1) * testRepeat;
        printWriter.println(" Total time: (" + (s1 / ratio) + ", " + (s2 / ratio) + ", " + (s3 / ratio) + ", " + (s4 / ratio) + " ) s.");
        double repeats = warmupRepeat + testRepeat, operationRatio = 1e5 * repeats;
        printWriter.println(" Operations: (" + (t1 / operationRatio) + ", " + (t2 / operationRatio) + ", " + (t3 / operationRatio) + ", " + (t4 / operationRatio) + " ) 1e5.");
        printWriter.println(" Memory Usage: (" + (m1 / repeats) + ", " + (m2 / repeats) + ", " + (m3 / repeats) + ", " + (m4 / repeats) + " ) Mb.");
        printWriter.close();

        System.out.println("+++++++++++++++++++++ END " + network.getName() + " END +++++++++++++++++++++");
    }

    private static double printMemory() {
        System.gc();
        double memoryAfter = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory(), ret = ((memoryAfter - memoryBefore) / byte2MB);
        System.out.println("Memory usage (verification): " + ret + " M");
        return ret;
    }

    private static double deltanet(Network network) {
        System.gc();
        AtomVerifier verifier = new AtomVerifier();
        for (Rule rule : network.getInitialRules()) {
            verifier.insertRule(rule);
        }
        System.out.println("Deltanet* #Atom (full snapshot): " + verifier.atomSize());
        if (testDeletion) {
            for (Rule rule : network.getInitialRules()) {
                verifier.removeRule(rule);
            }
            System.out.println("Deltanet* #Atom (deleted to empty): " + verifier.atomSize());
        }
        m1 += printMemory();
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
        System.out.println("APKeep* #EC (full snapshot): " + verifier.predSize());
        if (testDeletion) {
            for (Rule rule : network.getInitialRules()) {
                verifier.removeRule(rule);
                verifier.update(true);
            }
            System.out.println("APKeep* #EC (deleted to empty): " + verifier.predSize());
        }
        m2 += printMemory();
        t2 += verifier.bddEngine.opCnt;
        return verifier.printTime(network.getInitialRules().size() * (testDeletion ? 2 : 1));
    }

    private static double seq(Network network, boolean useFFMT) {
        System.gc();
        InverseModel verifier = new InverseModel(network, new PersistentPorts());
        if (useFFMT) {
            Changes changes = verifier.insertMiniBatch(network.getInitialRules());
            verifier.update(changes);
            if (testDeletion) {
                System.out.println("Flash #EC (full snapshot): " + verifier.predSize() + " with FFMT");
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
                System.out.println("Flash #EC (deleted to empty): " + verifier.predSize() + " w/o FFMT");
                for (Rule rule : network.getInitialRules()) {
                    Changes changes = verifier.miniBatch(new ArrayList<>(), new ArrayList<>(Collections.singletonList(rule)));
                    verifier.update(changes);
                }
            }
            t4 += verifier.bddEngine.opCnt;
        }
        System.out.println("Flash #EC: " + verifier.predSize() + (useFFMT ? " with FFMT" : " w/o FFMT"));
        if (useFFMT) {
            m3 += printMemory();
        } else {
            m4 += printMemory();
        }
        return verifier.printTime(network.getInitialRules().size() * (testDeletion ? 2 : 1));
    }
}
