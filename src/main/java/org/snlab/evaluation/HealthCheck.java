package org.snlab.evaluation;

import org.snlab.evaluation.others.APVerifier;
import org.snlab.evaluation.others.AtomVerifier;
import org.snlab.flash.ModelManager.Changes;
import org.snlab.flash.ModelManager.InverseModel;
import org.snlab.flash.ModelManager.Ports.ArrayPorts;
import org.snlab.flash.ModelManager.Ports.PersistentPorts;
import org.snlab.network.Network;
import org.snlab.network.Rule;
import org.snlab.networkLoader.Airtel1Network;
import org.snlab.networkLoader.I2Network;
import org.snlab.networkLoader.StanfordNetwork;

import java.util.ArrayList;
import java.util.Collections;

public class HealthCheck {
    public static void run() {
        healthCheck(I2Network.getNetwork().setName("Internet2"));
        healthCheck(StanfordNetwork.getNetwork().setName("Stanford"));
        // healthCheck(Airtel1Network.getNetwork().setName("Airtel1"));
    }

    private static void healthCheck(Network network) {
        // For every snapshot, APVerifier and FIMT should generate the same number of ECs.
        // The #Atom can be larger. But with minimization, it should be equal to #ECs.
        // (Notice the minimization is expensive, we only use it in health-check)
        System.gc();
        System.out.println(network.getName() + ": # Rules: " + network.getInitialRules().size() +
                " # Switches: " + network.getAllDevices().size());

        APVerifier APVerifier = new APVerifier(network, new ArrayPorts());
        InverseModel FIMT = new InverseModel(network, new PersistentPorts());
        AtomVerifier AtomVerifier = new AtomVerifier();

        int cnt = 0;
        for (Rule rule : network.getInitialRules()) {
            cnt ++;

            APVerifier.insertRule(rule);
            APVerifier.update();

            AtomVerifier.insertRule(rule);

            Changes changes = FIMT.insertMiniBatch(new ArrayList<>(Collections.singletonList(rule)));
            FIMT.update(changes);

            if (APVerifier.predSize() != FIMT.predSize()) {
                System.out.println("Something wrong at " + cnt + " updates while #ECs of APVerifier, FIMT = (" + APVerifier.predSize() + ", " + FIMT.predSize() + ")");
            }
        }
        if (AtomVerifier.checkPECSize() != APVerifier.predSize()) {
            System.out.println("Something wrong about AtomVerifier");
            System.out.println("#PEC, #EC = " + AtomVerifier.checkPECSize() + ", " + APVerifier.predSize());
        }
        for (Rule rule : network.getInitialRules()) {
            cnt ++;

            APVerifier.removeRule(rule);
            APVerifier.update();

            AtomVerifier.removeRule(rule);

            Changes changes = FIMT.miniBatch(new ArrayList<>(), new ArrayList<>(Collections.singletonList(rule)));
            FIMT.update(changes);

            if (APVerifier.predSize() != FIMT.predSize()) {
                System.out.println("Something wrong at " + cnt + " updates while #ECs of APVerifier, FIMT = (" + APVerifier.predSize() + ", " + FIMT.predSize() + ")");
            }
        }
        if (AtomVerifier.checkPECSize() != APVerifier.predSize()) {
            System.out.println("Something wrong about AtomVerifier");
            System.out.println("#PEC, #EC = " + AtomVerifier.checkPECSize() + ", " + APVerifier.predSize());
        }
    }
}
