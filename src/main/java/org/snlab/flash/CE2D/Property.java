package org.snlab.flash.CE2D;

import org.snlab.network.Device;

import dk.brics.automaton.Automaton;
import dk.brics.automaton.RegExp;

public class Property {
    private Device source;
    private Automaton automaton;
    private long hs;
    private int hsbdd;

    public Property(Device source, long hs, Automaton automaton) {
        this.source = source;
        this.hs = hs;
        this.automaton = automaton;
    }

    public Property(Device source, Automaton automaton) {
        this.source = source;
        this.hs = 1;
        this.automaton = automaton;
    }

    public Device getSource() {
        return source;
    }

    public long getHs() {
        return hs;
    }

    public void setHs(int hs) {
        this.hs = hs;
    }

    public int getHsbdd() {
        return hsbdd;
    }

    public void setHsbdd(int hsbdd) {
        this.hsbdd = hsbdd;
    }

    public Automaton getAutomata() {
        return automaton;
    }

    public static void main(String[] args) {

        long s = System.nanoTime();
        for (int i = 0; i < 100; i++) {
            RegExp regExp = new RegExp("ssw-9-999virtual-pod-0");
            regExp.toAutomaton(false);
        }
        System.out.println("test time: " + (System.nanoTime() - s));
    }
}
