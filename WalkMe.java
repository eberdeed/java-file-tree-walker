/**
 *  WalkMe: A java class to support the FileTreeWalker class.
 *  Created by Edward Charles Eberle <eberdeed@eberdeed.net>
 *  March 13, 2018 San Diego California USA  
 *  
 */  


import java.nio.file.Files;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.io.IOException;
import java.util.Objects;
import java.util.ArrayDeque;
import java.nio.file.LinkOption;

/**
 * A Class to walk a file tree.  This class will:
 * 1. List the entire contents of a directory and put all the directories in it into an ArrayDeque.
 * 2. Descend into each directory and accomplish (1) for each directory.
 * 3. Upon listing the last directory or file in a given directory it will ascend to the parent directory and list the next directory.
 * 4. If the parent directory is the root and has no other files or directories it will terminate.
 * Further this class will have options for: 
 * 1. Maximum depth recursion
 * 2. Following links
 * 3. Disabling security exceptions
 * 4. Using a file attribute cache to speed analysis.
 */  
public class WalkMe {
    public static void main (String args[]) {
        boolean followLinks = false;
        boolean ignoreSecurityException = false;
        boolean useAttributeCache = false;
        int maxDepth = Integer.MAX_VALUE;
        String dirName = "";
        if (args.length > 0){
             if ((args.length == 1) && ((args[0].compareTo("--help") == 0) || (args[0].compareTo("--usage") == 0))){
                  System.out.println("\n\n\tUsage:\n\tjava -jar TreeWalker.jar directory [--maxdepth n] [--followlinks] [--ignoresecurityexception] [--useattributecache] \n\n");
                  return;
            }               
             dirName = args[0];
             if (args.length > 1){
                for (int x = 1; x < args.length; x++)
                {
                     args[x] = args[x].trim();
                     if (args[x].compareTo("--followlinks") == 0){
                        followLinks = true;
                     } else if (args[x].compareTo("--ignoresecurityexception") == 0){
                        ignoreSecurityException = true;
                     } else if (args[x].compareTo("--useattributecache") == 0){
                        useAttributeCache = true;
                     } else if (args[x].compareTo("--maxdepth") == 0) {
                        if (args.length > x) {
                           Integer intConv = null;
                           x += 1;
                           try{
                              intConv = new Integer(args[x]);
                           }  catch (Exception exc) {
                              System.out.println("\n\n\tUsage:\n\tjava -jar TreeWalker.jar directory [--maxdepth n] [--followlinks] [--ignoresecurityexception] [--useattributecache]");
                              System.out.println("\tYou used --maxdepth without an improper integer value.\n\n");
                           }
                           maxDepth = intConv;
                           System.out.println("Maximum Depth:  " + intConv.toString());
                        } else {
                           System.out.println("\n\n\tUsage:\n\tjava -jar TreeWalker.jar directory [--maxdepth n] [--followlinks] [--ignoresecurityexception] [--useattributecache]");
                           System.out.println("\tYou used --maxdepth without an integer value.\n\n");
                        }
                     } else {
                        System.out.println("\n\n\tUsage:\n\tjava -jar TreeWalker.jar directory [--maxdepth n] [--followlinks] [--ignoresecurityexception] [--useattributecache] \n\n");
                        return;
                     }
                }
            }
            try {
                WalkMe theWalk = new WalkMe(dirName, maxDepth, followLinks, ignoreSecurityException, useAttributeCache);
            } catch (Exception exc) {
                System.out.println("\n\n\tError:  " + exc.getCause() + "\n\n");
                return;
            }
           
        } else {
            System.out.println("\n\n\tUsage:\n\tjava WalkMe directory [--followlinks] [--ignoresecurityexception] [--useattributecache] \n\n");
        }
        return;
    }
    
    public WalkMe(String dirname, int maxDepth, boolean followLinks, boolean ignoreSecurityException, boolean useAttributeCache) throws Exception{
        /**
         * Create a FileTreeWalker to walk the file tree.
         */
        FileSystem fSystem = FileSystems.getDefault();
        Path begin = fSystem.getPath(dirname);
        FileTreeWalker walker = new FileTreeWalker(maxDepth, followLinks, ignoreSecurityException, useAttributeCache);
        walker.walk(begin);
        return;
    }

    
        
};

    