package gitlet;
import static  gitlet.Utils.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.io.File;
import java.util.Arrays;
import java.util.Map;

/** Driver class for Gitlet, a subset of the Git version-control system.
 * Will be use to execute all the git basic commands
 *  @author Lekane Styve
 *  @author Chudah Yakung
 */


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
                Repository.add(args[1]);
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
            	Repository.rm(args[1]);
                break;
            case "rm-branch":
                Repository.removeBranch(args[1]);
                break;

            case "checkout":
                if (args.length == 3)
                    Repository.checkoutFileInHead(args[2]);
                else if (args.length == 4)
                    Repository.checkoutFileInCommit(args[1], args[3]);
                else{
                    Repository.chekoutBranch(args[1]);
                }
                break;
            case "branch":
                Repository.branch(args[1]);
                break;
            case "reset":
                Repository.reset(args[1]);
                break;
            case "merge":
                Repository.mergeBranch(args[1]);
        }
    }
}
