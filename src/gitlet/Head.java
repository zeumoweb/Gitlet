package gitlet;

import java.io.File;
import java.io.Serializable;

/**
 * This class represents the Head which references the currently active commit
 */
public class Head implements Dumpable {

    // current commit being referenced by the Head node
    private String ref = null;

    private static final File HEAD_FILE = Utils.join(Repository.GITLET_DIR, "refs", "heads", "HEAD");

    public String getRef(){
        return ref;
    }

    public void updateRef(String commit){
        this.ref = commit;
        this.saveToFile();
    }

    // saves head object
    public void saveToFile(){
        Utils.writeObject(HEAD_FILE, this);
    }

    /**
     * reads head object from file
     * @return - the head object
     */
    public static Head readFromFile(){
        return Utils.readObjectFromFile(HEAD_FILE, Head.class);
    }

    @Override
    public void dump() {
        System.out.println("HEAD References: " + this.ref);
    }
}