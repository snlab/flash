package org.snlab.flash.model;


import java.util.ArrayList;

import org.snlab.network.Rule;

public class TrieRules {
    ArrayList<Rule> rules;
    TrieRules left, right, dst;

    public TrieRules() {
        rules = new ArrayList<>();
        left = right = null;
    }

    private TrieRules buildNext(int flag) {
        if (flag == 0) {
            if (this.left == null) {
                this.left = new TrieRules();
            }
            return this.left;
        } else {
            if (this.right == null) {
                this.right = new TrieRules();
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

    private ArrayList<Rule> getRules() {
        return this.rules;
    }

    private void explore(ArrayList<Rule> ret) {
        if (this.left != null) this.left.explore(ret);
        if (this.right != null) this.right.explore(ret);
        ret.addAll(this.getRules());
    }

    public void read(Rule rule, ArrayList<Rule> ret, int size) {
        if (this.dst == null) return;
        TrieRules t = this.dst;
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

    public TrieRules traverse(Rule rule, int size) {
        if (this.dst == null) this.dst = new TrieRules();
        TrieRules t = this.dst;

        long dstIp = rule.getMatch().longValue();
        for (int i = 0; i < rule.getPrefix() - (32 - size); i++) {
            long bit = (dstIp >> (size - 1 - i)) & 1;
            t = t.buildNext(bit == 0 ? 0 : 1);
        }

        return t;
    }

    public TrieRules traverseSrc(Rule rule, ArrayList<Rule> ret, int size) {
        TrieRules t = this;
        if (ret != null) t.read(rule, ret, size);

        long srcIp = rule.getSrc();
        for (int i = 0; i < rule.getSrcPrefix(); i++) {
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
