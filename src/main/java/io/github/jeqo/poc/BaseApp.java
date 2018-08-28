package io.github.jeqo.poc;

import brave.Tracing;
import brave.jms.JmsTracing;
import brave.sampler.Sampler;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import weblogic.jms.client.JMSXAConnectionFactory;
import zipkin2.Span;
import zipkin2.reporter.AsyncReporter;
import zipkin2.reporter.Reporter;
import zipkin2.reporter.Sender;
import zipkin2.reporter.urlconnection.URLConnectionSender;

import javax.jms.ConnectionFactory;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import java.util.Hashtable;

public class BaseApp {

  private final Config config;

  final JmsTracing jmsTracing;
  final String destinationName;

  public BaseApp(String serviceName) {
    config = ConfigFactory.load();

    final Sender sender =
        URLConnectionSender.create(config.getString("zipkin.api.url")).toBuilder().build();
    final Reporter<Span> reporter = AsyncReporter.create(sender);
    final Tracing tracing =
        Tracing.newBuilder()
            .spanReporter(reporter)
            .localServiceName(serviceName)
            .sampler(Sampler.ALWAYS_SAMPLE)
            .build();
    jmsTracing =
        JmsTracing.newBuilder(tracing)
            .remoteServiceName(config.getString("zipkin.api.remote-service"))
            .build();

    this.destinationName =
        String.format(
            "%s/%s!%s",
            config.getString("weblogic.jms.server"),
            config.getString("weblogic.jms.module"),
            config.getString("weblogic.jms.queue"));
  }

  public ConnectionFactory connectionFactory() throws NamingException {
    Hashtable<String, String> properties = new Hashtable<>();
    properties.put(Context.INITIAL_CONTEXT_FACTORY, "weblogic.jndi.WLInitialContextFactory");
    // NOTE: The port number of the server is provided in the next line,
    //       followed by the userid and password on the next two lines.
    properties.put(Context.PROVIDER_URL, config.getString("weblogic.jms.provider-url"));
    properties.put(Context.SECURITY_PRINCIPAL, config.getString("weblogic.jms.username"));
    properties.put(Context.SECURITY_CREDENTIALS, config.getString("weblogic.jms.password"));

    InitialContext ctx = new InitialContext(properties);
    return (JMSXAConnectionFactory) ctx.lookup(config.getString("weblogic.jms.connection-factory"));
  }
}
