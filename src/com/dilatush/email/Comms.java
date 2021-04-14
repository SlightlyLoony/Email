package com.dilatush.email;

import com.dilatush.util.Files;
import com.dilatush.util.Outcome;
import jakarta.mail.internet.AddressException;
import jakarta.mail.internet.InternetAddress;

import java.io.File;
import java.util.logging.Logger;

public class Comms {

    public static final Comms INSTANCE = new Comms();

    private final Logger LOGGER;

    private Config config;

    private EmailService emailService;

    private Comms() {

        // set the configuration file location (must do before any logging actions occur)...
        System.getProperties().setProperty( "java.util.logging.config.file", "logging.properties" );
        LOGGER = Logger.getLogger( new Object(){}.getClass().getEnclosingClass().getSimpleName() );

    }


    /**
     * Initialization method just in case we ever need to interpret command line arguments.
     *
     * @param _args The command line arguments.
     */
    private void init( final String[] _args ) {
        // naught to do for now...
    }


    private void run() {

        LOGGER.info( "Comms is starting..." );

        // get our configuration...
        Config config = new Config();
        Outcome<?> result = config.init( "CommsConfigurator", "configuration.java", Files.readToString( new File( "credentials.txt" ) ) );

        // if our configuration is not valid, just get out of here...
        if( !result.ok() ) {
            LOGGER.severe( "Aborting; configuration is invalid\n" + result.msg() );
            System.exit( 1 );
        }

        // set up our services...
        emailService = new EmailService( config.email );

        /*
         * Test code
         */

        try {
            String html = """
                    <include-file src="transfer://default/test.msg"/>
                    """;
            OutboundEmailMessage msg = new OutboundEmailMessage( "Dilatush Empire<empire@dilatush.com>", "Test", "Who cares what I say in here?", html );
            InternetAddress[] to = InternetAddress.parse( "Tom Dilatush<tom@dilatush.com>" );
            InternetAddress[] bcc = InternetAddress.parse( "wifi@dilatush.com" );
            Result<?> sent = emailService.getSender().send( to, null, bcc, msg  );

            if( sent.ok() )
                System.out.println( "Successfully sent message" );
            else {
                System.out.println( "Send message failed" );
                System.out.println( sent.msg() );
            }
            html.hashCode();
        }
        catch( AddressException _e ) {
            _e.printStackTrace();
        }

    }

    public static void main( final String[] _args ) {
        INSTANCE.init( _args );
        INSTANCE.run();
    }
}
