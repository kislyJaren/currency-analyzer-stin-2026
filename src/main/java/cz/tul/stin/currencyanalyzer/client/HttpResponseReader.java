package cz.tul.stin.currencyanalyzer.client;

import java.net.URI;

public interface HttpResponseReader {

    String get(URI uri);
}
