package com.dilatush.email;

import com.dilatush.util.Outcome;
import jakarta.mail.internet.InternetAddress;

import java.io.UnsupportedEncodingException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.dilatush.util.Strings.isEmpty;

/**
 * Instances of this class represent a subset of valid standard Internet email addresses.  This subset includes all but the oddest and least-used
 * forms of email addresses.  The significant variations from the complete standard include:
 * <ul>
 *     <li>Comments in domains, like {@code joe@bogus.com(comment)}, are not supported.</li>
 *     <li>IP addresses in domains, like {@code joe@[10.34.222.101]}, are not supported.</li>
 *     <li>Reserved domain names (in RFC 2606, like {@code example.net}) are not allowed.</li>
 *     <li>Only ASCII domain names are supported (no UTF-8 encoding).</li>
 * </ul>
 * Instances of this class are immutable and threadsafe; the fields are final and publicly available.
 */
@SuppressWarnings( "unused" )
public class EmailAddress {

    private final static Outcome.Forge<EmailAddress> OUTCOME = new Outcome.Forge<>();

    /** The mailbox name.  For example, the {@code tom} in {@code Tom Dilatush<tom@dilatush.com>}. */
    public final String mailbox;

    /** The domain name.  For example, the {@code dilatush.com} in {@code Tom Dilatush<tom@dilatush.com>}. */
    public final String domain;

    /** The display name.  For example, the {@code Tom Dilatush} in {@code Tom Dilatush<tom@dilatush.com>}. */
    public final String displayName;


    /**
     * Create a new instance of this class with the optional display name, mailbox, and domain.  Note that this constructor does no validation of
     * any kind; the public factory methods must do that.
     *
     * @param _mailbox The mailbox portion of this email address.
     * @param _domain The domain portion of this email address.
     * @param _displayName The optional (may be null) display name for this email address.
     */
    private EmailAddress( final String _mailbox, final String _domain, final String _displayName ) {
        mailbox = _mailbox;
        domain = _domain;
        displayName = _displayName;
    }


    // RFC 3696 and RFC 2606 are the sources for these rules...
    private final static Pattern DISPLAY_NAME_EXTRACTOR = Pattern.compile( "([^<]*)<([^>]+)>" );
    private final static Pattern DOMAIN_VALIDATOR = Pattern.compile( "((([a-zA-Z0-9]+-)*[a-zA-Z0-9]+\\.)+([a-zA-Z0-9]+-)*[a-zA-Z0-9]+)" );
    private final static Pattern MAILBOX_VALIDATOR = Pattern.compile( "((([a-zA-Z0-9!#$%&'*+\\-/=?^_`{|}~]|\\\\.)+\\.)*([a-zA-Z0-9!#$%&'*+\\-/=?^_`{|}~]|\\\\.)+)|\"[^\"]+\"" );
    private final static Pattern TLD_VALIDATOR = Pattern.compile( "[0-9]+|test|example|invalid|localhost" );


    /**
     * Create a new instance of {@link EmailAddress} from the given string.  The string must be in standard form, either {@code mailbox@domain} or
     * {@code displayName<mailbox@domain>}, and the parts must comply with the rules outlined in the class comments.  In general, though, all this
     * means is that the given string must look like a normal email address.
     *
     * @param _emailAddress The string containing a compliant email address.
     * @return The outcome of this method.  If ok, then the info is the instance of {@link EmailAddress} created.  If not ok, the message contains
     * an explanation of why.
     */
    public static Outcome<EmailAddress> fromString( final String _emailAddress ) {

        // fail fast if we didn't get anything at all...
        if( isEmpty( _emailAddress ) )
            return OUTCOME.notOk( "No email address was supplied." );

        // starting assumption is that the actual email address is the entire given string and that there's no display name...
        String emailAddress = _emailAddress;
        String displayName = null;

        // handle the display name, if we had one...
        Matcher mat = DISPLAY_NAME_EXTRACTOR.matcher( _emailAddress );
        if( mat.matches() ) {
            displayName = mat.group( 1 ).trim();
            emailAddress = mat.group( 2 );
        }

        // make sure we actually GOT a display name...
        if( isEmpty( displayName ) )
            return OUTCOME.notOk( "Display name is empty: " + _emailAddress );

        // split the actual email address into mailbox and domain, using the last "@" as the split point...
        int localSplit = emailAddress.lastIndexOf( '@' );
        if( localSplit < 0 )
            return OUTCOME.notOk( "Email address has no '@': " + emailAddress );
        String mailbox = emailAddress.substring( 0, localSplit );
        String domain = emailAddress.substring( localSplit + 1 );

        // validate the domain...
        mat = DOMAIN_VALIDATOR.matcher( domain );
        if( !mat.matches() )
            return OUTCOME.notOk( "Domain in email address is not valid: " + domain );
        if( domain.length() > 255 )  // per RFC 3696...
            return OUTCOME.notOk( "Domain in email address is too long: " + domain );

        // validate the top level domain...
        int tldSplit = domain.lastIndexOf( '.' );
        if( tldSplit < 0 )
            return OUTCOME.notOk( "Domain in email address is just the top level domain: " + domain );
        String tld = domain.substring( tldSplit + 1 );
        mat = TLD_VALIDATOR.matcher( tld );
        if( mat.matches() )
            return OUTCOME.notOk( "Top level domain in email address is not valid (see RFC 2606): " + tld );

        // validate the first and second level domain (per RFC 2606)...
        if( domain.endsWith( "example.com" ) || domain.endsWith( "example.net" ) || domain.endsWith( "example.org" ) )
            return OUTCOME.notOk( "Domain in email address is not valid (see RFC 2606): " + domain );

        // validate the mailbox...
        if( isEmpty( mailbox ) )
            return OUTCOME.notOk( "No mailbox name supplied: " + emailAddress );
        mat = MAILBOX_VALIDATOR.matcher( mailbox );
        if( !mat.matches() )
            return OUTCOME.notOk( "Mailbox name in email address is not valid: " + mailbox );

        // if we get here, then everything is hunky-dory...
        return OUTCOME.ok( new EmailAddress( mailbox, domain, displayName ));
    }


    public static Outcome<EmailAddress> fromInternetAddress( final InternetAddress _internetAddress ) {
        return fromString( _internetAddress.toString() );
    }


    @Override
    public String toString() {
        return (displayName == null)
                ? mailbox + "@" + domain
                : displayName + "<" + mailbox + "@" + domain + ">";
    }


    public static void main( final String[] _args ) throws UnsupportedEncodingException {

        Outcome<EmailAddress> result = fromString( "tom@dilatush.com" );
        result = fromString( "Tom Dilatush<tom@dilatush.com>" );
        result = fromString( "<tom@dilatush.com>" );

        InternetAddress ia = new InternetAddress( "tom@dilatush.com", "Tom Dilatush" );
        result = fromInternetAddress( ia );

        new Object().hashCode();
    }
}
