JFLAGS = -g
JC = javac
.SUFFIXES: .java .class
.java.class:
	$(JC) $(JFLAGS) $*.java

CLASSES = \
		server/HostAddress.java \
		server/ServerListenerWorker.java \
		server/Authenticator.java \
		server/ServerWorker.java \
		server/Server.java \
		server/ServerTester.java \
		client/Client.java \
		client/ClientTester.java \
		client/ClientWorker.java \
		client/ClientHeartbeat.java 
default: classes

classes: $(CLASSES:.java=.class)

clean:
	$(RM) server/*.class
	$(RM) client/*.class

