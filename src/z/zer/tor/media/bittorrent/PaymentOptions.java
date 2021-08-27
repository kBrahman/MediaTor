package z.zer.tor.media.bittorrent;

import com.frostwire.jlibtorrent.Entry;

import java.util.Map;

public class PaymentOptions implements Mappable {
    /**
     * BitCoin URI, see BIP-0021 - https://github.com/bitcoin/bips/blob/master/bip-0021.mediawiki
     * bitcoinurn     = "bitcoin:" bitcoinaddress [ "?" bitcoinparams ]
     * bitcoinaddress = base58 *base58
     * Example: bitcoin:175tWpb8K1S7NmH4Zx6rewF9WQrcZv245W
     * <p>
     * To be serialized as dictionary in the .torrent as follows
     * paymentOptions: {
     * bitcoin: "bitcoin:13hbpRfDT1HKmK4jejHgh7MM9W1NCPFT8v",
     * paypalUrl: "http://frostwire.com/donate"
     * }
     */
    public final String bitcoin;

    /**
     * Simply a valid email address for creating a paypal payment form
     */
    public final String paypalUrl;

    private String itemName;

    public PaymentOptions(String bitcoin, String paypal) {
        this.bitcoin = bitcoin;
        this.paypalUrl = paypal;
    }

    public PaymentOptions(Map<String, Entry> paymentOptionsMap) {
        final Entry paymentOptions = paymentOptionsMap.get("paymentOptions");
        if (paymentOptions != null) {
            final Map<String, Entry> dictionary = paymentOptions.dictionary();
            if (dictionary.containsKey("bitcoin")) {
                this.bitcoin = dictionary.get("bitcoin").string();
            } else {
                this.bitcoin = null;
            }

            if (dictionary.containsKey("paypalUrl")) {
                this.paypalUrl = dictionary.get("paypalUrl").string();
            } else {
                this.paypalUrl = null;
            }
        } else {
            this.bitcoin = null;
            this.paypalUrl = null;
        }
    }

    public void setItemName(String name) {
        itemName = name;
    }

    public boolean isEmpty() {
        return bitcoin == null && paypalUrl == null;
    }
}