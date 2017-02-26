package common;

import java.io.Serializable;

/**
 * Created by anthonyaltieri on 2/23/17.
 */
public class Component
    implements Serializable
{
    String name;

    public Component(String name)
    {
        this.name = name;
    }

    @Override
    public String toString()
    {
        return this.name;
    }

    public boolean equals(Object o)
    {
        if (!(o instanceof Component)) return false;
        return (this.hashCode() == ((Component) o).hashCode());
    }

    public int hashCode()
    {
        return this.toString().hashCode();
    }
}
