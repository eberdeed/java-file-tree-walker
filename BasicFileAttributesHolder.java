/**
 *  BasicFileAttributesHolder: A java class to contain file attributes.
 *  Obtained from the source code to JDK-1.9.0 by Sun Java.
 *  Edward Charles Eberle <eberdeed@eberdeed.net>
 *  March 13, 2018 San Diego California USA  
 *  
 */  
/*
 * Copyright (c) 2008, 2009, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 */



import java.nio.file.attribute.BasicFileAttributes;

/**
 * Implemented by objects that may hold or cache the attributes of a file.
 */

public interface BasicFileAttributesHolder {
    /**
     * Returns cached attributes (may be null). If file is a symbolic link then
     * the attributes are the link attributes and not the final target of the
     * file.
     */
    BasicFileAttributes get();

    /**
     * Invalidates cached attributes
     */
    void invalidate();
}
