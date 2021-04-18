package com.dilatush.email;

import jakarta.mail.Session;

import java.util.Map;
import java.util.Properties;

public class EmailProvider {

    /** The internal name (or handle) for this provider. */
    public final String     name;

    /** The properties for the Jakarta Mail {@link Session} for this provider. */
    public final Properties sessionProperties;

    /** {@code true} if this provider will send email via the SMTP protocol */
    public final boolean    canSMTP;

    /** {@code true} if this provider will allow received emails to be read via the POP protocol. */
    public final boolean    canPOP;

    /** {@code true} if this provider will allow received emails to be read via the IMAP protocol. */
    public final boolean    canIMAP;

    /** Relative priority of this provider (larger numbers mean higher priority. */
    public final int        priority;


    public EmailProvider( final String _name, final Properties _sessionProperties,
                          final boolean _canSMTP, final boolean _canPOP, final boolean _canIMAP, final int _priority ) {

        name              = _name;
        sessionProperties = _sessionProperties;
        canSMTP           = _canSMTP;
        canPOP            = _canPOP;
        canIMAP           = _canIMAP;
        priority          = _priority;
    }
}
