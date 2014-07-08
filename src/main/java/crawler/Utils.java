package crawler;

import java.net.URI;
import java.net.URISyntaxException;

/**
 * Miscellaneous utils
 */
public class Utils {
    /**
     * Remove "fragment" part from URI, returning new URI.
     */
    static URI truncateFragment(URI uri) throws URISyntaxException {
        return new URI(uri.getScheme(), uri.getHost(), uri.getPath(), uri.getQuery());
    }
}
