package fi.bitit;

import com.microsoft.aad.msal4j.*;
import com.sun.mail.smtp.SMTPTransport;

import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.activation.FileDataSource;
import javax.mail.*;
import javax.mail.internet.*;
import java.net.InetAddress;
import java.net.URI;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Properties;
import java.util.Set;


public class mailtest {
    //public static final Set<String> SCOPE = Set.of("Mail.ReadAll","Mail.SendAll");

    public static void main(String[] args) {
        IAuthenticationResult authresult = null;
        try {
            Properties prop = new ReadPropertyValues().getProperties();
            String applicationId=prop.getProperty("applicationId");
            String authEndpoint=prop.getProperty("authEndpoint");
            String redirectURI=prop.getProperty("redirectURI");
            String scopes[]=prop.getProperty("scopes").split(",");
            String userName = prop.getProperty("username");
            Set<String> SCOPE = Set.of(scopes);
            String testRecipients = prop.getProperty("testRecipients");

            authresult = mailtest.acquireTokenInteractive(applicationId,authEndpoint,redirectURI, SCOPE);
            sendTestMail(testRecipients,
                    authresult.accessToken(),
                    userName);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    private static String tokenforsmtp(String userName, String accessToken) {
        final String ctrlA=Character.toString((char) 1);

        final String coded= "user=" + userName + ctrlA+"auth=Bearer " + accessToken + ctrlA+ctrlA;
        return Base64.getEncoder().encodeToString(coded.getBytes());
        //base64("user=" + userName + "^Aauth=Bearer " + accessToken + "^A^A")
    }
    private static void sendTestMail(String tos, String accessToken,String username) throws MessagingException {
        Properties props = new Properties();
       // props.put("mail.imap.ssl.enable", "true"); // required for Gmail
        props.put("mail.smtp.auth.xoauth2.disable","false");
        props.put("mail.smtp.sasl.enable", "true");
        props.put("mail.imap.auth.mechanisms", "XOAUTH2");
        props.put("mail.smtp.auth.mechanisms","XOAUTH2");
        props.put("mail.smtp.port", "587");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.transport.protocol","smtp");
        props.put("mail.smtp.host","smtp.office365.com");
        String token = tokenforsmtp(username,accessToken);
        props.put("mail.debug",true);

        Session session = Session.getInstance(props);
        try {
            Message m1 = testMessage(userName,session,tos);
            SMTPTransport transport = (SMTPTransport) session.getTransport("smtp");
            transport.connect("smtp.office365.com",username,null);
            transport.issueCommand("AUTH XOAUTH2 " + token, 235);
            transport.sendMessage(m1, m1.getAllRecipients());

        } catch (NoSuchProviderException e) {
            e.printStackTrace();
        }
        //
    }

    public static Message testMessage(String from,Session session,String tos) {
        try {
            // Create a default MimeMessage object.
            Message message = new MimeMessage(session);

            // Set From: header field of the header.
            message.setFrom(new InternetAddress(from));

            // Set To: header field of the header.
            var recipients = InternetAddress.parse(tos);
            message.setRecipients(Message.RecipientType.TO,
                    recipients);

            // Set Subject: header field
            message.setSubject("Eino's example to send through Office365");

            // Create the message part
            BodyPart messageBodyPart = new MimeBodyPart();

            // Now set the actual message
            messageBodyPart.setText("This is message body");

            // Create a multipar message
            Multipart multipart = new MimeMultipart();

            // Set text message part
            multipart.addBodyPart(messageBodyPart);

            // Part two is attachment
            messageBodyPart = new MimeBodyPart();
            String filename = "/temp/WhatsApp Video 2020-04-09 at 09.00.52.mp4";
            DataSource source = new FileDataSource(filename);
            messageBodyPart.setDataHandler(new DataHandler(source));
            messageBodyPart.setFileName(filename);
            multipart.addBodyPart(messageBodyPart);

            // Send the complete message parts
            message.setContent(multipart);
            return message;
        } catch (AddressException e) {
            e.printStackTrace();
        } catch (MessagingException e) {
            e.printStackTrace();
        }
        return null;
    }

    private static IAuthenticationResult acquireTokenInteractive(String applicationId, String authEndpoint, String redirectURI, Set<String> SCOPE) throws Exception {

        // Load token cache from file and initialize token cache aspect. The token cache will have
        // dummy data, so the acquireTokenSilently call will fail.
        TokenCacheAspect tokenCacheAspect = new TokenCacheAspect("fi/bitit/sample_cache.json");

        PublicClientApplication pca = PublicClientApplication.builder(applicationId)
                .authority(authEndpoint)
                .setTokenCacheAccessAspect(tokenCacheAspect)
                .build();

        Set<IAccount> accountsInCache = pca.getAccounts().join();
        // Take first account in the cache. In a production application, you would filter
        // accountsInCache to get the right account for the user authenticating.
        IAccount account = accountsInCache.iterator().next();

        IAuthenticationResult result;
        try {
            SilentParameters silentParameters =
                    SilentParameters
                            .builder(SCOPE, account)
                            .build();

            // try to acquire token silently. This call will fail since the token cache
            // does not have any data for the user you are trying to acquire a token for
            result = pca.acquireTokenSilently(silentParameters).join();
        } catch (Exception ex) {
            if (ex.getCause() instanceof MsalException) {

                InteractiveRequestParameters parameters = InteractiveRequestParameters
                        .builder(new URI(redirectURI))
                        .scopes(SCOPE)
                        .build();

                // Try to acquire a token interactively with system browser. If successful, you should see
                // the token and account information printed out to console
                result = pca.acquireToken(parameters).join();
            } else {
                // Handle other exceptions accordingly
                throw ex;
            }
        }
        return result;
    }
}
