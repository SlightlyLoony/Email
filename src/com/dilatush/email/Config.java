package com.dilatush.email;


import com.dilatush.util.config.AConfig;

import java.util.List;
import java.util.logging.Logger;

/**
 * Configuration POJO for the ShedSolar application.
 *
 * @author Tom Dilatush  tom@dilatush.com
 */
public class Config extends AConfig {

    @SuppressWarnings( "unused" )
    private static final Logger LOGGER = Logger.getLogger( new Object(){}.getClass().getEnclosingClass().getCanonicalName() );

    public EmailService.Config                      email               = new EmailService.Config();


    /**
     * Verify the fields of this configuration.
     */
    @Override
    public void verify( final List<String> _messages ) {

//        validate( () -> isOneOf( mode, "normal", "tempTest", "assemblyTest"), _messages,
//                "ShedSolar mode is invalid: " + mode );

        email         .verify( _messages );
    }
}
