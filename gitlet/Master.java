package gitlet;

import java.io.File;

/**
 * This class represents the state of the Master branch and it always references the must recent commit
 */
public class Master implements Dumpable {

    // current commit being referenced by the Master node
    private String ref = null;
    private static final File MASTER_FILE = Utils.join(Repository.GITLET_DIR, "refs", "heads", "MASTER");


    public String getRef(){
        return ref;
    }

    public void updateRef(String commit){
        this.ref = commit;
        this.saveToFile();
    }

    // saves head object
    public void saveToFile(){
        Utils.writeObject(MASTER_FILE, this);
    }

    /**
     * reads Master object from file
     * @return Master object
     */
    public static Master readFromFile(){
        return Utils.readObjectFromFile(MASTER_FILE, Master.class);
    }

    @Override
    public void dump() {
        System.out.println("Master References: " + this.ref);
    }
}
