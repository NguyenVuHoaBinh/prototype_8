# Configuration Repository

This directory contains the configuration files for the LLM Workflow System. The Config Server uses these files to provide centralized configuration to all services.

## Directory Structure

```
config-repo/
├── application/           # Common configurations for all services
│   ├── default/           # Default environment configuration (applied to all environments)
│   ├── dev/               # Development environment configuration
│   ├── test/              # Test environment configuration
│   └── prod/              # Production environment configuration
│
├── [service-name]/        # Service-specific configurations
│   ├── default/
│   ├── dev/
│   ├── test/
│   └── prod/
│
└── README.md
```

## Naming Convention

Configuration files should be named according to the following pattern:

- `application.yml` - For common settings shared across all services in a given environment
- `[service-name].yml` - For service-specific settings in a given environment

## Configuration Precedence

The following precedence (from highest to lowest) is used when resolving configuration properties:

1. Service-specific configuration for the active profile (e.g., `service-name-dev.yml`)
2. Service-specific configuration for the default profile (e.g., `service-name.yml`)
3. Common configuration for the active profile (e.g., `application-dev.yml`)
4. Common configuration for the default profile (e.g., `application.yml`)

## Environment Variables

Sensitive information should be externalized using environment variables. Use the `${VARIABLE_NAME}` syntax in YAML files to reference environment variables.