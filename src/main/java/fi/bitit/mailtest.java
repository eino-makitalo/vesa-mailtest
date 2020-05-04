package fi.bitit;

import com.microsoft.aad.msal4j.*;

import java.io.PrintStream;
import java.net.URI;
import java.util.Properties;
import java.util.Set;


public class mailtest {
    final static Set<String> SCOPE = Set.of("User.Read","Mail.Read");
    public static void main(String[] args) {
        IAuthenticationResult authresult = null;
        try {
            Properties prop = new ReadPropertyValues().getProperties();
            String applicationId=prop.getProperty("applicationId");
            String authEndpoint=prop.getProperty("authEndpoint");
            authresult = mailtest.acquireTokenInteractive(applicationId,authEndpoint);
            System.out.println(authresult.accessToken());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void print(PrintStream out) {
        out.println("Hello, World!");
    }

    private static IAuthenticationResult acquireTokenInteractive(String applicationId,String authEndpoint) throws Exception {

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
                        .builder(new URI("http://localhost"))
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
