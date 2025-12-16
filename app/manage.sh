#!/bin/bash

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Functions
print_success() {
    echo -e "${GREEN}✓ $1${NC}"
}

print_error() {
    echo -e "${RED}✗ $1${NC}"
}

print_info() {
    echo -e "${BLUE}ℹ $1${NC}"
}

print_warning() {
    echo -e "${YELLOW}⚠ $1${NC}"
}

# Check if docker-compose is available
check_docker() {
    if ! command -v docker &> /dev/null; then
        print_error "Docker is not installed"
        exit 1
    fi

    if ! command -v docker-compose &> /dev/null; then
        print_error "Docker Compose is not installed"
        exit 1
    fi

    print_success "Docker and Docker Compose are available"
}

# Start all services
start_all() {
    print_info "Starting all services..."

    print_info "1. Starting ELK Stack..."
    docker-compose -f docker-compose.elk.yml up -d

    print_info "Waiting for ELK to be healthy (30s)..."
    sleep 30

    print_info "2. Starting Application Stack..."
    docker-compose up -d

    print_success "All services started!"
    show_urls
}

# Start only ELK
start_elk() {
    print_info "Starting ELK Stack..."
    docker-compose -f docker-compose.elk.yml up -d

    print_info "Waiting for services to be ready..."
    sleep 20

    print_success "ELK Stack started!"
    print_info "Kibana: http://localhost:5602"
    print_info "Elasticsearch: http://localhost:9222"
}

# Start only app
start_app() {
    print_info "Starting Application Stack..."
    docker-compose up -d

    print_success "Application Stack started!"
    print_info "Application: http://localhost:8888"
    print_info "PgAdmin: http://localhost:5778"
}

# Stop all services
stop_all() {
    print_info "Stopping all services..."

    docker-compose down
    docker-compose -f docker-compose.elk.yml down

    print_success "All services stopped!"
}

# Stop only ELK
stop_elk() {
    print_info "Stopping ELK Stack..."
    docker-compose -f docker-compose.elk.yml down
    print_success "ELK Stack stopped!"
}

# Stop only app
stop_app() {
    print_info "Stopping Application Stack..."
    docker-compose down
    print_success "Application Stack stopped!"
}

# Restart all
restart_all() {
    print_info "Restarting all services..."
    stop_all
    sleep 2
    start_all
}

# Show logs
show_logs() {
    case "$1" in
        elk)
            docker-compose -f docker-compose.elk.yml logs -f
            ;;
        app)
            docker-compose logs -f
            ;;
        elasticsearch|es)
            docker logs -f cms_elasticsearch
            ;;
        logstash|ls)
            docker logs -f cms_logstash
            ;;
        kibana|kb)
            docker logs -f cms_kibana
            ;;
        spring|cms)
            docker logs -f cms_app
            ;;
        sql)
            tail -f logs/sql.log
            ;;
        error)
            tail -f logs/error.log
            ;;
        *)
            print_info "Showing all logs..."
            docker-compose -f docker-compose.elk.yml logs -f &
            docker-compose logs -f
            ;;
    esac
}

# Show status
show_status() {
    print_info "Service Status:"
    echo ""

    # ELK Services
    echo -e "${BLUE}ELK Stack:${NC}"
    docker ps --format "table {{.Names}}\t{{.Status}}\t{{.Ports}}" | grep "cms_elasticsearch\|cms_logstash\|cms_kibana\|cms_filebeat"
    echo ""

    # App Services
    echo -e "${BLUE}Application Stack:${NC}"
    docker ps --format "table {{.Names}}\t{{.Status}}\t{{.Ports}}" | grep "cms_app\|postgres_db\|redis_cache\|pgadmin2"
    echo ""
}

# Show URLs
show_urls() {
    echo ""
    print_info "═══════════════════════════════════════════════════════"
    print_info "                    Access URLs                        "
    print_info "═══════════════════════════════════════════════════════"
    echo ""

    echo -e "${GREEN}Application:${NC}"
    echo "  • API:            http://localhost:8888"
    echo "  • Health Check:   http://localhost:8888/api/test/actuator/health"
    echo "  • Test Items:     http://localhost:8888/api/test/items"
    echo ""

    echo -e "${GREEN}Monitoring (ELK):${NC}"
    echo "  • Kibana:         http://localhost:5602"
    echo "  • Elasticsearch:  http://localhost:9222"
    echo "  • Logstash:       http://localhost:9611/_node/stats"
    echo ""

    echo -e "${GREEN}Database:${NC}"
    echo "  • PgAdmin:        http://localhost:5778"
    echo "    - Email:        admin@admin.com"
    echo "    - Password:     admin123"
    echo "  • PostgreSQL:     localhost:5777"
    echo "  • Redis:          localhost:6666"
    echo ""

    echo -e "${GREEN}Log Files:${NC}"
    echo "  • Application:    ./logs/application.log"
    echo "  • SQL Queries:    ./logs/sql.log"
    echo "  • Errors:         ./logs/error.log"
    echo ""

    print_info "═══════════════════════════════════════════════════════"
}

# Health check
health_check() {
    print_info "Checking service health..."
    echo ""

    # Elasticsearch
    if curl -sf http://localhost:9222/_cluster/health > /dev/null 2>&1; then
        print_success "Elasticsearch is healthy (port 9222)"
    else
        print_error "Elasticsearch is not responding"
    fi

    # Kibana
    if curl -sf http://localhost:5602/api/status > /dev/null 2>&1; then
        print_success "Kibana is healthy (port 5602)"
    else
        print_error "Kibana is not responding"
    fi

    # Logstash
    if curl -sf http://localhost:9611/_node/stats > /dev/null 2>&1; then
        print_success "Logstash is healthy (port 9611)"
    else
        print_error "Logstash is not responding"
    fi

    # Application
    if curl -sf http://localhost:8888/api/test/actuator/health > /dev/null 2>&1; then
        print_success "Application is healthy (port 8888)"
    else
        print_error "Application is not responding"
    fi

    # PostgreSQL
    if docker exec postgres_db pg_isready -U postgres > /dev/null 2>&1; then
        print_success "PostgreSQL is healthy (port 5777)"
    else
        print_error "PostgreSQL is not responding"
    fi

    # Redis
    if docker exec redis_cache redis-cli ping > /dev/null 2>&1; then
        print_success "Redis is healthy (port 6666)"
    else
        print_error "Redis is not responding"
    fi
}

# Clean volumes
clean_volumes() {
    print_warning "This will remove all data (database, logs, etc.)"
    read -p "Are you sure? (yes/no): " -r
    echo
    if [[ $REPLY =~ ^[Yy][Ee][Ss]$ ]]; then
        print_info "Stopping services..."
        stop_all

        print_info "Removing volumes..."
        docker-compose down -v
        docker-compose -f docker-compose.elk.yml down -v

        print_info "Cleaning logs directory..."
        rm -rf logs/*

        print_success "All data cleaned!"
    else
        print_info "Cancelled"
    fi
}

# Test SQL logging
test_sql() {
    print_info "Testing SQL logging..."
    echo ""

    print_info "Creating a test item to generate SQL logs..."
    curl -X POST http://localhost:8888/api/test/items \
      -H "Content-Type: application/json" \
      -d '{"name":"SQL Test Item","description":"Testing SQL logging"}' \
      2>/dev/null | jq '.'

    echo ""
    print_info "Fetching all items to generate SELECT query..."
    curl http://localhost:8888/api/test/items 2>/dev/null | jq '.'

    echo ""
    print_success "SQL queries logged! Check:"
    echo "  • Console output above"
    echo "  • File: ./logs/sql.log"
    echo ""
    print_info "View SQL logs with: ./manage.sh logs sql"
}

# Setup Kibana index patterns
setup_kibana() {
    print_info "Setting up Kibana Index Patterns..."
    echo ""

    # Wait for Kibana to be ready
    print_info "Waiting for Kibana to be ready..."
    until curl -sf http://localhost:5602/api/status > /dev/null 2>&1; do
        echo -n "."
        sleep 2
    done
    echo ""
    print_success "Kibana is ready!"
    echo ""

    # Wait a bit more for Kibana to fully initialize
    sleep 5

    # Create index pattern for application logs
    print_info "Creating index pattern: cms-logs-*"
    curl -X POST "http://localhost:5602/api/saved_objects/index-pattern/cms-logs" \
      -H "kbn-xsrf: true" \
      -H "Content-Type: application/json" \
      -d '{
        "attributes": {
          "title": "cms-logs-*",
          "timeFieldName": "@timestamp"
        }
      }' > /dev/null 2>&1

    # Create index pattern for SQL logs
    print_info "Creating index pattern: cms-sql-*"
    curl -X POST "http://localhost:5602/api/saved_objects/index-pattern/cms-sql" \
      -H "kbn-xsrf: true" \
      -H "Content-Type: application/json" \
      -d '{
        "attributes": {
          "title": "cms-sql-*",
          "timeFieldName": "@timestamp"
        }
      }' > /dev/null 2>&1

    # Create index pattern for error logs
    print_info "Creating index pattern: cms-error-*"
    curl -X POST "http://localhost:5602/api/saved_objects/index-pattern/cms-error" \
      -H "kbn-xsrf: true" \
      -H "Content-Type: application/json" \
      -d '{
        "attributes": {
          "title": "cms-error-*",
          "timeFieldName": "@timestamp"
        }
      }' > /dev/null 2>&1

    # Set default index pattern
    print_info "Setting default index pattern..."
    curl -X POST "http://localhost:5602/api/kibana/settings/defaultIndex" \
      -H "kbn-xsrf: true" \
      -H "Content-Type: application/json" \
      -d '{
        "value": "cms-logs"
      }' > /dev/null 2>&1

    echo ""
    print_success "Kibana Index Patterns created successfully!"
    echo ""
    echo -e "${GREEN}Available Index Patterns:${NC}"
    echo "  • cms-logs-*   → All application logs"
    echo "  • cms-sql-*    → SQL query logs"
    echo "  • cms-error-*  → Error logs only"
    echo ""
    print_info "Access Kibana: http://localhost:5602"
}

# Show help
show_help() {
    cat << EOF
${BLUE}CMS Application Service Manager${NC}

Usage: ./manage.sh [COMMAND]

${GREEN}Commands:${NC}
  start              Start all services (ELK + App)
  start-elk          Start only ELK stack
  start-app          Start only Application stack

  stop               Stop all services
  stop-elk           Stop only ELK stack
  stop-app           Stop only Application stack

  restart            Restart all services

  status             Show status of all services
  health             Check health of all services
  urls               Show access URLs

  logs [service]     Show logs (elk|app|es|ls|kb|cms|sql|error)
                     Examples:
                       ./manage.sh logs elk
                       ./manage.sh logs cms
                       ./manage.sh logs sql    # View SQL queries
                       ./manage.sh logs error  # View errors only
                       ./manage.sh logs        # All logs

  test-sql           Test SQL logging by creating sample data
  setup-kibana       Setup Kibana index patterns (run after first start)

  clean              Clean all volumes and data

  help               Show this help message

${GREEN}Examples:${NC}
  ./manage.sh start              # Start everything
  ./manage.sh setup-kibana       # Setup Kibana (after first start)
  ./manage.sh start-elk          # Start only ELK
  ./manage.sh logs cms           # Follow CMS app logs
  ./manage.sh logs sql           # Follow SQL query logs
  ./manage.sh health             # Check service health
  ./manage.sh test-sql           # Test SQL logging
  ./manage.sh clean              # Clean all data

${GREEN}Port Configuration:${NC}
  Application:      8888
  PostgreSQL:       5777
  Redis:            6666
  PgAdmin:          5778
  Elasticsearch:    9222
  Kibana:           5602
  Logstash:         5055, 9611, 5001

EOF
}

# Main script
main() {
    check_docker

    case "$1" in
        start)
            start_all
            ;;
        start-elk)
            start_elk
            ;;
        start-app)
            start_app
            ;;
        stop)
            stop_all
            ;;
        stop-elk)
            stop_elk
            ;;
        stop-app)
            stop_app
            ;;
        restart)
            restart_all
            ;;
        status)
            show_status
            ;;
        health)
            health_check
            ;;
        urls)
            show_urls
            ;;
        logs)
            show_logs "$2"
            ;;
        test-sql)
            test_sql
            ;;
        setup-kibana)
            setup_kibana
            ;;
        clean)
            clean_volumes
            ;;
        help|--help|-h)
            show_help
            ;;
        *)
            print_error "Unknown command: $1"
            echo ""
            show_help
            exit 1
            ;;
    esac
}

# Run main
main "$@"