package org.snlab.flash.CE2D;

import org.jgrapht.graph.DefaultEdge;
import org.snlab.network.Port;

public class PGEdge extends DefaultEdge {
    public Port port;

    public PGEdge(Port port) {
        this.port = port;
    }
}
