import com.dilatush.email.Config;
import com.dilatush.util.config.Configurator;
import com.dilatush.util.config.AConfig;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.Map;
import java.util.List;
import java.util.Arrays;
import java.util.Properties;

public class CommsConfigurator implements Configurator {

    public void config( final AConfig _config ) {

        Config config = (Config) _config;

        /* email configuration */

        // JavaMail session properties configuration (in this case, for gmail)...
        var props = new Properties();   // the Java Properties object we store these properties in...

        // These properties were researched via a large number of web sites.  There are many, many more
        // JavaMail properties not specified here, which may be needed for providers other than gmail.

        // properties related to email sending...
        props.put( "mail.smtp.user",                   === email user ===               );
        props.put( "mail.smtp.password",               === email password ===           );
        props.put( "mail.smtp.host",                   "smtp.gmail.com"                 );
        props.put( "mail.smtp.starttls.enable",        true                             );
        props.put( "mail.smtp.starttls.required",      true                             );
        props.put( "mail.smtp.auth",                   true                             );
        props.put( "mail.smtp.port",                   587                              );

        // properties related to email reading...
        props.put( "mail.pop3.socketFactory.class",    "javax.net.ssl.SSLSocketFactory" );
        props.put( "mail.pop3.socketFactory.fallback", false                            );
        props.put( "mail.pop3.socketFactory.port",     995                              );
        props.put( "mail.pop3.port",                   995                              );
        props.put( "mail.pop3.host",                   "pop.gmail.com"                  );
        props.put( "mail.pop3.user",                   === email user ===               );
        props.put( "mail.store.protocol",              "pop3"                           );

        config.email.sessionProperties = props;  // stuff the configured properties into the configuration object...

        // An array of objects each containing three string properties: name, path, and mode.  The name must be unique amongst all configured transfer
        // directories; ideally it should be treated like a variable name.  The path is the absolute or relative path to the transfer directory; if
        // relative the root is Comms' working directory.  The mode must be one of (exactly) "READ_ONLY", "READ_WRITE", "WRITE_ONLY", or "READ_AUTO".
        config.email.transferDirectories = Arrays.asList(
            Map.of( "name", "default", "path", "transfer", "mode", "READ_WRITE" )
        );
    }
}
