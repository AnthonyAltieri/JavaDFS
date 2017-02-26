package rmi;




import java.io.Serializable;
import java.lang.reflect.Method;
import java.lang.reflect.Type;

class Shuttle implements Serializable
{
    int hashCode;
    Pair<Type, Object>[] args;
    Class<?>[] paramTypes;
    String methodString;
    Type returnType;
    String name;

    Shuttle(Method method, Pair<Type, Object>[] args)
    {
        this.hashCode = method.hashCode();
        this.args = args;
        this.methodString = method.toString();
        this.returnType = method.getGenericReturnType();
        this.paramTypes = method.getParameterTypes();
        this.name = method.getName();
    }

    public String toString()
    {
        String string = "[SHUTTLE] " + methodString + "(";
        if (this.args == null)
        {
            string += ")";
            return string;
        }
        for (int i = 0 ; i < this.args.length ; i++)
        {
            Pair<Type, Object> pair = this.args[i];
            string += "{" + pair.getKey().toString() + "} {" + pair.getValue().toString() + "}";
            if (i != this.args.length - 1)
                string += ", ";
        }
        string += ")";
        return string;
    }
}

