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

public class Figure9 {
    private static final boolean testDeletion = true;
    private static final int warmup = 0, test = 1;

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
        final long ratio = 1000L * network.getInitialRules().size() * (testDeletion ? 2 : 1) * test;

        double s;
        int tot = network.getInitialRules().size(), b = tot + 1, cnt = 0;
        for (int size = 1; size <= tot; size ++) {
            s = 0;

            if (size > 100 && size != tot) {
                if ((tot / size) < b) {
                    b = tot / size;
                    cnt ++;
                } else {
                    continue;
                }
            }

            System.out.println("==================== Size " + size + " ==================== ");
            for (int i = 0; i < warmup; i ++) {
                if (network.getName().equals("Airtel1")) {
                    testWithBatchSizePrime(network, size);
                } else {
                    testWithBatchSize(network, size);
                }
            }
            System.out.println("==================== Loaded ==================== ");
            for (int i = 0; i < test; i ++) {
                if (network.getName().equals("Airtel1")) {
                    testWithBatchSizePrime(network, size);
                } else {
                    testWithBatchSize(network, size);
                }
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
            System.out.println("#EC: " + verifier.predSize());
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
        System.out.println("#EC: " + verifier.predSize());
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
