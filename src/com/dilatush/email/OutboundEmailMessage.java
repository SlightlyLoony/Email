package com.dilatush.email;

import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;

/**
 * Encapsulates an email message, including both plain text and HTML mail with optional embedded images.  An embedded image in the HTML body is
 * specified by including the URL in the {@code <img/>} tag's "src" attribute.  The email sender will parse the HTML body looking for the image tags, and will
 * fetch and embed the image automagically.
 *
 * @author Tom Dilatush  tom@dilatush.com
 */
public record OutboundEmailMessage( InternetAddress from, String subject, String text, String html ) {

    public OutboundEmailMessage {

        // we must have at least one kind of body...
        if( (text == null) && (html == null ) )
            throw new IllegalArgumentException( "Missing both a text body and an HTML body" );
    }

    public OutboundEmailMessage( final String _from, String _subject, String _text, String _html ) throws AddressException {
        this( new InternetAddress( _from ), _subject, _text, _html );
    }


    public boolean hasText() {
        return (text != null);
    }


    public boolean hasHTML() {
        return (html != null);
    }


    public boolean hasTextOnly() {
        return hasText() && !hasHTML();
    }


    public boolean hasHTMLOnly() {
        return !hasText() && hasHTML();
    }
}
