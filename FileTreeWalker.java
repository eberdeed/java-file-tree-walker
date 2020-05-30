/**
 *  FileTreeWalker: A java class to walk a file tree.
 *  Adapted from a class by the same name from the Sun Java jdk-1.9.0
 *  by Edward Charles Eberle <eberdeed@eberdeed.net>
 *  March 13, 2018 San Diego California USA  
 *  
 */  


import java.nio.file.attribute.BasicFileAttributes;
import java.io.Closeable;
import java.io.IOException;
import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Iterator;
import java.nio.file.LinkOption;
import java.nio.file.FileVisitOption;
import java.nio.file.Path;
import java.nio.file.DirectoryStream;
import java.nio.file.DirectoryIteratorException;
import java.nio.file.Files;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.io.File;

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

class FileTreeWalker{
    private boolean followLinks = false;
    private LinkOption[] linkOptions;
    private final int maxDepth;
    private final ArrayDeque<DirectoryNode> stack = new ArrayDeque();
    public final ArrayDeque<Path>files = new ArrayDeque();
    private boolean canUseCached = false;
    private boolean ignoreSecurityException = false;
    private Path begin;
    private int depth;

    /**
      * A helper class to contain a file directory.
      */
    private static class DirectoryNode {
        private final Path dir;
        private final Object key;
        private final DirectoryStream<Path> stream;
        private final Iterator<Path> iterator;
        private boolean skipped;

        DirectoryNode(Path dir, Object key, DirectoryStream<Path> stream) {
            this.dir = dir;
            this.key = key;
            this.stream = stream;
            this.iterator = stream.iterator();
        }

        Path directory() {
            return dir;
        }

        Object key() {
            return key;
        }

        DirectoryStream<Path> stream() {
            return stream;
        }

        Iterator<Path> iterator() {
            return iterator;
        }

        void skip() {
            skipped = true;
        }

        boolean skipped() {
            return skipped;
        }
    }

    /**
     * The event types.
     */
    static enum EventType {
        /**
         * An Inactive directory.
         */
         DIRECTORY,
        /**
         * Start of a directory
         */
        START_DIRECTORY,
        /**
         * An entry in a directory
         */
        ENTRY,
        /**
         * A link
         */
        LINK;
    }

    /**
     * A Helper class to describe the types of files involved.
     */
    static class Event {
        private EventType type;
        private final Path file;
        private final BasicFileAttributes attrs;
        private final IOException ioe;

        private Event(EventType type, Path file, BasicFileAttributes attrs, IOException ioe) {
            this.type = type;
            this.file = file;
            this.attrs = attrs;
            this.ioe = ioe;
        }

        Event(EventType type, Path file, BasicFileAttributes attrs) {
            this(type, file, attrs, null);
        }

        Event(EventType type, Path file, IOException ioe) {
            this(type, file, null, ioe);
        }
        
        EventType type() {
            return type;
        }
        
        void setType(EventType entryType){
            type = entryType;
        }
        
        Path file() {
            return file;
        }

        BasicFileAttributes attributes() {
            return attrs;
        }

        IOException ioeException() {
            return ioe;
        }
    }

   /**
    * The constructor for FileTreeWalker passing along the various options.
    */
    FileTreeWalker(int maxDepth, boolean followLinks, boolean ignoreSecurityException, boolean canUseCached) {
        this.ignoreSecurityException = ignoreSecurityException;
        this.canUseCached = canUseCached;
        this.followLinks = followLinks;
        this.linkOptions =  new LinkOption[1];
        this.linkOptions[0]= LinkOption.NOFOLLOW_LINKS;
        this.maxDepth = maxDepth;
    }

    /**
     * Returns the attributes of the given file, taking into account whether
     * the walk is following sym links is not. The {@code canUseCached}
     * argument determines whether this method can use cached attributes.
     */
    private BasicFileAttributes getAttributes(Path file, boolean canUseCached)
        throws IOException
    {
        // if attributes are cached then use them if possible
        if (canUseCached && (file instanceof BasicFileAttributesHolder) &&
        (System.getSecurityManager() == null))
        {
            // If the file attributes are identical in a directory, 
            // just send copies of the attributes.
            BasicFileAttributes cached = ((BasicFileAttributesHolder)file).get();
            if (cached != null && (!followLinks || !cached.isSymbolicLink())) {
                return cached;
            }
        }

        // Attempt to get attributes of file. If this fails and we are following
        // links then a link target might not exist so get attributes of link
        BasicFileAttributes attrs;
        try {
            attrs = Files.readAttributes(file, BasicFileAttributes.class, linkOptions);
        } catch (IOException ioe) {
            throw ioe;
        }
        return attrs;
    }

    /**
     * Visits the given file, returning the {@code Event} corresponding to that
     * visit.
     *
     * The {@code ignoreSecurityException} parameter determines whether
     * any SecurityException should be ignored or not. If a SecurityException
     * is thrown, and is ignored, then this method returns {@code null} to
     * mean that there is no event corresponding to a visit to the file.
     *
     * The {@code canUseCached} parameter determines whether cached attributes
     * for the file can be used or not.
     */
    private Event visit(Path entry) {
        // Need the file attributes
        BasicFileAttributes attrs = null;
        Event ev = null;
        try {
            attrs = getAttributes(entry, canUseCached);
        } catch (IOException ioe) {
           // We have a link.
            if (attrs.isSymbolicLink()) {
               // Not following links, return it as an entry.
               if (!followLinks) {
                 return new Event(EventType.LINK, entry, ioe);
               }else {
                  // Get the absolute canonical location that the link points to.
                  File testfile = new File(entry.toString());
                  if (!testfile.canRead()){
                     return null;
                  } 
                  System.out.println("Link Value:  " + entry.toString());
                  try {
                     String dirname = testfile.getCanonicalPath();
                     FileSystem fSystem = FileSystems.getDefault();
                     entry = fSystem.getPath(dirname);
                     System.out.println("Path Value:  " + entry.toString());
                     return(visit(entry));
                  } catch (IOException exc) {
                       // Return the entry as an entry.
                       return new Event(EventType.ENTRY, entry, exc);
                  }
               }
            }
            if (attrs.isRegularFile()) {
               return new Event(EventType.ENTRY, entry, ioe);
            } else if (attrs.isDirectory()) {
               return new Event(EventType.DIRECTORY, entry, ioe);
            }
        } catch (SecurityException se) {
            if (ignoreSecurityException){
               return null;
            } else {
               throw se;
            }
        }
         IOException ioe = new IOException("\n\n\tDummy Exception.\n\n");
         // We have a link.
         if (attrs.isSymbolicLink()) {
            // Not following links, return it as an entry.
            if (!followLinks) {
               return new Event(EventType.LINK, entry, ioe);
            }else {
               // Get the absolute canonical location that the link points to.
               File testfile = new File(entry.toString());
               if (!testfile.canRead()){
                  return null;
               } 
               System.out.println("Link Value:  " + entry.toString());
               try {
                     String dirname = testfile.getCanonicalPath();
                     FileSystem fSystem = FileSystems.getDefault();
                     entry = fSystem.getPath(dirname);
                     System.out.println("Path Value:  " + entry.toString());
                     return(visit(entry));
               } catch (IOException exc) {
                    // Return the entry as an entry.
                    return new Event(EventType.ENTRY, entry, exc);
               }
            }
         }
         if (attrs.isRegularFile()) {
            return new Event(EventType.ENTRY, entry, ioe);
         } else if (attrs.isDirectory()) {
            return new Event(EventType.DIRECTORY, entry, ioe);
         }
         return null;
    }


    /**
     * Start walking from the given file.
     */
    void walk(Path entry) throws SecurityException{
        System.out.println("Currently listing root directory:  " + entry.toString());
        DirectoryStream<Path> stream = null;
        BasicFileAttributes attrs = null;
        begin = entry;
        Event ev = null;
        Path tmp = null;
        try {
            attrs = getAttributes(entry, canUseCached);
        } catch (IOException ioe) {
            ev = new Event(EventType.START_DIRECTORY, entry, ioe);
        } catch (SecurityException se) {
            if (ignoreSecurityException){
                return;
            } else {
               throw(se);
            }
        }

        try {
            stream = Files.newDirectoryStream(entry);
        } catch (IOException ioe) {
            ev = new Event(EventType.DIRECTORY, entry, attrs, ioe);
        } catch (SecurityException se) {
            if (ignoreSecurityException){
                return;
            } else {
                throw(se);
            }
        }
        // push The starting directory node to the stack.
        DirectoryNode dirNode = new DirectoryNode(entry, attrs.fileKey(), stream);
        stack.push(dirNode);
        boolean computing = true;
        while(computing) {
            computing = next();
        };
    }

    /**
     * Iterates through the directory and prints all the values.
     * Pushes all directories onto the stack.
     */
    boolean next() {
        // Entry is a directory.
        Event ev = null;
        BasicFileAttributes attrs = null;
        Path entry = begin;
        DirectoryNode dirNode = pop();
        entry = dirNode.directory();
        try {
            attrs = getAttributes(entry, canUseCached);
        } catch (IOException ioe) {
            ev = new Event(EventType.DIRECTORY, entry, ioe);
        } catch (SecurityException se) {
            if (ignoreSecurityException){
                return true;
            } else {
               throw(se);
            }
        }
         if (dirNode == null) {
            return false;      // stack is empty, we are done
         }
         if (ev == null) {
            IOException ioe = new IOException("\nDummy IOException.\n");
            ev = new Event(EventType.DIRECTORY, entry, attrs, null);
        }
         readData(ev);
         // Read all the directory values.
         readDirectory(dirNode);
         Path tmpPath = null;
         // Print all the directory values.
         if (files.size() > 0){
            do {
                 tmpPath = files.pop();
                 ev = visit(tmpPath);
                 if (ev == null) {
                     System.out.println("Entry:  " + entry.toString() + " failed to resolve to a type.");
                     tmpPath = files.pop();
                     continue;
                 }
                 readData(ev);
           }while(files.size() > 0);
       }
       // Check to see if there is more.
       return (stack.size() > 0);
    }
    /**
     * Output the data.
     */
     void readData(Event ev) {
          Path tmp = null;
          switch (ev.type()) {
              case ENTRY :
                  tmp = ev.file();
                  System.out.println("Entry:  " + tmp.toString());
                  break;
              case LINK :
                  tmp = ev.file();
                  System.out.println("Link:  " + tmp.toString());
                  break;
              case START_DIRECTORY :
                  tmp = ev.file();
                  System.out.println("Start Directory Entry:  " + tmp.toString());
                  break;
              case DIRECTORY :
                  tmp = ev.file();
                  System.out.println("Directory Entry:  " + tmp.toString());
                  break;
              default:
            }
    }
     
    /**
     * Pops the directory node that is the current top of the stack safely.
     */
    DirectoryNode pop() {
        if (!stack.isEmpty()) {
            DirectoryNode node = stack.pop();
            return node;
        }
        return null;
    }
   
   /**
    * Get the depth of a path.
    */
   int getDepth(Path entry){
       File tmpFile = new File(entry.toString());
       Character seper = tmpFile.separatorChar;
       String sep = "";
       if (seper == '\\') {
         sep = "\\\\";
       } else {
         sep = seper.toString();
       }
       String entryVal = entry.toString();
       String beginVal = begin.toString();
       String tmpVal = "";
       int retVal = 0;
       int length = beginVal.length();
       String finalChar = beginVal.substring(length - 1, length);
       if (finalChar.compareTo(sep) != 0)
       {
         length++;
       }
       int size = entryVal.length();
       if ( size < length){
           return 0;
       }
       tmpVal = entryVal.substring(length, size);
       CharSequence test = sep.subSequence(0, 1);
       if (tmpVal.contains(test)) {
         String tmpArray[] = tmpVal.split(sep);
         retVal = tmpArray.length;
       }
       return(retVal);
   }
   
   /**
    * Read all the elements of a directory.
    */
   void readDirectory(DirectoryNode dirNode) {
        DirectoryStream<Path>reader = dirNode.stream();
        Iterator<Path>dirData = dirNode.iterator();
        Event ev = null;
        EventType dirtype = null;
        BasicFileAttributes attrs = null;
        Path entry = null;
        while(dirData.hasNext()) {
              entry = dirData.next();
              ev = visit(entry);
              if (ev == null) {
                  System.out.println("Entry:  " + entry.toString() + " failed to resolve to a type.");
                  continue;
              }
              dirtype = ev.type();
              int deep = getDepth(entry);
              // Add everything to the directory listing.
              files.add(entry);
              // Seperate out directories for further processing.
              if ((dirtype == EventType.DIRECTORY) && (deep < maxDepth)) {
                 try {
                     attrs = getAttributes(entry, canUseCached);
                 } catch (IOException ioe) {
                     ev = new Event(EventType.START_DIRECTORY, entry, ioe);
                 } catch (SecurityException se) {
                     if (ignoreSecurityException){
                         continue;
                     } else {
                        throw(se);
                     }
                 }
                 DirectoryStream<Path> stream = null;
                 try {
                     stream = Files.newDirectoryStream(entry);
                 } catch (IOException ioe) {
                     System.out.println("Directory " + entry.toString() + " cannot be resolved.");
                     continue;
                 } catch (SecurityException se) {
                     if (ignoreSecurityException){
                        continue;
                     } else {
                        throw se;
                     }
                 }
                 DirectoryNode tmpNode = new DirectoryNode(entry, attrs.fileKey(), stream);
                 stack.push(tmpNode);
              }
         };
       // Close the directory stream.
       try {
          reader.close();
       } catch (IOException ignore) { }
    }
}
