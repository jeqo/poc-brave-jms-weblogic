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

import static java.lang.System.out;

public class ConsumerApp {
  public static void main(String[] args) throws JMSException, NamingException, InterruptedException {
    Hashtable<String, String> properties = new Hashtable<>();
    properties.put(Context.INITIAL_CONTEXT_FACTORY, "weblogic.jndi.WLInitialContextFactory");
    // NOTE: The port number of the server is provided in the next line,
    //       followed by the userid and password on the next two lines.
    properties.put(Context.PROVIDER_URL, "t3://localhost:7001");
    properties.put(Context.SECURITY_PRINCIPAL, "weblogic");
    properties.put(Context.SECURITY_CREDENTIALS, "welcome1");

    InitialContext ctx = new InitialContext(properties);
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
            .localServiceName("jms-consumer")
            .sampler(Sampler.ALWAYS_SAMPLE)
            .build();
    final JmsTracing jmsTracing =
        JmsTracing.newBuilder(tracing)
            .remoteServiceName("my-broker")
            .build();

    try (JMSContext context = connectionFactory.createContext()) {
      Destination queue = context.createQueue("JMSServer-0/SystemModule-0!Queue-0");
      JMSConsumer consumer = context.createConsumer(queue);
      String body = consumer.receiveBody(String.class);
      out.println(body);
    }

    try (Connection connection = connectionFactory.createConnection()) {
      Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
      Destination queue = session.createQueue("JMSServer-0/SystemModule-0!Queue-0");
      MessageConsumer messageConsumer = session.createConsumer(queue);
      MessageConsumer tracingMessageConsumer = jmsTracing.messageConsumer(messageConsumer);

      connection.start();
      TextMessage textMessage = (TextMessage) tracingMessageConsumer.receive();
      brave.Span span = jmsTracing.nextSpan(textMessage).name("print").start();
      String body = textMessage.getText();
      span.finish();
      out.println(body);

    }

    Thread.sleep(10_000L);

  }
}
