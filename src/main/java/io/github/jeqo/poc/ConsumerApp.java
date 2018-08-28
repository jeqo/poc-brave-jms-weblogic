package io.github.jeqo.poc;

import javax.jms.*;
import javax.naming.NamingException;

import static java.lang.System.out;

public final class ConsumerApp extends BaseApp {
  private ConsumerApp() {
    super("jms-consumer-1.1");
  }

  private void consume() throws NamingException, JMSException {
    final ConnectionFactory cf = jmsTracing.connectionFactory(connectionFactory());

    try (Connection connection = cf.createConnection()) {
      Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
      Destination queue = session.createQueue(destinationName);
      MessageConsumer messageConsumer = session.createConsumer(queue);

      connection.start();
      TextMessage textMessage = (TextMessage) messageConsumer.receive();
      brave.Span span = jmsTracing.nextSpan(textMessage).name("print").start();
      String body = textMessage.getText();
      span.finish();
      out.println("Message received: " + body);
    }
  }

  public static void main(String[] args) throws JMSException, InterruptedException, NamingException {
    ConsumerApp app = new ConsumerApp();

    app.consume();

    Thread.sleep(10_000L);

  }
}
