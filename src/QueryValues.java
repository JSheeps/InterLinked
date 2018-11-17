import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;

public class QueryValues extends HashMap<String, String> {
    public QueryValues(String query) {
        int prevIndex = 0, ampersandIndex;
        while ( (ampersandIndex = query.indexOf('&', prevIndex)) != -1) {
            String queryString = query.substring(prevIndex, ampersandIndex);
            add(queryString);

            prevIndex = ampersandIndex + 1;
        }

        String queryString = query.substring(prevIndex);
        add(queryString);
    }

    public QueryValues(){}

    public void add(String query) {
        int equalIndex = query.indexOf('=');
        String variable = query.substring(0, equalIndex);
        String value = query.substring(equalIndex + 1);

        put(variable, value);
    }

    String toQueryString() {
        StringBuilder queryString = new StringBuilder("?");
        for(String key : this.keySet()){
            queryString.append(key).append("=").append(URLEncoder.encode(get(key), StandardCharsets.UTF_8)).append("&");
        }
        queryString = new StringBuilder(queryString.substring(0, queryString.length() - 1));

        return queryString.toString();
    }

}