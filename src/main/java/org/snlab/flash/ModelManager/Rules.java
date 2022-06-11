package org.snlab.flash.ModelManager;

import java.util.ArrayList;
import java.util.HashSet;

import org.snlab.network.Rule;

public class Rules {
    HashSet<Rule> rules;

    public Rules() {
        rules = new HashSet<>();
    }

    boolean conflicts(Rule x, Rule y) {
        if (x.getPriority() == y.getPriority()) return true;

        if (x.getPrefix() > 0 && y.getPrefix() > 0) {
            long maskX = (1L << x.getPrefix()) - 1, maskY = (1L << y.getPrefix()) - 1, mask = maskX & maskY;
            if ((x.getPrefix() & mask) != (y.getPrefix() & mask)) return true;
        }

        if (x.getSrcSuffix() > 0 && y.getSrcSuffix() > 0) {
            long maskX = (1L << x.getSrcSuffix() ) - 1, maskY = (1L << y.getSrcSuffix()) - 1, mask = maskX & maskY;
            if ((x.getSrc() & mask) != (y.getSrc() & mask)) return true;
        }

        return false;
    }

    /**
     * @param rule the target rule
     * @return all rules overlapped with the target rule
     *         here "overlapping" means there is no conflict between non-wildcards bits
     */
    public ArrayList<Rule> getAllOverlappingWith(Rule rule, int size) {
        ArrayList<Rule> ret = new ArrayList<>();

        // For the purpose of evaluation, we use a generic linear scan;
        // Instead of a Tire tree which is optimized for lpm-match.
        for (Rule r : rules) {
            if (conflicts(r, rule)) continue;
            ret.add(r);
        }

        return ret;
    }

    public void remove(Rule rule, int size) {
        this.rules.remove(rule);
    }

    public void insert(Rule rule, int size) {
        this.rules.add(rule);
    }
}
