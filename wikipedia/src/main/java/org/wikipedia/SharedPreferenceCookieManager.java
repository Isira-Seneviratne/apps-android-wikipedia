package org.wikipedia;

import android.content.SharedPreferences;
import android.support.v4.text.TextUtilsCompat;
import android.text.TextUtils;
import android.util.Log;

import java.io.IOException;
import java.net.CookieManager;
import java.net.CookieStore;
import java.net.HttpCookie;
import java.net.URI;
import java.util.*;

public class SharedPreferenceCookieManager extends CookieManager {

    private final HashMap<String, HashMap<String, String>> cookieJar = new HashMap<String, HashMap<String, String>>();
    private final SharedPreferences prefs;

    public SharedPreferenceCookieManager(SharedPreferences prefs) {
        this.prefs = prefs;
        List<String> domains = makeList(prefs.getString(WikipediaApp.PREFERENCE_COOKIE_DOMAINS, ""));
        for (String domain: domains) {
            String key = String.format(WikipediaApp.PREFERENCE_COOKIES_FOR_DOMAINS, domain);
            String cookies = prefs.getString(key, "");
            cookieJar.put(domain, makeCookieMap(makeList(cookies)));
        }
    }

    @Override
    public Map<String, List<String>> get(URI uri, Map<String, List<String>> requestHeaders) throws IOException {
        if (uri == null || requestHeaders == null) {
            throw new IllegalArgumentException("Argument is null");
        }

        Map<String, List<String>> cookieMap = new HashMap<String, List<String>>();

        Map<String, String> cookies = cookieJar.get(uri.getAuthority());

        if (cookies != null) {
            cookieMap.put("Cookie", makeCookieList(cookies));
        }

        return Collections.unmodifiableMap(cookieMap);
    }

    @Override
    public void put(URI uri, Map<String, List<String>> responseHeaders) throws IOException {
        // pre-condition check
        if (uri == null || responseHeaders == null) {
            throw new IllegalArgumentException("Argument is null");
        }

        for (String headerKey : responseHeaders.keySet()) {
            if (headerKey == null || !headerKey.equalsIgnoreCase("Set-Cookie")) {
                continue;
            }

            String domain = uri.getAuthority();

            for (String headerValue : responseHeaders.get(headerKey)) {
                try {
                    List<HttpCookie> cookies = HttpCookie.parse(headerValue);
                    if (!cookieJar.containsKey(domain)) {
                        cookieJar.put(domain, new HashMap<String, String>());
                    }
                    for (HttpCookie cookie : cookies) {
                        cookieJar.get(domain).put(cookie.getName(), cookie.getValue());
                    }
                } catch (IllegalArgumentException e) {
                    // invalid set-cookie header string
                    // no-op
                }
            }
            String prefKey = String.format(WikipediaApp.PREFERENCE_COOKIES_FOR_DOMAINS, domain);
            prefs.edit()
                    .putString(prefKey, makeString(makeCookieList(cookieJar.get(domain))))
                    .putString(WikipediaApp.PREFERENCE_COOKIE_DOMAINS, makeString(cookieJar.keySet()))
                    .commit();
        }
    }

    @Override
    public CookieStore getCookieStore() {
        // We don't actually have one. hehe
        throw new UnsupportedOperationException("We poor. We no have CookieStore");
    }

    private HashMap<String, String> makeCookieMap(List<String> cookies) {
        HashMap<String, String> cookiesMap = new HashMap<String, String>();
        for (String cookie : cookies) {
            String[] parts = cookie.split("=");
            cookiesMap.put(parts[0], parts[1]);
        }
        return cookiesMap;
    }

    private List<String> makeCookieList(Map<String, String> cookies) {
        ArrayList<String> cookiesList = new ArrayList<String>();
        for (Map.Entry<String, String> entry: cookies.entrySet()) {
            cookiesList.add(entry.getKey() + "=" + entry.getValue());
        }
        return cookiesList;
    }

    private String makeString(Iterable<String> list) {
        return TextUtils.join(";", list);
    }

    private List<String> makeList(String str) {
        return Arrays.asList(TextUtils.split(str, ";"));
    }
}
