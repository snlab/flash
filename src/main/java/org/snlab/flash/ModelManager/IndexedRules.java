package org.snlab.flash.ModelManager;

import java.util.ArrayList;
import java.util.HashSet;

import org.snlab.network.Rule;

// Before checking whether two matches are overlapped using BDD-intersection, there is a simple quick check:
//    -  if there exists a pair of conflicting non-wildcard bits, e.g., **10 v.s. 0*0* conflicts at the 3rd bit.
//
// We build a Trie to check conflicts between lpm-match part;
// Notice ternary-match is still allowed, but we do not build index on it.
//
// This index is used by both of APKeepStar and Flash.
// It has performance impact on Internet2 dataset (while a linear scanning is fine for other dataset).
public class IndexedRules {
    HashSet<Rule> rules;
    IndexedRules left, right, dst;

    public IndexedRules() {
        rules = new HashSet<>();
        left = right = null;
    }

    private IndexedRules buildNext(int flag) {
        if (flag == 0) {
            if (this.left == null) {
                this.left = new IndexedRules();
            }
            return this.left;
        } else {
            if (this.right == null) {
                this.right = new IndexedRules();
            }
            return this.right;
        }
    }

    private void rm(Rule rule) {
        this.rules.remove(rule);
    }

    private void add(Rule rule) {
        this.rules.add(rule);
    }

    private HashSet<Rule> getRules() {
        return this.rules;
    }

    private void explore(ArrayList<Rule> ret) {
        if (this.left != null) this.left.explore(ret);
        if (this.right != null) this.right.explore(ret);
        ret.addAll(this.getRules());
    }

    public void read(Rule rule, ArrayList<Rule> ret, int size) {
        if (this.dst == null) return;
        IndexedRules t = this.dst;
        if (ret != null) ret.addAll(t.getRules());

        long dstIp = rule.getMatch().longValue();
        for (int i = 0; i < rule.getPrefix() - (32 - size); i++) {
            long bit = (dstIp >> (size - 1 - i)) & 1;
            t = (bit == 0) ? t.left : t.right;
            if (t == null) return;

            if (ret != null) ret.addAll(t.getRules());
        }

        if (ret != null) t.explore(ret);
    }

    private void exploreSrc(Rule rule, ArrayList<Rule> ret, int size) {
        if (this.left != null) {
            this.left.read(rule, ret, size);
            this.left.exploreSrc(rule, ret, size);
        }
        if (this.right != null) {
            this.right.read(rule, ret, size);
            this.right.exploreSrc(rule, ret, size);
        }
    }

    public IndexedRules traverse(Rule rule, int size) {
        if (this.dst == null) this.dst = new IndexedRules();
        IndexedRules t = this.dst;

        long dstIp = rule.getMatch().longValue();
        for (int i = 0; i < rule.getPrefix() - (32 - size); i++) {
            long bit = (dstIp >> (size - 1 - i)) & 1;
            t = t.buildNext(bit == 0 ? 0 : 1);
        }

        return t;
    }

    public IndexedRules traverseSrc(Rule rule, ArrayList<Rule> ret, int size) {
        IndexedRules t = this;
        if (ret != null) t.read(rule, ret, size);

        long srcIp = rule.getSrc();
        for (int i = 0; i < rule.getSrcSuffix(); i++) {
            long bit = (srcIp >> i) & 1;
            t = t.buildNext(bit == 0 ? 0 : 1);
            if (ret != null) t.read(rule, ret, size);
        }

        if (ret != null) t.exploreSrc(rule, ret, size);
        return t.traverse(rule, size);
    }

    /**
     * @param rule the target rule
     * @return all rules overlapped with the target rule
     *         here "overlapping" means there is no conflict between prefix-matches (ternary-match is not checked here)
     */
    public ArrayList<Rule> getAllOverlappingWith(Rule rule, int size) {
        ArrayList<Rule> ret = new ArrayList<>();
        this.traverseSrc(rule, ret, size);
        return ret;
    }

    public void remove(Rule rule, int size) {
        this.traverseSrc(rule, null, size).rm(rule);
    }

    public void insert(Rule rule, int size) {
        this.traverseSrc(rule, null, size).add(rule);
    }
}