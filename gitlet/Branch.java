package gitlet;

import java.io.File;
import java.io.IOException;
import  java.util.HashSet;
import  java.util.*;
import static gitlet.Utils.*;

/**
 * This class represents a branch object and will be used to fascilitate all operations that involves branching.
 */
public class Branch implements Dumpable, Comparable {

    // current commit being referenced by the Master node

    // referehce to the head commit
    private String ref = null;
    private String name;
    private static final File BRANCH_DIR = join(Repository.GITLET_DIR, "refs", "heads");

    public Branch(String name){
        this.name = name;
    }


    /**
     * Checks if a given branch exist
     * @return - returns true if the branch exists and false otherwise.
     */
    public Boolean exist(){
        File branch = join(BRANCH_DIR, this.name);
        if (branch.isFile()) {
            return true;
        }
        return false;
    }


    /**
     * This methods returns the Branch object that the HEAD points at.
     * @return the currently active branch
     */
    public static Branch getActiveBranch(){
        Head head = Head.readFromFile();
        String branchName = head.getRef();
        Branch branch = readObjectFromFile(join(BRANCH_DIR, branchName ), Branch.class);
        return branch;
    }


    /**
     * This method returns the branch Object that corresponds to a given branch name.
     * @param name -  The name of the branch
     * @return - Branch
     */
    public static Branch getBranch(String name){
        File file = join(BRANCH_DIR, name);
        if(!file.isFile()){
            exitWithError("A branch with that name does not exist.", 0);
        }
        return readObjectFromFile(file, Branch.class);
    }



    /**
     * Set the active commit of the given branch
     * @param commit - Commit Id
     */
    public void updateRef(String commit){
        this.ref = commit;
        this.saveToFile();
    }


    // saves branch object to the object tree in the repository
    public void saveToFile(){
        File branchFile = join(BRANCH_DIR, this.name);
        try{
            if(!branchFile.isFile()){
            branchFile.createNewFile();
            }
        }catch (IOException e){
            exitWithError("Could not create branch " + this.name, 0);
        }
        writeObject(branchFile, this);
    }


    /**
     * removes a given branch from the branch tree
     */
    public void remove(){
        File branch = join(BRANCH_DIR, this.name);
        branch.delete();
    }

    /**
     * Merge Two branches into one
     * @param givenBranch
     */
    public void merge(String branchName){
        Branch givenBranch = Branch.getBranch(branchName);
        Index stageArea = Index.readFromFile();

        // HANDLE FAILURE CASES OF THE MERGE METHOD


        if (!stageArea.isEmpty()){
            exitWithError("You have uncommitted changes.", 0);
        }

        if (this.name.equals(branchName)){
            exitWithError("Cannot merge a branch with itself.", 0);
        }

        Commit splitCommit = this.getSplit(branchName);
        Commit givenBranchCommit =  Commit.getCommit(givenBranch.ref);
        Commit activeCommit = Commit.getActiveCommit();

        // check if there are untracked files in the active commit

        List<String> filesInDir = getPlaneFileNameInDir(Repository.CWD);
        for (String file : filesInDir){
            if (!activeCommit.getContent().containsKey(file)){
                exitWithError("There is an untracked or Modified file in the way; delete it, or add and commit it first.", 0);
            }
            Blob blob = new Blob(file);
            if (!blob.getHash().equals(activeCommit.getContent().get(file))){
                exitWithError("There is an untracked or Modified file in the way; delete it, or add and commit it first.", 0);
            }
        }

        // If the split point is the current branch, then the effect is to check out the given branch
        if (splitCommit.compareTo(activeCommit) > -1){
            Repository.chekoutBranch(branchName);
            exitWithError("Current branch fast-forwarded.", 0);
        }

        // If the split point is the same commit as the given branch, then we do nothing; the merge is complete
        if (splitCommit.compareTo(givenBranchCommit) > -1){
            exitWithError("Given branch is an ancestor of the current branch. No merge to execute", 0);
        }


        // ACTUAL IMPLEMENTATION OF THE METHOD

        HashMap<String, String> splitContent = splitCommit.getContent();
        HashMap<String, String> activeContent = activeCommit.getContent();
        HashMap<String, String> givenBranchContent = givenBranchCommit.getContent();

        // TO BE COMPLETE

        // Any files that have been modified in the given branch since the split point, but not modified in the current
        // branch since the split point should be changed to their versions in the given branch



        // Any files that were not present at the split point and are present only in the given branch
        // should be checked out and staged.
        for (Map.Entry<String, String> file: givenBranchContent.entrySet()) {
            if(!splitContent.containsKey(file.getKey()) && !activeContent.containsKey(file.getKey())){
                Repository.checkoutFileInCommit(givenBranchCommit.getCommitHash(), file.getKey());
                Repository.add(file.getKey());
            }

        }


        // Any files present at the split point, unmodified in the current branch,
        // and absent in the given branch should be removed (and untracked).
        for (Map.Entry<String, String> file: splitContent.entrySet()) {

            if(activeContent.containsKey(file.getKey()) &&
                    activeContent.get(file.getKey()).equals(file.getValue()) &&
                    !givenBranchContent.containsKey(file.getKey())){
                Repository.rm(file.getKey());
            }

            // Handle merge conflic if file in the current branch has different content from file in the given branch
            if (activeContent.containsKey(file.getKey()) && givenBranchContent.containsKey(file.getKey())){
                if (!activeContent.get(file.getValue())
                        .equals(givenBranchContent.get(file.getValue()))){
                    File fileObj = join(Repository.CWD, file.getKey());
                    writeContents(fileObj, "<<<<<<< HEAD\n" +
                            "contents of " + file.getKey() + " in current branch\n" +
                            "=======\n" +
                            "contents of " + file.getKey() + "  in given branch\n" +
                            ">>>>>>>");
                    System.out.println("Encountered a merge conflict.");

                }
            }
        }

        Repository.makeCommit("Merged " + givenBranch.name + " into " +  this.name);


    }


    /**
     * Branch Merge Helper Function
     * return the split point.
     * The split point is a latest common ancestor of the current and given branch heads.
     * Move along everybranch and add all the commit ids to a hashset. The first commit id that will
     * be a duplicate in the hashset will be the splitting point.
     * @param givenBranch
     * @return - commit object
     */
    public Commit getSplit(String branchName){

        HashSet<String> commitSet = new HashSet<>();
        Commit current = Commit.getActiveCommit();
        commitSet.add(this.ref); // adding the first commit id to the commit set

        // add all the commit id in current branch to commitSet
        while(current.getParent()!=null) {
            commitSet.add(current.getParent());
            current= readObjectFromFile(getHashAsFile(current.getParent()), Commit.class);
        }

        // add all the commit id in the given branch to the commitSet
        Branch givenBranch = Branch.getBranch(branchName);
        Commit headCommit =  Commit.getCommit(givenBranch.ref);

        if(!commitSet.add(givenBranch.ref)){
            return Commit.getCommit(givenBranch.ref);
        }

        while(headCommit.getParent()!=null) {
            if(!commitSet.add(headCommit.getParent())){
                return Commit.getCommit(headCommit.getParent());
            }
            headCommit= readObjectFromFile(getHashAsFile(headCommit.getParent()), Commit.class);

            current= readObjectFromFile(getHashAsFile(headCommit.getParent()), Commit.class);
        }

        return null;

    }


    public String getName(){
        return this.name;
    }


    public String getRef(){
        return ref;
    }

    @Override
    public void dump() {
        System.out.println(this.name + " References: " + this.ref);
    }

    @Override
    public int compareTo(Object o) {
        Branch branch = (Branch) o;
        if (branch.name.equals(this.name)){
            return 0;
        }
        return -1;
    }
}
