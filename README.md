
ixa-pipe-time
=============
[![Build Status](https://travis-ci.org/ixa-ehu/ixa-pipe-time.svg?branch=master)](https://travis-ci.org/ixa-ehu/ixa-pipe-time)
[![GitHub license](https://img.shields.io/badge/license-Apache%202-blue.svg)](https://raw.githubusercontent.com/apache/opennlp/master/LICENSE)

ixa-pipe-time is a multilingual Temporal processing tagger developed within the IXA pipes tools  [http://ixa2.si.ehu.es/ixa-pipes].

## TABLE OF CONTENTS

1. [Overview of ixa-pipe-time](#overview)
  + [Distributed models](#time-models)
2. [Usage of ixa-pipe-time](#cli-usage)
  + [Tagging](#tagging)
  + [Server mode](#server)
3. [API via Maven Dependency](#api)
4. [Git installation](#installation)

## OVERVIEW

We provide competitive models based on robust local features and exploiting unlabeled data
via clustering features. The clustering features are based on Brown, Clark (2003)
and Word2Vec clustering. To avoid duplication of efforts, we use and contribute to the API provided by the
[Apache OpenNLP project](http://opennlp.apache.org) with our own custom developed features for each of the three tasks.

### TIME-Models

+ [English Temporal extraction models trained on TimeBank](http://ixa2.si.ehu.es/ixa-pipes/models/time-models.tar.gz)

## CLI-USAGE

ixa-pipe-time provides a runable jar with the following command-line basic functionalities:

1. **server**: starts a TCP service loading the model and required resources.
2. **client**: sends a NAF document to a running TCP server.
3. **tag**: reads a NAF document containing *wf* elements and tags and normalizes temporal expressions.

Each of these functionalities are accessible by adding (server|client|tag) as a
subcommand to ixa-pipe-time-${version}-exec.jar. Please read below and check the -help
parameter:

````shell
java -jar ixa-pipe-time-${version}-exec.jar (tag|server|client) -help
````

### Tagging

If you are in hurry, just execute:

````shell
cat file.txt | java -jar ixa-pipe-tok-$version-exec.jar tok -l en | java -jar ixa-pipe-time-${version}-exec.jar tag -m model.bin
````

If you want to know more, please follow reading.

ixa-pipe-time reads NAF documents (with *wf* elements) via standard input and outputs NAF
through standard output. The NAF format specification is here:

(http://wordpress.let.vupr.nl/naf/)

You can get the necessary input for ixa-pipe-time by piping
[ixa-pipe-tok](https://github.com/ixa-ehu/ixa-pipe-tok) as shown in the example:

There are several options to tag with ixa-pipe-time:

+ **model**: pass the model as a parameter.
+ **language**: pass the language as a parameter.
+ **outputFormat**: Output annotation in a format: timeml and NAF. It defaults to NAF.

**Example**:

````shell
cat file.txt | java -jar ixa-pipe-tok-$version-exec.jar tok -l en | java -jar ixa-pipe-time-${version}-exec.jar tag -m en-model-tempeval3.bin
````

### Server

We can start the TCP server as follows:

````shell
java -jar ixa-pipe-time-${version}-exec.jar server -l en --port 2060 -m en-model-tempeval3.bin
````
Once the server is running we can send NAF documents containing (at least) the WF layer like this:

````shell
 cat file.tok.naf | java -jar ixa-pipe-time-${version}-exec.jar client -p 2060
````

## API

The easiest way to use ixa-pipe-time programatically is via Apache Maven. Add
this dependency to your pom.xml:

````shell
<dependency>
    <groupId>eus.ixa</groupId>
    <artifactId>ixa-pipe-time</artifactId>
    <version>1.0.0</version>
</dependency>
````

## JAVADOC

The javadoc of the module is located here:

````shell
ixa-pipe-time/target/ixa-pipe-time-$version-javadoc.jar
````

## Module contents

The contents of the module are the following:

    + formatter.xml           Apache OpenNLP code formatter for Eclipse SDK
    + pom.xml                 maven pom file which deals with everything related to compilation and execution of the module
    + src/                    java source code of the module and required resources
    + Furthermore, the installation process, as described in the README.md, will generate another directory:
    target/                 it contains binary executable and other directories

## INSTALLATION

Installing the ixa-pipe-time requires the following steps:

If you already have installed in your machine the Java 1.8+ and MAVEN 3, please go to step 3
directly. Otherwise, follow these steps:

### 1. Install JDK 1.8

If you do not install JDK 1.8+ in a default location, you will probably need to configure the PATH in .bashrc or .bash_profile:

````shell
export JAVA_HOME=/yourpath/local/java8
export PATH=${JAVA_HOME}/bin:${PATH}
````

If you use tcsh you will need to specify it in your .login as follows:

````shell
setenv JAVA_HOME /usr/java/java18
setenv PATH ${JAVA_HOME}/bin:${PATH}
````

If you re-login into your shell and run the command

````shell
java -version
````

You should now see that your JDK is 1.8.

### 2. Install MAVEN 3

Download MAVEN 3.3.9+ from

````shell
https://maven.apache.org/download.cgi
````
Now you need to configure the PATH. For Bash Shell:

````shell
export MAVEN_HOME=/home/ragerri/local/apache-maven-3.3.9
export PATH=${MAVEN_HOME}/bin:${PATH}
````

For tcsh shell:

````shell
setenv MAVEN3_HOME ~/local/apache-maven-3.3.9
setenv PATH ${MAVEN3}/bin:{PATH}
````

If you re-login into your shell and run the command

````shell
mvn -version
````

You should see reference to the MAVEN version you have just installed plus the JDK that is using.

### 3. Get module source code

If you must get the module source code from here do this:

````shell
git clone https://github.com/ixa-ehu/ixa-pipe-time
````

### 4. Compile

Execute this command to compile ixa-pipe-time:

````shell
cd ixa-pipe-time
mvn clean package
````
This step will create a directory called target/ which contains various directories and files.
Most importantly, there you will find the module executable:

ixa-pipe-time-${version}-exec.jar

This executable contains every dependency the module needs, so it is completely portable as long
as you have a JVM 1.8 installed.

To install the module in the local maven repository, usually located in ~/.m2/, execute:

````shell
mvn clean install
````

## Contact information

````shell
Rodrigo Agerri
IXA NLP Group
University of the Basque Country (UPV/EHU)
E-20018 Donostia-San Sebastián
rodrigo.agerri@ehu.eus
````
