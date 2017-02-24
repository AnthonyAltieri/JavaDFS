package common;

/** RMI exceptions. */
public class DFSException extends Exception
{
    /** Creates an <code>DFSException</code> with the given message string. */
    public DFSException(String message)
    {
        super(message);
    }

    /** Creates an <code>DFSException</code> with a message string and the given
     cause. */
    public DFSException(String message, Throwable cause)
    {
        super(message, cause);
    }

    /** Creates an <code>DFSException</code> from the given cause. */
    public DFSException(Throwable cause)
    {
        super(cause);
    }
}
