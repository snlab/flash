package org.snlab.flash.CE2D;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.jgrapht.Graph;
import org.jgrapht.graph.AbstractBaseGraph;
import org.jgrapht.graph.DefaultDirectedGraph;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.EdgeReversedGraph;
import org.jgrapht.traverse.BreadthFirstIterator;
import org.snlab.flash.Dispatcher;
import org.snlab.flash.ModelManager.BDDEngine;
import org.snlab.flash.ModelManager.Ports.Ports;
import org.snlab.network.Device;
import org.snlab.network.Network;
import org.snlab.network.Port;

public class AllPairChecker {
    private Map<Integer, Graph<Device, PGEdge>> ecToGraph = new HashMap<>();
    private Set<Device> closed = new HashSet<>();
    private Network network;
    private BDDEngine bddEngine;
    private int hs;
    public boolean foundBug = false;

    public AllPairChecker(Network network, BDDEngine bddEngine, int hs) {
        this.network = network;
        this.bddEngine = bddEngine;
        this.hs = hs;
        ecToGraph.put(1, createGraph());
    }

    public void check(Collection<Integer> changedECs, Device newClosed, Map<Integer, Ports> ecToPorts, HashMap<Port, HashSet<Integer>> model) {
        this.closed.add(newClosed);
        System.out.println("#closed: " + closed.size());
        for (int ec : changedECs) {
            ecToGraph.computeIfAbsent(ec, k -> createGraphForEC(ec, ecToPorts));
        }

        ecToGraph.keySet().removeIf(integer -> !changedECs.contains(integer));
        for (int ec : changedECs) {
            int tmp = bddEngine.and(ec, this.hs);
            if (tmp == 0) {
                bddEngine.deRef(tmp);
                continue;
            }
            bddEngine.deRef(tmp);
            Graph<Device, PGEdge> g = ecToGraph.get(ec);
            long s = System.nanoTime();
            Set<PGEdge> needToRemove = new HashSet<>();
            for (PGEdge edge : g.outgoingEdgesOf(newClosed)) {
                if (model.get(edge.port) == null || !model.get(edge.port).contains(ec)) {
                    needToRemove.add(edge);
                }
            }
            for (PGEdge edge : needToRemove) {
                g.removeEdge(edge);
            }

            Set<Device> connected = connectedSetOfReverse(g, network.getDevice("rsw-0-1"));
            
            // if the nodes of connected graph != all devices of network, there must be unreachable nodes to dst
            if (connected.size() != network.getAllDevices().size()) {
                network.getAllDevices().removeAll(connected);
                System.out.println(network.getAllDevices().stream().findFirst().get().getName());
                System.out.println(needToRemove.size());
                System.out.println("bug at closed: " + closed.size() + " " + newClosed.getName());
                foundBug = true;
            }
            
            Dispatcher.logger.logPrintln("$allpair: " + (System.nanoTime() - s));
        }
    }

    public Set<Device> connectedSetOfReverse(Graph<Device, PGEdge> g, Device vertex) {
        EdgeReversedGraph<Device, PGEdge> reversedGraph = new EdgeReversedGraph<>(g);
        Set<Device> connectedSet = new HashSet<>();
        BreadthFirstIterator<Device, PGEdge> i = new BreadthFirstIterator<>(reversedGraph, vertex);
        while(i.hasNext()) {
            (connectedSet).add(i.next());
        }

        return connectedSet;
    }

    private Graph<Device, PGEdge> createGraphForEC(int ec, Map<Integer, Ports> ecToPorts) {
        for (int oldec : ecToGraph.keySet()) {
            int tmp = bddEngine.or(ec, oldec);
            if (tmp == oldec) {
                bddEngine.deRef(tmp);
                return (Graph<Device, PGEdge>) ((AbstractBaseGraph)(ecToGraph.get(oldec))).clone();
            }
            bddEngine.deRef(tmp);
        }
        System.out.println("new graph");
        Graph<Device, PGEdge> graph = new DefaultDirectedGraph<>(PGEdge.class);
        for (Device device : network.getAllDevices()) {
            graph.addVertex(device);
        }

        // add edges for closed switches
        Ports ports = ecToPorts.get(ec);
        for (Port port : ports.getAll()) {
            if (port.getPeerDevice() == null || !this.closed.contains(port.getDevice())) {
                continue;
            }
            graph.addEdge(port.getDevice(), port.getPeerDevice(), new PGEdge(port));
        }

        // add edges for opened switches
        for (Device device : network.getAllDevices().stream().filter(device -> !closed.contains(device)).collect(Collectors.toList())) {
            for (Port port : device.getPorts()) {
                if (port.getPeerDevice() == null) {
                    continue;
                }
                graph.addEdge(device, port.getPeerDevice(), new PGEdge(port));
            }
        }
        return graph;
    }

    private Graph<Device, PGEdge> createGraph() {
        Graph<Device, PGEdge> graph = new DefaultDirectedGraph<>(PGEdge.class);
        for (Device device : network.getAllDevices()) {
            graph.addVertex(device);
        }

        // add edges for opened switches
        for (Device device : network.getAllDevices()) {
            for (Port port : device.getPorts()) {
                if (port.getPeerDevice() == null) {
                    continue;
                }
                graph.addEdge(device, port.getPeerDevice(), new PGEdge(port));
            }
        }
        return graph;
    }
    
}
