package com.dilatush.email;

import com.dilatush.util.Checks;

import java.io.File;

/**
 * Encapsulates the notion of a transfer directory that Comms can use to retrieve or store email attachments, and to retrieve inline email images.
 */
public record TransferDirectory( String name, File directory, Mode mode ) {


    public TransferDirectory {

        // fail fast if we're missing something vital (meaning: everything)...
        Checks.notEmpty( name );
        Checks.required( directory, mode );

        // verify that the given file represents a directory, and that we have the correct rights for it...
        Checks.isTrue( directory.isDirectory(), "Not a directory: " + directory.getAbsolutePath() );
        Checks.isTrue( directory.canExecute(), "Directory is not enterable by Comms: " + directory.getAbsolutePath() );
        if( mode != Mode.WRITE_ONLY )
            Checks.isTrue( directory.canRead(), "Directory is not readable by Comms: " + directory.getAbsolutePath() );
        if( (mode != Mode.READ_ONLY) )
            Checks.isTrue( directory.canWrite(), "Directory is not writable by Comms: " + directory.getAbsolutePath() );
    }


    public boolean isReadable() {
        return mode != Mode.WRITE_ONLY;
    }


    public boolean isWritable() {
        return (mode == Mode.READ_WRITE) || (mode == Mode.WRITE_ONLY);
    }


    public boolean isAuto() {
        return mode == Mode.READ_AUTO;
    }


    public enum Mode {
        READ_ONLY, READ_WRITE, WRITE_ONLY, READ_AUTO
    }
}
