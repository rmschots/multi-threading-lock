package com.rmschots.mavenplugins.multithreadinglockafter;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * A Maven plugin that releases a lock with the given name.
 */
@Mojo(name = "unlock", threadSafe = true)
public class MultiThreadingLockAfterMojo extends AbstractMojo {
    /**
     * Directory to store lock files.
     * Defaults to a 'maven-locks' directory in the system temp directory.
     */
    @Parameter(property = "lockDir", defaultValue = "${java.io.tmpdir}/maven-locks")
    private String lockDir;

    /**
     * The name of the lock to release.
     */
    @Parameter(property = "lockName", required = true)
    private String lockName;

    /**
     * The Maven project being built.
     */
    @Parameter(defaultValue = "${project}", readonly = true, required = true)
    private MavenProject project;

    /**
     * The Maven session.
     */
    @Parameter(defaultValue = "${session}", readonly = true, required = true)
    private MavenSession session;

    private String logPrefix;

    @Override
    public void execute() {
        String moduleInfo = project.getGroupId() + ":" + project.getArtifactId() + ":" + project.getVersion();
        logPrefix = "Module [" + moduleInfo + "] - ";

        // Initialize the lock directory
        initLockDirectory();

        logInfo("Releasing lock: " + lockName);

        // Get the session start time
        long sessionStartTime = session.getStartTime().getTime();
        logDebug("Session start time: " + sessionStartTime);

        releaseLock(lockName, sessionStartTime);
        logInfo("Successfully released lock: " + lockName);
    }

    /**
     * Releases a lock with the given name.
     *
     * @param lockName         The name of the lock to release
     * @param sessionStartTime The start time of the Maven session
     */
    private void releaseLock(String lockName, long sessionStartTime) {
        logDebug("Attempting to release lock: " + lockName + ", session start time: " + sessionStartTime);

        // Get the lock file with session start time
        File lockFile = getLockFile(lockName, sessionStartTime);

        // Check if the lock file exists
        if (lockFile.exists()) {
            logDebug("Lock file exists: " + lockFile.getAbsolutePath());

            try {
                Files.delete(lockFile.toPath());
                logDebug("Successfully deleted lock file using Files.delete: " + lockFile.getAbsolutePath());
            } catch (IOException e) {
                logWarn("Error deleting lock file: " + e.getMessage());

                // As a last resort, mark the file for deletion on JVM exit
                lockFile.deleteOnExit();
                logWarn("Marked lock file for deletion on JVM exit: " + lockFile.getAbsolutePath());
            }
        } else {
            logDebug("Lock file does not exist: " + lockFile.getAbsolutePath());
        }
    }

    /**
     * Gets the file for a lock with the given name and session start time.
     *
     * @param lockName         The name of the lock
     * @param sessionStartTime The start time of the Maven session
     * @return The file for the lock
     */
    private File getLockFile(String lockName, long sessionStartTime) {
        // Sanitize the lock name to make it a valid filename
        String sanitizedName = lockName.replaceAll("[^a-zA-Z0-9.-]", "_");
        return new File(lockDir, sanitizedName + "_" + sessionStartTime + ".lock");
    }

    // Initialize lock directory
    private void initLockDirectory() {
        try {
            Path lockDirPath = Paths.get(lockDir);
            if (!Files.exists(lockDirPath)) {
                Files.createDirectories(lockDirPath);
            }
            logDebug("Lock directory initialized: " + lockDir);
        } catch (IOException e) {
            logError("Error creating lock directory: " + e.getMessage());
        }
    }

    private void logDebug(String message) {
        getLog().debug(logPrefix + message);
    }

    private void logInfo(String message) {
        getLog().info(logPrefix + message);
    }

    private void logWarn(String message) {
        getLog().warn(logPrefix + message);
    }

    private void logError(String message) {
        getLog().error(logPrefix + message);
    }
}
