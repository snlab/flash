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
            long maskX = (1L << x.getPrefix()) - 1, maskY = (1L << y.getPrefix()) - 1;
            if ((x.getPrefix() & maskX) != (y.getPrefix() & maskY)) return true;
        }

        if (x.getSrcSuffix() > 0 && y.getSrcSuffix() > 0) {
            long maskX = (1L << x.getSrcSuffix() ) - 1, maskY = (1L << y.getSrcSuffix()) - 1;
            if ((x.getSrc() & maskX) != (y.getSrc() & maskY)) return true;
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
        for (Rule r : rules) {
            if (conflicts(r, rule)) continue;
            ret.add(rule);
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
