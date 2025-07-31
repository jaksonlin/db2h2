#!/bin/bash

# DB2H2 Migration Tool Runner Script
# Usage: ./run.sh [config-file] [options]

set -e

# Default values
CONFIG_FILE=""
JAR_FILE="target/db2h2-migration-1.0.0.jar"
JAVA_OPTS="-Xmx2g -Xms512m"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Function to print colored output
print_info() {
    echo -e "${GREEN}[INFO]${NC} $1"
}

print_warn() {
    echo -e "${YELLOW}[WARN]${NC} $1"
}

print_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# Function to show usage
show_usage() {
    echo "Usage: $0 [config-file] [options]"
    echo ""
    echo "Options:"
    echo "  -h, --help          Show this help message"
    echo "  -j, --jar <file>    Specify JAR file path"
    echo "  -m, --memory <size> Set JVM memory (e.g., 2g, 512m)"
    echo "  --dry-run           Show what would be executed without running"
    echo ""
    echo "Examples:"
    echo "  $0 config.json"
    echo "  $0 config.json --memory 4g"
    echo "  $0 config.json --jar custom-db2h2.jar"
    echo ""
}

# Function to check prerequisites
check_prerequisites() {
    print_info "Checking prerequisites..."
    
    # Check if Java is installed
    if ! command -v java &> /dev/null; then
        print_error "Java is not installed or not in PATH"
        exit 1
    fi
    
    # Check Java version
    JAVA_VERSION=$(java -version 2>&1 | head -n 1 | cut -d'"' -f2 | cut -d'.' -f1)
    if [ "$JAVA_VERSION" -lt 11 ]; then
        print_error "Java 11 or higher is required. Found version: $JAVA_VERSION"
        exit 1
    fi
    
    print_info "Java version: $(java -version 2>&1 | head -n 1)"
    
    # Check if JAR file exists
    if [ ! -f "$JAR_FILE" ]; then
        print_warn "JAR file not found: $JAR_FILE"
        print_info "Building project..."
        if command -v mvn &> /dev/null; then
            mvn clean package
        else
            print_error "Maven is not installed. Please build the project manually."
            exit 1
        fi
    fi
    
    print_info "Prerequisites check completed"
}

# Function to validate config file
validate_config() {
    if [ -n "$CONFIG_FILE" ]; then
        if [ ! -f "$CONFIG_FILE" ]; then
            print_error "Configuration file not found: $CONFIG_FILE"
            exit 1
        fi
        
        # Basic JSON validation
        if command -v jq &> /dev/null; then
            if ! jq empty "$CONFIG_FILE" 2>/dev/null; then
                print_error "Invalid JSON in configuration file: $CONFIG_FILE"
                exit 1
            fi
        else
            print_warn "jq not found, skipping JSON validation"
        fi
        
        print_info "Configuration file validated: $CONFIG_FILE"
    fi
}

# Function to run migration
run_migration() {
    print_info "Starting DB2H2 migration..."
    
    # Build command
    CMD="java $JAVA_OPTS -jar $JAR_FILE"
    
    if [ -n "$CONFIG_FILE" ]; then
        CMD="$CMD --config $CONFIG_FILE"
    fi
    
    # Add additional arguments
    if [ $# -gt 0 ]; then
        CMD="$CMD $@"
    fi
    
    print_info "Executing: $CMD"
    
    if [ "$DRY_RUN" = true ]; then
        print_info "Dry run mode - command would be: $CMD"
        return 0
    fi
    
    # Execute migration
    if eval "$CMD"; then
        print_info "Migration completed successfully!"
        exit 0
    else
        print_error "Migration failed!"
        exit 1
    fi
}

# Parse command line arguments
DRY_RUN=false

while [[ $# -gt 0 ]]; do
    case $1 in
        -h|--help)
            show_usage
            exit 0
            ;;
        -j|--jar)
            JAR_FILE="$2"
            shift 2
            ;;
        -m|--memory)
            JAVA_OPTS="-Xmx$2 -Xms512m"
            shift 2
            ;;
        --dry-run)
            DRY_RUN=true
            shift
            ;;
        -*)
            print_error "Unknown option: $1"
            show_usage
            exit 1
            ;;
        *)
            if [ -z "$CONFIG_FILE" ]; then
                CONFIG_FILE="$1"
            else
                # Additional arguments to pass to the JAR
                ADDITIONAL_ARGS="$ADDITIONAL_ARGS $1"
            fi
            shift
            ;;
    esac
done

# Main execution
print_info "DB2H2 Migration Tool"
print_info "===================="

check_prerequisites
validate_config
run_migration $ADDITIONAL_ARGS 