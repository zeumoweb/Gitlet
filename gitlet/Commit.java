package gitlet;

// TODO: any imports you need here
import java.io.File;
import java.io.IOException;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import static gitlet.Utils.*;

/**
 *  Represents a gitlet commit object.
 *  This class will contain some of the fundamental methods are attributes of a commit Object.
 *  @author Chudah Yakung
 *  @author Lekane Styve
 */
public class Commit implements Dumpable, Comparable {

    static final File OBJECT_FOLDER = join(Repository.GITLET_DIR, "objects"); // to be filled
    static final File REFS_FOLDER = join(Repository.GITLET_DIR, "refs"); // to be filled


    private String message;
    private String timestamp;
    private String parent;
    private String commitHash;

    private HashMap<String, String> content = new HashMap<>();

    /**
     * Constructor to create a new commit with given message
     * @param message commit message
     */
    public Commit() {
        this("Initial Commit", null);
    }

    public  Commit(String message, String parent){
        this.message = message;
        this.timestamp = this.getCurrentTime();
        this.parent = parent;
    }


    /**
     * Clones the content of its parent commit
     * @param  Commit parent
     */
    public void cloneParentCommit(Commit parent){
        for(Map.Entry<String, String> data : parent.content.entrySet()){
            String filename = data.getKey();
            String hash = data.getValue();
            this.content.put(filename, hash);
        }
    }


    /**
     * Gets the current date and time the commit was made
     * @return Date
     */
    private String getCurrentTime(){
        DateTimeFormatter dt = DateTimeFormatter.ofPattern("dd-MM-yyyy   HH:mm:ss");
        LocalDateTime dateObj = LocalDateTime.now();
        String time = dateObj.format(dt);
        return "Date: " + time;
    }


    /**
     * Returns the commit object with the given commit id
     * @param id - Commit Id
     * @return - Commit object
     */
    public static Commit getCommit(String id){
        File file = getHashAsFile(id);
        Commit commit = readObjectFromFile(file, Commit.class);
        return commit;
    }


    /**
     * This method saves a commit Object to the object folder using their sha1 values as name.
     * @param String hash
     */
    public void saveToFile(String hash){
        this.commitHash = hash;

        File dir = Utils.join(OBJECT_FOLDER, getHashHead(hash));
        File file = Utils.join(OBJECT_FOLDER, getHashHead(hash), getHashBody(hash));
        dir.mkdir();

        try {
            file.createNewFile();
            Utils.writeObject(file, this);
        }
        catch (IOException E){
            exitWithError("Could not save commit to file", 0);
        }
    }


    /**
     * this method return the most recent commit made (The active commit)
     * @return Commit object
     */
    public static Commit getActiveCommit(){
        Head head = Head.readFromFile();
        Branch activeBranch = Branch.getActiveBranch();
        String commitHash = activeBranch.getRef();
        File commit = getHashAsFile(commitHash);
        Commit activeCommit = readObjectFromFile(commit, Commit.class);
        return activeCommit;
    }


    /**
     * Update the content of the commit object with the information found in the staging area.
     */
    public void updateWithStagedFile(){
        Index stageArea = Index.readFromFile();

        if (stageArea.getStageEntry().isEmpty() && stageArea.getStageRemove().isEmpty()){
            exitWithError("No File To Commit! Use the add command to add files", 0);
        }

        this.content.putAll(stageArea.getStageEntry()); // update the commit content with the files in staging area for addition
        // delete all the files present in current commit and in stage for removal area
        for (Map.Entry<String, String> file : stageArea.getStageRemove().entrySet()) {
            this.content.remove(file.getKey());
        }
        stageArea.clear();
        stageArea.saveToFile();
    }


    /**
     * saves all the content of the commit in the object folder
     */
    public void saveBlobs(){
        for (Map.Entry<String, String> file : this.content.entrySet()) {
            Blob blob = new Blob(file.getKey());
            blob.saveBlob();
        }
    }


    /** Getter Methods **/

    public String getParent() {
        return parent;
    }

    public String getMessage(){
        return message;
    }

    public HashMap<String, String> getContent(){
        return content;
    }

    public String getTimestamp(){
        return timestamp;
    }

    public String getCommitHash(){return commitHash; }


    /**
     * Starting at the current head commit, display information about
     * each commit backwards along the commit tree until the initial commit
     */
    public static void logCommit() {
	        Commit current = getActiveCommit();

	        while(current.parent!=null) {
                displayLog(current.commitHash, current.timestamp, current.message);
    		    current= readObjectFromFile(getHashAsFile(current.parent), Commit.class);
            }
            displayLog(current.commitHash, current.timestamp, current.message);
    }


    /**
     * displays information about all commits across all branches
     */
    public static void  globalLog() {
        List<String>branches =  getPlaneFileNameInDir(join(Commit.REFS_FOLDER, "heads"));
        HashSet<Commit> commitSet = new HashSet<>(); // for storing commit objects
        HashSet<String> commitIDSet = new HashSet<>();  // for storing commit ids
        Commit current = null;
        for(String branchName :branches){

            if (branchName.equals("HEAD")) continue;

            else {
                Branch branch = Branch.getBranch(branchName);
                current = getCommit(branch.getRef());

                while(current.parent!=null){
                    if (commitIDSet.add(current.commitHash)){
                        commitSet.add(current);
                    }
                    current= readObjectFromFile(getHashAsFile(current.parent), Commit.class);
                }
            }
        }
        commitSet.add(current);

        for (Commit commit: commitSet){
            displayLog(commit.commitHash, commit.timestamp, commit.message);
        }
    }


    // To be modify after branching implementation
    public static void find(String message) {
    	Commit current = getActiveCommit();

        while(current.parent!=null) {
            if(current.message.contains(message)){
                System.out.println(current.commitHash);
            }
            current= readObjectFromFile(getHashAsFile(current.parent), Commit.class);
        }
    }


    private static void displayLog(String commit, String time, String message){
        System.out.println("=== ");
        System.out.println("Commit:" + " " + commit);
        System.out.println("Date:" + " " + time);
        System.out.println("Message:" + " " + message);
    }


    @Override
    public void dump() {
        System.out.println("Commit Message: " + this.message);
        System.out.println("Time created: " + this.timestamp);
        System.out.println("Parent " + this.parent);
        System.out.println();
    }


    @Override
    public int compareTo(Object o){
        Commit commit = (Commit) o;
        if (this.commitHash.equals(commit.commitHash)){
            return 1;
        }
        return -1;
    }

}