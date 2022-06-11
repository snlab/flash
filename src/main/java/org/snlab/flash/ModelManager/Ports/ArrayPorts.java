package org.snlab.flash.ModelManager.Ports;

import java.util.*;

import org.snlab.network.Port;

// Simply store ports as an array list
public class ArrayPorts extends Ports {
    private final HashMap<Integer, Integer> uidToPos;
    private final Port[] ports;
    private int hash;

    public ArrayPorts() {
        this.uidToPos = null;
        this.ports = null;
    }

    @Override
    public Ports create(ArrayList<Port> ports, int lPorts, int rPorts) {
        return new ArrayPorts(ports, lPorts, rPorts);
    }

    public ArrayPorts(List<Port> ports, int lPorts, int rPorts) {
        ports.sort(Comparator.comparingInt((Port p) -> p.getDevice().uid));
        this.hash = 0;
        this.ports = new Port[rPorts];
        this.uidToPos = new HashMap<>();
        for (int i = lPorts; i < rPorts; i ++) {
            Port p = ports.get(i);
            this.uidToPos.put(p.getDevice().uid, i);
            this.ports[i] = p;
            this.hash ^= p.hashCode();
        }
    }

    public ArrayPorts(Port[] ports, int hash, HashMap<Integer, Integer> uidToPos) {
        this.hash = hash;
        this.ports = ports;
        this.uidToPos = uidToPos;
    }

    @Override
    public ArrayPorts change(Port change) {
        this.hash ^= this.ports[uidToPos.get(change.getDevice().uid)].hashCode() ^ change.hashCode();
        this.ports[uidToPos.get(change.getDevice().uid)] = change;
        return this;
    }

    @Override
    public ArrayPorts createWithChanges(TreeMap<Integer, Port> changes) {
        int newHash = hash;
        Port[] newPorts = ports.clone();

        for (Map.Entry<Integer, Port> entry : changes.entrySet()) {
            newHash ^= this.ports[uidToPos.get(entry.getKey())].hashCode() ^ entry.getValue().hashCode();
            newPorts[uidToPos.get(entry.getKey())] = entry.getValue();
        }

        return new ArrayPorts(newPorts, newHash, uidToPos);
    }

    @Override
    public Collection<Port> getAll() {
        return Arrays.asList(ports);
    }

    @Override
    public int hashCode() {
        return this.hash;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o instanceof ArrayPorts && this.hash == ((ArrayPorts) o).hash
                && this.ports.length == ((ArrayPorts) o).ports.length) {
            for (int i = 0; i < this.ports.length; i ++) {
                if (ports[i] != ((ArrayPorts) o).ports[i]) return false;
            }
            return true;
        }
        return false;
    }
}
