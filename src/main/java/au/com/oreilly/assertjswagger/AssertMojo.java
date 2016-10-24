package au.com.oreilly.assertjswagger;


import io.github.robwin.swagger.test.SwaggerAssert;
import io.github.robwin.swagger.test.SwaggerAssertionConfig;
import io.github.robwin.swagger.test.SwaggerAssertionType;
import io.swagger.models.Swagger;
import io.swagger.parser.SwaggerParser;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.assertj.core.api.SoftAssertionError;

import java.io.File;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Properties;

@Mojo(name="assert")
public class AssertMojo extends AbstractMojo {

    @Parameter
    private String inputSpec;

    @Parameter
    private String satisfiesContract;

    @Parameter
    private String isEqualTo;

    @Parameter(defaultValue = "true")
    private boolean failOnError;

    private Swagger parse(String location) {
        SwaggerParser parser = new SwaggerParser();
        try {
            URL url = new URL(location);
            InputStream in = url.openStream();
            return parser.parse(IOUtils.toString(in));
        }
        catch (MalformedURLException e) {
            File f = new File(location);
            return parser.read(f.toString());
        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void execute() throws MojoExecutionException, MojoFailureException {
        Properties props = new Properties();
        props.setProperty("assertj.swagger." + SwaggerAssertionType.INFO.getBarePropertyName(), "true");
        props.setProperty("assertj.swagger." + SwaggerAssertionType.VERSION.getBarePropertyName(), "true");
        SwaggerAssertionConfig config = new SwaggerAssertionConfig(props);
        SwaggerAssert asserter = new SwaggerAssert(parse(inputSpec), config);

        String test = "";
        Swagger contract = null;
        try {
            if (StringUtils.isNotBlank(isEqualTo)) {
                contract = parse(isEqualTo);
                test = "in not equal to";
                asserter.isEqualTo(contract);
                test = "is equal to";
            } else if (StringUtils.isNotBlank(satisfiesContract)) {
                contract = parse(satisfiesContract);
                test = "does not satisfy";
                asserter.satisfiesContract(contract);
                test = "satisfies";
            }

            getLog().info(inputSpec.toString() + " " + test + " " + contract.toString());
        }
        catch (SoftAssertionError e) {
            throw new MojoFailureException(inputSpec.toString() + " " + test + " " + contract.toString(), e);
        }
        catch (Exception e) {
            getLog().error(e);
            if (failOnError) {
                throw new MojoExecutionException(e.getMessage());
            }
        }
    }
}
