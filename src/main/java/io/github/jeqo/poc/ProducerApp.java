package io.github.jeqo.poc;

import brave.Tracing;
import brave.jms.JmsTracing;
import brave.sampler.Sampler;
import weblogic.jms.client.JMSXAConnectionFactory;
import zipkin2.Span;
import zipkin2.reporter.AsyncReporter;
import zipkin2.reporter.Reporter;
import zipkin2.reporter.Sender;
import zipkin2.reporter.urlconnection.URLConnectionSender;

import javax.jms.*;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import java.util.Hashtable;

/**
 * Hello world!
 */
public class ProducerApp {
  public static void main(String[] args) throws JMSException, InterruptedException, NamingException {
    //from https://redstack.wordpress.com/2009/12/21/a-simple-jms-client-for-weblogic-11g/
    Hashtable<String, String> properties = new Hashtable<>();
    properties.put(Context.INITIAL_CONTEXT_FACTORY, "weblogic.jndi.WLInitialContextFactory");
    // NOTE: The port number of the server is provided in the next line,
    //       followed by the userid and password on the next two lines.
    properties.put(Context.PROVIDER_URL, "t3://localhost:7001");
    properties.put(Context.SECURITY_PRINCIPAL, "weblogic");
    properties.put(Context.SECURITY_CREDENTIALS, "welcome1");

    InitialContext ctx = new InitialContext(properties);
    // create QueueConnectionFactory
    ConnectionFactory connectionFactory = null;
    try {
      connectionFactory = (JMSXAConnectionFactory) ctx.lookup("jms.cf0");
    } catch (NamingException ne) {
      ne.printStackTrace(System.err);
      System.exit(1);
    }

    final Sender sender =
        URLConnectionSender.create("http://localhost:9411/api/v2/spans").toBuilder().build();
    final Reporter<Span> reporter = AsyncReporter.create(sender);
    final Tracing tracing =
        Tracing.newBuilder()
            .spanReporter(reporter)
            .sampler(Sampler.ALWAYS_SAMPLE)
            .localServiceName("jms-producer")
            .build();
    final JmsTracing jmsTracing =
        JmsTracing.newBuilder(tracing)
            .remoteServiceName("my-broker")
            .build();

    final Connection connection = connectionFactory.createConnection();
    try (Session session = connection.createSession()) {
      Destination queue = session.createQueue("JMSServer-0/SystemModule-0!Queue-0");
      MessageProducer messageProducer = session.createProducer(queue);
      MessageProducer tracingMessageProducer = jmsTracing.messageProducer(messageProducer);

      Message message = session.createTextMessage("hello world");

      tracingMessageProducer.send(message);
    }

    Thread.sleep(10_000L);
  }
}