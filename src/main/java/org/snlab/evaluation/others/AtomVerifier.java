package org.snlab.evaluation.others;

import org.jgrapht.alg.util.Pair;
import org.snlab.network.Device;
import org.snlab.network.Port;
import org.snlab.network.Rule;

import java.util.*;

/**
 * We implement Delta-net* according to "Delta-net: Real-time Network Verification Using Atoms"
 *
 * Each atom with range [l, r) is represented as an Long equals to l.
 * Notice the ``source'' is represented as Port (= rule.getOutPort()).
 */
public class AtomVerifier {
    private final HashMap<Long, HashMap<Device, TreeMap<Integer, Rule>>> owner; // maps a source (of link) to rules ordered by priority
    private final HashMap<Port, HashSet<Long>> label;
    private final TreeSet<Long> atoms; // red-black tree

    private double s = 0;
    public final Long MIN = 0L, MAX = 1L << 32;

    public double opCnt = 0;

    public AtomVerifier() {
        owner = new HashMap<>();

        atoms = new TreeSet<>();
        atoms.add(MIN); owner.putIfAbsent(MIN, new HashMap<>());
        atoms.add(MAX); owner.putIfAbsent(MAX, new HashMap<>());

        label = new HashMap<>();
    }

    private long rH(long rL, int prefix) {
        return rL + (1L << (32 - prefix));
    }

    private void createInterval(long L, long H, ArrayList<Pair<Long, Long>> ret) {
        if (!atoms.contains(L)) ret.add(new Pair<>(atoms.lower(L), L));
        if (!atoms.contains(H)) ret.add(new Pair<>(atoms.lower(H), H));

        atoms.add(L);
        atoms.add(H);
    }

    /**
     * The half-closed interval previously represented by a needs to
     * be now represented by two atoms instead, namely a and aPrime.
     *
     * @param r the rule to be inserted
     * @return changes in form of {(a, aPrime), ...}
     */
    private ArrayList<Pair<Long, Long>> createAtom(Rule r) {
        long L = r.getMatch().longValue(), H = rH(L, r.getPrefix());
        ArrayList<Pair<Long, Long>> ret = new ArrayList<>();

        // For lpm-match only, each rule is represented as a single interval;
        // In our multi-field extension (suffix-match on src), each rule is represented as many intervals.
        // To avoid long-overflow, we assume the 16-bits src field with 8-bits suffix.
        int wildcards = r.getSrcSuffix() == 0 ? 0 : 16 - r.getSrcSuffix();
        for (int i = 0; i < (1L << wildcards); i ++) {
            long srcEncoding = (i * (1L << r.getSrcSuffix()) + r.getSrc()) << 32;
            createInterval(L + srcEncoding, H + srcEncoding, ret);
        }

        return ret;
    }

    /**
     * @param r the rule to be looked up
     * @return [[interval(r)]]
     */
    private TreeSet<Long> interval(Rule r) {
        TreeSet<Long> ret = new TreeSet<>();
        long L = r.getMatch().longValue(), H = rH(L, r.getPrefix());  // [L, H)

        int wildcards = r.getSrcSuffix() == 0 ? 0 : 16 - r.getSrcSuffix();
        for (int i = 0; i < (1L << wildcards); i ++) {
            long srcEncoding = (i * (1L << r.getSrcSuffix()) + r.getSrc()) << 32;
            ret.addAll(atoms.subSet(L + srcEncoding, H + srcEncoding));
        }

        return ret;
    }

    /**
     * owner[a'] <- owner[a]
     * @param ownerA the value owner[a] to be cloned
     * @return the deep clone, due to each atom maintains related rules in isolation
     */
    @SuppressWarnings("unchecked")
    private HashMap<Device, TreeMap<Integer, Rule>> deepClone(HashMap<Device, TreeMap<Integer, Rule>> ownerA) {
        HashMap<Device, TreeMap<Integer, Rule>> ret = new HashMap<>();

        for (Map.Entry<Device, TreeMap<Integer, Rule>> entry : ownerA.entrySet()) {
            ret.put(entry.getKey(), (TreeMap<Integer, Rule>) entry.getValue().clone());
        }

        return ret;
    }

    public void insertRule(Rule r) {
        s -= System.nanoTime();
        ArrayList<Pair<Long, Long>> change = createAtom(r);

        for (Pair<Long, Long> aToPrime : change) {
            opCnt ++;

            Long a = aToPrime.getFirst(), aPrim = aToPrime.getSecond();
            owner.put(aPrim, deepClone(owner.get(a))); // WARNING: this is a deep clone
            for (TreeMap<Integer, Rule> bst : owner.get(a).values()) { // assert bst != null
                Rule rPrime = bst.lastEntry().getValue();
                label.get(rPrime.getOutPort()).add(aPrim);
            }
        }

        for (Long a : interval(r)) {
            opCnt ++;

            Rule rPrime = null;
            // owner[a] cannot be null, but owner[a][source[r]] may be null
            owner.get(a).putIfAbsent(r.getDevice(), new TreeMap<>());
            TreeMap<Integer, Rule> bst = owner.get(a).get(r.getDevice());
            if (!bst.isEmpty()) {
                rPrime = bst.lastEntry().getValue(); // highest-priority rule at r.device
            }
            if ((rPrime == null) || (rPrime.getPriority() < r.getPriority())) {
                label.putIfAbsent(r.getOutPort(), new HashSet<>());
                label.get(r.getOutPort()).add(a);

                if ((rPrime != null) && (rPrime.getOutPort() != r.getOutPort())) {
                    label.get(rPrime.getOutPort()).remove(a);
                }
            }
            bst.put(r.getPriority(), r);
        }
        s += System.nanoTime();
    }

    public void removeRule(Rule r) {
        s -= System.nanoTime();
        for (Long a : interval(r)) {
            opCnt ++;

            TreeMap<Integer, Rule> bst = owner.get(a).get(r.getDevice());

            if (bst == null || bst.isEmpty()) {
                s += System.nanoTime();
                return;
                // WARNING: it means deletion of a rule not in owner[a]
            }

            Rule rPrime = bst.lastEntry().getValue();
            bst.remove(r.getPriority());
            if (rPrime == r) {
                label.get(r.getOutPort()).remove(a);
                if (!bst.isEmpty()) {
                    Rule rPrimePrime = bst.lastEntry().getValue();
                    label.get(rPrimePrime.getOutPort()).add(a);
                }
            }

            // Extension to remove deleted atom
            if (bst.isEmpty()) {
                owner.get(a).remove(r.getDevice());
                if (owner.get(a).isEmpty()) {
                    owner.remove(a);
                    atoms.remove(a);
                }
            }
        }
        s += System.nanoTime();
    }

    public HashMap<Port, HashSet<Long>> getActionToAtoms() {
        return label;
    }

    public int atomSize() {
        // Notice there may be "empty atom" (which is also mentioned in ddNF's paper)
        return atoms.size() - 1;
    }

    /**
     * Merge t into this
     * @param t t the target verifier to be merged
     */
    public void Aggregate(AtomVerifier t) {
        this.label.putAll(t.label);
        for (Long a : this.atoms) if (!t.atoms.contains(a)) { // scan this.atoms in ascending order
            // if a is not labeled in t, then label to at
            Long at = t.atoms.lower(a);
            for (TreeMap<Integer, Rule> entry : t.owner.get(at).values()) {
                this.label.get(entry.lastEntry().getValue().getOutPort()).add(a);
            }
        }

        // vice versa
        for (Long at : t.atoms) if (!this.atoms.contains(at)) {
            Long a = this.atoms.lower(at);
            for (TreeMap<Integer, Rule> entry : this.owner.get(a).values()) {
                this.label.get(entry.lastEntry().getValue().getOutPort()).add(at);
                this.owner.put(at, deepClone(owner.get(a)));
            }
        }

        this.atoms.addAll(t.atoms);
        for (Long at : t.atoms) { // merge t.devices into this
            // assert this.owner.containsKey(at);
            this.owner.get(at).putAll(t.owner.get(at));
        }
    }

    /**
     * Compute the minimal number of PEC for correctness check
     * @return #PEC
     */
    public int checkPECSize() {
        int ret = 0; // MAX -> \inf is not counted

        HashMap<Long, HashSet<Port>> atomToActions = new HashMap<>();
        for (Map.Entry<Port, HashSet<Long>> entry : label.entrySet()) {
            for (Long atom : entry.getValue()) {
                atomToActions.putIfAbsent(atom, new HashSet<>());
                atomToActions.get(atom).add(entry.getKey());
            }
        }

        HashSet<Long> skip = new HashSet<>();
        for (Long atom : atoms) if (atom != MAX) {
            if (skip.contains(atom)) continue;

            skip.add(atom);
            ret += 1;

            for (Long aPrime : atoms) if (aPrime != MAX) {
                if (skip.contains(aPrime)) continue;
                if (checkEquivalence(atomToActions.get(atom), atomToActions.get(aPrime))) skip.add(aPrime);
            }
        }

        // Notice there may be "empty atom" (which is also mentioned in ddNF's paper)
        if (ret <= 0) ret = 1;
        return ret;
    }

    private boolean checkEquivalence(HashSet<Port> o1, HashSet<Port> o2) {
        if (o1 == o2) return true;
        if (o1 == null || o2 == null) return  false;
        if (o1.size() != o2.size()) return false; // the default actions are not counted
        for (Port p1 : o1) if (!o2.contains(p1)) return false;
        return true;
    }

    public double printTime(int size) {
        if (size == 0) {
            long nsToMs = 1000L * 1000L;
            System.out.println("    Time " + (s / nsToMs) + " ms in total");
        } else {
            long nsToUsPU = 1000L * size;
            System.out.println("    Time " + (s / nsToUsPU) + " us per-update");
        }
        return s;
    }

    public HashMap<Port, HashSet<Integer>> getPortToPredicate() {
        HashMap<Port, HashSet<Integer>> ret = new HashMap<>();

        for (Map.Entry<Port, HashSet<Long>> entry : label.entrySet()) {
            HashSet<Integer> preds = new HashSet<>();
            for (Long atom : entry.getValue()) preds.add(atoms.subSet(0L, atom).size());
            ret.put(entry.getKey(), preds);
        }

        return ret;
    }
}