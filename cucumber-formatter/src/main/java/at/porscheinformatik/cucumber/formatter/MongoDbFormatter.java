package at.porscheinformatik.cucumber.formatter;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;

import cucumber.runtime.CucumberException;
import gherkin.formatter.NiceAppendable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.config.DefaultClientConfig;
import com.sun.jersey.api.client.filter.HTTPBasicAuthFilter;

/**
 * Use SystemProperty {@link at.porscheinformatik.cucumber.formatter.MongoDbFormatter#BASEURL_SYS_PROP} to specify a
 * url where the cucumber-report-web is provided
 */
public class MongoDbFormatter extends AbstractJsonFormatter
{
    private static final Logger LOGGER = LoggerFactory.getLogger(MongoDbFormatter.class);

    public static final String COLLECTION_NAME_PROP = "cucumber.report.collection.name";
    public static final String DEFAULT_COLLECTION = "product_version";

    public static final String PRODUCT_NAME_PROP = "cucumber.report.product.name";
    public static final String PRODUCT_VERSION_PROP = "cucumber.report.product.version";
    public static final String DEFAULT_PRODUCT_VERSION = "1.0";

    public static final String BASEURL_SYS_PROP = "cucumber.report.server.baseUrl";
    public static final String DEFAULT_BASE_URL = "http://localhost:8081";

    public static final String USERNAME_SYS_PROP = "cucumber.report.server.username";
    public static final String PASSWORD_SYS_PROP = "cucumber.report.server.password";

    private NiceAppendable jsonOutput;

    private WebResource restResource;

    public MongoDbFormatter()
    {
        Client client = Client.create(new DefaultClientConfig());
        restResource = client.resource(getBaseUrlWithDefault());
        if (isReportDbUserNameSet())
        {
            restResource.addFilter(new HTTPBasicAuthFilter(getReportDbUserName(), getReportDbPassword()));
        }

        try
        {
            jsonOutput = new NiceAppendable(new OutputStreamWriter(new DbOutputStream(), "UTF-8"));
        }
        catch (UnsupportedEncodingException e)
        {
            throw new CucumberException(e);
        }
    }

    private String getBaseUrlWithDefault()
    {
        return System.getProperty(BASEURL_SYS_PROP, DEFAULT_BASE_URL);
    }

    private String getReportDbUserName()
    {
        return System.getProperty(USERNAME_SYS_PROP);
    }

    private boolean isReportDbUserNameSet()
    {
        return System.getProperty(USERNAME_SYS_PROP) != null;
    }

    private String getReportDbPassword()
    {
        return System.getProperty(PASSWORD_SYS_PROP);
    }

    protected void dbInsertJson(String data)
    {
        try
        {
            restResource.path("rest").path("cucumberplugin").path(getCollection()).path("report").entity(data).post();
            LOGGER.info("JSON sent to cucumber-report-db");
        }
        catch (Exception e)
        {
            LOGGER.info("Error occured sending JSON to cucumber-report-db", e);
        }
    }

    protected void dbInsertMedia(String fileName, final String mimeType, InputStream inputStream)
    {
        try
        {
            restResource.path("rest").path("cucumberplugin").path(getCollection()).path("media")
                    .queryParam("filename", fileName)
                    .type(mimeType).entity(inputStream).post();
            LOGGER.info("Image {} sent to cucumber-report-db", fileName);
        }
        catch (Exception e)
        {
            LOGGER.error("Error occured sending image {} to cucumber-report-db", fileName, e);
        }
    }

    protected String getCollection()
    {
        if (System.getProperty(PRODUCT_NAME_PROP)!= null)
        {
            return System.getProperty(PRODUCT_NAME_PROP) + "_" + System.getProperty(PRODUCT_VERSION_PROP, DEFAULT_PRODUCT_VERSION);
        }
        return System.getProperty(COLLECTION_NAME_PROP, DEFAULT_COLLECTION);
    }

    @Override
    protected String doEmbedding(String extension, String mimeType, byte[] data)
    {
        String fileName = String.format("embedded%d_%s.%s", embeddedIndex++, getFormattedDate(), extension);

        dbInsertMedia(fileName, mimeType, new ByteArrayInputStream(data));
        return fileName;
    }

    @Override
    protected NiceAppendable jsOut()
    {
        return jsonOutput;
    }

    public class DbOutputStream extends OutputStream
    {
        private static final String DATE_REGEX = "\"date\":\"(.*)\"";
        private static final String DATE_REPLACEMENT = "\"date\":\\{\"\\$date\":\"$1\"\\}";

        private StringBuilder output = new StringBuilder();

        @Override
        public void write(int b) throws IOException
        {
            output.append((char) b);
        }

        @Override
        public void close() throws IOException
        {
            final String preparedJsonForDb = output.toString().replaceAll(DATE_REGEX, DATE_REPLACEMENT);
            dbInsertJson(preparedJsonForDb);
        }
    }

}
