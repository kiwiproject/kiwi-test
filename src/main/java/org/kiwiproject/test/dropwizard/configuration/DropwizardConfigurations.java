package org.kiwiproject.test.dropwizard.configuration;

import static org.kiwiproject.base.KiwiStrings.f;
import static org.kiwiproject.test.constants.KiwiTestConstants.OBJECT_MAPPER;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.dropwizard.configuration.FileConfigurationSourceProvider;
import io.dropwizard.configuration.YamlConfigurationFactory;
import io.dropwizard.core.Configuration;
import jakarta.validation.Validator;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.kiwiproject.test.constants.KiwiTestConstants;
import org.kiwiproject.test.validation.ValidationTestHelper;

import java.io.File;
import java.nio.file.Path;

/**
 * A series of static factory methods designed to create a configuration object from Dropwizard.
 * <p>
 * The default {@link ObjectMapper} when not specified is {@link KiwiTestConstants#OBJECT_MAPPER}.
 * The default {@link Validator} when not specified is the one returned by {@link ValidationTestHelper#getValidator()}.
 * The default property prefix is {@code "dw."}.
 *
 * @implNote There is a {@link io.dropwizard.configuration.JsonConfigurationFactory} in Dropwizard that can load
 * JSON-based configuration files. This is not currently implemented here, but could be if we ever think it is
 * useful.
 */
@Slf4j
@UtilityClass
public class DropwizardConfigurations {

    private static final String DEFAULT_PREFIX = "dw.";
    private static final Path RESOURCES_DIR = Path.of("src", "test", "resources");

    /**
     * Creates a new Dropwizard configuration using the default {@link Validator}, {@link ObjectMapper}, and
     * property prefix.
     *
     * @param configClass           the class of the configuration to instantiate
     * @param configurationFilePath the path to the YAML file that contains the configuration
     * @param <C>                   the type of the configuration class
     * @return a new configuration
     */
    public static <C extends Configuration> C newConfiguration(Class<C> configClass,
                                                               Path configurationFilePath) {
        return newConfiguration(configClass, ValidationTestHelper.getValidator(), configurationFilePath);
    }

    /**
     * Creates a new Dropwizard configuration using the default {@link ObjectMapper} and property prefix.
     *
     * @param configClass           the class of the configuration to instantiate
     * @param validator             the validator to use when validating the configuration
     * @param configurationFilePath the path to the YAML file that contains the configuration
     * @param <C>                   the type of the configuration class
     * @return a new configuration
     */
    public static <C extends Configuration> C newConfiguration(Class<C> configClass,
                                                               Validator validator,
                                                               Path configurationFilePath) {
        return newConfiguration(configClass, validator, OBJECT_MAPPER, configurationFilePath);
    }

    /**
     * Creates a new Dropwizard configuration using the default property prefix.
     *
     * @param configClass           the class of the configuration to instantiate
     * @param validator             the validator to use when validating the configuration
     * @param objectMapper          the object mapper to use when deserializing the YAML configuration
     * @param configurationFilePath the path to the YAML file that contains the configuration
     * @param <C>                   the type of the configuration class
     * @return a new configuration
     */
    public static <C extends Configuration> C newConfiguration(Class<C> configClass,
                                                               Validator validator,
                                                               ObjectMapper objectMapper,
                                                               Path configurationFilePath) {
        return newConfiguration(configClass, validator, objectMapper, DEFAULT_PREFIX, configurationFilePath);
    }

    /**
     * Creates a new Dropwizard configuration.
     *
     * @param configClass           the class of the configuration to instantiate
     * @param validator             the validator to use when validating the configuration
     * @param objectMapper          the object mapper to use when deserializing the YAML configuration
     * @param propertyPrefix        the system property name prefix used by overrides
     * @param configurationFilePath the path to the YAML file that contains the configuration
     * @param <C>                   the type of the configuration class
     * @return a new configuration
     */
    public static <C extends Configuration> C newConfiguration(Class<C> configClass,
                                                               Validator validator,
                                                               ObjectMapper objectMapper,
                                                               String propertyPrefix,
                                                               Path configurationFilePath) {
        return newConfiguration(configClass, validator, objectMapper, propertyPrefix, configurationFilePath.toString());
    }

    /**
     * Creates a new Dropwizard configuration using the default {@link Validator}, {@link ObjectMapper}, and
     * property prefix.
     *
     * @param configClass           the class of the configuration to instantiate
     * @param configurationFilename the path to the YAML file that contains the configuration
     * @param <C>                   the type of the configuration class
     * @return a new configuration
     */
    public static <C extends Configuration> C newConfiguration(Class<C> configClass,
                                                               String configurationFilename) {
        return newConfiguration(configClass, ValidationTestHelper.getValidator(), configurationFilename);
    }

    /**
     * Creates a new Dropwizard configuration using the default {@link ObjectMapper} and property prefix.
     *
     * @param configClass           the class of the configuration to instantiate
     * @param validator             the validator to use when validating the configuration
     * @param configurationFilename the path to the YAML file that contains the configuration
     * @param <C>                   the type of the configuration class
     * @return a new configuration
     */
    public static <C extends Configuration> C newConfiguration(Class<C> configClass,
                                                               Validator validator,
                                                               String configurationFilename) {
        return newConfiguration(configClass, validator, OBJECT_MAPPER, configurationFilename);
    }

    /**
     * Creates a new Dropwizard configuration using the default property prefix.
     *
     * @param configClass           the class of the configuration to instantiate
     * @param validator             the validator to use when validating the configuration
     * @param objectMapper          the object mapper to use when deserializing the YAML configuration
     * @param configurationFilename the path to the YAML file that contains the configuration
     * @param <C>                   the type of the configuration class
     * @return a new configuration
     */
    public static <C extends Configuration> C newConfiguration(Class<C> configClass,
                                                               Validator validator,
                                                               ObjectMapper objectMapper,
                                                               String configurationFilename) {
        return newConfiguration(configClass, validator, objectMapper, DEFAULT_PREFIX, configurationFilename);
    }

    /**
     * Creates a new Dropwizard configuration.
     *
     * @param configClass           the class of the configuration to instantiate
     * @param validator             the validator to use when validating the configuration
     * @param objectMapper          the object mapper to use when deserializing the YAML configuration
     * @param propertyPrefix        the system property name prefix used by overrides
     * @param configurationFilename the path to the YAML file that contains the configuration
     * @param <C>                   the type of the configuration class
     * @return a new configuration
     */
    public static <C extends Configuration> C newConfiguration(Class<C> configClass,
                                                               Validator validator,
                                                               ObjectMapper objectMapper,
                                                               String propertyPrefix,
                                                               String configurationFilename) {

        var filename = adjustLocationIfNecessary(configurationFilename);
        var factory = new YamlConfigurationFactory<>(configClass, validator, objectMapper, propertyPrefix);

        try {
            return factory.build(new FileConfigurationSourceProvider(), filename);
        } catch (Exception e) {
            var message = f("Problem occurred while factory was trying to create config from {}", filename);
            throw new ConfigurationFactoryException(message, e);
        }
    }

    private static String adjustLocationIfNecessary(String filename) {
        if (new File(filename).exists()) {
            LOG.debug("Using configuration file {}", filename);
            return filename;
        }

        var resourcesDirPath = RESOURCES_DIR.resolve(filename);

        if (resourcesDirPath.toFile().exists()) {
            LOG.debug("Found {} in {}, using file found there", filename, RESOURCES_DIR);
            return resourcesDirPath.toString();
        }

        var message = f("Unable to locate {} in either the root path or in {}", filename, RESOURCES_DIR);
        throw new ConfigurationFactoryException(message);
    }

    static final class ConfigurationFactoryException extends RuntimeException {
        ConfigurationFactoryException(String message) {
            super(message);
        }

        ConfigurationFactoryException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
