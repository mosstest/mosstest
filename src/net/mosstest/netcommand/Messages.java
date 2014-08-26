package net.mosstest.netcommand;

import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

// TODO: Auto-generated Javadoc
/**
 * Helper class for internationalization.
 */
class Messages {

    /** The Constant BUNDLE_NAME. */
    private static final String BUNDLE_NAME = "net.mosstest.netcommand.messages"; //$NON-NLS-1$

    /** The res bundle. */
    private static ResourceBundle resBundle = ResourceBundle
            .getBundle(BUNDLE_NAME);

    /**
     * Change language.
     *
     * @param identifier the identifier
     */
    public static void changeLanguage(Locale identifier) {
        resBundle = ResourceBundle.getBundle(BUNDLE_NAME, identifier);
    }

    /**
     * Instantiates a new messages.
     */
    private Messages() {
    }

    /**
     * Gets the string.
     *
     * @param key the key
     * @return the string
     */
    public static String getString(String key) {
        try {
            return resBundle.getString(key);
        } catch (MissingResourceException e) {
            return '!' + key + '!';
        }
    }
}

