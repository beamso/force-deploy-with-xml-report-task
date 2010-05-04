package com.claimvantage.force.ant;

import java.io.File;

import org.apache.tools.ant.BuildException;

import com.salesforce.ant.DeployTask;
import com.sforce.soap.metadata.AsyncResult;
import com.sforce.soap.metadata.CodeCoverageWarning;
import com.sforce.soap.metadata.DeployMessage;
import com.sforce.soap.metadata.DeployResult;
import com.sforce.soap.metadata.MetadataConnection;
import com.sforce.soap.metadata.RunTestFailure;
import com.sforce.soap.metadata.RunTestsResult;
import com.sforce.ws.ConnectionException;

/**
 * Extension of the salesforce Ant task that converts the structured test result
 * data returned from a deploy into junitreport format XML so that it can be
 * consumed by tools such as Hudson's JUnit report publisher.
 */
public class DeployWithXmlReportTask extends DeployTask {
    
    private File junitreportdir;

    public File getJunitreportdir() {
        return junitreportdir;
    }

    public void setJunitreportdir(File junitreportdir) {
        this.junitreportdir = junitreportdir;
    }

    /**
     * Necessary information already part of the response so grab it and format it.
     */
    public void handleResponse(MetadataConnection metadataConnection, AsyncResult response)
            throws ConnectionException {
        
        if (junitreportdir != null) {
            DeployResult result = metadataConnection.checkDeployStatus(response.getId());
            RunTestsResult rtr = result.getRunTestResult();
            new XmlReport(junitreportdir).report(rtr);
        }
        
        try {
            // This sometimes throws an ArrayIndexOutOfBoundsException.
            // So when there is one fall back to a hopefully fixed version of the code.
            // If the method is ever fixed this workaround will just never get called.
            super.handleResponse(metadataConnection, response);
        } catch (ArrayIndexOutOfBoundsException e) {
            fixedDecompiledHandleResponse(metadataConnection, response);
        }
    }
    
    private void fixedDecompiledHandleResponse(MetadataConnection metadataConnection, AsyncResult response)
            throws ConnectionException {
        
        // Some code related to debug logging removed from here...
        
        DeployResult result = metadataConnection.checkDeployStatus(response.getId());
        if (!result.isSuccess()) {
            
            StringBuilder buf = new StringBuilder("Failures:\n");
            
            DeployMessage messages[] = result.getMessages();
            for (DeployMessage message : messages) {
                if (message.isSuccess()) {
                    continue;
                }
                String loc = message.getLineNumber() != 0
                        ? "(" + message.getLineNumber() + "," + message.getColumnNumber()+ ")"
                        : "";
                if (loc.length() == 0 && !message.getFileName().equals(message.getFullName())) {
                    loc = "(" + message.getFullName() + ")";
                }
                buf.append(message.getFileName() + ":" + message.getProblem().toString() + "\n");
            }

            RunTestsResult rtr = result.getRunTestResult();
            if (rtr.getFailures() != null) {
                RunTestFailure[] failures = rtr.getFailures();
                for (RunTestFailure failure : failures) {
                    String n = (failure.getNamespace() != null ? failure.getNamespace() + "." : "") + failure.getName();
                    buf.append("Test failure, method: "
                            + n + "."
                            + failure.getMethodName()
                            + " -- "
                            + failure.getMessage()
                            + " stack "
                            + failure.getStackTrace()
                            + "\n\n");
                }

            }
            if (rtr.getCodeCoverageWarnings() != null) {
                CodeCoverageWarning[] ccws = rtr.getCodeCoverageWarnings();
                for (CodeCoverageWarning ccw : ccws) {
                    buf.append("Code coverage issue");
                    if (ccw.getName() != null) {
                        String n = (ccw.getNamespace() != null ? ccw.getNamespace() + "." : "") + ccw.getName();
                        buf.append(", class: " + n);
                    }
                    buf.append(" -- " + ccw.getMessage() + "\n");
                }
            }
            throw new BuildException(buf.toString());
        } else {
            return;
        }
    }
}
