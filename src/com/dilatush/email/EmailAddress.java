package com.dilatush.email;

/**
 * Instances of this class represent a subset of valid standard Internet email addresses:
 * <ul>
 *     <li>Comments in domains, like {@code joe@bogus.com(comment)}, are not supported.</li>
 *     <li>IP addresses in domains, like {@code joe@[10.34.222.101]}, are not supported.</li>
 *     <li>Reserved domain names (in RFC 2606) are not allowed.</li>
 *     <li>Only ASCII domain names are supported (no UTF-8 encoding).</li>
 * </ul>
 */
public class EmailAddress( String mailbox, String domain, String displayName ) {

    public final String mailbox;
    public final String domain;
    public final String displayName;

    public EmailAddress( final String _address ) {

    }
}
/*

                   .test
                .example
                .invalid
              .localhost

        example.com
        example.net
        example.org


    The domain name part of an email address has to conform to strict guidelines: it must match the requirements for a hostname, a list of
    dot-separated DNS labels, each label being limited to a length of 63 characters and consisting of:[5]:§2


        uppercase and lowercase Latin letters A to Z and a to z;
        digits 0 to 9, provided that top-level domain names are not all-numeric;
        hyphen -, provided that it is not the first or last character.

        This rule is known as the LDH rule (letters, digits, hyphen). In addition, the domain may be an IP address literal, surrounded by square
        brackets [], such as jsmith@[192.168.2.1] or jsmith@[IPv6:2001:db8::1], although this is rarely seen except in email spam. Internationalized
        domain names (which are encoded to comply with the requirements for a hostname) allow for presentation of non-ASCII domains. In mail systems
         compliant with RFC 6531 and RFC 6532 an email address may be encoded as UTF-8, both a local-part as well as a domain name.

        Comments are allowed in the domain as well as in the local-part; for example, john.smith@(comment)example.com and
        john.smith@example.com(comment) are equivalent to john.smith@example.com.

        Reserved domains
        RFC 2606 specifies that certain domains, for example those intended for documentation and testing, should not be resolvable and that as a
        result mail addressed to mailboxes in them and their subdomains should be non-deliverable. Of note for e-mail are example, invalid,
        example.com, example.net, and example.org.

 */