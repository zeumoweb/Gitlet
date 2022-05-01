package gitlet;

import static  gitlet.Utils.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

/** Driver class for Gitlet, a subset of the Git version-control system.
 * Will be use to execute all the git basic commands
 *  @author Lekane Styve
 *  @author Chudah Yakung
 */
import java.io.File;
import java.util.Arrays;
import java.util.Map;

public class Main {

    /** Usage: java gitlet.Main ARGS, where ARGS contains
     *  <COMMAND> <OPERAND1> <OPERAND2> ...
     */
    public static void main(String[] args) {

        String firstArg = args[0];
        switch(firstArg) {
            case "init":
                Repository.initializeRepo(args);
                break;
            case "add":
                validateNumArgs("add",args, 2);
                Index stageArea = Index.readFromFile();
                stageArea.addFileToStage(args[1]);
                break;
            case "commit":
                validateNumArgs("commit", args, 3);
                Repository.makeCommit(args[2]);
                break;
            case "status":
                validateNumArgs("status", args, 1);
                Repository.viewStatus();
                break;

            case "log":
                validateNumArgs("log", args, 1);
                Commit.logCommit();
                break;
            case "global-log":
            	validateNumArgs("global-log", args, 1);
                Commit.globalLog();
                break;
                
            case "find":
            	validateNumArgs("find", args, 2);
                Commit.find(args[1]);
                break;
            case "rm":
            	validateNumArgs("rm", args, 2);
                Index indexArea = Index.readFromFile();
                indexArea.remove(args[1]);
                break;
   
            case "checkout":
                Repository.checkout(args[1].substring(2));

        }
    }
}
