package org.kiwiproject.test.util;

import static com.google.common.base.Preconditions.checkState;
import static org.kiwiproject.collect.KiwiLists.second;

import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * This utility class helps find service or emulator names from Maven POM files. It is useful only if you need to
 * get the service names, for example, if resources such as databases follow a naming convention that includes the
 * service name. As a concrete example, {@code order-service_development} might be the order service database in
 * the development environment when the naming convention is {@code <service-name>_<environment>}.
 * <p>
 * <strong>NOTE: This class is very specific to how we've typically organized our (Dropwizard) services
 * and makes the following assumptions:</strong>
 * <ul>
 *     <li>
 *         If there is a subdirectory named service, look for pom.xml there first (i.e., service/pom.xml). Otherwise,
 *         use the pom.xml at the top/root level directory as the Maven POM.
 *     </li>
 *     <li>
 *         The service/emulator name is expected to be in the <em>second</em> {@code artifactId} (the first is
 *         assumed to contain the parent POM's {@code artifactId})
 *     </li>
 *     <li>
 *         Service artifact IDs end with "-service" (e.g., order-service)
 *     </li>
 *     <li>
 *         Emulator artifact IDs end with "-emulator" (e.g., third-party-payments-emulator)
 *     </li>
 * </ul>
 * <em>If the above assumptions are not met, this class won't work as expected or will give really weird results.</em>
 */
@UtilityClass
@Slf4j
public class ServiceNames {

    private static final String ARTIFACT_ID_START_TAG = "<artifactId>";
    private static final String ARTIFACT_ID_END_TAG = "</artifactId>";
    private static final int SERVICE_NAME_START_INDEX = ARTIFACT_ID_START_TAG.length();

    /**
     * Find service/emulator name from the given project root path.
     *
     * @param rootPath the project root directory
     * @return the service/emulator name
     * @throws UncheckedIOException  if some I/O error occurred reading the POM
     * @throws IllegalStateException if the project structure is not as expected
     */
    public static String findServiceOrEmulatorNameFromRoot(String rootPath) {
        try {
            var pomPath = selectPomPathFromRoot(rootPath);
            var serviceName = findServiceOrEmulatorNameInPom(pomPath);
            checkState(isServiceOrEmulator(serviceName),
                    "%s in pom.xml does not seem to be a service or emulator. Expecting the second %s element to" +
                            " contain the service/emulator name. The first should contain the parent POM information",
                    serviceName, ARTIFACT_ID_START_TAG);

            return serviceName;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * Find the path of the POM from the given project root path.
     *
     * @param rootPath the project root directory
     * @return the {@link Path} of the POM to use for finding the service/emulator name
     * @throws IllegalStateException if the project structure is not as expected or the POM doesn't exist
     */
    public static Path selectPomPathFromRoot(String rootPath) {
        var servicePomPath = Path.of(rootPath, "service/pom.xml");
        if (servicePomPath.toFile().exists()) {
            LOG.trace("Using POM from service/pom.xml");
            return servicePomPath;
        }

        LOG.trace("Using root pom.xml");
        var rootPomPath = Path.of(rootPath, "pom.xml");
        checkState(rootPomPath.toFile().exists(),
                "Root pom.xml does not exist! Path: %s", rootPomPath.toAbsolutePath());

        return rootPomPath;
    }

    /**
     * Finds the service/emulator name in the given Maven POM file.
     *
     * @param pomPath the path to the Maven pom.xml file
     * @return the service/emulator name
     * @throws IOException           if an error occurs reading the POM
     * @throws IllegalStateException if no parent element is found in the POM
     */
    public static String findServiceOrEmulatorNameInPom(Path pomPath) throws IOException {
        try (var lineStream = Files.lines(pomPath)) {
            var lines = lineStream.toList();
            checkParentTagExists(lines);
            var artifactIds = findFirstTwoArtifactIdTags(lines);
            LOG.trace("First 2 lines with {} element: {}", ARTIFACT_ID_START_TAG, artifactIds);

            var artifactId = second(artifactIds);
            var serviceName = artifactId.substring(SERVICE_NAME_START_INDEX, artifactId.indexOf(ARTIFACT_ID_END_TAG));
            LOG.trace("Found {} as the service name from the pom file", serviceName);

            return serviceName;
        }
    }

    private static void checkParentTagExists(List<String> lines) {
        var parentTagFound = lines.stream()
                .map(String::trim)
                .anyMatch(line -> line.startsWith("<parent>"));

        if (!parentTagFound) {
            throw new IllegalStateException("Did not find <parent> tag in POM!");
        }
    }

    private static List<String> findFirstTwoArtifactIdTags(List<String> lines) {
        return lines.stream()
                .map(String::trim)
                .filter(line -> line.startsWith(ARTIFACT_ID_START_TAG))
                .limit(2)
                .toList();
    }

    /**
     * Check if the given service name is a service or emulator according to the assumptions noted in the class docs.
     *
     * @param serviceName the potential service/emulator name
     * @return true if the given name ends with the service or emulator suffix defined for this class
     */
    public static boolean isServiceOrEmulator(String serviceName) {
        return serviceName.endsWith("-service") || serviceName.endsWith("-emulator");
    }

    /**
     * Asserts that the given service name is named according to the assumptions noted in the class docs.
     *
     * @param serviceName the potential service/emulator name
     * @throws IllegalStateException if the given name violates the suffix assumptions defined for this class
     */
    public static void assertIsServiceOrEmulator(String serviceName) {
        checkState(isServiceOrEmulator(serviceName), "%s does not end with -service or -emulator", serviceName);
    }
}
