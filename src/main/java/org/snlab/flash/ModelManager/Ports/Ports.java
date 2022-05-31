package org.snlab.flash.ModelManager.Ports;


import java.util.ArrayList;
import java.util.Collection;
import java.util.TreeMap;

import org.snlab.network.Port;

public abstract class Ports {
    public abstract Ports create(ArrayList<Port> ports, int lPorts, int rPorts);
    public abstract Ports change(Port change);
    public abstract Ports createWithChanges(TreeMap<Integer, Port> changes);
    public abstract Collection<Port> getAll();
}
