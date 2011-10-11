package com.claimvantage.force.ant;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.tools.ant.Project;
import org.apache.tools.ant.types.FileSet;
import org.apache.tools.ant.types.Resource;
import org.apache.tools.ant.types.ResourceCollection;
import org.apache.tools.ant.types.resources.Resources;

/**
 * Modeled on org.apache.tools.ant.taskdefs.optional.junit.BatchTest.
 */
public class BatchTest {
    
    private String namespace;
    private Project project;
    private Resources resources = new Resources();

    public BatchTest(Project project) {
        this.project = project;
    }

    public String getNamespace() {
        return namespace;
    }

    public void setNamespace(String namespace) {
        this.namespace = namespace;
    }

    /**
     * Add a new FileSet instance to this BatchTest.
     * Whatever the FileSet is, only filename that are <tt>.cls</tt> will be considered as 'candidates'.
     */
    public void addFileSet(FileSet fs) {
        add(fs);
        if (fs.getProject() == null) {
            fs.setProject(project);
        }
    }

    /**
     * Add a new ResourceCollection instance to this BatchTest.
     * Whatever the collection is, only names that are <tt>.cls</tt> will be considered as 'candidates'.
     */
    public void add(ResourceCollection rc) {
        resources.add(rc);
    }
    
    @SuppressWarnings("unchecked")
    public List<String> getFilenames() {
        
        final String extension = ".cls";
        
        String prefix = namespace != null && namespace.trim().length() > 0 ? namespace.trim() + "." : "";
        List<String> names = new ArrayList<String>();
        for (Iterator<Resource> iter = resources.iterator(); iter.hasNext(); ) {
            Resource r = iter.next();
            if (r.isExists()) {
                String pathname = r.getName();
                if (pathname.endsWith(extension)) {
                    names.add(prefix + pathname.substring(0, pathname.length() - extension.length()));
                }
            }
        }
        return names;
    }
}
