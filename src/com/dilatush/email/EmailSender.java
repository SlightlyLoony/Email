package com.dilatush.email;

import com.dilatush.util.Streams;
import jakarta.activation.DataHandler;
import jakarta.activation.FileDataSource;
import jakarta.activation.URLDataSource;
import jakarta.mail.*;
import jakarta.mail.internet.*;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Instances of this class provide an email sender that can be used to send an email message directly to, cc, or bcc any number of recipients.
 *
 * @author Tom Dilatush  tom@dilatush.com
 */
@SuppressWarnings( "unused" )
public class EmailSender {

    private static final Logger LOGGER = Logger.getLogger( new Object(){}.getClass().getEnclosingClass().getCanonicalName() );

    private static final Pattern IMG_PATTERN = Pattern.compile( "<img.* src=(['\"])(.*)\\1.*/>" );

    private final EmailService service;


    public EmailSender( final EmailService _emailService ) {
        service = _emailService;
    }


    // TODO: figure out jsoup
    // TODO: add support for <attach-file> custom tag
    // TODO: add support for transfer:// URLs for both images and attachments
    // TODO: add support for <include-file> custom tag and recursive includes

    /**
     * Sends the given email message to the given recipients (TO, CC, and BCC).  If an HTML message is included, it may contain embedded
     * images so long as they have a valid and readable "src" attribute.  These images will be read by this method and then embedded as inline
     * attachments in the email being sent.
     *
     * @param _to the addressees to send the message directly to
     * @param _cc the addressees to cc (carbon copy)
     * @param _bcc the addressees to bcc (blind carbon copy)
     * @param _message the message to send (which contains the from and subject)
     * @return ok if successful, false otherwise with explanatory message
     */
    public Result<?> send( final InternetAddress[] _to, final InternetAddress[] _cc, final InternetAddress[] _bcc,
                           final OutboundEmailMessage _message) {

        // fail fast if important things are missing...
        if( _message == null )
            throw new IllegalArgumentException( "Missing email message" );
        if( (_to == null) || (_to.length == 0) )
            throw new IllegalArgumentException( "No 'to' addressees" );

        try {

            MimeMessage msg;

            Session session = service.getSession();

            // handle the simple case wherein we have only a text body...
            if( _message.hasTextOnly() )
                msg = handleTextOnlyEmail( session, _to, _cc, _bcc, _message );

            // handle the case wherein we have only an HTML body...
            else if( _message.hasHTMLOnly() )
                msg = handleHTMLOnlyEMail( session, _to, _cc, _bcc, _message );

            // handle the case wherein we have both a text body and an HTML body...
            else
                msg = handleTextAndHTMLEMail( session, _to, _cc, _bcc, _message );

            Transport transport = session.getTransport( "smtp" );
            Transport.send( msg );
            transport.close();
        }
        catch( Exception _e ) {

            // TODO: make this work when there's no cause, too...
            String msg;
            msg = "Problem sending email: " + _e.getCause().getClass().getName() + ": " + _e.getCause().getMessage();

            LOGGER.log( Level.WARNING, msg, _e );
            return new Result<>( msg );
        }

        return Result.OK;
    }


    private static final Pattern INCLUDE_FINDER = Pattern.compile( "(<INCLUDE-FILE.*? src=(['\"])(.*?)\\2.*?/>)", Pattern.CASE_INSENSITIVE );

    private String expandIncludes( final String _html ) throws IOException {

        // expand until there's nothing to expand...
        String html = _html;
        int index;
        do {

            // a place to build our result...
            StringBuilder result = new StringBuilder();

            // look for <include-file/> tags...
            Matcher mat = INCLUDE_FINDER.matcher( html );
            index = 0;
            while( mat.find( index ) ) {

                // append everything from index to the start of our match into the result...
                result.append( html, index, mat.start() );

                // append the include file specified by the URL in the <include-file> "src" attribute...
                String url = mat.group( 3 );
                DataSource source = getDataSource( url );
                String include = Streams.toString( source.handler.getInputStream(), StandardCharsets.UTF_8);
                result.append( include );

                // if we need to delete the source file, do so...
                if( (source.deleteFile != null) && !source.deleteFile.delete() )
                    throw new IllegalStateException( "Could not delete source file: " + source.deleteFile.getAbsolutePath() );

                // update the index for the next go-round...
                index = mat.end();
            }

            // if the index is non-zero (meaning we matched at least once), then append the remaining characters and get our new html string...
            if( index != 0 ) {
                result.append( html.substring( index ) );
                html = result.toString();
            }

        } while( index != 0 );

        return html;
    }


    private static final Pattern TRANSFER_PARSER = Pattern.compile( "transfer://(.*?)/(.*)", Pattern.CASE_INSENSITIVE );

    private DataSource getDataSource( final String _url ) throws MalformedURLException {

        // if we have a web URL, handle that...
        if( _url.startsWith( "http://" ) || _url.startsWith( "https://" ) )
            return new DataSource( new DataHandler( new URLDataSource( new URL( _url ) ) ), null );

        // if we have a transfer URL, handle that...
        else if( _url.startsWith( "transfer://" )) {

            // get the transfer directory name and the relative path...
            Matcher mat = TRANSFER_PARSER.matcher( _url );
            if( mat.matches() ) {
                String name = mat.group( 1 );
                String path = mat.group( 2 );

                // get a File for our file...
                TransferDirectory transferDirectory = service.getTransferDirectory( name );
                if( transferDirectory == null )
                    throw new IllegalArgumentException( "Transfer directory does not exist: " + name );
                if( !transferDirectory.isReadable() )
                    throw new IllegalArgumentException( "Transfer directory is not readable: " + name );
                File file = new File( transferDirectory.directory(), path );

                // get our result...
                DataHandler handler = new DataHandler( new FileDataSource( file ) );
                return transferDirectory.isAuto()
                        ? new DataSource( handler, file )
                        : new DataSource( handler, null );
            }
            else
                throw new IllegalArgumentException( "Invalid transfer URL: " + _url );
        }

        // if it's not one of the preceding, then we have an error...
        throw new IllegalArgumentException( "Invalid URL: " + _url );
    }


    /**
     * Always contains a handler; deleteFile is null unless the transfer directory mode is READ_AUTO.
     */
    private static record DataSource( DataHandler handler, File deleteFile ) {}


    /**
     * Handles the case where the supplied message contains both an HTML message and a plain text message.  The HTML message may contain embedded
     * images so long as they have a valid and readable "src" attribute.
     *
     * @param _session the Session to use when creating the MimeMessage
     * @param _to the addressees to send the message directly to
     * @param _cc the addressees to cc (carbon copy)
     * @param _bcc the addressees to bcc (blind carbon copy)
     * @param _message the message to send (which contains the from and subject)
     * @return the fully configured MimeMessage, ready to be sent
     * @throws MessagingException on any problems creating or configuring the MimeMessage
     * @throws MalformedURLException if the URLs for any embedded images are malformed
     */
    private MimeMessage handleTextAndHTMLEMail( final Session _session, final InternetAddress[] _to, final InternetAddress[] _cc, final InternetAddress[] _bcc,
                                             final OutboundEmailMessage _message ) throws MessagingException, IOException {

        final MimeMessage msg = getMimeMessage( _session, _to, _cc, _bcc, _message );

        // plain text version...
        final MimeBodyPart textPart = new MimeBodyPart();
        textPart.setContent( _message.text(), "text/plain; charset=UTF-8" );

        // HTML version...
        final MimeBodyPart htmlPart = new MimeBodyPart();
        final MimeMultipart parts = new MimeMultipart( "related" );
        htmlPart.setContent( parts );

        // get the HTML body with (possibly) embedded images, attachments, and include files in it...
        String html = _message.html();

        // expand any include files, recursively...
        html = expandIncludes( html );

        // find all the images, by searching for image references in the HTML body...
        final List<String> imageURLs = new ArrayList<>();
        final Matcher mat = IMG_PATTERN.matcher( html );
        int index = 0;
        while( mat.find( index ) ) {
            String urlString = mat.group( 2 );

            // if we haven't seen this URL already, we need to add the image part...
            if( !imageURLs.contains( urlString ) ) {
                imageURLs.add( urlString );   // remember that we've seen this URL...
            }
            index = mat.end();
        }

        // iterate over the image URLs and replace them with their content IDs...
        for( int i = 0; i < imageURLs.size(); i++ ) {
            html = html.replaceAll( imageURLs.get( i ), "cid:" + i );
        }

        // first the HTML body (the text part)...
        final MimeBodyPart htmlBody = new MimeBodyPart();
        htmlBody.setContent( html, "text/html; charset=UTF-8");
        parts.addBodyPart( htmlBody );

        // now add any images...
        for( int i = 0; i < imageURLs.size(); i++ ) {
            final BodyPart img = new MimeBodyPart();
            img.setHeader( "Content-ID", "<" + i + ">" );
            img.setDisposition("inline");
            img.setDataHandler(new DataHandler( new URLDataSource( new URL( imageURLs.get( i ) ) ) ) );
            parts.addBodyPart(img);
        }

        // create our multipart - note that the order of part addition is critical...
        final Multipart mp = new MimeMultipart( "alternative" );
        mp.addBodyPart(textPart);
        mp.addBodyPart(htmlPart);

        // Set multipart as the message's content...
        msg.setContent(mp);

        return msg;
    }


    /**
     * Handles the case where the supplied message contains an HTML message but no plain text message, by adding a default plain text message that
     * simply tells the recipient (if they're using an email client that can only read plain text messages) that the message must be viewed with
     * an HTML-capable email client.  The HTML message may contain embedded images so long as they have a valid and readable "src" attribute.
     *
     * @param _session the Session to use when creating the MimeMessage
     * @param _to the addressees to send the message directly to
     * @param _cc the addressees to cc (carbon copy)
     * @param _bcc the addressees to bcc (blind carbon copy)
     * @param _message the message to send (which contains the from and subject)
     * @return the fully configured MimeMessage, ready to be sent
     * @throws MessagingException on any problems creating or configuring the MimeMessage
     * @throws MalformedURLException if the URLs for any embedded images are malformed
     */
    private MimeMessage handleHTMLOnlyEMail( final Session _session,
                                             final InternetAddress[] _to, final InternetAddress[] _cc, final InternetAddress[] _bcc,
                                             final OutboundEmailMessage _message )
            throws MessagingException, IOException {

        // just add a default text message...
        final String defaultText = "(must be viewed with HTML-capable email client)";
        final OutboundEmailMessage msg = new OutboundEmailMessage( _message.from(), _message.subject(), defaultText, _message.html() );
        return handleTextAndHTMLEMail( _session, _to, _cc, _bcc, msg );
    }


    /**
     * Handles the case where the supplied message contains a plain text message and no HTML message (and therefore no embedded images).
     *
     * @param _session the Session to use when creating the MimeMessage
     * @param _to the addressees to send the message directly to
     * @param _cc the addressees to cc (carbon copy)
     * @param _bcc the addressees to bcc (blind carbon copy)
     * @param _message the message to send (which contains the from and subject)
     * @return the fully configured MimeMessage, ready to be sent
     * @throws MessagingException on any problems creating or configuring the MimeMessage
     */
    private MimeMessage handleTextOnlyEmail( final Session _session, final InternetAddress[] _to, final InternetAddress[] _cc, final InternetAddress[] _bcc,
                                             final OutboundEmailMessage _message )
            throws MessagingException {

        MimeMessage msg = getMimeMessage( _session, _to, _cc, _bcc, _message );
        msg.setText( _message.text(), "UTF-8" );
        return msg;
    }


    /**
     * Creates and returns a new MimeMessage instance with the given session, after adding the from, to, cc, bcc, and subject to the message.
     *
     * @param _session the Session to use when creating the MimeMessage
     * @param _to the addressees to send the message directly to
     * @param _cc the addressees to cc (carbon copy)
     * @param _bcc the addressees to bcc (blind carbon copy)
     * @param _message the message to send (which contains the from and subject)
     * @return the MimeMessage created
     * @throws MessagingException on any problem creating or configuring the MimeMessage
     */
    private MimeMessage getMimeMessage( final Session _session,
                                        final InternetAddress[] _to, final InternetAddress[] _cc, final InternetAddress[] _bcc,
                                        final OutboundEmailMessage _message )
            throws MessagingException {

        final MimeMessage msg = new MimeMessage( _session );
        msg.setFrom( _message.from() );
        msg.setRecipients( Message.RecipientType.TO, _to );
        if( _cc != null )
            msg.setRecipients( Message.RecipientType.CC, _cc );
        if( _bcc != null )
            msg.setRecipients( Message.RecipientType.BCC, _bcc );
        msg.setSubject( _message.subject(), "UTF-8" );
        return msg;
    }


    public static void main( final String[] _args ) throws AddressException, IOException {
    }
}
