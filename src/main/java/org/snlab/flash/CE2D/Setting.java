package org.snlab.flash.CE2D;

public class Setting {
    public int nDampping;
    public int nCase;
    public long startAt;

    public Setting(int nDampping, int nCase, long startAt) {
        this.nDampping = nDampping;
        this.nCase = nCase;
        this.startAt = startAt;
    }

    @Override
    public String toString() {
        return "Setting{" +
                "nDampping=" + nDampping +
                ", nCase=" + nCase +
                '}';
    }
}
