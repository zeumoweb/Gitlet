package gitlet;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static gitlet.Utils.*;

/**
 * This class represents the staging area which will track changes in our entire directory.
 * It will have two major areas. The Stage Active and Stage Remove area.
 *
 */

public class Index implements Dumpable {

    private static final File INDEX_FILE = Utils.join(Repository.GITLET_DIR, "INDEX");

    /*
    * stageEntry - track the files in the staging area that are ready to be committed.
    * stageRemove - track the files in the staging area that should not be included in the commit.
    */
    private HashMap<String, String> stageEntry = new HashMap<>();
    private  HashMap<String, String> stageRemove = new HashMap<>();



    public Index(){
        saveToFile();
    }


    // saves index object to file
    public void saveToFile(){
        writeObject(INDEX_FILE, this);
    }


    /**
     * reads INDEX object from file
     * @return - Index Object
     */
    public static Index readFromFile(){
        return readObjectFromFile(INDEX_FILE, Index.class);
    }


    /**
     * Adds a file to the staging agrea
     * @param filename
     */
    public void addFileToStage(String filename){
        Commit activeCommit = Commit.getActiveCommit();
        if (filename.equals(".")){
            List<String> files = getPlaneFileNameInDir(Repository.CWD);
            for (String file : files){
                addFile(activeCommit, file);
            }
        }else{
            addFile(activeCommit, filename);
        }
        saveToFile();
    }


    /**
     * Helper method to add file to staging area
     * @param activeCommit
     * @param filename
     */
    private void addFile(Commit activeCommit, String filename){
        Blob blob = new Blob(filename);
        String blob_hash = blob.getHash();
        if (activeCommit.getContent().containsKey(filename)){
            // check if the content of the blob has changed since the most recent commit
            String prev_version = activeCommit.getContent().get(filename);
            if (prev_version.equals(blob_hash)) {
                // remove the file from the staging area if already exist there
                if (stageEntry.containsKey(filename)) stageEntry.remove(filename);
                exitWithError("File Was Not Staged: No change made to file " + filename, 0);
            }
        }

        // display message to user
        System.out.println("File: " + filename + "  was added to staging area for addition");

        if (stageEntry.containsKey(filename)){
            String stageVersion = stageEntry.get(filename);
            if (stageVersion.equals(blob_hash)){
                exitWithError("This version of the file is already staged", 0);
            }
        }
        stageEntry.put(filename, blob_hash);
    }


    // to be completed. Add all the files to the staging area
    public void addAllFilesToStage(){
        List<String> files = getPlaneFileNameInDir(Repository.CWD);
        for (String filename: files){
            Blob blob = new Blob(filename);
            String blob_hash = blob.getHash();
            stageEntry.put(filename, blob_hash);
        }
    }


    public void remove(String filename) {
    	Commit activeCommit = Commit.getActiveCommit();
    	if (stageEntry.containsKey(filename))
    		stageEntry.remove(filename);
    	else if (activeCommit.getContent().containsKey(filename)) {
    		   Blob blob = new Blob(filename);
    	       String blob_hash = blob.getHash();
    	       stageRemove.put(filename, blob_hash);
               Utils.restrictDelete(filename);
    	}
    	 else System.out.print("No reason to remove the file.");
    }



    public HashMap<String, String> getStageEntry(){
        return this.stageEntry;
    }

    public HashMap<String, String> getStageRemove(){
        return this.stageRemove;
    }

    /**
     * Clears stagging area
     */
    public void clear(){
        this.stageRemove.clear();
        this.stageEntry.clear();
        this.saveToFile();
    }

    /**
     * returns true if the staging area is empty and false otherwise
     * @return - Boolean
     */
    public boolean isEmpty(){
        return stageEntry.isEmpty() && stageRemove.isEmpty();
    }

    @Override
    public void dump() {
        System.out.println("Staging index for addition contains...");
        for (Map.Entry content: this.stageEntry.entrySet()) {
            String filename = (String) content.getKey();
            String hash = (String) content.getValue();
            System.out.println(filename + " : " + hash);
        }
    }
}

