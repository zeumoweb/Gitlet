package gitlet;

// TODO: any imports you need here
import java.io.File;
import java.io.IOException;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import static gitlet.Utils.*;

/** Represents a gitlet commit object.
 *  TODO: It's a good idea to give a description here of what else this Class
 *  does at a high level.
 *
 *  @author Chudah Yakung
 *  @author Lekane Styve
 */
public class Commit implements Dumpable {

    static final File OBJECT_FOLDER = join(Repository.GITLET_DIR, "objects");

    private String message;
    private String timestamp;
    private String parent;
    private String commitHash;

    private HashMap<String, String> content = new HashMap<>();

    public Commit() {
        this("Initial Commit", null);
    }

    /**
     * Constructor to create a new commit with given message
     * @param message commit message
     */
    public  Commit(String message, String parent){
        this.message = message;
        this.timestamp = this.getCurrentTime();
        this.parent = parent;
    }
    
    /**
     * 
     * @return Date 
     * Gets the current date and time the commit was made
     */
    private String getCurrentTime(){
        DateTimeFormatter dt = DateTimeFormatter.ofPattern("dd-MM-yyyy   HH:mm:ss");
        LocalDateTime dateObj = LocalDateTime.now();
        String time = dateObj.format(dt);
        return "Date: " + time;
    }


    /**
     * 
     * @param String hash
     * this method saves a commit to the object folder using
     * their sha1 values
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
     * 
     * @return Commit object
     * this method return the most recent comit made
     */
    public static Commit getActiveCommit(){
        Head head = Head.readFromFile();
        String commitHash = head.getRef();
        File commit = join(OBJECT_FOLDER, getHashHead(commitHash), getHashBody(commitHash));
        Commit activeCommit = readObjectFromFile(commit, Commit.class);
        return activeCommit;
    }

    /**
     * 
     * @return the Parent of the commit
     */
    public String getParent() {
        return parent;
    }

    /**
     * 
     * @return String Commit message
     */
    public String getMessage(){
        return message;
    }

    public HashMap<String, String> getContent(){
        return content;
    }

    /**
     * 
     * @param  Commit parent
     * Method clones the parent commit
     * 
     */
    public void cloneParentCommit(Commit parent){
        for(Map.Entry<String, String> data : parent.content.entrySet()){
            String filename = data.getKey();
            String hash = data.getValue();
            this.content.put(filename, hash);
        }
    }

    public void updateWithStagedFile(){
        Index stageArea = Index.readFromFile(); // to be cleared and saved back
      
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
     * Methods saves the content of a file -blob
     */
    public void saveBlobs(){
        for (Map.Entry<String, String> file : this.content.entrySet()) {
            Blob blob = new Blob(file.getKey());
            blob.saveBlob();
        }
    }

    @Override
    /**
     * Helper method for printing attributes of a commit
     */
    public void dump() {
        System.out.println("Commit Message: " + this.message);
        System.out.println("Time created: " + this.timestamp);
        System.out.println("Parent " + this.parent);
        System.out.println("commit id " + this.commitHash);
        System.out.println();
        for (Map.Entry<String, String> file : content.entrySet()) {
            System.out.println(file.getKey() + "   " + file.getValue());
        }
    }
    public String getTimestamp(){
        return timestamp;
    }

    /**
     * Starting at the current head commit, display information about 
     * each commit backwards along the commit tree until the initial commit
     */
    public static void logCommit() {
	        Commit commit = getActiveCommit();	        
	        while(commit.parent!=null) {
    	    	System.out.println("=== ");
    	    	System.out.println("Commit:" + " " +commit.commitHash);
    	    	System.out.println("Date:" + " " + commit.timestamp);
    	    	System.out.println("Message:" + " " + commit.message);
    		    commit = readObjectFromFile(getHashAsFile(commit.parent), Commit.class);
    	    	}
	        
	        if(commit.parent==null) {
    	    	System.out.println("=== ");
    	    	System.out.println("Commit:" + " " +commit.commitHash);
    	    	System.out.println("Date:" + " " + commit.timestamp);
    	    	System.out.println("Message:" + " " + commit.message);
    	    	}
	 
   	}
  /**
   * displays information about all commits ever made
   */
  public static void  globalLog() {
	    String[] files= OBJECT_FOLDER.list();      		
  		File temp = join(OBJECT_FOLDER,files[0]);
  		File file=join(temp, getPlaneFileNameInDir(temp).get(0));
		Commit current = readObjectFromFile(file, Commit.class);
		
    	while(current.parent!=null) {
    		System.out.println("=== ");
        	System.out.println("Commit:" + " " + current.commitHash);
        	System.out.println("Date:" + " " + current.timestamp);
        	System.out.println("Message:" + " " + current.message);
        	current= readObjectFromFile(getHashAsFile(current.parent), Commit.class);

    	}
    	if (current.parent==null) {
    	System.out.println("=== ");
    	System.out.println("Commit:" + " " + current.commitHash);
    	System.out.println("Date:" + " " + current.timestamp);
    	System.out.println("Message:" + " " + current.message);
    	}
  }
    	
    /**
     * 
     * @param message
     * Prints out the ids of all commits that have the given commit message
     */
    public static void find(String message) {
    	String[] files= OBJECT_FOLDER.list();      		
    	for (String f: files) {
    		File temp = join(OBJECT_FOLDER,f);
    		File file=join(temp, getPlaneFileNameInDir(temp).get(0));
    		if(file.length()!=0) {
    			Commit currFile = readObjectFromFile(file, Commit.class);
    			if (currFile.message.equals(message)) System.out.println(currFile.commitHash);
    			else System.out.println("Found no commit with that message");
    		}
    		
    	}

    }

}