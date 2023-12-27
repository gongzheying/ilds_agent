FROM openjdk:8-jdk-slim-stretch

ADD  "./jboss-eap-7.3-mq.tar.gz"  "/opt/ilds"
COPY "./standalone-hornetq-cluster.xml" "/opt/ilds/jboss-eap-7.3-mq/standalone/configuration"

WORKDIR "/opt/ilds/jboss-eap-7.3-mq"
CMD ["./bin/standalone.sh", "-b=172.18.0.5", "-Djboss.bind.address.management=172.18.0.5", "-Djboss.bind.address.unsecure=172.18.0.5", "-Djboss.server.default.config=standalone-hornetq-cluster.xml"]

