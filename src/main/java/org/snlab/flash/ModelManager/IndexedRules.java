package org.snlab.flash.ModelManager;

import java.util.ArrayList;
import java.util.HashSet;

import org.snlab.network.Rule;

// Build tire-index for prefix-match to reduce the scope of "overlapped rules".
// Notice the ternary-match is allowed, but still indexed by it prefix-part (no prefix is concerned as a 0-length prefix).
//
// It works best for dataset whose
// (1) most rules use lpm-match (makes the index efficient)
// and (2) some rules use ternary-match (lpm-specific approach does no work).
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

    public void read(Rule rule, ArrayList<Rule> ret, int length) {
        if (this.dst == null) return;
        IndexedRules t = this.dst;
        if (ret != null) ret.addAll(t.getRules());

        long dstIp = rule.getMatch().longValue();
        for (int i = 0; i < rule.getPrefix() - (32 - length); i++) {
            long bit = (dstIp >> (length - 1 - i)) & 1;
            t = (bit == 0) ? t.left : t.right;
            if (t == null) return;

            if (ret != null) ret.addAll(t.getRules());
        }

        if (ret != null) t.explore(ret);
    }

    public IndexedRules traverse(Rule rule, int length) {
        if (this.dst == null) this.dst = new IndexedRules();
        IndexedRules t = this.dst;

        long dstIp = rule.getMatch().longValue();
        for (int i = 0; i < rule.getPrefix() - (32 - length); i++) {
            long bit = (dstIp >> (length - 1 - i)) & 1;
            t = t.buildNext(bit == 0 ? 0 : 1);
        }

        return t;
    }

    /**
     * @param rule the target rule
     * @return all rules overlapped with the target rule
     *         here "overlapping" means there is no conflict between prefix-matches
     */
    public ArrayList<Rule> getAllOverlappingWith(Rule rule, int length) {
        ArrayList<Rule> ret = new ArrayList<>();
        this.read(rule, ret, length);
        return ret;
    }

    public void remove(Rule rule, int length) {
        this.traverse(rule, length).rm(rule);
    }

    public void insert(Rule rule, int length) {
        this.traverse(rule, length).add(rule);
    }
}
