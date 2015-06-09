package com.claimvantage.force.ant;

import com.sforce.soap.metadata.CodeCoverageResult;
import com.sforce.soap.metadata.CodeCoverageWarning;
import com.sforce.soap.metadata.RunTestFailure;
import com.sforce.soap.metadata.RunTestSuccess;
import com.sforce.soap.metadata.RunTestsResult;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.util.DOMElementWriter;
import org.apache.tools.ant.util.DateUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Text;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.Date;


/**
 * Generate JUnit XML output for an Apex test run.
 * Formatting from org.apache.tools.ant.taskdefs.optional.junit.XMLJUnitResultFormatter.
 */
public class XmlReport {

    // XML names
    private static final String TESTSUITE = "testsuite";
    private static final String TESTCASE = "testcase";
    private static final String FAILURE = "failure";
    private static final String SYSTEM_ERR = "system-err";
    private static final String SYSTEM_OUT = "system-out";
    private static final String ATTR_NAME = "name";
    private static final String ATTR_TIME = "time";
    private static final String ATTR_ERRORS = "errors";
    private static final String ATTR_FAILURES = "failures";
    private static final String ATTR_TESTS = "tests";
    private static final String ATTR_TYPE = "type";
    private static final String ATTR_MESSAGE = "message";
    private static final String PROPERTIES = "properties";
    private static final String ATTR_CLASSNAME = "classname";
    private static final String TIMESTAMP = "timestamp";
    
    // Other stuff
    private static final String SUITE_NAME = "Apex";
    private static final long MS_PER_SECOND = 1000;
    
    private File toDir;
    private Element rootElement;

    public XmlReport(File toDir) {
        this.toDir = toDir;
    }

    public void report(RunTestsResult results) {

        startTestSuiteXml();
        for (RunTestSuccess success : results.getSuccesses()) {
            reportTestXml(
                    success.getNamespace(),
                    success.getName(),
                    success.getMethodName(),
                    success.getTime(),
                    null,
                    null,
                    null
                    );
        }
        for (RunTestFailure failure : results.getFailures()) {
            reportTestXml(
                    failure.getNamespace(),
                    failure.getName(),
                    failure.getMethodName(),
                    failure.getTime(),
                    failure.getType(),
                    failure.getMessage(),
                    failure.getStackTrace()
                    );
        }
        reportCoverage(results);
        endTestSuiteXml(
                  results.getNumTestsRun() - results.getNumFailures(),
                  0,
                  results.getNumFailures(),
                  results.getTotalTime()
                  );
        write();
    }
    
    // Output one "test" that shows coverage results.
    // Might be a better output format (e.g. Clover?) but that would take more research.
    private void reportCoverage(RunTestsResult results) {
        final String name = "ApexCodeCoverageTest";
        final String method = "testCoverage";
        CodeCoverageWarning[] warnings = results.getCodeCoverageWarnings();
        if (warnings.length == 0) {
            reportTestXml(
                    null,
                    name,
                    method,
                    0.0d,
                    null,
                    null,
                    null
                    );
            reportSystemOut(coverageSummary(results));
        } else {
            String message = "See Standard Output for failure detail";
            StringBuilder detail = new StringBuilder(1024);
            for (CodeCoverageWarning warning : warnings) {
               if (warning.getName() == null) {
                   message = warning.getMessage();
               } else {
                   detail.append(warning.getName()).append(": ").append(warning.getMessage()).append("\n");
               }
            }
            reportTestXml(
                    null,
                    name,
                    method,
                    0.0d,
                    null,
                    message,
                    null
                    );
            reportSystemOut(detail.toString() + "\n" + coverageSummary(results));
        }
    }
    
    // These figures don't exactly agree with the web UI, don't know why
    private String coverageSummary(RunTestsResult results) {
         int allCovered = 0;
         int allTotal = 0;
         StringBuilder sb = new StringBuilder(4096);
         CodeCoverageResult[] coverages = results.getCodeCoverage();
         for (CodeCoverageResult coverage : coverages) {
              int total = coverage.getNumLocations();
              int notCovered = coverage.getNumLocationsNotCovered();
              int covered = total - notCovered; 
              allCovered += covered;
              allTotal += total;
              sb.append(coverageLine(coverage.getName(), covered, total));
         }
         return coverageLine("Total", allCovered, allTotal) + "\n" + sb.toString();
    }
    
    private String coverageLine(String name, int covered, int total) {
         // For some classes the total is zero so skip those to avoid divide by zero
         if (total > 0) {
              int percentage = (100 * covered) / total;
              return name + ": "
                      + percentage + "%"
                      + " (" + covered + "/" + total + ")"
                      + (percentage < 75 ? " below 75%" : "")
                      + "\n";
         } else {
              return "";
         }
    }
    
    private void startTestSuiteXml() {
        rootElement = createDocument().createElement(TESTSUITE);
        rootElement.setAttribute(ATTR_NAME, SUITE_NAME);
        rootElement.setAttribute(TIMESTAMP, DateUtils.format(new Date(), DateUtils.ISO8601_DATETIME_PATTERN));
        rootElement.appendChild(rootElement.getOwnerDocument().createElement(PROPERTIES));
    }

    private void reportTestXml(String namespace, String className, String methodName,
            double time, String type, String message, String stacktrace) {
        Element currentTest = rootElement.getOwnerDocument().createElement(TESTCASE);
        currentTest.setAttribute(ATTR_NAME, methodName);
        String qualifiedClassName = (namespace != null && namespace.length() > 0 ? namespace + "." : "") + className;
        currentTest.setAttribute(ATTR_CLASSNAME, qualifiedClassName);
        currentTest.setAttribute(ATTR_TIME, String.valueOf(time / MS_PER_SECOND));
        if (type != null || message != null || stacktrace != null) {
            Element nested = rootElement.getOwnerDocument().createElement(FAILURE);
            nested.setAttribute(ATTR_TYPE, type);
            nested.setAttribute(ATTR_MESSAGE, message);
            if (stacktrace != null) {
                Text trace = rootElement.getOwnerDocument().createTextNode(stacktrace);
                nested.appendChild(trace);
            }
            currentTest.appendChild(nested);
        }
        rootElement.appendChild(currentTest);
    }
    
    private void reportSystemOut(String text) {
        Element systemOut = rootElement.getOwnerDocument().createElement(SYSTEM_OUT);
        systemOut.appendChild(rootElement.getOwnerDocument().createCDATASection(text));
        rootElement.appendChild(systemOut);
    }
    
    private void endTestSuiteXml(int passes, int errors, int failures, double time) throws BuildException {
        rootElement.setAttribute(ATTR_TESTS, "" + (passes + errors + failures));
        rootElement.setAttribute(ATTR_FAILURES, "" + failures);
        rootElement.setAttribute(ATTR_ERRORS, "" + errors);
        rootElement.setAttribute(ATTR_TIME, String.valueOf(time / MS_PER_SECOND));
        
        rootElement.appendChild(rootElement.getOwnerDocument().createElement(SYSTEM_OUT));
        rootElement.appendChild(rootElement.getOwnerDocument().createElement(SYSTEM_ERR));
    }
        
    private void write() {
        try {
            Writer writer = new BufferedWriter(new OutputStreamWriter(createOutputStream(), "UTF8"));
            try {
                writer.write("<?xml version=\"1.0\" encoding=\"UTF-8\" ?>\n");
                (new DOMElementWriter()).write(rootElement, writer, 0, "  ");
            } finally {
                writer.flush();
                writer.close();
            }
        } catch (IOException exc) {
            throw new BuildException("Unable to write log file", exc);
        }
    }

    private OutputStream createOutputStream() {
        if (!toDir.exists()) {
            toDir.mkdirs();
        }
        File file = new File(toDir, "TEST-" + SUITE_NAME + ".xml");
        try {
            return new FileOutputStream(file);
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }
    }
    
    private Document createDocument() {
        try {
            DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
            return builder.newDocument();
        } catch (ParserConfigurationException e) {
            throw new RuntimeException(e);
        }
    }
}
