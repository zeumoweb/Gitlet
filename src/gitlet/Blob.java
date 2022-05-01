package gitlet;

import java.io.File;
import java.io.IOException;

import static  gitlet.Utils.*;

public class Blob {

        private File file;
        private String blobHash;
        private byte[] content;
        private static final File PLAIN_FILES_DIR = Repository.CWD;

        public Blob(String filename){
            this.file = join(PLAIN_FILES_DIR, filename);
            this.content = readContents(this.file);
            this.blobHash = sha1(this.content);
        }

        public void saveBlob(){
            File file_blob = getHashAsFile(this.blobHash);
            if(!file_blob.isFile()){
            File file = createEmptyBlobFile(this.blobHash);
            writeContents(file, this.content);
            }
        }

        public String getHash(){
            return this.blobHash;
        }

        private File createEmptyBlobFile(String blob_sha){
            String head_name = getHashHead(blob_sha);  // get the first two char of the sha1 string
            String body_name = getHashBody(blob_sha);  // get the last n - 2 characters of the sha1 string

            // create directory and file to save blob
            File dir = Utils.join(Commit.OBJECT_FOLDER, head_name);
            File file = Utils.join(Commit.OBJECT_FOLDER, head_name, body_name);

            try {
                dir.mkdir();
                file.createNewFile();
            }
            catch (IOException E){
                exitWithError("Could not save blob to file", 0);
            }
            return file;
        }
}
