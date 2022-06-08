package org.snlab.flash.ModelManager;


import java.util.*;

import org.snlab.flash.ModelManager.Ports.Ports;
import org.snlab.flash.ModelManager.Ports.PersistentPorts;
import org.snlab.network.Device;
import org.snlab.network.Network;
import org.snlab.network.Port;
import org.snlab.network.Rule;

public class InverseModel {
    public final BDDEngine bddEngine;
    private int size = 32; // length of packet header

    private final HashMap<Device, TrieRules> deviceToRules; // FIB snapshots
    public HashMap<Ports, Integer> portsToPredicate; // network inverse model

    private double s1 = 0, s1to2 = 0, s2 = 0, sports = 0;

    public InverseModel(Network network) {
        this(network, new BDDEngine(32), new PersistentPorts());
    }

    public InverseModel(Network network, int size) {
        this(network, new BDDEngine(size), new PersistentPorts());
        this.size = size;
    }

    public InverseModel(Network network, Ports base) {
        this(network, new BDDEngine(32), base);
    }

    public InverseModel(Network network, int size, Ports base) {
        this(network, new BDDEngine(size), base);
        this.size = size;
    }

    public InverseModel(Network network, BDDEngine bddEngine, Ports base) {
        this.bddEngine = bddEngine;
        this.deviceToRules = new HashMap<>();

        // Relabel every device as the index used by Ports, starting from 0
        for (Device device : network.getAllDevices()) this.deviceToRules.put(device, new TrieRules());

        // Each device has a default rule with default action.
        ArrayList<Port> key = new ArrayList<>();
        for (Device device : network.getAllDevices()) {
            Port p = device.getPort("default");
            key.add(p);
            Rule rule = new Rule(device, 0, 0, -1, p);
            rule.setBddmatch(BDDEngine.BDDTrue);
            deviceToRules.get(device).insert(rule, size);
        }

        // The only one EC takes default actions.
        this.portsToPredicate = new HashMap<>();
        this.portsToPredicate.put(base.create(key, 0, key.size()), BDDEngine.BDDTrue);
    }

    public Changes insertMiniBatch(List<Rule> insertions) {
        return this.miniBatch(insertions, new ArrayList<>());
    }

    /**
     * Notates current data-plane (flow rules) as f, consider transition to f'
     * @param insertions f' - f
     * @param deletions  f - f'
     * @return the change \chi
     */
    public Changes miniBatch(List<Rule> insertions, List<Rule> deletions) {
        s1 -= System.nanoTime();
        HashSet<Rule> deleted = new HashSet<>();
        HashSet<Rule> inserted = new HashSet<>();
        for (Rule rule : deletions) {
            deleted.add(rule);
            deviceToRules.get(rule.getDevice()).remove(rule, size);
        }
        for (Rule rule : insertions) {
            if (deleted.contains(rule)) {
                deleted.remove(rule);
                continue;
            }
            inserted.add(rule);
            rule.setBddmatch(bddEngine.encodeIpv4(rule.getMatch(), rule.getPrefix(), rule.getSrc(), rule.getSrcPrefix()));
            deviceToRules.get(rule.getDevice()).insert(rule, size);
        }
        Changes ret = new Changes(bddEngine);
        for (Rule rule : deleted) identifyChangesDeletion(rule, ret);
        for (Rule rule : inserted) identifyChangesInsert(rule, ret);
        s1 += System.nanoTime();
        return ret;
    }

    /**
     * @param rule an inserted rule
     * @param ret  the pointer to the value returned by this function
     */
    private void identifyChangesInsert(Rule rule, Changes ret) {
        rule.setHit(bddEngine.ref(rule.getBddmatch()));

        for (Rule r : deviceToRules.get(rule.getDevice()).getAllOverlappingWith(rule, size)) {
            if (r.getPriority() > rule.getPriority()) {
                int newHit = bddEngine.diff(rule.getHit(), r.getBddmatch());
                bddEngine.deRef(rule.getHit());
                rule.setHit(newHit);
            }

            if (rule.getHit() == BDDEngine.BDDFalse) break;

            if (r.getPriority() < rule.getPriority()) {
                int intersection = bddEngine.and(r.getHit(), rule.getHit());

                int newHit = bddEngine.diff(r.getHit(), intersection);
                bddEngine.deRef(r.getHit());
                r.setHit(newHit);

                bddEngine.deRef(intersection);
            }
        }

        if (rule.getHit() != BDDEngine.BDDFalse) {
            s1 += System.nanoTime();
            s1to2 -= System.nanoTime();
            bddEngine.ref(rule.getHit());
            ret.add(rule.getHit(), null, rule.getOutPort());
            s1to2 += System.nanoTime();
            s1 -= System.nanoTime();
        }
    }

    private void identifyChangesDeletion(Rule rule, Changes ret) {
        TrieRules targetNode = deviceToRules.get(rule.getDevice());

        ArrayList<Rule> sorted = targetNode.getAllOverlappingWith(rule, size);
        Comparator<Rule> comp = (Rule lhs, Rule rhs) -> rhs.getPriority() - lhs.getPriority();
        sorted.sort(comp);

        for (Rule r : sorted) {
            if (rule.getHit() == BDDEngine.BDDFalse) break;

            if (r.getPriority() < rule.getPriority()) {
                int intersection = bddEngine.and(r.getBddmatch(), rule.getHit());

                int newHit = bddEngine.or(r.getHit(), intersection);
                bddEngine.deRef(r.getHit());
                r.setHit(newHit);

                newHit = bddEngine.diff(rule.getHit(), intersection);
                bddEngine.deRef(rule.getHit());
                rule.setHit(newHit);

                if (intersection != BDDEngine.BDDFalse && r.getOutPort() != rule.getOutPort()) {
                    s1 += System.nanoTime();
                    s1to2 -= System.nanoTime();
                    ret.add(intersection, rule.getOutPort(), r.getOutPort());
                    s1to2 += System.nanoTime();
                    s1 -= System.nanoTime();
                } else {
                    bddEngine.deRef(intersection);
                }
            }
        }
        targetNode.remove(rule, size);
        bddEngine.deRef(rule.getBddmatch());
        bddEngine.deRef(rule.getHit());
    }


    private void insertPredicate(HashMap<Ports, Integer> newPortsToPreds, Ports newPorts, Integer predicate) {
        if (newPortsToPreds.containsKey(newPorts)) {
            int t = newPortsToPreds.get(newPorts);
            newPortsToPreds.replace(newPorts, bddEngine.or(t, predicate));
            bddEngine.deRef(predicate);
            bddEngine.deRef(t);
        } else {
            newPortsToPreds.put(newPorts, predicate);
        }
    }

    /**
     * Fast Inverse Model Transformation
     * Updates the PPM following changes and returns all transferred ECs.
     *
     * @param changes -
     * @return -
     */
    public HashSet<Integer> update(Changes changes) {
        s1to2 -= System.nanoTime();
        changes.aggrBDDs();
        s1to2 += System.nanoTime();


        s2 -= System.nanoTime();
        HashSet<Integer> transferredECs = new HashSet<>();

        for (Map.Entry<Integer, TreeMap<Integer, Port>> entryI : changes.getAll().entrySet()) {
            int delta = entryI.getKey();
            bddEngine.ref(delta);

            HashMap<Ports, Integer> newPortsToPreds = new HashMap<>();
            for (Map.Entry<Ports, Integer> entry : portsToPredicate.entrySet()) {
                Ports ports = entry.getKey();
                Integer predicate = entry.getValue();
                if (delta == BDDEngine.BDDFalse) { // change already becomes empty
                    insertPredicate(newPortsToPreds, ports, predicate);
                    continue;
                }

                int intersection = bddEngine.and(predicate, delta);
                if (intersection == BDDEngine.BDDFalse) { // EC is not affected by change
                    insertPredicate(newPortsToPreds, ports, predicate);
                    bddEngine.deRef(intersection);
                    continue;
                } else {
                    // clean up the intermediate variables
                    int t = bddEngine.diff(delta, intersection);
                    bddEngine.deRef(delta);
                    delta = t;
                }


                if (intersection != predicate) {
                    // EC is partially affected by change, which causes split
                    // transferredECs.add(intersection);
                    insertPredicate(newPortsToPreds, ports, bddEngine.diff(predicate, intersection));
                }
                // The intersection is transferred
                transferredECs.add(intersection);
                sports -= System.nanoTime();
                Ports portsT = ports.createWithChanges(entryI.getValue());
                sports += System.nanoTime();
                insertPredicate(newPortsToPreds, portsT, intersection);
                bddEngine.deRef(predicate);
            }

            bddEngine.deRef(delta);
            portsToPredicate = newPortsToPreds;
        }
        s2 += System.nanoTime();

        // Manually deref BDDs used by Changes since its deconstructor doesn't handle this.
        changes.releaseBDDs();
        return transferredECs;
    }

    public HashMap<Port, HashSet<Integer>> getPortToPredicate() {
        HashMap<Port, HashSet<Integer>> ret = new HashMap<>();
        for (Map.Entry<Ports, Integer> entry : portsToPredicate.entrySet())
            for (Port p : entry.getKey().getAll()) {
                ret.putIfAbsent(p, new HashSet<>());
                ret.get(p).add(entry.getValue());
            }
        return ret;
    }

    public int predSize() {
        return this.portsToPredicate.size();
    }

    public double printTime(int size) {
        long nsToUsPU = 1000L * size;
        if (size == 0)  nsToUsPU = 1000L * 1000L;
        System.out.println("    Stage 1 (Update Block Computation) " + (s1 / nsToUsPU) + " us per-update");
        System.out.println("    Converting to Conflict-free Update Block " + (s1to2 / nsToUsPU) + " us per-update");
        System.out.println("    Stage 2 (Model Update) " + (s2 / nsToUsPU) + " us per-update");
        System.out.println("    Ports " + (sports / nsToUsPU) + " us per-update");
        return s1 + s1to2 + s2;
    }
}
