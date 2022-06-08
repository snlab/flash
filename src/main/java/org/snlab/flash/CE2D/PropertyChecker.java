package org.snlab.flash.CE2D;


import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.jgrapht.Graph;
import org.jgrapht.alg.connectivity.ConnectivityInspector;
import org.jgrapht.event.GraphEdgeChangeEvent;
import org.jgrapht.graph.DefaultDirectedGraph;
import org.jgrapht.graph.EdgeReversedGraph;
import org.jgrapht.traverse.BreadthFirstIterator;
import org.snlab.flash.Dispatcher;
import org.snlab.flash.ModelManager.BDDEngine;
import org.snlab.flash.ModelManager.Ports.Ports;
import org.snlab.network.Device;
import org.snlab.network.Network;
import org.snlab.network.Port;

public class PropertyChecker {
    public boolean hasLoop = false;
    private BDDEngine bddEngine;
    private Network network;
    private Graph<Device, PGEdge> pg;
    private int hs;
    private Set<Device> closed = new HashSet<>();
    private Map<Integer, Graph<Device, PGEdge>> ecToPg = new HashMap<>();
    private Map<Integer, ConnectivityInspector<Device, PGEdge>> ecToCI = new HashMap<>();
    private List<Device> sources;

    public PropertyChecker() {}

    public void checkLoop(Network network, Map<Port, HashSet<Integer>> model, Set<Integer> transfered) {
        for (Device device : network.getAllDevices()) {
            traverse(device, transfered, new HashSet<>(), model);
        }
    }

    private void traverse(Device current, Set<Integer> predicates, HashSet<Device> history, Map<Port, HashSet<Integer>> networkModel) {
        if (this.hasLoop) return;
        if (current == null) return; // reach to external
        if (predicates != null && predicates.isEmpty()) return;
        if (history.contains(current)) {
            this.hasLoop = true;
            return;
        }

        history.add(current);
        for (Port egress : current.getPorts()) {
            // if egress is default, alter blackhole
            Device t = egress.getPeerDevice();
            HashSet<Integer> labels = networkModel.get(egress), intersection;
            if (labels != null) {
                if (predicates != null) {
                    intersection = new HashSet<>(predicates);
                    intersection.retainAll(labels);
                } else {
                    intersection = new HashSet<>(labels);
                }

                traverse(t, intersection, history, networkModel);
            }
        }
        history.remove(current);
    }

    public PropertyChecker(Network network, Graph<Device, PGEdge> pg, BDDEngine bddEngine, int hs) {
        this.network = network;
        this.pg = pg;
        this.bddEngine = bddEngine;
        this.hs = hs;
        sources = network.getAllDevices().stream().filter(d -> d.getName().contains("rsw")).collect(Collectors.toList());
    }


    public void check(Device newClosed, Collection<Integer> ECs, Map<Integer, Ports> ecToPorts) {
        this.closed.add(newClosed);
        if (!this.pg.vertexSet().contains(newClosed)) {
            return;
        }

        for (int ec : ECs) {
            if (bddEngine.and(ec, hs) != 0) {
                if (!ecToPg.containsKey(ec)) {
                    // create pg copy
                    Graph<Device, PGEdge> pg = clonePG();
                    // remove edges for closed switches
                    reducePG(pg, ecToPorts, ec);
                    this.ecToPg.put(ec, pg);
                    ConnectivityInspector<Device, PGEdge> ci = new ConnectivityInspector<>(pg);
                    this.ecToCI.put(ec, ci);
                }
                Graph<Device, PGEdge> pg = ecToPg.get(ec);

                Set<PGEdge> needToRemove = new HashSet<>();
                for (PGEdge edge : pg.outgoingEdgesOf(newClosed)) {
                    if (!ecToPorts.get(ec).getAll().contains(edge.port)) {
                        needToRemove.add(edge);
                    }
                }
                for (PGEdge edge : needToRemove) {
                    pg.removeEdge(edge);
                    GraphEdgeChangeEvent<Device, PGEdge> edgeChangeEvent = new GraphEdgeChangeEvent<>(this, GraphEdgeChangeEvent.EDGE_REMOVED, edge, pg.getEdgeSource(edge), pg.getEdgeTarget(edge));
                    this.ecToCI.get(ec).edgeRemoved(edgeChangeEvent);
                }

//                DijkstraShortestPath dijk = new DijkstraShortestPath(pg);
//                GraphPath<Device, PGEdge> path = dijk.getPath(network.getDevice("rsw-0-0"), network.getDevice("rsw-111-0"));
                
                Set<Device> connected = connectedSetOfReverse(pg, network.getDevice("rsw-111-0"));
                
                connected.retainAll(sources);
                if (connected.size() != sources.size()) {
                    System.out.println("cannot reach");
                    System.out.println(connected.size());
                    System.out.println(sources.size());
                }
        //         for (int i = 0; i < 112; i++) {
        //             for (int j = 0; j < 48; j++) {
        //                 Device src = network.getDevice("rsw-"+i+"-"+j);
        //                 Device dst = network.getDevice("rsw-111-0");
        //                 long s = System.nanoTime();
        //                 boolean connected = ecToCI.get(ec).pathExists(src, dst);
        //                 System.out.println("$compute connected: " + (System.nanoTime() - s));
        //                 if (!connected) {
        // //                if (path == null) {
        //                     System.out.println("cannot reach!");
        //                     System.out.println("#closed: " + closed.size());
        //                     System.out.println("$time: " + (System.nanoTime() - Dispatcher.logger.startAt));
        // //                    System.exit(0);
        //                 }
        //             }
        //         }
            }
        }
    }
    

    private Set<Device> connectedSetOfReverse(Graph<Device, PGEdge> g, Device vertex) {
        EdgeReversedGraph<Device, PGEdge> reversedGraph = new EdgeReversedGraph<>(g);
        Set<Device> connectedSet = new HashSet<>();
        BreadthFirstIterator<Device, PGEdge> i = new BreadthFirstIterator<>(reversedGraph, vertex);
        while(i.hasNext()) {
            (connectedSet).add(i.next());
        }

        return connectedSet;
    }

    private Graph<Device, PGEdge> clonePG() {
        Graph<Device, PGEdge> pg = new DefaultDirectedGraph<>(PGEdge.class);
        for (Device device : this.pg.vertexSet()) {
            pg.addVertex(device);
        }
        for (PGEdge edge : this.pg.edgeSet()) {
            Device src = this.pg.getEdgeSource(edge);
            Device dst = this.pg.getEdgeTarget(edge);
            Port p = src.getPort(src.getName() + ">" + dst.getName());
            PGEdge e = new PGEdge(p);
            pg.addEdge(src, dst, e);
        }
        return pg;
    }

    private void reducePG(Graph<Device, PGEdge> pg, Map<Integer, Ports> ecToPorts, int ec) {
        for (Device device : pg.vertexSet()) {
            if (!closed.contains(device)) {
                continue;
            }

            Set<PGEdge> needToRemove = new HashSet<>();
            for (PGEdge edge : pg.outgoingEdgesOf(device)) {
                if (!ecToPorts.get(ec).getAll().contains(edge.port)) {
                    needToRemove.add(edge);
                }
            }
            for (PGEdge edge : needToRemove) {
                pg.removeEdge(edge);
            }
        }
    }
}
