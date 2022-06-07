package org.snlab.evaluation;

import java.math.BigInteger;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.snlab.flash.CE2D.AllPairChecker;
import org.snlab.flash.ModelManager.Changes;
import org.snlab.flash.ModelManager.InverseModel;
import org.snlab.flash.ModelManager.Ports.Ports;
import org.snlab.network.Device;
import org.snlab.network.Network;
import org.snlab.network.Rule;
import org.snlab.networkLoader.LNetNetwork;

public class LNet1AllPair {

    public static void run() {
        Network network = LNetNetwork.getLNET1();
        // The subspace
        long ip = (2 << 16);

        InverseModel model = new InverseModel(network);

        AllPairChecker policyChecker = new AllPairChecker(network, model.bddEngine,
                model.bddEngine.encodeIpv4(BigInteger.valueOf(ip), 16));
        System.out.println(network.getAllDevices().size());
        for (Device device : network.getAllDevices()) {
            if (policyChecker.foundBug)
                return;
            List<Rule> rulesInSubspace = device.getInitialRules().stream()
                    .filter(rule -> rule.getMatch().longValue() == ip)
                    .collect(Collectors.toList());
            Changes changes = model.insertMiniBatch(rulesInSubspace);
            Set<Integer> transfered = model.update(changes);
            
            long s = System.nanoTime();
            Map<Integer, Ports> ecToPorts = new HashMap<>();
            for (Map.Entry<Ports, Integer> entry : model.portsToPredicate.entrySet()) {
                ecToPorts.put(entry.getValue(), entry.getKey());
            }
            policyChecker.check(model.portsToPredicate.values(), device, ecToPorts, model.getPortToPredicate());
            System.out.println(System.nanoTime() - s);
        }
        System.out.println("#ECs: " + model.predSize());
    }
}
