package org.snlab.evaluation.others;

import java.util.*;

import org.snlab.flash.ModelManager.Ports.Ports;
import org.snlab.flash.ModelManager.BDDEngine;
import org.snlab.flash.ModelManager.TrieRules;
import org.snlab.network.Device;
import org.snlab.network.Network;
import org.snlab.network.Port;
import org.snlab.network.Rule;

class Change {
    Port oldPort, newPort;
    int bdd;

    Change(int bdd, Port oldPort, Port newPort) {
        this.bdd = bdd;
        this.oldPort = oldPort;
        this.newPort = newPort;
    }
}

/**
 * Cite "APKeep: Realtime Verification for Real Networks"
 */
public class APVerifier {
    public final BDDEngine bddEngine;
    private final ArrayList<Change> changes;
    private final HashMap<Device, TrieRules> deviceToRules;

    private int size = 32;
    private final HashMap<Port, HashSet<Integer>> portToPreds;
    private final HashMap<Integer, Ports> predToPorts;

    private double s1 = 0, s2 = 0, sports = 0;

    /**
     * Initialize the Network Model with topology and default rules.
     * A network owns a model.
     */
    public APVerifier(Network network, Ports base) {
        this(network, new BDDEngine(32), base);
    }

    public APVerifier(Network network, int size, Ports base) {
        this(network, new BDDEngine(size), base);
        this.size = size;
    }

    public APVerifier(Network network, BDDEngine bddEngine, Ports base) {
        this.bddEngine = bddEngine;
        this.changes = new ArrayList<>();
        this.portToPreds = new HashMap<>();
        this.predToPorts = new HashMap<>();
        this.deviceToRules = new HashMap<>();

        // Relabel every device as the index used by Ports, starting from 0
        for (Device device : network.getAllDevices()) this.deviceToRules.put(device, new TrieRules());

        // Each device has a default rule with default action.
        ArrayList<Port> key = new ArrayList<>();
        for (Device device : network.getAllDevices()) {
            Port p = device.getPort("default");
            key.add(p);
            Rule rule = new Rule(device, 0, 0, -1, p);
            deviceToRules.get(device).insert(rule, size);
            rule.setHit(BDDEngine.BDDTrue);
            rule.setBddmatch(BDDEngine.BDDTrue);

            this.portToPreds.putIfAbsent(rule.getOutPort(), new HashSet<>());
            this.portToPreds.get(rule.getOutPort()).add(rule.getHit());
        }
        this.predToPorts.put(BDDEngine.BDDTrue, base.create(key, 0, key.size()));
    }

    public void insertRule(Rule rule) {
        s1 -= System.nanoTime();
        rule.setHit(bddEngine.encodeIpv4(rule.getMatch(), rule.getPrefix(), rule.getSrc(), rule.getSrcPrefix()));
        rule.setBddmatch(bddEngine.ref(rule.getHit()));
        TrieRules targetNode = deviceToRules.get(rule.getDevice());

        for (Rule r : targetNode.getAllOverlappingWith(rule, size)) {
            if (r.getPriority() > rule.getPriority()) {
                int newHit = bddEngine.diff(rule.getHit(), r.getBddmatch());
                bddEngine.deRef(rule.getHit());
                rule.setHit(newHit);
            }

            if (rule.getHit() == BDDEngine.BDDFalse) break;

            if (r.getPriority() <= rule.getPriority() && r != rule) {
                int intersection = bddEngine.and(r.getHit(), rule.getHit());

                int newHit = bddEngine.diff(r.getHit(), intersection);
                bddEngine.deRef(r.getHit());
                r.setHit(newHit);

                if (intersection != BDDEngine.BDDFalse && r.getOutPort() != rule.getOutPort()) {
                    changes.add(new Change(intersection, r.getOutPort(), rule.getOutPort()));
                } else {
                    bddEngine.deRef(intersection);
                }
            }
        }
        targetNode.insert(rule, size);
        s1 += System.nanoTime();
    }

    public void removeRule(Rule rule) {
        s1 -= System.nanoTime();
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
                    changes.add(new Change(intersection, rule.getOutPort(), r.getOutPort()));
                } else {
                    bddEngine.deRef(intersection);
                }
            }
        }
        targetNode.remove(rule, size);
        bddEngine.deRef(rule.getBddmatch());
        bddEngine.deRef(rule.getHit());
        s1 += System.nanoTime();
    }

    private HashSet<Integer> transferredPreds;

    // Eagerly merge predicates by default
    public HashSet<Integer> update() {
        return this.update(true);
    }

    @SuppressWarnings("unchecked")
    public HashSet<Integer> update(boolean merge) {
        if (changes.isEmpty()) {
            return new HashSet<>();
        }

        s2 -= System.nanoTime();
        this.transferredPreds = new HashSet<>();
        for (Change c : changes) {
            this.portToPreds.putIfAbsent(c.oldPort, new HashSet<>());
            this.portToPreds.putIfAbsent(c.newPort, new HashSet<>());

            HashSet<Integer> oldPredicates = (HashSet<Integer>) this.portToPreds.get(c.oldPort).clone();
            for (int p : oldPredicates) {
                int intersection = bddEngine.and(p, c.bdd);
                if (intersection == BDDEngine.BDDFalse) {
                    bddEngine.deRef(intersection);
                    continue;
                } else {
                    int tmp = c.bdd;
                    c.bdd = bddEngine.diff(c.bdd, intersection);
                    bddEngine.deRef(tmp);
                }

                if (intersection != p) {
                    // if intersection takes intersection with p, due to the atomicity, it must be a new predicate
                    this.subtract(p, intersection, c.oldPort, c.newPort);
                } else {
                    // otherwise, simply transfer p
                    this.transfer(intersection, c.oldPort, c.newPort);
                    bddEngine.deRef(intersection);
                }

                if (merge) {
                    HashSet<Integer> newPredicates = (HashSet<Integer>) this.portToPreds.get(c.newPort).clone();
                    int oldTarget = intersection;
                    for (int t : newPredicates) {
                        if (oldTarget != t && predToPorts.get(intersection).equals(predToPorts.get(t))) {
                            intersection = this.merge(intersection, t);
                        }
                    }
                }

                if (c.bdd == BDDEngine.BDDFalse) break;
            }
            bddEngine.deRef(c.bdd);
        }
        changes.clear();
        s2 += System.nanoTime();
        return this.transferredPreds;
    }

    private void transfer(int p, Port from, Port to) {
        if (from == to) return;

        //  (predicate: p) takes (port:to)
        sports -= System.nanoTime();
        this.predToPorts.put(p, this.predToPorts.get(p).change(to));
        sports += System.nanoTime();

        // (port:to) takes (predicate:p)
        this.portToPreds.get(from).remove(p);
        this.portToPreds.get(to).add(p);

        this.transferredPreds.add(p);
    }

    private int subtract(int p, int p1, Port pPort, Port p1Port) {
        if (p == p1) {
            System.out.println("Error: in subtract() function, p equals to p1");
        }

        int p2 = bddEngine.diff(p, p1);

        // (predicate:p1) takes p1Port
        if (this.predToPorts.containsKey(p1)) {
            System.out.println("Error: in subtract() function, p1 is not a new predicate");
        }
        sports -= System.nanoTime();
        TreeMap<Integer, Port> mp = new TreeMap<>();
        mp.put(p1Port.getDevice().uid, p1Port);
        Ports ports = this.predToPorts.get(p).createWithChanges(mp);
        this.predToPorts.put(p1, ports);

        // (predicate:p2) inherits ports from (predicate:p) excepting ports for (predicate:p1)
        if (this.predToPorts.containsKey(p2)) {
            System.out.println("Error: in subtract() function, p2 is not a new predicate");
        }
        this.predToPorts.put(p2, this.predToPorts.get(p));
        // remove p
        this.predToPorts.remove(p);

        // (ports for p) now takes p2
        for (Port port : this.predToPorts.get(p2).getAll()) {
            this.portToPreds.get(port).remove(p);
            this.portToPreds.get(port).add(p2);
            if (port != pPort) {
                this.portToPreds.get(port).add(p1);
            }
        }
        // p1Port takes p1
        this.portToPreds.get(p1Port).add(p1);
        sports += System.nanoTime();

        this.transferredPreds.add(p1);
        if (this.transferredPreds.contains(p)) {
            this.transferredPreds.remove(p);
            this.transferredPreds.add(p2);
        }

        bddEngine.deRef(p);
        return p2;
    }

    private int merge(int p1, int p2) {
        if (!predToPorts.get(p1).equals(predToPorts.get(p2))) {
            System.out.println("Error: in merge() function, p1 and p2 takes different set of ports");
        }

        int p = bddEngine.or(p1, p2);

        // (predicate:p) takes the union of (ports for p1) and (ports for p2)
        if (this.predToPorts.containsKey(p)) {
            System.out.println("Error: in merge() function, p is not a new predicate");
        }
        this.predToPorts.put(p, this.predToPorts.get(p1));
        // this.predToPorts.get(p).addAll(this.predToPorts.get(p2));

        sports -= System.nanoTime();
        // update portToPreds according to predToPorts
        for (Port t : this.predToPorts.get(p).getAll()) {
            this.portToPreds.get(t).remove(p1);
            this.portToPreds.get(t).remove(p2);
            this.portToPreds.get(t).add(p);
        }
        sports += System.nanoTime();

        // remove p1, p2
        this.predToPorts.remove(p1);
        this.predToPorts.remove(p2);

        if (this.transferredPreds.contains(p1) || this.transferredPreds.contains(p2)) {
            this.transferredPreds.remove(p1);
            this.transferredPreds.remove(p2);
            this.transferredPreds.add(p);
        }

        bddEngine.deRef(p1);
        bddEngine.deRef(p2);
        return p;
    }

    public int predSize() {
        return predToPorts.size();
    }

    public double printTime(int size) {
        long nsToUsPU = 1000L * size;
        if (size == 0)  nsToUsPU = 1000L * 1000L;
        System.out.println("    Stage 1 (Update Block Computation) " + (s1 / nsToUsPU) + " us per-update");
        System.out.println("    Stage 2 (Model Update) " + (s2 / nsToUsPU) + " us per-update");
        System.out.println("    Ports " + (sports / nsToUsPU) + " us per-update");
        return s1 + s2;
    }
}
