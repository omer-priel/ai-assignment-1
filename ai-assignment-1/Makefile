JAVAC="/usr/lib/jvm/java-1.8.0-openjdk-amd64/bin/javac"
JAVA="/usr/lib/jvm/java-1.8.0-openjdk-amd64/bin/java"

clean:
	rm -rf *.class

build:
	$(JAVAC) *.java

build-dev:
	$(JAVAC) *.java

start:
	$(JAVA) Ex1

test:
	$(JAVA) Ex1 < input.txt > my_output.txt

test-1: clean build
	rm -rf outputs
	mkdir outputs
	$(JAVA) Ex1 < inputs/alarm_net_input.txt > outputs/alarm_net_input.txt

all-start: clean build start

all-test: clean build test

publish:
	rm -rf ID_ID.zip
	zip ID_ID.zip *.java