JAVAC="/usr/lib/jvm/java-8-openjdk-amd64/bin/javac"
JAVA="/usr/lib/jvm/java-8-openjdk-amd64/bin/java"

clean:
	rm -rf *.class out
	rm -rf test-build outputs

build: clean
	cp -rf test test-build
	cp src/*.java test-build
	mkdir outputs
	cd test-build && $(JAVAC) *.java

start: build
	cd test-build && $(JAVA) Ex1

test: build
	cd test-build && $(JAVA) Ex1 < input.txt > my_output.txt

test-1: build
	cd test-build && $(JAVA) Ex1 < ../inputs/alarm_net_input.txt

test-2: build
	cd test-build && $(JAVA) Ex1 < ../inputs/big_net_input.txt

test-3: build
	cd test-build && $(JAVA) Ex1 < ../inputs/alarm_net_2_input.txt

test-4: build
	cd test-build && $(JAVA) Ex1 < ../inputs/big_net_2_input.txt

publish:
	rm -rf ../207964859.zip
	zip ../207964859.zip src/*.java