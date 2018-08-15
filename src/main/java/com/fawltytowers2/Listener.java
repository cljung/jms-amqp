/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
//package example;

import org.apache.qpid.jms.*;

import javax.jms.*;

class Listener {

    public static void main(String[] args) throws JMSException {

        final String TOPIC_PREFIX = "topic://";

        String ConnectionURI = GetConnectionURI(args);
        System.out.println("ConnectionURI: " + ConnectionURI);

        String destinationName = arg(args, 0, "topic://event");

        JmsConnectionFactory factory = new JmsConnectionFactory(ConnectionURI);
        Connection connection = factory.createConnection();
        connection.start();
        Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);

        Destination destination = null;
        if (destinationName.startsWith(TOPIC_PREFIX)) {
            destination = session.createTopic(destinationName.substring(TOPIC_PREFIX.length()));
        } else {
            destination = session.createQueue(destinationName);
        }

        System.out.println("destination: " + destination);

        MessageConsumer consumer = session.createConsumer(destination);
        long start = System.currentTimeMillis();
        long count = 1;
        System.out.println("Waiting for messages...");
        while (true) {
            Message msg = consumer.receive();
            if (msg instanceof TextMessage) {
                String body = ((TextMessage) msg).getText();
                System.out.println("Message body: " + body);
                if ("SHUTDOWN".equals(body)) {
                    long diff = System.currentTimeMillis() - start;
                    System.out.println(String.format("Received %d in %.2f seconds", count, (1.0 * diff / 1000.0)));
                    connection.close();
                    try {
                        Thread.sleep(10);
                    } catch (Exception e) {
                    }
                    System.exit(1);
                } else {
                    try {
                        if (count != msg.getIntProperty("id")) {
                            System.out.println("mismatch: " + count + "!=" + msg.getIntProperty("id"));
                        }
                    } catch (NumberFormatException ignore) {
                    }

                    if (count == 1) {
                        start = System.currentTimeMillis();
                    } else if (count % 1000 == 0) {
                        System.out.println(String.format("Received %d messages.", count));
                    }
                    count++;
                }

            } else {
                System.out.println("Unexpected message type: " + msg.getClass());
            }
        }
    }

    private static String GetConnectionURI(String[] args) {
        String user = env("JMS_USER", "admin");
        String password = env("JMS_PASSWORD", "password");
        String host = env("JMS_HOST", "localhost");
        String protocol = "amqp";
        int port = Integer.parseInt(env("JMS_PORT", "5672"));
        if (port == 5671) {
            protocol = "amqps";
        }
        String connectTimeout = env("JMS_CONNECTTIMEOUT", "");
        String idleTimeout = env("JMS_IDLETIMEOUT", "");
        String timeouts = "";
        if (connectTimeout.length() > 0) {
            timeouts += "&transport.connectTimeout=" + connectTimeout;
        }
        if (idleTimeout.length() > 0) {
            timeouts += "&amqp.idleTimeout=" + idleTimeout;
        }
        String ConnectionURI = protocol + "://" + host + ":" + port + "/"
                + "?jms.username=" + user
                + "&jms.password=" + password
                + timeouts;
        return ConnectionURI;
    }

    private static String env(String key, String defaultValue) {
        String rc = System.getenv(key);
        if (rc == null)
            return defaultValue;
        return rc;
    }

    private static String arg(String[] args, int index, String defaultValue) {
        if (index < args.length)
            return args[index];
        else
            return defaultValue;
    }
}