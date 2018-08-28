package io.github.jeqo.poc;

import javax.jms.Destination;
import javax.jms.JMSConsumer;
import javax.jms.JMSContext;
import javax.naming.NamingException;

import static java.lang.System.out;

public final class Consumer20App extends BaseApp {

  private Consumer20App() {
    super("jms-consumer-2.0");
  }

  private void consume() throws NamingException {
    try (JMSContext context = jmsTracing.connectionFactory(connectionFactory()).createContext()) {
      Destination queue = context.createQueue(destinationName);
      JMSConsumer consumer = context.createConsumer(queue);
      String body = consumer.receiveBody(String.class);
      out.println(body);
    }
  }

  public static void main(String[] args) throws NamingException, InterruptedException {
    Consumer20App app = new Consumer20App();

    app.consume();

    Thread.sleep(10_000L);
  }
}
