FROM openjdk:8-jdk-slim-stretch

ADD  "./jboss-eap-7.3.tar.gz"  "/opt/ilds"

WORKDIR "/opt/ilds/jboss-eap-7.3"

COPY ["./*.properties", "./*.xml", "./standalone/configuration"]

CMD ["./bin/standalone.sh", "-b=172.18.0.5", "-Djboss.bind.address.management=172.18.0.5", "-Djboss.bind.address.unsecure=172.18.0.5", "-Djboss.server.default.config=standalone-hornetq-cluster.xml"]

