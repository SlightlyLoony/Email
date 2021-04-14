package com.dilatush.email;

import com.dilatush.util.config.AConfig;
import jakarta.mail.Authenticator;
import jakarta.mail.PasswordAuthentication;
import jakarta.mail.Session;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import static com.dilatush.util.Strings.isEmpty;

/**
 *
 *
 * @author Tom Dilatush  tom@dilatush.com
 */
public class EmailService {


    private final Properties sessionProperties;
    private final Map<String,TransferDirectory> transferDirectoryMap;
    private final EmailSender sender;


    public EmailService( final Config _config ) {
        this( _config.sessionProperties, _config.transferDirectoryMap );
    }


    public EmailService( final Properties _sessionProperties, Map<String,TransferDirectory> _transferDirectoryMap ) {
        sessionProperties = _sessionProperties;
        transferDirectoryMap = _transferDirectoryMap;
        sender = new EmailSender( this );
    }


    public EmailSender getSender() {
        return sender;
    }


    public Session getSession() {

        String user     = sessionProperties.getProperty( "mail.smtp.user"     );
        String password = sessionProperties.getProperty( "mail.smtp.password" );

        Authenticator auth = new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication( user, password );
            }
        };

        return Session.getInstance( sessionProperties, auth );
    }


    public TransferDirectory getTransferDirectory( final String _name ) {
        return transferDirectoryMap.get( _name );
    }


    public static class Config extends AConfig {


        public Properties sessionProperties;
        public List<Map<String,String>> transferDirectories;
        public Map<String,TransferDirectory> transferDirectoryMap;


        @Override
        public void verify( final List<String> _messages ) {
            validate( () -> sessionProperties != null,           _messages, "Session properties not set"  );
            validate( () -> mapTransferDirectories( _messages ), _messages, "Transfer directory problems" );
        }


        private boolean mapTransferDirectories( final List<String> _messages  ) {

            // if there's no list, or its empty, than we've got a problem...
            if( (transferDirectories == null) || (transferDirectories.size() == 0) ) {
                _messages.add( "No transfer directories configured" );
                return false;
            }

            // iterate over our list, building our map, verifying as we go...
            transferDirectoryMap = new HashMap<>();
            boolean ok = true;
            for( Map<String, String> map : transferDirectories ){

                // validate the transfer directory name...
                String name = map.get( "name" );
                if( isEmpty( name ) ) {
                    _messages.add( "Transfer directory name is missing" );
                    ok = false;
                    continue;
                }
                if( transferDirectoryMap.containsKey( name ) ) {
                    _messages.add( "Transfer directory name is a duplicate: " + name );
                }

                // validate the transfer directory path...
                String path = map.get( "path" );
                if( isEmpty( path ) ) {
                    _messages.add( "Transfer directory path is missing" );
                    ok = false;
                    continue;
                }
                if( path.charAt( 0 ) != '/' )
                    path = System.getProperty( "user.dir" ) + "/" + path;
                File file = new File( path );

                // validate the mode...
                String modeString = map.get( "mode" );
                TransferDirectory.Mode mode;
                try {
                    mode = TransferDirectory.Mode.valueOf( modeString );
                }
                catch( Exception _e ) {
                    _messages.add( "Invalid transfer directory mode: " + modeString );
                    ok = false;
                    continue;
                }

                // create and map our transfer directory, or catch any error that occurs...
                try {
                    TransferDirectory transferDirectory = new TransferDirectory( name, file, mode );
                    transferDirectoryMap.put( name, transferDirectory );
                }
                catch( Exception _e ) {
                    _messages.add( "Could not create transfer directory '" + name + ": " + _e.getMessage() );
                    ok = false;
                }
            }

            return ok;
        }
    }
}
