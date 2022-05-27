package org.snlab.flash.CE2D;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;

public class Automata implements Cloneable {
    private HashSet<String> states;
    private HashSet<String> symbols;
    private String start;
    private HashSet<String> finals;
    private HashMap<String, HashMap<String, String>> transfers;

    public Automata(HashSet<String> symbols) {
        this.states = new HashSet<>();
        this.symbols = symbols;
        this.finals = new HashSet<>();
        this.transfers = new HashMap<>();
//        for (String symbol : symbols) {
//            this.transfers.put(symbol, new HashMap<>());
//        }
    }

    public static Automata fromString(String regex, HashSet<String> symbols) {
        Automata automata = new Automata(symbols);
        automata.addState("start");
        automata.setStart("start");
        automata.addState("error");

        String[] elements = regex.split(",");
        int idx = 0;
        String lastState = "start";
        for (String element : elements) {
            if (element.equals(".*")) {
                for (String sym : symbols) {
                    automata.addTransfer(lastState, sym, lastState);
                }
            } else {
                String newState = String.valueOf(idx++);
                automata.addState(newState);
                automata.addTransfer(lastState, element, newState);
                lastState = newState;
            }
        }
        HashSet<String> finals = new HashSet<>();
        finals.add(lastState);
        automata.setFinals(finals);
        return automata;
    }

    public Object clone()throws CloneNotSupportedException{
        return super.clone();
    }

    public void addState(String label) {
        this.states.add(label);
        this.transfers.put(label, new HashMap<>());
        for (String sym : symbols) {
            this.transfers.get(label).put(sym, "error");
        }
    }

    public void addTransfer(String src, String symbol, String dst) {
        this.transfers.get(src).put(symbol, dst);
    }

    public Automata product(Automata another) {
        Automata result = new Automata(this.symbols);
//        result.addState("error");
        // compute state product
        HashSet<String> states = new HashSet<>();
        for (String s : this.states) {
            for (String s1 : another.getStates()) {
                result.addState(s + "/" + s1);
            }
        }

        // compute transfer product
        for (String s : this.states) {
            for (String s1 : another.getStates()) {
                for (String sym : this.symbols) {
                    result.transfers.get(s + "/" + s1).put(sym, this.transfers.get(s).get(sym) + "/" + another.transfers.get(s1).get(sym));
                }
            }
        }

        // compute finals
        HashSet<String> finals = new HashSet<>();
        for (String fin : this.finals) {
            for (String fin1 : another.getFinals()) {
                finals.add(fin + "/" + fin1);
            }
        }

        result.setStart(this.start + "/" + another.getStart());
        result.setFinals(finals);
        return result;
    }

    public boolean checkValid() {
        HashSet<String> U = new HashSet<>(Arrays.asList("start/start"));
        boolean extend = true;
        while (extend) {
            extend = false;
            HashSet<String> delta = new HashSet<>();
            for (String e : U) {
                for (String next : this.transfers.get(e).values()) {
                    if (!U.contains(next)) {
                        extend = true;
                        delta.add(next);
                    }
                }
            }
            U.addAll(delta);
        }
        return U.containsAll(this.finals);
    }

    public String getStart() {
        return start;
    }

    public void setStart(String start) {
        this.start = start;
    }

    public HashSet<String> getSymbols() {
        return symbols;
    }

    public HashSet<String> getStates() {
        return states;
    }

    public void setStates(HashSet<String> states) {
        this.states = states;
    }

    public void setTransfers(HashMap<String, HashMap<String, String>> transfers) {
        this.transfers = transfers;
    }

    public HashSet<String> getFinals() {
        return finals;
    }

    public void setFinals(HashSet<String> finals) {
        this.finals = finals;
    }

    public static void main(String[] args) {
        HashSet<String> symbols = new HashSet<>(Arrays.asList("a", "b", "c", "d"));
        Automata a = Automata.fromString("a,.*,d", symbols);
        long s = System.nanoTime();
        for (int i = 0; i < 1; i++) {
            Automata b = Automata.fromString("a,b,d", symbols);
            Automata ab = a.product(b);
            System.out.println(ab.checkValid());
        }
        System.out.println(System.nanoTime() - s);
    }
}
