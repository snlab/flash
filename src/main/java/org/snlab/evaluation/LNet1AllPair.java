package org.snlab.evaluation;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.jgrapht.Graph;
import org.jgrapht.graph.DefaultDirectedGraph;
import org.snlab.flash.Dispatcher;
import org.snlab.flash.CE2D.AllPairChecker;
import org.snlab.flash.CE2D.PGEdge;
import org.snlab.flash.CE2D.PropertyChecker;
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
        pgCheck(network);
        Dispatcher.logger.writeFile();
        System.exit(0);
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
            Dispatcher.logger.logPrintln("pc: " + (System.nanoTime() - s));
        }
        System.out.println("#ECs: " + model.predSize());
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
        for (Device device : allDevices) {
            List<Rule> rulesInSubspace = device.getInitialRules().stream()
                    .filter(rule -> rule.getMatch().longValue() == ip)
                    .collect(Collectors.toList());
            Changes changes = verifier.insertMiniBatch(rulesInSubspace);
            verifier.update(changes);
            // if (policyChecker.stop) {
            // break;
            // }
            HashMap<Integer, Ports> ecToPorts = new HashMap<>();
            for (Map.Entry<Ports, Integer> entry : verifier.portsToPredicate.entrySet()) {
                ecToPorts.put(entry.getValue(), entry.getKey());
            }
            long s = System.nanoTime();
            policyChecker.check(device, verifier.portsToPredicate.values(), ecToPorts);
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

        // Device src = network.getDevice("rsw-0-0");
        // Device dst = network.getDevice("rsw-111-0");
        // graph.addVertex(src);
        // graph.addVertex(dst);
        // for (int i = 0; i < 4; i++) {
        //     graph.addVertex(network.getDevice("fsw-0-" + i));
        //     graph.addVertex(network.getDevice("fsw-111-" + i));
        //     for (int j = 0; j < 48; j++) {
        //         Device ssw = network.getDevice("ssw-" + i + "-" + j);
        //         graph.addVertex(ssw);
        //     }
        // }
        // for (int i = 0; i < 4; i++) {
        //     graph.addEdge(src, network.getDevice("fsw-0-" + i),
        //             new PGEdge(src.getPort(src.getName() + ">" + "fsw-0-" + i)));
        //     graph.addEdge(network.getDevice("fsw-111-" + i), dst,
        //             new PGEdge(network.getDevice("fsw-111-" + i).getPort("fsw-111-" + i + ">" + dst.getName())));
        //     for (int j = 0; j < 48; j++) {
        //         graph.addEdge(network.getDevice("fsw-0-" + i), network.getDevice("ssw-" + i + "-" + j),
        //                 new PGEdge(network.getDevice("fsw-0-" + i).getPort("fsw-0-" + i + ">" + "ssw-" + i + "-" + j)));
        //         graph.addEdge(network.getDevice("ssw-" + i + "-" + j), network.getDevice("fsw-111-" + i),
        //                 new PGEdge(network.getDevice("ssw-" + i + "-" + j)
        //                         .getPort("ssw-" + i + "-" + j + ">" + "ssw-" + i + "-" + j)));
        //     }
        // }
        // return graph;
    }
}
