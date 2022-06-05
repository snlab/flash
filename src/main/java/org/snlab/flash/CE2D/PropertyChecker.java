package org.snlab.flash.CE2D;


import java.util.HashSet;
import java.util.Map;

import org.snlab.network.Device;
import org.snlab.network.Network;
import org.snlab.network.Port;

public class PropertyChecker {
    public boolean hasLoop = false;

    public void checkLoop(Network network, Map<Port, HashSet<Integer>> model) {
        for (Device device : network.getAllDevices()) {
            traverse(device, null, new HashSet<>(), model);
        }
    }

    private void traverse(Device current, HashSet<Integer> predicates, HashSet<Device> history, Map<Port, HashSet<Integer>> networkModel) {
        if (this.hasLoop) return;
        if (current == null) return; // reach to external
        if (predicates != null && predicates.isEmpty()) return;
        if (history.contains(current)) {
            this.hasLoop = true;
            System.out.println(" found loop at: ");
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
}
