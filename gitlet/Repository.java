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
 *  @author Lekane Styve
 *  @author Chudah Yakung
 *  
 */
public class Repository {

    public static final File CWD = new File(System.getProperty("user.dir"));

    // The .gitlet directory.
    public static final File GITLET_DIR = join(CWD, ".gitlet");



    /**
     *  This method creates an empty github repository and sets the initial commit.
     *  @param args
     */
    static void initializeRepo(String[] args) {

        validateNumArgs("init", args, 1 );
        setUpFolderStructure();

        // create initial commit
        Commit initCommit = new Commit();

        // create HEAD, MASTER, and, INDEX Objects and save them
        Head head = new Head();
        Branch master = new Branch("master");
        Index stageArea = new Index();

        // set reference to initial commit
        String commitHash = hashObj(initCommit);
        head.updateRef("master");
        master.updateRef(commitHash);
        initCommit.saveToFile(commitHash);
        System.out.println("New getlet repository initialized!");
    }


    /**
     * add a file to the staging area
     * @param filename
     */
    public static void add(String filename){
        Index stageArea = Index.readFromFile();
        stageArea.addFileToStage(filename);
    }

    public static void rm(String filename) {
        Index indexArea = Index.readFromFile();
        indexArea.remove(filename);
    }

    public static void mergeBranch(String branchName) {
        Branch activeBranch =  Branch.getActiveBranch();
        activeBranch.merge(branchName);
    }

    /** COMMIT RELATED METHODS **/


    /**
     * This methods create a commit with a given message and saves a snapshot of the working directory
     * @param msg
     */
    public static void makeCommit(String msg){
        // Get active/parent commit id
        Branch activeBranch = Branch.getActiveBranch();
        Commit commit = new Commit(msg, activeBranch.getRef());
        Commit activeCommit = Commit.getActiveCommit();
        commit.cloneParentCommit(activeCommit);
        commit.updateWithStagedFile();
        commit.saveBlobs();
        String commitHash = hashObj(commit);
        commit.dump();
        activeBranch.updateRef(commitHash);
        commit.saveToFile(commitHash);
    }


    /**
     *  Checks out all the files tracked by the given commit.
     *  Removes tracked files that are not present in that commit.
     *  Also moves the current branch’s head to that commit node
     * @param commitID - Commit Id to reset to
     */
    public static void reset(String commitID){
        Branch branch = Branch.getActiveBranch();
        Commit commit = Commit.getCommit(commitID);
        Commit currentCommit = Commit.getActiveCommit();
        List<String> filesInDir = getPlaneFileNameInDir(CWD);
        for (String file : filesInDir){
            if (!currentCommit.getContent().containsKey(file)){
                exitWithError("There is an untracked or Modified file in the way; delete it, or add and commit it first.", 0);
            }
            Blob blob = new Blob(file);
            if (!blob.getHash().equals(currentCommit.getContent().get(file))){
                exitWithError("There is an untracked or Modified file in the way; delete it, or add and commit it first.", 0);
            }
        }
        for (Map.Entry<String, String> file : commit.getContent().entrySet()) {
            checkoutFileInCommit(commitID, file.getKey());
            filesInDir.remove(file.getKey());
        }

        for (String file : filesInDir){
            restrictDelete(join(CWD, file));
        }
        branch.updateRef(commitID);
    }


    /** STATUS RELATED METHODS **/


    /**
     * This method shows the state of the working directory and currect commit. It display all the files
     * that are staged for addition, removal, and files that have been modified or deleted.
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
                    modifiedFiles.add(filename + " (modified)");
                }
            }

            if (stageRemove.containsKey(filename)){
                toRemoveFiles.add(filename);
            }else if (stageEntry.containsKey(filename)){
               toAddFiles.add(filename);
               untrackFiles.remove(filename);
               modifiedFiles.remove(filename + " (modified)");
            }
        }

        // check if there are files  in the current commit that are absent in the working directory
        for (Map.Entry<String, String> file : activeCommit.getContent().entrySet()){
            if (!filesInDir.contains(file.getKey())){
                modifiedFiles.add(file.getKey() + " (deleted)");
                stageEntry.remove(file.getKey()); // remove from the stage for addition area if deleted.
            }
        }
        String branch = Branch.getActiveBranch().getName(); // active branch name

        System.out.println("Branches  " + "*" + branch);
        displayStatus("Staged Files For Addition", toAddFiles);
        displayStatus("Stage For Removed Files", toRemoveFiles);
        displayStatus("Modifications Not Staged For Commit", modifiedFiles);
        displayStatus("Untracked Files", untrackFiles);
        stageArea.saveToFile();
    }


    /**
     * This methods replaces the content of a file with the version found in the current head commit.
     * @param filename -  The name of the file that should be checkout
     */
    public static  void checkoutFileInHead(String filename){
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


    /** BRANCH RELATED METHODS **/


    /**
     * Create a new working branch but don't switch to it.
     * @param name - new Branch name.
     */
    public static void branch(String name){
        Branch branch = new Branch(name);
        if (branch.exist()){
            Utils.exitWithError("A branch with that name already exists.", 0);
        }
        Branch active = Branch.getBranch("master");
        if (!active.getName().equals("master")){
            exitWithError("Warning!! Master is not your active branch", 0);
        }
        branch.updateRef(active.getRef());
    }


    /**
     *
     * Takes the version of the file as it exists in the commit with the given id, and puts it in the
     * working directory, overwriting the version of the file that’s already there if there is one.
     * The new version of the file is not staged.
     * @param commitId - The commit id, should be a valid commit id.
     * @param filename - The name of the file to be retrieve.
     *
     */
    public static void checkoutFileInCommit(String commitId, String filename){
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


    /**
     * This methods switches the pointer of the head to a new branch.
     * It replaces all the files in the working directory with their corresponding version present in the branch
     * delete all files that are present in the working directory but absent in the branch.
     * @param branchName -  branch name.
     */
    public  static  void chekoutBranch(String branchName){
        Head head = Head.readFromFile();
        Branch currentBranch = Branch.getActiveBranch();
        Branch branch = Branch.getBranch(branchName);
        Commit currentCommit = Commit.getActiveCommit();
        List<String> filesInDir = getPlaneFileNameInDir(CWD);
        if (currentBranch.compareTo(branch) == 0){
            exitWithError("No need to checkout the current branch.", 0);
        }
        for (String file : filesInDir){
            if (!currentCommit.getContent().containsKey(file)){
                exitWithError("There is an untracked or Modified file in the way; delete it, or add and commit it first.", 0);
            }
            Blob blob = new Blob(file);
            if (!blob.getHash().equals(currentCommit.getContent().get(file))){
                exitWithError("There is an untracked or Modified file in the way; delete it, or add and commit it first.", 0);
            }
        }
        Commit commitHead = readObjectFromFile(getHashAsFile(branch.getRef()), Commit.class);

        // Update the working directory with version of files present in the branch
        for (Map.Entry<String, String> file : commitHead.getContent().entrySet()){
            File fileBlob = getHashAsFile(file.getValue());
            byte[] fileContent = readContents(fileBlob);
            File updatedFile = join(CWD, file.getKey());
            writeContents(updatedFile, fileContent);
            filesInDir.remove(file.getKey());
        }

        // remove all the files present in the working directory and absent in the branch
        for (String file : filesInDir){
            restrictDelete(join(CWD, file));
        }
        head.updateRef(branchName);
    }


    /**
     *  Deletes the branch with the given name.
     *  This only means to delete the pointer associated with the branch
     *  @param name - branch name to be deleted.
     */
    public static void removeBranch(String name){
        Branch branch = new Branch(name);
        Branch active = Branch.getActiveBranch();
        if (!branch.exist()){
            exitWithError("A branch with that name does not exist.", 0);
        }
        if (active.compareTo(branch) == 0){
            exitWithError("Cannot remove the current active branch.", 0);
        }
        branch.remove();

    }


    public static void merge(Branch branch){

    }


    /** HELPER METHOD **/


    /**
     * Helper method to print status
     * @param type
     * @param content
     */
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
        File index = join(GITLET_DIR, "INDEX");
        try{
            objects.mkdir();
            refs.mkdir();
            heads.mkdir();
            head.createNewFile();
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
