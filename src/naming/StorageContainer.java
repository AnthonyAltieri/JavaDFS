package naming;

import storage.Command;
import storage.Storage;

/**
 * Created by anthonyaltieri on 2/28/17.
 */
public class StorageContainer
{
    Storage storage;
    Command command;

    StorageContainer(Storage storage, Command command)
    {
        this.storage = storage;
        this.command = command;
    }

    public Command getCommand()
    {
        return command;
    }

    public Storage getStorage()
    {
        return storage;
    }

    public String toString()
    {
        String string = "[StorageServer| Command: " + this.command + ", Storage";
        string = string + ": " + this.storage;
        return string;
    }
}
