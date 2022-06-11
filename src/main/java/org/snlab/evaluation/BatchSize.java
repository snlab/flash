package org.snlab.evaluation;

import org.jgrapht.alg.util.Pair;
import org.snlab.flash.ModelManager.ConflictFreeChanges;
import org.snlab.flash.ModelManager.InverseModel;
import org.snlab.flash.ModelManager.Ports.PersistentPorts;
import org.snlab.network.Network;
import org.snlab.network.Rule;
import org.snlab.networkLoader.Airtel1Network;
import org.snlab.networkLoader.I2Network;
import org.snlab.networkLoader.LNetNetwork;
import org.snlab.networkLoader.StanfordNetwork;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;

public class BatchSize {
    private static final boolean testDeletion = true;
    private static final int warmupRepeat = 3, testRepeat = 1;

    public static void run() {
        Network network = LNetNetwork.getLNET().setName("LNet0");
        network.filterIntoSubsapce(1L << 24, ((1L << 8) - 1) << 24);
        batchSize(network);

        network = LNetNetwork.getLNET1().setName("LNet1");
        network.filterIntoSubsapce(1L << 24, ((1L << 8) - 1) << 24);
        batchSize(network);

        network = LNetNetwork.getLNETStar().setName("LNet*");
        network.filterIntoSubsapce(1L << 24, ((1L << 8) - 1) << 24);
        batchSize(network);

        try {
            batchSize(Airtel1Network.getNetwork().setName("Airtel1"));
        } catch (IOException e) {
            e.printStackTrace();
        }

        batchSize(StanfordNetwork.getNetwork().setName("Stanford"));

        batchSize(I2Network.getNetwork().setName("Internet2"));
    }

    private static void batchSize(Network network) {
        System.out.println("==================== " + network.getName());
        FileWriter fileWriter = null;
        try {
            fileWriter = new FileWriter(network.getName() + "bPuUs.txt");
        } catch (IOException e) {
            e.printStackTrace();
        }
        assert fileWriter != null;
        PrintWriter printWriter = new PrintWriter(fileWriter);
        if (network.getName().equals("Airtel1")) {
            printWriter.println(network.updateSequence.size());
        } else {
            printWriter.println(network.getInitialRules().size() * (testDeletion ? 2 : 1));
        }
        printWriter.close();

        final double ratio = 1e9  * (testDeletion ? 2 : 1) * testRepeat;

        double s;
        int tot = network.getInitialRules().size(), b = tot + 1, cnt = 0;

        for (int i = 1; i <= warmupRepeat; i ++) {
            if (network.getName().equals("Airtel1")) {
                testWithBatchSizePrime(network, i);
            } else {
                testWithBatchSize(network, i);
            }
        }
        System.out.println("==================== Warmed ==================== ");

        for (int size = 1; size <= tot; size ++) {
            if (cnt > 50 && size < tot) continue;
            if (size > 10 && size != tot) {
                if ((tot / size) < b) {
                    b = tot / size;
                } else {
                    continue;
                }
            }

            cnt ++;
            System.out.println("==================== Size " + size + " ==================== ");
            s = 0;
            for (int i = 0; i < testRepeat; i ++) {
                if (network.getName().equals("Airtel1")) {
                    s += testWithBatchSizePrime(network, size);
                } else {
                    s += testWithBatchSize(network, size);
                }
                System.gc();
            }
            System.out.println("==================== Ended ==================== ");

            try {
                fileWriter = new FileWriter(network.getName() + "bPuUs.txt", true);
            } catch (IOException e) {
                e.printStackTrace();
            }
            printWriter = new PrintWriter(fileWriter);
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
                ConflictFreeChanges conflictFreeChanges = verifier.insertMiniBatch(rules);
                verifier.update(conflictFreeChanges);
                rules.clear();
            }
        }
        if (rules.size() > 0) {
            ConflictFreeChanges conflictFreeChanges = verifier.insertMiniBatch(rules);
            verifier.update(conflictFreeChanges);
            rules.clear();
        }
        if (testDeletion) {
            cnt = 0;
            System.out.println("Flash #EC: " + verifier.predSize());
            for (Rule rule : network.getInitialRules()) {
                rules.add(rule);
                cnt ++;
                if (cnt % size == 0) {
                    ConflictFreeChanges conflictFreeChanges = verifier.miniBatch(new ArrayList<>(), rules);
                    verifier.update(conflictFreeChanges);
                    rules.clear();
                }
            }
            if (rules.size() > 0) {
                ConflictFreeChanges conflictFreeChanges = verifier.miniBatch(new ArrayList<>(), rules);
                verifier.update(conflictFreeChanges);
                rules.clear();
            }
        }
        System.out.println("Flash #EC: " + verifier.predSize());
        return verifier.printTime(network.getInitialRules().size());
    }


    private static double testWithBatchSizePrime(Network network, int size) {
        System.gc();
        InverseModel verifier = new InverseModel(network, new PersistentPorts());

        int cnt = 0;
        ArrayList<Rule> insertion = new ArrayList<>(), deletion = new ArrayList<>();
        for (Pair<Boolean, Rule> pair : network.updateSequence) {
            cnt ++;
            if (pair.getFirst()) insertion.add(pair.getSecond()); else deletion.add(pair.getSecond());

            if (cnt % size == 0) {
                ConflictFreeChanges conflictFreeChanges = verifier.miniBatch(insertion, deletion);
                verifier.update(conflictFreeChanges);

                insertion.clear();
                deletion.clear();
            }
        }
        if (cnt % size != 0) {
            ConflictFreeChanges conflictFreeChanges = verifier.miniBatch(insertion, deletion);
            verifier.update(conflictFreeChanges);
        }

        System.out.println("Flash #EC: " + verifier.predSize() + (" 100 batch"));
        // m4 += printMemory();
        // t4 += verifier.bddEngine.opCnt;
        return verifier.printTime(network.updateSequence.size());
    }
}
