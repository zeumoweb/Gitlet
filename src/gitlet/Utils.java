package gitlet;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Formatter;
import java.util.List;

/**
 * This class will serve as a helper class that will provide us with utility functions
 * to easily manipulate files.
 * @author Lekane Styve
 * @author Chudah Yakung
 */

public class Utils {

    // length of complete sha-1 uid
    final static int UID_LENGTH = 40;

    /**
     * convert content into a Sha-1 hash. the parameter content may contain a combination
     * of strings and byte arrays.
     * @throws IllegalArgumentException
     * @param content can be a series of strings or byte array.
     * @return the sha-1 hash of the parameter passed to it
     */
    public static String sha1(Object... content){
        try{
            MessageDigest m = MessageDigest.getInstance("SHA-1");
            for (Object ob: content){
                if (ob instanceof byte[]){
                    m.update((byte[]) ob);
                }else if(ob instanceof String){
                    m.update(((String)ob).getBytes(StandardCharsets.UTF_8));
                }
                else{
                    throw new IllegalArgumentException("incorrect value to sha1");
                }
            }

            // format output
            Formatter output = new Formatter();
            for (byte b : m.digest()) output.format("%02x", b);
            return output.toString();

        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     *  returns the sha1 hash of the concatenation of strings in content
     * @param content
     * @return the String sha1 representation
     */
    public static String sha1(List<Object> content) {
        return sha1(content.toArray(new Object[content.size()]));
    }


    /**
     * Deletes a file if that file is not a directory and that file is in the
     * same directory as .gitlet
     * Returns true if the file has been succesfully deleted and false otherwise
     * @throws IllegalArgumentException
     * @param file
     * @return
     */
    public static  boolean restrictDelete(File file){

    	if (!(new File(file.getParentFile(), ".gitlet")).isDirectory()) {
            throw new IllegalArgumentException("not .gitlet working directory");
        }
    	if (!file.isDirectory()) return file.delete();

        else  {
          return false;
        } 
    }

    public static boolean restrictDelete(String file){
        return restrictDelete(new File(file));
    }
    /**
     * Write the output of concatenating the bytes in contents parameter to a file.
     * It can create a new file or overwrite and existing one. Each object in contents can
     * be either a byte array or a string.
     * @throws IllegalArgumentException
     * @param file
     * @param contents
     */
    public static void writeContents(File file, Object... contents){
    	try {
    	   if (file.isDirectory()) throw new IllegalArgumentException ("Cannot overwrite a directory");
    	
    	    BufferedOutputStream str = new BufferedOutputStream(Files.newOutputStream(file.toPath()));
    	
    	    for (Object ob: contents) {
    		    if (ob instanceof byte[]) str.write((byte[])ob);

    		    else str.write(((String) ob).getBytes(StandardCharsets.UTF_8));
    		
        }
    	str.close();
    	} catch (IOException | ClassCastException excep) {
            throw new IllegalArgumentException(excep.getMessage());
        }
    }


    /**
     * Writes an Object to a file
     * @param file
     * @param obj
     */
    public  static void writeObject(File file, Serializable obj){
        writeContents(file, serialize(obj));
    }


    /**
     * reads and return object from a file
     * @param file
     * @param type - the type of object stored in the file
     * @param <T>
     * @return Object
     */
    public static <T extends Serializable> T readObjectFromFile(File file, Class<T> type){
        try {
    		ObjectInputStream input = new ObjectInputStream (new FileInputStream(file));
    		
    		T obj= type.cast(input.readObject());
    		
    		input.close();
    		return obj;
                  	
        } catch (IOException | ClassNotFoundException e) {

			// TODO Auto-generated catch block
			 e.printStackTrace();
			 throw new IllegalArgumentException("An error occurred while reading the Object");
		}
       
    }


    /**
     * return all the content of a file as a byte array
     * @param file
     * @return byte[]
     */
    public static byte[] readContents(File file) {
        if (!file.isFile()) {
            exitWithError("Argument Must be a valid file", 0);
        }
        try {
            return Files.readAllBytes(file.toPath());

        } catch (IOException e) {
            throw new IllegalArgumentException(e.getMessage());
        }
    }


    /**
     * read a file content as a String
     * @param file
     * @return a String
     */
    public static String readContentAsString(File file){
        return new String(readContents(file), StandardCharsets.UTF_8);
    }

    // filter for all plain files. ie files that are not directory
    private  static final FilenameFilter PLAIN_FILES = new FilenameFilter() {
        @Override
        public boolean accept(File dir, String name) {
            return new File(dir, name).isFile();
        }
    };


    /**
     * This methods filters all the plane files in a directory and returns them as
     * a list of names representing these files in lexicographic order. return null if no file is found
     * @param dir - Directory
     * @return List<String>
     */
    public static List<String> getPlaneFileNameInDir(File dir){
        String[] files = dir.list(PLAIN_FILES);
        if (files.length == 0){
            return null;
        }else{
            Arrays.sort(files);
            return Arrays.asList(files);
        }
    }


    // Overloaded method for getPlaneFileNameInDir
    public static List<String> getPlaneFileNameInDir(String dir){
        return getPlaneFileNameInDir(new File(dir));
    }


    /**
     * returns a file or directory that correspond to that found at the path form
     * from concatenating first and other
     * @param first - String
     * @param others - list of Strings
     * @return  FILE
     */
    public static File join(String first, String... others){
        return Paths.get(first, others).toFile();
    }

    // Method overload
    public static File join(File file, String... others){
        return Paths.get(file.getPath(), others).toFile();
    }

    /**
     * return the byte array representing a serialize object
     * @param obj
     * @return byte[]
     */
    /*SERIALIZATION FUNCTION*/


    public static byte[] serialize(Serializable obj){
       try{
           ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
           ObjectOutputStream objectStream = new ObjectOutputStream(byteStream);
           objectStream.writeObject(obj);
           objectStream.close();
           byteStream.close();
           return byteStream.toByteArray();
       } catch (IOException e) {
           throw error("Error occured while serializing");
       }
    }

    public static <T extends  Serializable> String hashObj(T obj){
        return sha1(serialize(obj));
    }


    /**
     * Returns the first two characters of the commit hash value.
     * This will be used for naming the commit folder and structuring the commit tree.
     * @param hash
     * @return String
     */
    public static String getHashHead(String hash) {
       return hash.substring(0,2);
    }

    /**
     * Returns the series of characters after the  first two characters of the commit hash value.
     * This will be used for naming the commit folder and structuring the commit tree.
     * @param hash
     * @return String
     */
    public static String getHashBody(String hash) {
        return hash.substring(2);
    }


    /* ERROR REPORTING METHODS */


    /** Return a GitletException whose message is composed from MSG and ARGS as
     *  for the String.format method. */
    static GitletException error(String msg, Object... args) {
        return new GitletException(String.format(msg, args));
    }


    /** Print a message composed from MSG and ARGS as for the String.format
     *  method, followed by a newline. */
    static void message(String msg, Object... args) {
        System.out.printf(msg, args);
        System.out.println();
    }

    /**
     * exit the program with error message.
     * @param msg
     * @param exitcode the type of exit code
     */
    public static void exitWithError(String msg, int exitcode){
        System.out.println(msg);
        System.exit(exitcode);
    }

    /**
     * Checks the number of arguments versus the expected number,
     * throws a RuntimeException if they do not match.
     *
     * @param cmd Name of command you are validating
     * @param args Argument array from command line
     * @param n Number of expected arguments
     */
    public static void validateNumArgs(String cmd, String[] args, int n) {
        if (args.length != n) {
            switch (cmd){
                case "commit":
                    exitWithError("Commit Message Should Be Specified", 0);
                    break;
                case "add":
                    exitWithError("Must Specify File To Stage", 0);
                    break;
            }
            exitWithError("Check the order of arguments", 0);
        }
    }

    public static File getHashAsFile(String hash){
        return join(Commit.OBJECT_FOLDER, getHashHead(hash), getHashBody(hash));
    }

}



