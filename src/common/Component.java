package common;

/**
 * Created by anthonyaltieri on 2/23/17.
 */
public class Component
{
    Type type;
    String name;

    Component(Type type, String name)
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
            case FILE:
                typeString = "File";
                break;
            case DIRECTORY:
                typeString = "Directory";
                break;
        }
        return typeString + ": " + this.name;
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
}
