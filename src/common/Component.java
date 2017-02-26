package common;

import java.io.Serializable;

/**
 * Created by anthonyaltieri on 2/23/17.
 */
public class Component
    implements Serializable
{
    Type type;
    String name;

    public Component(Type type, String name)
    {
        this.type = type;
        this.name = name;
    }

    @Override
    public String toString()
    {
        String typeString = null;
        switch (this.type)
        {
            case ROOT:
                return "/";
            case CHILD:
                return this.name;
            default:
                return "";
        }
    }

    public boolean equals(Object o)
    {
        if (!(o instanceof Component)) return false;
        return (this.hashCode() == ((Component) o).hashCode());
    }

    public int hashCode()
    {
        String hashString = "" + this.type + this.name;
        return hashString.hashCode();
    }

    public boolean isRoot()
    {
        return this.type == Type.ROOT;
    }

    public String getName()
    {
        return this.name;
    }
}
