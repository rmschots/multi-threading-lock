package com.rmschots.mavenplugins.multithreadinglockbefore;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.TimeUnit;

@Mojo(name = "lock", threadSafe = true)
public class MultiThreadingLockBeforeMojo extends AbstractMojo {
    /**
     * Directory to store lock files.
     * Defaults to a 'maven-locks' directory in the system temp directory.
     */
    @Parameter(property = "lockDir", defaultValue = "${java.io.tmpdir}/maven-locks")
    private String lockDir;

    /**
     * The name of the lock to acquire.
     */
    @Parameter(property = "lockName", required = true)
    private String lockName;

    /**
     * Timeout in milliseconds to wait for the lock to be acquired.
     * Default is 5 minutes (300,000 ms).
     */
    @Parameter(property = "timeout", defaultValue = "300000")
    private long timeout;

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
    public void execute() throws MojoExecutionException {
        String moduleInfo = project.getGroupId() + ":" + project.getArtifactId() + ":" + project.getVersion();
        logPrefix = "Module [" + moduleInfo + "] - ";

        // Initialize the lock directory
        initLockDirectory();

        logInfo("Attempting to acquire lock: " + lockName);

        // Get the session start time
        long sessionStartTime = session.getStartTime().getTime();
        logDebug("Session start time: " + sessionStartTime);

        try {
            boolean acquired = acquireLock(lockName, timeout, moduleInfo, sessionStartTime);
            if (!acquired) {
                throw new MojoExecutionException("%sFailed to acquire lock '%s' after waiting for %d milliseconds".formatted(logPrefix, lockName, timeout));
            }
            logInfo("Successfully acquired lock: " + lockName);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new MojoExecutionException("%sInterrupted while waiting for lock: %s".formatted(logPrefix, lockName), e);
        }
    }

    /**
     * Acquires a lock with the given name by creating a lock file if it doesn't exist.
     *
     * @param lockName         The name of the lock to acquire
     * @param timeoutMillis    The maximum time to wait for the lock
     * @param moduleInfo       Information about the Maven module being built
     * @param sessionStartTime The start time of the Maven session
     * @return true if the lock was acquired, false otherwise
     * @throws InterruptedException if the thread is interrupted while waiting
     */
    private boolean acquireLock(String lockName, long timeoutMillis, String moduleInfo, long sessionStartTime) throws InterruptedException {
        logDebug("Attempting to acquire lock: " + lockName + ", timeout: " + timeoutMillis + "ms, session start time: " + sessionStartTime);

        // Get the lock file
        File lockFile = getLockFile(lockName, sessionStartTime);
        logDebug("Using lock file: " + lockFile.getAbsolutePath());

        // Calculate how long to wait
        long startTime = System.currentTimeMillis();
        long endTime = startTime + timeoutMillis;

        // Keep trying to acquire the lock until timeout
        while (System.currentTimeMillis() < endTime) {
            // Check if the lock file exists
            if (lockFile.exists()) {
                logDebug("Lock file exists, waiting for it to be deleted: " + lockFile.getAbsolutePath());
                TimeUnit.MILLISECONDS.sleep(100);
                continue;
            }

            // Try to create the lock file
            try {
                boolean created = lockFile.createNewFile();
                if (created) {
                    // Successfully created the lock file
                    logDebug("Lock acquisition successful for: " + lockName);

                    // Write the current time and process ID to the file for debugging
                    try (RandomAccessFile raf = new RandomAccessFile(lockFile, "rw")) {
                        String lockInfoText = "Locked by process " + ProcessHandle.current().pid() +
                                              " at " + System.currentTimeMillis() +
                                              " by module " + moduleInfo;
                        raf.write(lockInfoText.getBytes());
                    } catch (IOException e) {
                        logWarn("Error writing to lock file: " + e.getMessage());
                        // Continue anyway, the lock is still acquired
                    }

                    return true;
                } else {
                    // Another process created the file between our check and create
                    logDebug("Another process acquired the lock between our check and create: " + lockName);
                    // Continue waiting instead of returning false
                    TimeUnit.MILLISECONDS.sleep(100);
                }
            } catch (IOException e) {
                logError("Error creating lock file: " + e.getMessage());
                // Continue waiting instead of returning false
                TimeUnit.MILLISECONDS.sleep(100);
            }
        }

        // If we get here, we've timed out
        logDebug("Failed to acquire lock after " + timeoutMillis + "ms: " + lockName);
        return false;
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
