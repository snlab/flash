package org.snlab.network;

public class ACLUse {
    String aclnumber;
    String interfacename;
    String direction;

    public ACLUse(String aclnumber, String interfacename, String direction)
    {
        this.aclnumber = aclnumber;
        this.interfacename = interfacename;
        this.direction = direction;
    }

    public boolean isin()
    {
        if(direction.equals("in"))
        {
            return true;
        }else
        {
            return false;
        }
    }

    public String getnumber()
    {
        return aclnumber;
    }

    public String getinterface()
    {
        return interfacename;
    }

    public String toString()
    {
        return aclnumber + " " + interfacename + " " + direction;
    }

}