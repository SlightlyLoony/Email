package com.dilatush.email;

import jakarta.mail.*;
import jakarta.mail.internet.MimeMultipart;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class EmailReader {

    private static final Logger LOGGER = Logger.getLogger( new Object(){}.getClass().getEnclosingClass().getCanonicalName() );

    private final String user;
    private final String password;

    // TODO: add support for separate text and html results
    // TODO: add support for saving attachments, inlined images, large HTML bodies

    public EmailReader( final String _user, final String _password ) {
        user = _user;
        password = _password;
    }


    public List<OutboundEmailMessage> read( final Session _session ) {

        List<OutboundEmailMessage> result = new ArrayList<>();

        try {

            Store store = _session.getStore( "pop3" );
            store.connect();

            Folder inbox = store.getFolder( "INBOX" );
            inbox.open( Folder.READ_ONLY );

            // read all messages in the inbox...
            Message[] messages = inbox.getMessages();
            for ( Message message : messages) {
                result.add( new OutboundEmailMessage( message.getFrom()[0].toString(), message.getSubject(), getTextFromMessage( message ), null ) );
            }

            // mark all the messages as deleted...
            for( Message message : messages ) {
                message.setFlag( Flags.Flag.DELETED, true );
            }

            // close the folder, expunging deleted messages, and close the store...
            inbox.close( true );
            store.close();

            return result;
        }
        catch( MessagingException | IOException _e ) {
            LOGGER.log( Level.SEVERE, "Problem receiving email", _e );
            return null;
        }
    }


    private String getTextFromMessage(Message message) throws MessagingException, IOException {
        String result = "";
        if (message.isMimeType("text/plain")) {
            result = message.getContent().toString();
        } else if (message.isMimeType("multipart/*")) {
            MimeMultipart mimeMultipart = (MimeMultipart) message.getContent();
            result = getTextFromMimeMultipart(mimeMultipart);
        }
        return result;
    }


    private String getTextFromMimeMultipart(
            MimeMultipart mimeMultipart)  throws MessagingException, IOException{
        StringBuilder result = new StringBuilder();
        int count = mimeMultipart.getCount();
        for (int i = 0; i < count; i++) {
            BodyPart bodyPart = mimeMultipart.getBodyPart(i);
            if (bodyPart.isMimeType("text/plain")) {
                result.append( "\n" ).append( bodyPart.getContent() );
                break; // without break same text appears twice in my tests
            } else if (bodyPart.isMimeType("text/html")) {
                String html = (String) bodyPart.getContent();
                result.append( "\n" ).append( org.jsoup.Jsoup.parse( html ).text() );
            } else if (bodyPart.getContent() instanceof MimeMultipart){
                result.append( getTextFromMimeMultipart( (MimeMultipart) bodyPart.getContent() ) );
            }
        }
        return result.toString();
    }
}
