package gitlet;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

import static gitlet.Utils.*;

// TODO: any imports you need here

/** Represents a gitlet repository.
 *  TODO: It's a good idea to give a description here of what else this Class
 *  does at a high level.
 *  
 *  @author Chudah Yakung
 *  @author Styve Lekane
 */
public class Repository {

    //The current working directory.
    public static final File CWD = new File(System.getProperty("user.dir"));

    // The .gitlet directory.
    public static final File GITLET_DIR = join(CWD, ".gitlet");
    
	/**
	 * 
	 * @param args
	 * initializes the respository
	 */
	  static void initializeRepo(String[] args) {

        validateNumArgs("init", args, 1 );
        setUpFolderStructure();

        // create initial commit
        Commit initCommit = new Commit();

        // create HEAD, MASTER, and, INDEX Objects and save them
        Head head = new Head();
        Master master = new Master();
        Index stageArea = new Index();

        // set reference to initial commit
        String commitHash = hashObj(initCommit);
        head.updateRef(commitHash);
        master.updateRef(commitHash);
        initCommit.saveToFile(commitHash);
        initCommit.dump();
    }

    
	/**
	 * 
	 * @param msg
	 * makes a commit
	 */
    public static void makeCommit(String msg){
        // Get active/parent commit id
        Head head = Head.readFromFile(); // to be updated at the end of the command
        Master master = Master.readFromFile(); // to be updated
        Commit commit = new Commit(msg, head.getRef());
        File commitFile = getHashAsFile(head.getRef());
        Commit activeCommit = readObjectFromFile(commitFile, Commit.class);
        commit.cloneParentCommit(activeCommit);
        commit.updateWithStagedFile();
        commit.saveBlobs();
        String commitHash = hashObj(commit);
        commit.dump();
        head.updateRef(commitHash);
        master.updateRef(commitHash);
        commit.saveToFile(commitHash);
    }

   /**
    *  Displays what branches currently exist, and marks the current branch with a *
    *  Also displays what files have been staged for addition or removal
    */

    public static void viewStatus(){
        Index stageArea = Index.readFromFile();
        Commit activeCommit = Commit.getActiveCommit();
        List<String> filesInDir = getPlaneFileNameInDir(Repository.CWD);
        List<String> untrackFiles = new ArrayList<>();
        List<String> modifiedFiles = new ArrayList<>();
        List<String> toRemoveFiles = new ArrayList<>();
        List<String> toAddFiles = new ArrayList<>();
        HashMap<String, String> stageEntry = stageArea.getStageEntry();
        HashMap<String, String> stageRemove = stageArea.getStageRemove();

        for (String filename : filesInDir) {
            if (!activeCommit.getContent().containsKey(filename)){
                untrackFiles.add(filename);
            }else {
                String fileHash = activeCommit.getContent().get(filename);
                String currentFileHash= sha1(readContents(join(CWD, filename)));
                if (!fileHash.equals(currentFileHash)){
                    modifiedFiles.add(filename);
                }
            }

            if (stageRemove.containsKey(filename)){
                toRemoveFiles.add(filename);
            }else if (stageEntry.containsKey(filename)){
               toAddFiles.add(filename);
               untrackFiles.remove(filename);
            }
        }
        displayStatus("Branches", Arrays.asList("*Master"));
        displayStatus("Staged Files For Addition", toAddFiles);
        displayStatus("Stage For Removed Files", toRemoveFiles);
        displayStatus("Modifications Not Staged For Commit", modifiedFiles);
        displayStatus("Untracked Files", untrackFiles);
        stageArea.saveToFile();
    }

    // chechout command first version
    public static  void checkout(String filename){
        Commit activeCommit = Commit.getActiveCommit();
        File file = join(CWD, filename);
        if (!activeCommit.getContent().containsKey(filename)){
            exitWithError("File does not exist in that commit.", 0);
        }
        try{
            if(!file.isFile()) file.createNewFile();
        }
        catch (IOException e){
            exitWithError("Could Not Create File " + filename, 0 );
        }

        String fileHash = activeCommit.getContent().get(filename);
        File fileBlob = getHashAsFile(fileHash);
        byte[] blobContent = readContents(fileBlob);
        writeContents(file, blobContent);
    }

    // checkout second version
    public static void checkout(String commitId, String filename){
        File commitFile = getHashAsFile(commitId);
        if (!commitFile.isFile()){
            exitWithError("No commit with that id exists.", 0);
        }
        Commit commit = readObjectFromFile(commitFile, Commit.class);
        String fileHash = commit.getContent().get(filename);
        if (fileHash == null){
            exitWithError("File does not exist in that commit.", 0);
        }
        File fileBlob = getHashAsFile(fileHash);
        byte[] fileContent = readContents(fileBlob);
        File file = join(CWD, filename);
        writeContents(file, fileContent);
    }

    // checkout third version
    public  static  void chekoutBranch(String branchName){
        // to be completed
    }

    private static void displayStatus(String type, List<String> content){
        System.out.println("");
        System.out.println("============== " + type + " ==============");
        for(String filename : content){
            System.out.println(filename);
        }
    }

    /**
     * This method will create the .gitlet folder and all the neccessary sub-folders.
     */
    private static void setUpFolderStructure(){
        if (GITLET_DIR.isDirectory()) {
            exitWithError("A Gitlet version-control system already exists in the current directory.", -1);
        }
        GITLET_DIR.mkdir();
        File objects = join(GITLET_DIR, "objects");
        File refs = join(GITLET_DIR, "refs");
        File heads = join(GITLET_DIR, "refs", "heads");
        File head = join(GITLET_DIR, "refs", "heads", "HEAD");
        File master = join(GITLET_DIR, "refs", "heads", "MASTER");
        File index = join(GITLET_DIR, "INDEX");
        try{
            objects.mkdir();
            refs.mkdir();
            heads.mkdir();
            head.createNewFile();
            master.createNewFile();
            index.createNewFile();

            /* Set the gitlet folder as hidden*/
            Path path = FileSystems.getDefault().getPath(CWD.getPath(), ".gitlet");
            Files.setAttribute(path, "dos:hidden", true);
        }
        catch (IOException E){
            throw new GitletException();
        }
    }

  


}
