package org.snlab.evaluation;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.jgrapht.Graph;
import org.jgrapht.graph.DefaultDirectedGraph;
import org.snlab.flash.Dispatcher;
import org.snlab.flash.CE2D.PGEdge;
import org.snlab.flash.CE2D.PropertyChecker;
import org.snlab.flash.ModelManager.ConflictFreeChanges;
import org.snlab.flash.ModelManager.InverseModel;
import org.snlab.flash.ModelManager.Ports.Ports;
import org.snlab.network.Device;
import org.snlab.network.Network;
import org.snlab.network.Rule;
import org.snlab.networkLoader.LNetNetwork;

public class LNet1AllPair {

    public static void run() {
        Network network = LNetNetwork.getLNET1();
        pgCheck(network);
        Dispatcher.logger.writeFile();
    }

    public static void pgCheck(Network network) {
        long ip = (111 << 24) + (1 << 16);
        Graph<Device, PGEdge> pg = buildPG(network);

        InverseModel verifier = new InverseModel(network);

        int hs = verifier.bddEngine.encodeIpv4(BigInteger.valueOf(ip), 16);
        PropertyChecker policyChecker = new PropertyChecker(network, pg, verifier.bddEngine, hs);
        Dispatcher.logger.startAt = System.nanoTime();

        List<Device> allDevices = new ArrayList<>(network.getAllDevices());
        Collections.shuffle(allDevices);
        int i = 0;
        for (Device device : allDevices) {
            List<Rule> rulesInSubspace = device.getInitialRules().stream()
                    .filter(rule -> rule.getMatch().longValue() == ip)
                    .collect(Collectors.toList());
            ConflictFreeChanges conflictFreeChanges = verifier.insertMiniBatch(rulesInSubspace);
            verifier.update(conflictFreeChanges);
            // if (policyChecker.stop) {
            // break;
            // }
            HashMap<Integer, Ports> ecToPorts = new HashMap<>();
            for (Map.Entry<Ports, Integer> entry : verifier.portsToPredicate.entrySet()) {
                ecToPorts.put(entry.getValue(), entry.getKey());
            }
            long s = System.nanoTime();
            policyChecker.check(device, verifier.portsToPredicate.values(), ecToPorts);
            i++;
            System.out.println("Finished FIB updates: " + i + "/" + allDevices.size());
            Dispatcher.logger.logPrintln("Allpair using reduction graph time: " + (System.nanoTime() - s));
        }
    }

    static public Graph<Device, PGEdge> buildPG(Network network) {

        Graph<Device, PGEdge> graph = new DefaultDirectedGraph<>(PGEdge.class);
        for (Device device : network.getAllDevices()) {
            graph.addVertex(device);
        }
        for (int iPod = 0; iPod < 112; iPod++) {
            for (int iRsw = 0; iRsw < 48; iRsw++) {
                if (iPod == 111 && iRsw == 0) continue; // dst
                Device src = network.getDevice("rsw-" + iPod + "-" + iRsw);
                for (int i = 0; i < 4; i++) {
                    Device dst = network.getDevice("fsw-" + iPod + "-" + i);
                    graph.addEdge(src, dst, new PGEdge(src.getPort(src.getName() + ">" + dst.getName())));
                }
            }
            for (int iFsw = 0; iFsw < 4; iFsw++) {
                Device src = network.getDevice("fsw-" + iPod + "-" + iFsw);
                if (iPod == 111) { // intra pod
                    Device dst = network.getDevice("rsw-111-0");
                    graph.addEdge(src, dst, new PGEdge(src.getPort(src.getName() + ">" + dst.getName())));
                } else {
                    for (int iSsw = 0; iSsw < 48; iSsw++) {
                        Device dst = network.getDevice("ssw-" + iFsw + "-" + iSsw);
                        graph.addEdge(src, dst, new PGEdge(src.getPort(src.getName() + ">" + dst.getName())));
                    }
                }
            }
        }
        for (int iSpine = 0; iSpine < 4; iSpine++) {
            for (int iSsw = 0; iSsw < 48; iSsw++) {
                Device src = network.getDevice("ssw-" + iSpine + "-" + iSsw);
                Device dst = network.getDevice("fsw-111-" + iSpine);
                graph.addEdge(src, dst, new PGEdge(src.getPort(src.getName() + ">" + dst.getName())));
            }

        }
        return graph;
    }
}
