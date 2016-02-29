# force-deploy-with-xml-report-task

## Extends com.salesforce.ant.DeployTask to produce junitreport XML output

### A copy of the repository at [https://code.google.com/p/force-deploy-with-xml-report-task/](https://code.google.com/p/force-deploy-with-xml-report-task/)

Extends the Force.com `com.salesforce.ant.DeployTask` to accept an optional `junitreportdir` argument that defines the folder that a JUnitReport XML file is output into. This file can be consumed directly by the Hudson continuous integration tool to produce trend graphs and test result details or by the JUnitReport Ant task.

So this extension makes the Force.com unit test results visible in a continuous integration environment.

*This code works with the Summer '13 `ant-salesforce.jar` but may not work with later versions as it relies on internal APIs.*

A work-around to an `ArrayIndexOutOfBoundsException` that `com.salesforce.ant.DeployTask` sometimes generates is also included.

Here is an example of using the task:

    <path id="ant.additions.classpath">
        <fileset dir="ant"/>
    </path>

    <target name="deployAndTestAndReport">
        <taskdef
            name="sfdeploy"
            classname="com.salesforce.ant.DeployWithXmlReportTask"
            classpathref="ant.additions.classpath"
            />
        <delete dir="test-report-xml" quiet="true"/>
        <sfdeploy
            username="${sf.username}"
            password="${sf.password}"
            serverurl="${sf.serverurl}"
            deployRoot="src"
            testLevel="RunLocalTests"
            junitreportdir="test-report-xml"
            >
            <!-- Run only tests with file names that match this pattern -->
            <batchtest>
                <fileset dir="src/classes">
                    <include name="*Test.cls"/>
                </fileset>
            </batchtest>
        </sfdeploy>
        </target>

Version 1.4 has this change:

* A method return value in `ant-salesforce.jar` has changed requiring recompilation. This version works with the Summer '13 version of `ant-salesforce.jar`.

Version 1.3 has this change:

* The nested batchtest element suports an optional namespace attribute. The supplied namespace is added as a dot separated prefix to the file names. This allows the tests to be run in the packaging org (that has a namespace defined).

Version 1.2 has this change:

* It is now possible to identify the Apex tests to run by adding a nested batchtest element that is modeled on the junit task's nested batchtest element. The example above has been updated to include this. If you are writing code that builds on a managed package this allows you to run only your tests and not the managed package's tests without having to explicitly name all your tests in the build file.

Version 1.1 has these changes:

* The code coverage numbers presented in "Standard Output" for each test now match the numbers presented in the Force.com web UI thanks to a fix contributed by Robert Scott.
* Marker text "below 75%" is added to the code coverage output for any class that has less than 75% code coverage to make such classes stand out a bit more.
* The stacktrace information (that includes line numbers) for errors is now being correctly formatted so that it is not lost.

### Compilation

The `ant-salesforce.jar` file has to be installed into your `~/.m2` directory (or equivalent).

    mvn install:install-file -Dfile=ant-salesforce.jar \
        -DgroupId=com.force.api -DartifactId=ant-salesforce \
        -Dversion=36.0.0 -Dpackaging=jar

### Running

The `ant-salesforce.jar` (as well as the `force-deploy-with-xml-report-task.jar`) have to be copied to the `/lib` of your ant install.
