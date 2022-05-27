package org.snlab.flash.CE2D;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.Sets;

import org.snlab.network.Device;
import org.snlab.network.Network;
import org.snlab.network.Port;

public class EarlyDetector {
    private Set<Device> closedDevices;
    private boolean hasLoop = false;

    public void detectLoop(Network network, Set<Device> newClosed, Map<Port, HashSet<Integer>> networkModel) {
        Set<Device> delta = Sets.difference(newClosed, this.closedDevices);
        this.closedDevices = newClosed;
        for (Device device : delta) {
            if (this.hasLoop)
                return;
            traverse(device, null, new HashSet<>(), networkModel); // null represents the universal set
        }
    }

    private void traverse(Device current, Set<Integer> predicates, HashSet<Device> history,
            Map<Port, HashSet<Integer>> networkModel) {
        if (this.hasLoop)
            return;
        if (predicates != null && predicates.isEmpty())
            return;
        if (history.contains(current)) {
            System.out.println("found loop at: " + System.nanoTime());
            this.hasLoop = true;
            return;
        }

        history.add(current);
        for (Port egress : current.getPorts()) {
            // if egress is default, alter blackhole
            Device t = egress.getPeerDevice();
            if (egress.getName().equals("default") || t == null || !this.closedDevices.contains(t)) {
                continue;
            }
            Set<Integer> labels = networkModel.get(egress), intersection;
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

    public boolean hasLoop() {
        return hasLoop;
    }
}
