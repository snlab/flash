package org.snlab.flash.model;

import java.util.*;

import org.snlab.network.Device;
import org.snlab.network.Port;

public class PersistentPorts extends Ports {
    private final Port p;
    private final int hash;
    private final PersistentPorts l, r;

    public PersistentPorts() {
        this.hash = 0;
        this.p = null;
        this.l = this.r = null;
    }

    /**
     * Initialization as a balanced binary search tree.
     */
    public PersistentPorts(ArrayList<Port> ports, int lPorts, int rPorts) {
        int ml = (lPorts + rPorts) >> 1;
        this.p = ports.get(ml);
        this.l = (lPorts < ml) ? new PersistentPorts(ports, lPorts, ml) : null;
        this.r = (ml + 1 < rPorts) ? new PersistentPorts(ports, ml + 1, rPorts) : null;

        int h;
        h = this.p.hashCode();
        if (this.l != null) h = 31 * h + this.l.hash;
        if (this.r != null) h = 31 * h + this.r.hash;
        this.hash = h;
    }

    @Override
    public PersistentPorts change(Port change) {
        TreeMap<Integer, Port> mp = new TreeMap<>();
        mp.put(change.getDevice().uid, change);
        return createWithChanges(mp);
    }

    @Override
    public PersistentPorts create(ArrayList<Port> ports, int lPorts, int rPorts) {
        ports.sort(Comparator.comparingInt((Port p) -> p.getDevice().uid));
        return new PersistentPorts(ports, lPorts, rPorts);
    }

    @Override
    public PersistentPorts createWithChanges(TreeMap<Integer, Port> ports) {
        if (ports.size() == 0) return this;
        return new PersistentPorts(ports, this, 0, Device.cnt);
    }

    /**
     * Create an copy of base modified by ports [lPorts, rPorts).
     * The list of ports.device.uid must be contained in base.
     */
    private PersistentPorts(TreeMap<Integer, Port> ports, PersistentPorts base, int l, int r) {
        // binary search [lPorts, rPorts) to find ports[ml].uid == m
        // invariants: ports[ml] <= m, ports[mr] > m
        int m = base.p.getDevice().uid;
        if (ports.containsKey(m)) {
            this.p = ports.get(m);
        } else {
            this.p = base.p;
        }
        this.l = ports.subMap(l, m).isEmpty() ? base.l : new PersistentPorts(ports, base.l, l, m);
        this.r = ports.subMap(m + 1, r).isEmpty() ? base.r : new PersistentPorts(ports, base.r, m + 1, r);

        int h;
        h = this.p.hashCode();
        if (this.l != null) h = 31 * h + this.l.hash;
        if (this.r != null) h = 31 * h + this.r.hash;
        this.hash = h;
    }

    private void getAll(LinkedList<Port> ret) {
        ret.add(p);
        if (this.l != null) this.l.getAll(ret);
        if (this.r != null) this.r.getAll(ret);
    }

    @Override
    public LinkedList<Port> getAll() {
        LinkedList<Port> ret = new LinkedList<>();
        this.getAll(ret);
        return ret;
    }

    @Override
    public int hashCode() {
        return this.hash;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o instanceof PersistentPorts && this.hash == ((PersistentPorts) o).hash) {
            return Objects.equals(this.l, ((PersistentPorts) o).l) && Objects.equals(this.r, ((PersistentPorts) o).r);
        }
        return false;
    }
}
