package org.snlab.table3;

import org.snlab.network.Device;
import org.snlab.network.Network;
import org.snlab.network.Port;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

public class Checker {
    static HashSet predicates;
    static HashMap<Device, HashSet> sourceToPreds;

    public static double allPair(Network network, HashMap<Port, HashSet<Integer>> model) {
        predicates = new HashSet<>();
        sourceToPreds = new HashMap<>();

        double s = 0;
        s -= System.nanoTime();
        for (HashSet<Integer> preds : model.values()) predicates.addAll(preds);
        for (Device device : network.getAllDevices()) sourceToPreds.put(device, new HashSet<>());
        for (Device device : network.getAllDevices()) {
            traverse(model, device, new HashSet<>(predicates), new ArrayList<>()); // null represents the universal set
        }
        s += System.nanoTime();

        predicates = null;
        sourceToPreds = null;
        System.gc();
        System.runFinalization();

        return s;
    }

    public static double allPairAtom(Network network, HashMap<Port, HashSet<Long>> model, List<Long> changes) {
        predicates = new HashSet<>();
        sourceToPreds = new HashMap<>();

        double s = 0;
        s -= System.nanoTime();
        // for (HashSet<Long> preds : model.values()) predicates.addAll(preds);
        for (Device device : network.getAllDevices()) sourceToPreds.put(device, new HashSet<>());
        for (Device device : network.getAllDevices()) {
            traverseAtom(model, device, new HashSet<>(changes), new ArrayList<>()); // null represents the universal set
        }
        s += System.nanoTime();

        predicates = null;
        sourceToPreds = null;
        // System.gc();
        // System.runFinalization();

        return s;
    }

    public static void traverseAtom(HashMap<Port, HashSet<Long>> model, Device current, HashSet<Long> pset, ArrayList<Device> history) {
        if (pset == null) return;
        // pset.removeAll(sourceToPreds.get(current));
        if (pset.size() == 0 || history.contains(current)) return;
        // sourceToPreds.get(current).addAll(pset);
        history.add(current);
        for (Port egress : current.getPorts()) {
            // if egress is default, alter blackhole
            Device t = egress.getPeerDevice();
            if (t == null) { // send to black hole (default) or an external port
                continue;
            }
            HashSet<Long> labels = model.get(egress), intersection;
            if (labels != null) {
                intersection = new HashSet<>(pset);
                intersection.retainAll(labels);
                traverseAtom(model, current, pset, history);
            }
        }
        history.remove(current);
    }

    public static void traverse(HashMap<Port, HashSet<Integer>> model, Device current, HashSet<Integer> pset, ArrayList<Device> history) {
        if (pset == null) return;
        pset.removeAll(sourceToPreds.get(current));
        if (pset.size() == 0 || history.contains(current)) return;
        sourceToPreds.get(current).addAll(pset);
        history.add(current);
        for (Port egress : current.getPorts()) {
            // if egress is default, alter blackhole
            Device t = egress.getPeerDevice();
            if (t == null) { // send to black hole (default) or an external port
                continue;
            }
            HashSet<Integer> labels = model.get(egress), intersection;
            if (labels != null) {
                intersection = new HashSet<>(pset);
                intersection.retainAll(labels);
                traverse(model, t, intersection, history);
            }
        }
        history.remove(current);
    }
}

