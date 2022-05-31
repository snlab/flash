package org.snlab.flash.CE2D;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import com.google.common.collect.Sets;

import org.snlab.network.Device;
import org.snlab.network.Network;
import org.snlab.network.Port;

public class EarlyDetector {

    private Set<Device> closedDevices = new HashSet<>();
    public boolean hasLoop = false;
    public boolean useSingleThread = false;

    // public static void detectPolicy(Network network, ModelManager verifier,
    // Collection<Device> newClosed, int hs, Graph<Device, DefaultEdge> graph) {
    // Set<Integer> relatedPreds = portsToPredicate.values().stream().filter(x ->
    // verifier.bddEngine.and(x, hs) != 0).collect(Collectors.toSet());
    // for (int p : relatedPreds) {
    // Automaton modelAutomata = new Automaton();
    // }
    // }

    // public void detectLoop(Set<Device> newClosed, HashMap<Port, HashSet<Integer>>
    // networkModel) {
    // this.closedDevices.addAll(newClosed);
    // Runnable loopDetector = new LoopDetector(Set.copyOf(this.closedDevices),
    // newClosed, networkModel);
    // new Thread(loopDetector).start();
    // }

    public void detectLoop(Setting setting, Network network, Set<Device> newClosed, Map<Port, HashSet<Integer>> model) {
        this.closedDevices.addAll(newClosed);
        if (this.useSingleThread) {
            LoopDetector ld = new LoopDetector(setting, network, Set.copyOf(this.closedDevices), newClosed, model);
            ld.run();
            this.hasLoop = ld.hasLoop;
        } else {
            Runnable loopDetector = new LoopDetector(setting, network, Set.copyOf(this.closedDevices), newClosed, model);
            new Thread(loopDetector).start();
        }
    }

    public boolean hasLoop() {
        return hasLoop;
    }
}

class LoopDetector implements Runnable {
    /**
     * all closed devices
     */
    private Set<Device> closed;
    /**
     * newly closed devices, should be subset of closed
     */
    private Set<Device> newClosed;
    private Network network;
    private Setting setting;
    public boolean hasLoop = false;
    private Map<Port, HashSet<Integer>> model;

    public LoopDetector(Setting setting, Network network, Set<Device> closed, Set<Device> newClosed,
            Map<Port, HashSet<Integer>> model) {
        this.setting = setting;
        this.network = network;
        this.closed = closed;
        this.newClosed = newClosed;
        this.model = model;
    }

    @Override
    public void run() {
        for (Device device : newClosed) {
            if (this.hasLoop)
                return;
            traverse(device, null, new HashSet<>(), model, closed); // null represents the universal set
        }
    }

    private void traverse(Device current, HashSet<Integer> predicates, HashSet<Device> history,
            Map<Port, HashSet<Integer>> networkModel, Set<Device> closed) {
        if (this.hasLoop)
            return;
        if (predicates != null && predicates.isEmpty())
            return;
        if (history.contains(current)) {
            long edTime = (System.nanoTime() - setting.startAt);
            int processedUpdates = this.network.getAllDevices().stream().filter(closed::contains)
                    .map(device -> device.getInitialRules().size()).collect(Collectors.toList()).stream()
                    .mapToInt(Integer::intValue).sum();
                    
            if (!this.hasLoop) {
                this.hasLoop = true;
                System.out.println(setting + " found loop at: " + edTime + " #closed: " + closed.size() + " #updates: "
                        + processedUpdates);
            }
            Thread.currentThread().interrupt();
            return;
        }

        history.add(current);
        for (Port egress : current.getPorts().stream().filter(port -> {
            Device t = port.getPeerDevice();
            return t != null && closed.contains(port.getPeerDevice());
        }).collect(Collectors.toList())) {
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

                traverse(t, intersection, history, networkModel, closed);
            }
        }
        history.remove(current);
    }
}
